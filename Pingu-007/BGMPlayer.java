import javax.sound.sampled.*;
import java.io.File;
import java.util.function.Supplier;

public class BGMPlayer {

    private static class Handle {
        volatile boolean running = true;
        volatile float volume = 1.0f;
        Thread thread;
    }

    private static class LoadedAudio {
        final AudioFormat format;
        final byte[] pcm;

        LoadedAudio(AudioFormat format, byte[] pcm) {
            this.format = format;
            this.pcm = pcm;
        }
    }

    private Handle current; // handle "oficial"
    private Thread fadeThread;
    private String currentPath = null;

    private static final int WRITE_CHUNK_SIZE = 2048;
    private static final int LINE_BUFFER_SIZE = 64 * 1024;

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

                line = openLine(loop.format, handle.volume);
                playLoop(line, loop.pcm, handle);
            } catch (Exception e) {
                e.printStackTrace();
                handle.running = false;
            } finally {
                closeLine(line);
            }
        }, "BGMPlayer");
        t.setDaemon(true);
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

                line = openLine(intro.format, handle.volume);
                playOnce(line, intro.pcm, handle);
                if (handle.running) {
                    playLoop(line, loop.pcm, handle);
                }
            } catch (Exception e) {
                e.printStackTrace();
                handle.running = false;
            } finally {
                closeLine(line);
            }
        }, "BGMPlayer");
        t.setDaemon(true);
        t.start();
        return t;
    }

    private LoadedAudio loadAudio(String path) throws Exception {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(new File(path))) {
            byte[] pcm = ais.readAllBytes();
            if (pcm.length == 0) {
                throw new IllegalArgumentException("BGMPlayer: arquivo de audio vazio: " + path);
            }
            return new LoadedAudio(ais.getFormat(), pcm);
        }
    }

    private SourceDataLine openLine(AudioFormat format, float volume) throws LineUnavailableException {
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format, LINE_BUFFER_SIZE);
        applyVolume(line, volume);
        line.start();
        return line;
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
            return; // ja tem um fade em andamento, ou nada tocando
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

    public void crossfadeToIntroThenLoop(String introPath, String loopPath, float targetVolume, long durationMs,
            boolean fadeIn) {
        crossfade(() -> {
            Handle h = new Handle();
            h.volume = fadeIn ? 0f : targetVolume;
            h.thread = startIntroLoopThread(introPath, loopPath, h);
            return h;
        }, targetVolume, durationMs, loopPath, fadeIn);
    }

    private void crossfade(Supplier<Handle> newHandleFactory, float targetVolume,
            long durationMs, String newCurrentPath, boolean fadeIn) {
        Handle oldHandle = current; // pode ser null, se nada estava tocando
        Handle newHandle = newHandleFactory.get();

        currentPath = newCurrentPath;
        current = newHandle; // a partir de agora, setVolume()/stop()/getVolume() controlam a nova musica

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
