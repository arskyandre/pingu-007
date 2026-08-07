import javax.sound.sampled.*;
import java.io.File;
import java.util.function.Supplier;
import java.util.function.Consumer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BGMPlayer {

    private static class Handle {
        volatile boolean running = true;
        volatile float volume = 1.0f;
        Thread thread;

        final Object seekLock = new Object();
        volatile boolean seekRequested = false;
        volatile double requestedTimestampSeconds = 0.0;

        volatile SourceDataLine line;

        volatile long anchorLineFrame = 0L;
        volatile double anchorTimestampSeconds = 0.0;
        volatile double lastTimestampSeconds = 0.0;

        volatile boolean hasIntro = false;
        volatile double introDurationSeconds = 0.0;
        volatile double loopDurationSeconds = 0.0;
    }

    private static class LoadedAudio {
        final AudioFormat format;
        final byte[] pcm;

        LoadedAudio(AudioFormat format, byte[] pcm) {
            this.format = format;
            this.pcm = pcm;
        }
    }

    private Handle current;
    private Thread fadeThread;
    private String currentPath = null;

    private static final int WRITE_CHUNK_SIZE = 8 * 1024;
    private static final int LINE_BUFFER_SIZE = 128 * 1024;
    private static final int AUDIO_THREAD_PRIORITY = Math.min(
            Thread.MAX_PRIORITY, Thread.NORM_PRIORITY + 2);

    private static final Map<String, LoadedAudio> AUDIO_CACHE = new ConcurrentHashMap<>();

    public void play(String path) {
        stop();
        currentPath = path;
        Handle handle = new Handle();
        handle.volume = 1.0f;
        current = handle;
        handle.thread = startLoopThread(path, handle);
    }

    public void playIntroThenLoop(String introPath, String loopPath) {
        stop();
        currentPath = loopPath;
        Handle handle = new Handle();
        handle.volume = 1.0f;
        current = handle;
        handle.thread = startIntroLoopThread(introPath, loopPath, handle);
    }

    private Thread startLoopThread(String path, Handle handle) {
        return startLoopThread(path, handle, 0.0);
    }

    private Thread startLoopThread(String path, Handle handle, double timestampInicial) {
        Thread t = new Thread(() -> {
            SourceDataLine line = null;
            try {
                LoadedAudio loop = loadAudio(path);
                if (!handle.running) {
                    return;
                }

                handle.hasIntro = false;
                handle.loopDurationSeconds = getDurationSeconds(loop);

                double timestampNormalizado = normalizeLoopTimestamp(
                        timestampInicial,
                        handle.loopDurationSeconds);

                handle.anchorTimestampSeconds = timestampNormalizado;
                handle.lastTimestampSeconds = timestampNormalizado;

                line = openLine(loop.format, handle.volume);
                handle.line = line;
                handle.anchorLineFrame = line.getLongFramePosition();

                int offsetInicial = timestampToByteOffset(
                        loop,
                        timestampNormalizado);

                playLoop(line, loop.pcm, handle, offsetInicial);
            } catch (Exception e) {
                e.printStackTrace();
                handle.running = false;
            } finally {
                closeLine(line);
                handle.line = null;
            }
        }, "BGMPlayer");
        t.setDaemon(true);
        t.setPriority(AUDIO_THREAD_PRIORITY);
        t.start();
        return t;
    }

    private Thread startIntroLoopThread(String introPath, String loopPath, Handle handle) {
        return startIntroLoopThread(introPath, loopPath, handle, 0.0);
    }

    private Thread startIntroLoopThread(
            String introPath,
            String loopPath,
            Handle handle,
            double timestampInicial) {

        Thread t = new Thread(() -> {
            SourceDataLine line = null;
            try {
                LoadedAudio intro = loadAudio(introPath);
                LoadedAudio loop = loadAudio(loopPath);

                if (!intro.format.matches(loop.format)) {
                    throw new IllegalArgumentException(
                            "BGMPlayer: formato diferente entre intro e loop");
                }

                if (!handle.running) {
                    return;
                }

                handle.hasIntro = true;
                handle.introDurationSeconds = getDurationSeconds(intro);
                handle.loopDurationSeconds = getDurationSeconds(loop);

                if (timestampInicial > handle.introDurationSeconds) {
                    handle.running = false;
                    return;
                }

                handle.anchorTimestampSeconds = timestampInicial;
                handle.lastTimestampSeconds = timestampInicial;

                line = openLine(intro.format, handle.volume);
                handle.line = line;
                handle.anchorLineFrame = line.getLongFramePosition();

                if (timestampInicial < handle.introDurationSeconds) {
                    int offsetIntro = timestampToByteOffset(intro, timestampInicial);
                    playOnce(line, intro.pcm, handle, offsetIntro);
                }

                if (handle.running) {
                    handle.anchorLineFrame = line.getLongFramePosition();
                    handle.anchorTimestampSeconds = handle.introDurationSeconds;
                    playLoop(line, loop.pcm, handle, 0);
                }
            } catch (Exception e) {
                e.printStackTrace();
                handle.running = false;
            } finally {
                closeLine(line);
                handle.line = null;
            }
        }, "BGMPlayer");
        t.setDaemon(true);
        t.setPriority(AUDIO_THREAD_PRIORITY);
        t.start();
        return t;
    }

    private LoadedAudio loadAudio(String path) throws Exception {
        LoadedAudio cached = AUDIO_CACHE.get(path);
        if (cached != null) {
            return cached;
        }

        LoadedAudio loaded;
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(new File(path))) {
            byte[] pcm = ais.readAllBytes();
            if (pcm.length == 0) {
                throw new IllegalArgumentException("BGMPlayer: arquivo de audio vazio: " + path);
            }
            loaded = new LoadedAudio(ais.getFormat(), pcm);
        }

        LoadedAudio previous = AUDIO_CACHE.putIfAbsent(path, loaded);
        return previous != null ? previous : loaded;
    }

    public void preload(String... paths) throws Exception {
        for (String path : paths) {
            loadAudio(path);
        }
    }

    public static void clearAudioCache() {
        AUDIO_CACHE.clear();
    }

    private double getDurationSeconds(LoadedAudio audio) {
        int frameSize = audio.format.getFrameSize();
        float frameRate = audio.format.getFrameRate();

        if (frameSize <= 0 || frameRate <= 0.0f) {
            return 0.0;
        }

        long frameCount = audio.pcm.length / frameSize;
        return frameCount / (double) frameRate;
    }

    private int timestampToByteOffset(LoadedAudio audio, double timestampSeconds) {
        int frameSize = audio.format.getFrameSize();
        float frameRate = audio.format.getFrameRate();

        if (frameSize <= 0 || frameRate <= 0.0f) {
            return 0;
        }

        long frame = (long) Math.floor(timestampSeconds * frameRate);
        long byteOffset = frame * frameSize;
        long alignedOffset = Math.clamp(byteOffset, 0L, (long) audio.pcm.length);

        return (int) alignedOffset;
    }

    private SourceDataLine openLine(AudioFormat format, float volume) throws LineUnavailableException {
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format, LINE_BUFFER_SIZE);
        applyVolume(line, volume);
        line.start();
        return line;
    }

    public void seek(double timestamp) {
        System.out.println("Deu seek");
        if (!Double.isFinite(timestamp) || timestamp < 0.0) {
            throw new IllegalArgumentException(
                    "BGMPlayer: timestamp deve ser finito e maior ou igual a 0");
        }

        Handle handle = current;

        if (handle == null || !handle.running) {
            return;
        }

        synchronized (handle.seekLock) {
            handle.requestedTimestampSeconds = timestamp;
            handle.seekRequested = true;
            handle.lastTimestampSeconds = timestamp;
        }
    }

    private double normalizeLoopTimestamp(
            double timestampSeconds,
            double loopDurationSeconds) {

        if (loopDurationSeconds <= 0.0) {
            return 0.0;
        }

        double resultado = timestampSeconds % loopDurationSeconds;

        if (resultado < 0.0) {
            resultado += loopDurationSeconds;
        }

        return resultado;
    }

    private double normalizeTimestamp(
            Handle handle,
            double timestampSeconds) {

        double timestampSeguro = Math.max(0.0, timestampSeconds);

        if (handle.loopDurationSeconds <= 0.0) {
            return timestampSeguro;
        }

        if (!handle.hasIntro) {
            return normalizeLoopTimestamp(
                    timestampSeguro,
                    handle.loopDurationSeconds);
        }

        if (timestampSeguro < handle.introDurationSeconds) {
            return timestampSeguro;
        }

        return handle.introDurationSeconds
                + normalizeLoopTimestamp(
                        timestampSeguro - handle.introDurationSeconds,
                        handle.loopDurationSeconds);
    }

    private double calculateTimestamp(Handle handle) {
        SourceDataLine line = handle.line;

        if (line == null) {
            return normalizeTimestamp(
                    handle,
                    handle.lastTimestampSeconds);
        }

        float frameRate = line.getFormat().getFrameRate();

        if (frameRate <= 0.0f) {
            return normalizeTimestamp(
                    handle,
                    handle.lastTimestampSeconds);
        }

        long framesReproduzidos = Math.max(
                0L,
                line.getLongFramePosition() - handle.anchorLineFrame);

        double segundosReproduzidos = framesReproduzidos / (double) frameRate;

        return normalizeTimestamp(
                handle,
                handle.anchorTimestampSeconds + segundosReproduzidos);
    }

    public double getTimestamp() {
        Handle handle = current;

        if (handle == null || !handle.running) {
            return 0.0;
        }

        synchronized (handle.seekLock) {
            if (handle.seekRequested) {
                return normalizeTimestamp(
                        handle,
                        handle.requestedTimestampSeconds);
            }
        }

        double timestamp = calculateTimestamp(handle);
        handle.lastTimestampSeconds = timestamp;

        return timestamp;
    }

    private void playOnce(SourceDataLine line, byte[] pcm, Handle handle) {
        playOnce(line, pcm, handle, 0);
    }

    private void playOnce(
            SourceDataLine line,
            byte[] pcm,
            Handle handle,
            int offsetInicial) {

        writePcm(line, pcm, handle, false, offsetInicial);
    }

    private void playLoop(SourceDataLine line, byte[] pcm, Handle handle) {
        playLoop(line, pcm, handle, 0);
    }

    private void playLoop(
            SourceDataLine line,
            byte[] pcm,
            Handle handle,
            int offsetInicial) {

        writePcm(line, pcm, handle, true, offsetInicial);
    }

    private void writePcm(
            SourceDataLine line,
            byte[] pcm,
            Handle handle,
            boolean loop,
            int offsetInicial) {

        int offset = Math.clamp(offsetInicial, 0, pcm.length);
        float volumeAplicado = Float.NaN;

        while (handle.running) {
            if (Float.compare(volumeAplicado, handle.volume) != 0) {
                volumeAplicado = handle.volume;
                applyVolume(line, volumeAplicado);
            }

            if (offset >= pcm.length) {
                if (!loop) {
                    return;
                }
                offset = 0;
                handle.anchorLineFrame = line.getLongFramePosition();
                handle.anchorTimestampSeconds = handle.hasIntro
                        ? handle.introDurationSeconds
                        : 0.0;
            }

            int bytesRestantes = pcm.length - offset;
            int quantidade = Math.min(WRITE_CHUNK_SIZE, bytesRestantes);
            int escritos = line.write(pcm, offset, quantidade);
            offset += escritos;
        }
    }

    private void closeLine(SourceDataLine line) {
        if (line == null) {
            return;
        }
        line.stop();
        line.flush();
        line.close();
    }

    public boolean isPlaying() {
        Handle handle = current;
        return handle != null && handle.running;
    }

    public void stop() {
        if (current != null) {
            stopHandle(current);
            current = null;
        }
    }

    private void stopHandle(Handle handle) {
        handle.running = false;

        SourceDataLine line = handle.line;
        if (line != null) {
            line.stop();
            line.flush();
        }

        if (handle.thread != null) {
            try {
                handle.thread.join(500);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void setVolume(float volume) {
        if (current != null) {
            current.volume = volume;
        }
    }

    public float getVolume() {
        return current != null ? current.volume : 0f;
    }

    private void applyVolume(SourceDataLine line, float volume) {
        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            float curved = volume * volume;
            float dB = (float) (Math.log10(Math.max(curved, 0.0001)) * 20);
            gain.setValue(Math.clamp(dB, gain.getMinimum(), gain.getMaximum()));
        }
    }

    public void fadeOut(long durationMs) {
        fadeOut(durationMs, null);
    }

    public void fadeOut(long durationMs, Runnable onComplete) {
        if (current == null || (fadeThread != null && fadeThread.isAlive())) {
            return;
        }
        Handle target = current;
        float startVolume = target.volume;

        fadeThread = new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                while (target.running) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (elapsed >= durationMs) {
                        target.volume = 0f;
                        break;
                    }
                    float progress = elapsed / (float) durationMs;
                    target.volume = startVolume * (1f - progress);
                    Thread.sleep(16);
                }
            } catch (InterruptedException ignored) {
            }
            stopHandle(target);
            if (current == target) {
                current = null;
            }
            if (onComplete != null) {
                onComplete.run();
            }
        }, "BGMFadeOut");
        fadeThread.setDaemon(true);
        fadeThread.start();
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

        crossfadeTo(
                newPath,
                targetVolume,
                durationMs,
                delaySeconds,
                false,
                timestampInicial);
    }

    public void crossfadeTo(
            String newPath,
            float targetVolume,
            long durationMs,
            double delaySeconds,
            boolean fadeIn,
            double timestampInicial) {

        validateTransitionArguments(delaySeconds, timestampInicial);

        crossfadeDelayed(
                h -> h.thread = startLoopThread(newPath, h, timestampInicial),
                targetVolume,
                durationMs,
                newPath,
                delaySeconds,
                fadeIn);
    }

    public void crossfadeToIntroThenLoop(
            String introPath,
            String loopPath,
            float targetVolume,
            long durationMs,
            boolean fadeIn) {

        crossfadeToIntroThenLoop(
                introPath,
                loopPath,
                targetVolume,
                durationMs,
                0.0,
                fadeIn,
                0.0);
    }

    public void crossfadeToIntroThenLoop(
            String introPath,
            String loopPath,
            float targetVolume,
            long durationMs,
            double delaySeconds) {

        crossfadeToIntroThenLoop(
                introPath,
                loopPath,
                targetVolume,
                durationMs,
                delaySeconds,
                false,
                0.0);
    }

    public void crossfadeToIntroThenLoop(
            String introPath,
            String loopPath,
            float targetVolume,
            long durationMs,
            double delaySeconds,
            boolean fadeIn) {

        crossfadeToIntroThenLoop(
                introPath,
                loopPath,
                targetVolume,
                durationMs,
                delaySeconds,
                fadeIn,
                0.0);
    }

    public void crossfadeToIntroThenLoop(
            String introPath,
            String loopPath,
            float targetVolume,
            long durationMs,
            double delaySeconds,
            double timestampInicial) {

        crossfadeToIntroThenLoop(
                introPath,
                loopPath,
                targetVolume,
                durationMs,
                delaySeconds,
                false,
                timestampInicial);
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

        try {
            LoadedAudio intro = loadAudio(introPath);
            double duracaoIntro = getDurationSeconds(intro);

            if (timestampInicial > duracaoIntro) {
                stop();
                currentPath = null;
                return;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "BGMPlayer: nao foi possivel validar o timestamp da intro",
                    e);
        }

        crossfadeDelayed(
                h -> h.thread = startIntroLoopThread(
                        introPath,
                        loopPath,
                        h,
                        timestampInicial),
                targetVolume,
                durationMs,
                loopPath,
                delaySeconds,
                fadeIn);
    }

    private void validateTransitionArguments(
            double delaySeconds,
            double timestampInicial) {

        if (!Double.isFinite(delaySeconds) || delaySeconds < 0.0) {
            throw new IllegalArgumentException(
                    "BGMPlayer: delaySeconds deve ser finito e >= 0");
        }

        if (!Double.isFinite(timestampInicial) || timestampInicial < 0.0) {
            throw new IllegalArgumentException(
                    "BGMPlayer: timestampInicial deve ser finito e >= 0");
        }
    }

    private void crossfadeDelayed(
            Consumer<Handle> newHandleStarter,
            float targetVolume,
            long durationMs,
            String newCurrentPath,
            double delaySeconds,
            boolean fadeIn) {

        Handle oldHandle = current;
        Handle newHandle = new Handle();
        newHandle.volume = fadeIn ? 0f : targetVolume;

        currentPath = newCurrentPath;
        current = newHandle;

        if (fadeThread != null && fadeThread.isAlive()) {
            fadeThread.interrupt();
        }

        float oldStartVolume = oldHandle != null ? oldHandle.volume : 0f;
        long safeDurationMs = Math.max(0L, durationMs);
        long delayMs = Math.round(delaySeconds * 1000.0);

        fadeThread = new Thread(() -> {
            boolean newMusicStarted = false;
            boolean oldMusicStopped = false;
            long newMusicStartTime = 0L;

            try {
                long transitionStartTime = System.currentTimeMillis();

                while (true) {
                    if (!newHandle.running && newMusicStarted) {
                        break;
                    }

                    long now = System.currentTimeMillis();
                    long transitionElapsed = now - transitionStartTime;

                    if (!newMusicStarted && transitionElapsed >= delayMs) {
                        newHandleStarter.accept(newHandle);
                        newMusicStarted = true;
                        newMusicStartTime = now;
                    }

                    boolean oldFadeFinished = safeDurationMs == 0L
                            || transitionElapsed >= safeDurationMs;

                    if (oldHandle != null && !oldMusicStopped) {
                        if (oldFadeFinished) {
                            oldHandle.volume = 0f;
                            stopHandle(oldHandle);
                            oldMusicStopped = true;
                        } else {
                            float progress = transitionElapsed / (float) safeDurationMs;
                            oldHandle.volume = oldStartVolume * (1f - progress);
                        }
                    }

                    boolean newFadeFinished = !fadeIn;

                    if (fadeIn && newMusicStarted && newHandle.running) {
                        long newMusicElapsed = now - newMusicStartTime;

                        if (safeDurationMs == 0L || newMusicElapsed >= safeDurationMs) {
                            newHandle.volume = targetVolume;
                            newFadeFinished = true;
                        } else {
                            float progress = newMusicElapsed / (float) safeDurationMs;
                            newHandle.volume = targetVolume * progress;
                        }
                    }

                    if (oldFadeFinished && newMusicStarted && newFadeFinished) {
                        break;
                    }

                    Thread.sleep(16);
                }
            } catch (InterruptedException ignored) {
            }

            if (oldHandle != null && !oldMusicStopped) {
                stopHandle(oldHandle);
            }

            if (!newHandle.running && current == newHandle) {
                current = null;
                currentPath = null;
            }
        }, "BGMDelayedCrossfade");

        fadeThread.setDaemon(true);
        fadeThread.start();
    }

    private void crossfade(Supplier<Handle> newHandleFactory, float targetVolume,
            long durationMs, String newCurrentPath, boolean fadeIn) {
        Handle oldHandle = current;

        Handle newHandle = newHandleFactory.get();

        currentPath = newCurrentPath;
        current = newHandle;

        if (fadeThread != null && fadeThread.isAlive()) {
            fadeThread.interrupt();
        }

        float oldStartVolume = oldHandle != null ? oldHandle.volume : 0f;

        fadeThread = new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                while (true) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (elapsed >= durationMs) {
                        newHandle.volume = targetVolume;
                        if (oldHandle != null) {
                            oldHandle.volume = 0f;
                        }
                        break;
                    }
                    float progress = elapsed / (float) durationMs;
                    if (fadeIn) {
                        newHandle.volume = targetVolume * progress;
                    }
                    if (oldHandle != null) {
                        oldHandle.volume = oldStartVolume * (1f - progress);
                    }
                    Thread.sleep(16);
                }
            } catch (InterruptedException ignored) {
            }
            if (oldHandle != null) {
                stopHandle(oldHandle);
            }
        }, "BGMCrossfade");
        fadeThread.setDaemon(true);
        fadeThread.start();
    }
}
