import games.rednblack.miniaudio.MAGroup;
import games.rednblack.miniaudio.MASound;
import games.rednblack.miniaudio.MiniAudio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * BGM controller backed by gdx-miniaudio.
 *
 * Music is streamed and intro/loop pairs are chained by MiniAudio's native
 * data-source chain, so the loop starts without a Java-side write thread or a
 * gap. Delayed transitions use one daemon scheduler; all actual audio mixing
 * and fading remains in MiniAudio.
 */
public class BGMPlayer implements AutoCloseable {
    private static final short MUSIC_FLAGS = (short) (
            MASound.Flags.MA_SOUND_FLAG_STREAM
                    | MASound.Flags.MA_SOUND_FLAG_NO_SPATIALIZATION
                    | MASound.Flags.MA_SOUND_FLAG_NO_PITCH);

    private static final class Handle {
        MASound primary;
        MASound intro;
        MASound loop;
        float volume;
        double introDuration;
        boolean started;
        boolean disposed;

        void setVolume(float value) {
            volume = Math.max(0f, value);
            if (intro != null) {
                intro.setVolume(volume);
            }
            if (loop != null) {
                loop.setVolume(volume);
            }
            if (primary != null && intro == null) {
                primary.setVolume(volume);
            }
        }

        void setPlaybackVolume(float value) {
            float safeValue = Math.max(0f, value);
            if (intro != null) {
                intro.setVolume(safeValue);
            }
            if (loop != null) {
                loop.setVolume(safeValue);
            }
            if (primary != null && intro == null) {
                primary.setVolume(safeValue);
            }
        }

        void start(double timestampSeconds) {
            if (disposed) {
                return;
            }

            if (intro != null && loop != null) {
                introDuration = Math.max(0.0, intro.getLength());
                if (timestampSeconds < introDuration) {
                    primary = intro;
                    intro.seekTo((float) Math.max(0.0, timestampSeconds));
                    intro.play();
                } else {
                    primary = loop;
                    loop.setLooping(true);
                    loop.seekTo((float) normalizeLoopTimestamp(
                            timestampSeconds - introDuration,
                            loop.getLength()));
                    loop.play();
                }
            } else if (primary != null) {
                primary.setLooping(true);
                primary.seekTo((float) normalizeLoopTimestamp(
                        timestampSeconds,
                        primary.getLength()));
                primary.play();
            }
            started = true;
        }

        void fadeIn(float milliseconds) {
            if (milliseconds <= 0f) {
                setPlaybackVolume(volume);
                return;
            }
            if (primary != null) {
                primary.fadeIn(milliseconds, volume);
            }
        }

        void fadeOut(float milliseconds) {
            if (milliseconds <= 0f) {
                setPlaybackVolume(0f);
                return;
            }
            if (intro != null) {
                intro.fadeOut(milliseconds, 0f);
            }
            if (loop != null) {
                loop.fadeOut(milliseconds, 0f);
            }
            if (primary != null && intro == null) {
                primary.fadeOut(milliseconds, 0f);
            }
        }

        boolean isPlaying() {
            return (primary != null && primary.isPlaying())
                    || (intro != null && intro.isPlaying())
                    || (loop != null && loop.isPlaying());
        }

        double timestamp() {
            if (primary == null) {
                return 0.0;
            }
            if (intro != null && primary == intro) {
                double introCursor = Math.max(0.0, intro.getCursorPosition());
                if (loop != null && loop.isPlaying()) {
                    return introDuration + Math.max(0.0, loop.getCursorPosition());
                }
                return Math.min(introDuration, introCursor);
            }
            double loopCursor = Math.max(0.0, primary.getCursorPosition());
            return intro != null ? introDuration + loopCursor : loopCursor;
        }

        void seek(double timestampSeconds) {
            if (disposed || primary == null) {
                return;
            }

            // The intro handle can remain primary after the native chain has
            // already advanced to the loop. Consider either voice active.
            boolean wasPlaying = isPlaying();
            double safeTimestamp = Math.max(0.0, timestampSeconds);

            if (intro != null && loop != null) {
                introDuration = Math.max(0.0, intro.getLength());
                if (safeTimestamp < introDuration) {
                    intro.stop();
                    loop.stop();
                    primary = intro;
                    intro.seekTo((float) safeTimestamp);
                    if (wasPlaying) {
                        intro.play();
                    }
                } else {
                    intro.stop();
                    loop.stop();
                    primary = loop;
                    loop.setLooping(true);
                    loop.seekTo((float) normalizeLoopTimestamp(
                            safeTimestamp - introDuration,
                            loop.getLength()));
                    if (wasPlaying) {
                        loop.play();
                    }
                }
                return;
            }

            primary.seekTo((float) normalizeLoopTimestamp(
                    safeTimestamp,
                    primary.getLength()));
        }

