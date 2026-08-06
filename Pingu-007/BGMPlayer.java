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
        Thread t = new Thread(() -> {
            SourceDataLine line = null;
            try {
                LoadedAudio loop = loadAudio(path);
                if (!handle.running) {
                    return;
                }

                handle.hasIntro = false;
                handle.loopDurationSeconds = getDurationSeconds(loop);

                line = openLine(loop.format, handle.volume);
                handle.line = line;
                playLoop(line, loop.pcm, handle);
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
        Thread t = new Thread(() -> {
            SourceDataLine line = null;
            try {
                LoadedAudio intro = loadAudio(introPath);
                LoadedAudio loop = loadAudio(loopPath);
                if (!intro.format.matches(loop.format)) {
                    throw new IllegalArgumentException("BGMPlayer: formato diferente entre intro e loop");
                }
                if (!handle.running) {
                    return;
                }

                handle.hasIntro = true;
                handle.introDurationSeconds = getDurationSeconds(intro);
                handle.loopDurationSeconds = getDurationSeconds(loop);

                line = openLine(intro.format, handle.volume);
                handle.line = line;
                playOnce(line, intro.pcm, handle);
                if (handle.running) {
                    playLoop(line, loop.pcm, handle);
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

    private SourceDataLine openLine(AudioFormat format, float volume) throws LineUnavailableException {
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format, LINE_BUFFER_SIZE);
        applyVolume(line, volume);
        line.start();
        return line;
    }

    public void seek(double timestamp) {
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
        writePcm(line, pcm, handle, false);
    }

    private void playLoop(SourceDataLine line, byte[] pcm, Handle handle) {
        writePcm(line, pcm, handle, true);
    }

    private void writePcm(SourceDataLine line, byte[] pcm, Handle handle, boolean loop) {
        int offset = 0;
        float volumeAplicado = Float.NaN;

        while (handle.running) {
            if (Float.compare(volumeAplicado, handle.volume) != 0) {
                volumeAplicado = handle.volume;
                applyVolume(line, volumeAplicado);
            }

            int bytesRestantes = pcm.length - offset;
            int quantidade = Math.min(WRITE_CHUNK_SIZE, bytesRestantes);
            int escritos = line.write(pcm, offset, quantidade);
            offset += escritos;

            if (offset >= pcm.length) {
                if (!loop) {
                    return;
                }
                offset = 0;
            }
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

    public void crossfadeTo(String newPath, float targetVolume, long durationMs, boolean fadeIn) {
        crossfade(() -> {
            Handle h = new Handle();
            h.volume = fadeIn ? 0f : targetVolume;
            h.thread = startLoopThread(newPath, h);
            return h;
        }, targetVolume, durationMs, newPath, fadeIn);
    }

    public void crossfadeTo(String newPath, float targetVolume, long durationMs, double delaySeconds,
            double timestampInicial) {
        crossfadeDelayed(
                h -> h.thread = startLoopThread(newPath, h),
                targetVolume,
                durationMs,
                newPath,
                delaySeconds);
    }

    public void crossfadeToIntroThenLoop(String introPath, String loopPath, float targetVolume, long durationMs,
            boolean fadeIn) {
        crossfade(() -> {
            Handle h = new Handle();
            h.volume = fadeIn ? 0f : targetVolume;
            h.thread = startIntroLoopThread(introPath, loopPath, h);
            return h;
        }, targetVolume, durationMs, loopPath, fadeIn);
    }

    public void crossfadeToIntroThenLoop(String introPath, String loopPath, float targetVolume, long durationMs,
            double delaySeconds, double timestampIincial) {
        crossfadeDelayed(
                h -> h.thread = startIntroLoopThread(introPath, loopPath, h),
                targetVolume,
                durationMs,
                loopPath,
                delaySeconds);
    }

    private void crossfadeDelayed(Consumer<Handle> newHandleStarter, float targetVolume,
            long durationMs, String newCurrentPath, double delaySeconds) {
        if (!Double.isFinite(delaySeconds) || delaySeconds < 0.0) {
            throw new IllegalArgumentException("BGMPlayer: delaySeconds deve ser finito e >= 0");
        }

        Handle oldHandle = current;
        Handle newHandle = new Handle();
        newHandle.volume = targetVolume;

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

            try {
                long startTime = System.currentTimeMillis();

                while (true) {
                    if (!newHandle.running) {
                        if (oldHandle != null && !oldMusicStopped) {
                            stopHandle(oldHandle);
                        }
                        return;
                    }

                    long elapsed = System.currentTimeMillis() - startTime;

                    if (!newMusicStarted && elapsed >= delayMs) {
                        newHandleStarter.accept(newHandle);
                        newMusicStarted = true;
                    }

                    boolean fadeFinished = safeDurationMs == 0L || elapsed >= safeDurationMs;

                    if (oldHandle != null && !oldMusicStopped) {
                        if (fadeFinished) {
                            oldHandle.volume = 0f;
                            stopHandle(oldHandle);
                            oldMusicStopped = true;
                        } else {
                            float progress = elapsed / (float) safeDurationMs;
                            oldHandle.volume = oldStartVolume * (1f - progress);
                        }
                    }

                    if (fadeFinished && newMusicStarted) {
                        break;
                    }

                    Thread.sleep(16);
                }
            } catch (InterruptedException ignored) {

            }

            if (oldHandle != null && !oldMusicStopped) {
                stopHandle(oldHandle);
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