        void stop() {
            if (intro != null) {
                intro.stop();
            }
            if (loop != null) {
                loop.stop();
            }
            if (primary != null && intro == null) {
                primary.stop();
            }
        }

        void dispose() {
            if (disposed) {
                return;
            }
            disposed = true;
            stopQuietly(intro);
            stopQuietly(loop);
            if (primary != intro && primary != loop) {
                stopQuietly(primary);
            }
            disposeQuietly(intro);
            if (loop != intro) {
                disposeQuietly(loop);
            }
            if (primary != intro && primary != loop) {
                disposeQuietly(primary);
            }
            primary = null;
            intro = null;
            loop = null;
        }

        private static void stopQuietly(MASound sound) {
            if (sound != null) {
                try {
                    sound.stop();
                } catch (RuntimeException ignored) {
                    // Native shutdown is best effort.
                }
            }
        }

        private static void disposeQuietly(MASound sound) {
            if (sound != null) {
                try {
                    sound.dispose();
                } catch (RuntimeException ignored) {
                    // Continue releasing the remaining voices.
                }
            }
        }
    }

    private final MiniAudio miniAudio;
    private final MAGroup musicGroup;
    private final ScheduledExecutorService scheduler;
    private final Object lock = new Object();
    private final Set<Handle> liveHandles = new HashSet<>();
    private final List<ScheduledFuture<?>> scheduledTransitions = new ArrayList<>();
    private final boolean ownsEngine;
    private Handle current;
    private boolean disposed;

    /** Constructor retained for standalone callers. SoundManager injects its shared engine. */
    public BGMPlayer() {
        this(new MiniAudio(), null, true);
    }

    public BGMPlayer(MiniAudio miniAudio, MAGroup musicGroup) {
        this(miniAudio, musicGroup, false);
    }

    private BGMPlayer(MiniAudio miniAudio, MAGroup musicGroup, boolean ownsEngine) {
        this.miniAudio = miniAudio;
        this.musicGroup = musicGroup != null || miniAudio == null
                ? musicGroup
                : miniAudio.createGroup((short) (
                        MASound.Flags.MA_SOUND_FLAG_NO_SPATIALIZATION
                                | MASound.Flags.MA_SOUND_FLAG_NO_PITCH), null);
        this.ownsEngine = ownsEngine;
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "BGMDelay");
            thread.setDaemon(true);
            return thread;
        };
        this.scheduler = Executors.newSingleThreadScheduledExecutor(factory);
    }

    public void play(String path) {
        synchronized (lock) {
            replaceCurrentLocked();
            Handle next = createSingle(path);
            if (next == null) {
                return;
            }
            next.setVolume(1f);
            next.start(0.0);
            current = next;
            liveHandles.add(next);
        }
    }

    public void playIntroThenLoop(String introPath, String loopPath) {
        synchronized (lock) {
            replaceCurrentLocked();
            Handle next = createIntroLoop(introPath, loopPath);
            if (next == null) {
                return;
            }
            next.setVolume(1f);
            next.start(0.0);
            current = next;
            liveHandles.add(next);
        }
    }

    /** Loads each path once to validate it, then releases the temporary voice. */
    public void preload(String... paths) {
        if (paths == null || miniAudio == null) {
            return;
        }
        for (String path : paths) {
            Handle handle = createSingle(path);
            if (handle != null) {
                handle.dispose();
            }
        }
    }

    /** Retained for callers of the former Java Sound cache; MiniAudio owns its resource cache. */
    public static void clearAudioCache() {
        // MiniAudio's native resource manager owns and shares decoded resources.
    }

    public void seek(double timestamp) {
        validateTimestamp(timestamp, "timestamp");
        synchronized (lock) {
            if (current != null) {
                current.seek(timestamp);
            }
        }
    }

    public double getTimestamp() {
        synchronized (lock) {
            return current == null ? 0.0 : current.timestamp();
        }
    }

    public boolean isPlaying() {
        synchronized (lock) {
            return current != null && current.isPlaying();
        }
    }

    public void stop() {
        synchronized (lock) {
            cancelScheduledLocked();
            disposeAllLocked();
            current = null;
        }
    }

    public void setVolume(float volume) {
        synchronized (lock) {
            if (current != null) {
                current.setVolume(volume);
            }
        }
    }

    public float getVolume() {
        synchronized (lock) {
            return current == null ? 0f : current.volume;
        }
    }

    public void fadeOut(long durationMs) {
        fadeOut(durationMs, null);
    }

    public void fadeOut(long durationMs, Runnable onComplete) {
        long safeDuration = Math.max(0L, durationMs);
        synchronized (lock) {
            cancelScheduledLocked();
            Handle target = current;
            if (target == null) {
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }

            // A previous crossfade may have left an old voice in flight. It
            // must not survive a direct fade-out or engine shutdown.
            retainOnlyLocked(target);

            target.fadeOut((float) safeDuration);
            ScheduledFuture<?> completion = scheduler.schedule(() -> {
                synchronized (lock) {
                    if (current == target) {
                        current = null;
                    }
                    liveHandles.remove(target);
                }
                target.dispose();
                if (onComplete != null) {
                    onComplete.run();
                }
            }, safeDuration, TimeUnit.MILLISECONDS);
            scheduledTransitions.add(completion);
        }
    }

    public void crossfadeTo(
            String newPath,
            float targetVolume,
            long durationMs,
            boolean fadeIn) {
        crossfadeTo(newPath, targetVolume, durationMs, 0.0, fadeIn, 0.0);
    }

    public void crossfadeTo(
            String newPath,
            float targetVolume,
            long durationMs,
            double delaySeconds) {
        crossfadeTo(newPath, targetVolume, durationMs, delaySeconds, false, 0.0);
    }

    public void crossfadeTo(
            String newPath,
            float targetVolume,
            long durationMs,
            double delaySeconds,
            boolean fadeIn) {
        crossfadeTo(newPath, targetVolume, durationMs, delaySeconds, fadeIn, 0.0);
    }

    public void crossfadeTo(
            String newPath,
            float targetVolume,
            long durationMs,
            double delaySeconds,
            double timestampInicial) {
        crossfadeTo(newPath, targetVolume, durationMs, delaySeconds, false, timestampInicial);
    }

    public void crossfadeTo(
            String newPath,
            float targetVolume,
            long durationMs,
            double delaySeconds,
            boolean fadeIn,
            double timestampInicial) {
        validateTransitionArguments(delaySeconds, timestampInicial);
        transition(
                () -> createSingle(newPath),
                targetVolume,
                durationMs,
                delaySeconds,
                fadeIn,
                timestampInicial);
    }

    public void crossfadeToIntroThenLoop(
            String introPath,
            String loopPath,
            float targetVolume,
            long durationMs,
            boolean fadeIn) {
        crossfadeToIntroThenLoop(
                introPath, loopPath, targetVolume, durationMs, 0.0, fadeIn, 0.0);
    }

    public void crossfadeToIntroThenLoop(
            String introPath,
            String loopPath,
            float targetVolume,
            long durationMs,
            double delaySeconds) {
        crossfadeToIntroThenLoop(
                introPath, loopPath, targetVolume, durationMs, delaySeconds, false, 0.0);
    }

    public void crossfadeToIntroThenLoop(
            String introPath,
            String loopPath,
            float targetVolume,
            long durationMs,
            double delaySeconds,
            boolean fadeIn) {
        crossfadeToIntroThenLoop(
                introPath, loopPath, targetVolume, durationMs, delaySeconds, fadeIn, 0.0);
    }

    public void crossfadeToIntroThenLoop(
            String introPath,
            String loopPath,
            float targetVolume,
            long durationMs,
            double delaySeconds,
            double timestampInicial) {
        crossfadeToIntroThenLoop(
                introPath, loopPath, targetVolume, durationMs, delaySeconds, false, timestampInicial);
    }

    public void crossfadeToIntroThenLoop(
            String introPath,
            String loopPath,
            float targetVolume,
            long durationMs,
            double delaySeconds,
            boolean fadeIn,
            double timestampInicial) {
        validateTransitionArguments(delaySeconds, timestampInicial);
        transition(
                () -> createIntroLoop(introPath, loopPath),
                targetVolume,
                durationMs,
                delaySeconds,
                fadeIn,
                timestampInicial);
    }

    private void transition(
            HandleFactory factory,
            float targetVolume,
            long durationMs,
            double delaySeconds,
            boolean fadeIn,
            double timestampInicial) {
        long safeDuration = Math.max(0L, durationMs);
        long delayMs = Math.max(0L, Math.round(delaySeconds * 1000.0));
        float safeTargetVolume = Math.max(0f, targetVolume);

        synchronized (lock) {
            if (disposed || miniAudio == null) {
                return;
            }
            cancelScheduledLocked();

            Handle old = current;
            retainOnlyLocked(old);
            Handle next;
            try {
                next = factory.create();
            } catch (RuntimeException e) {
                warn("unable to load transition target: " + conciseMessage(e));
                return;
            }
            if (next == null) {
                return;
            }

            next.setVolume(safeTargetVolume);
            if (fadeIn) {
                if (next.intro != null) {
                    // The intro fades in; the chained loop must already have
                    // the target level when the native chain reaches it.
                    next.intro.setVolume(0f);
                    next.loop.setVolume(safeTargetVolume);
                } else {
                    next.setPlaybackVolume(0f);
                }
            }
            current = next;
            liveHandles.add(next);

            if (old != null) {
                old.fadeOut((float) safeDuration);
            }

            ScheduledFuture<?> start = scheduler.schedule(() -> {
                synchronized (lock) {
                    if (disposed || !liveHandles.contains(next)) {
                        return;
                    }
                    try {
                        next.start(timestampInicial);
                        if (fadeIn) {
                            next.fadeIn((float) safeDuration);
                        }
                    } catch (RuntimeException e) {
                        warn("unable to start transition target: " + conciseMessage(e));
                        liveHandles.remove(next);
                        if (current == next) {
                            current = null;
                        }
                        next.dispose();
                    }
                }
            }, delayMs, TimeUnit.MILLISECONDS);
            scheduledTransitions.add(start);

            if (old != null) {
                ScheduledFuture<?> finish = scheduler.schedule(() -> {
                    synchronized (lock) {
                        liveHandles.remove(old);
                        if (current == old) {
                            current = null;
                        }
                    }
                    old.dispose();
                }, safeDuration, TimeUnit.MILLISECONDS);
                scheduledTransitions.add(finish);
            }
        }
    }

    private Handle createSingle(String path) {
        if (miniAudio == null || path == null || path.isBlank()) {
            return null;
        }
        try {
            Handle handle = new Handle();
            handle.primary = miniAudio.createSound(path, MUSIC_FLAGS, musicGroup);
            return handle;
        } catch (RuntimeException e) {
            warn("audio unavailable for " + path + ": " + conciseMessage(e));
            return null;
        }
    }

    private Handle createIntroLoop(String introPath, String loopPath) {
        if (miniAudio == null || introPath == null || loopPath == null
                || introPath.isBlank() || loopPath.isBlank()) {
            return null;
        }

        Handle handle = new Handle();
        try {
            handle.intro = miniAudio.createSound(introPath, MUSIC_FLAGS, musicGroup);
            handle.loop = miniAudio.createSound(loopPath, MUSIC_FLAGS, musicGroup);
            handle.loop.setLooping(true);
            handle.intro.chainSound(handle.loop);
            handle.primary = handle.intro;
            return handle;
        } catch (RuntimeException e) {
            handle.dispose();
            warn("audio unavailable for intro/loop: " + conciseMessage(e));
            return null;
        }
    }

    private void replaceCurrentLocked() {
        cancelScheduledLocked();
        disposeAllLocked();
        current = null;
    }

    private void retainOnlyLocked(Handle keep) {
        for (Handle handle : new ArrayList<>(liveHandles)) {
            if (handle != keep) {
                handle.dispose();
                liveHandles.remove(handle);
            }
        }
        if (keep != null) {
            liveHandles.add(keep);
        }
    }

    private void disposeAllLocked() {
        for (Handle handle : new ArrayList<>(liveHandles)) {
            handle.dispose();
        }
        liveHandles.clear();
    }

    private void cancelScheduledLocked() {
        for (ScheduledFuture<?> transition : scheduledTransitions) {
            transition.cancel(false);
        }
        scheduledTransitions.clear();
    }

    private void validateTransitionArguments(double delaySeconds, double timestampInicial) {
        validateTimestamp(delaySeconds, "delaySeconds");
        validateTimestamp(timestampInicial, "timestampInicial");
    }

    private static void validateTimestamp(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(
                    "BGMPlayer: " + name + " deve ser finito e >= 0");
        }
    }

    private static double normalizeLoopTimestamp(double timestamp, double duration) {
        if (!Double.isFinite(timestamp) || timestamp < 0.0 || duration <= 0.0) {
            return 0.0;
        }
        double result = timestamp % duration;
        return result < 0.0 ? result + duration : result;
    }

    private static String conciseMessage(RuntimeException e) {
        String message = e.getMessage();
        return message == null || message.isBlank()
                ? e.getClass().getSimpleName()
                : e.getClass().getSimpleName() + " - " + message;
    }

    private static void warn(String message) {
        System.err.println("BGMPlayer warning: " + message);
    }

    @Override
    public void close() {
        dispose();
    }

    public void dispose() {
        synchronized (lock) {
            if (disposed) {
                return;
            }
            disposed = true;
            cancelScheduledLocked();
            disposeAllLocked();
            current = null;
        }
        scheduler.shutdownNow();
        if (ownsEngine) {
            if (musicGroup != null) {
                try {
                    musicGroup.dispose();
                } catch (RuntimeException ignored) {
                    // Best effort during shutdown.
                }
            }
            if (miniAudio != null) {
                miniAudio.dispose();
            }
        }
    }

    @FunctionalInterface
    private interface HandleFactory {
        Handle create();
    }
}
