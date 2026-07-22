import javax.sound.sampled.*;
import java.io.File;
import java.util.function.Supplier;

public class BGMPlayer {

    private static class Handle {
        volatile boolean running = true;
        volatile float volume = 1.0f;
        Thread thread;
    }

    private Handle current; // handle "oficial"
    private Thread fadeThread;
    private String currentPath = null;

    private static final int BUFFER_SIZE = 2048;

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
            while (handle.running) {
                try {
                    playFileOnce(path, handle);
                } catch (Exception e) {
                    e.printStackTrace();
                    handle.running = false;
                }
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
                AudioInputStream introAis = AudioSystem.getAudioInputStream(new File(introPath));
                AudioFormat format = introAis.getFormat();
                line = AudioSystem.getSourceDataLine(format);
                line.open(format, BUFFER_SIZE);
                line.start();
                applyVolume(line, handle.volume);

                byte[] buf = new byte[BUFFER_SIZE];
                int bytesRead;

                while (handle.running && (bytesRead = introAis.read(buf, 0, buf.length)) != -1) {
                    applyVolume(line, handle.volume);
                    line.write(buf, 0, bytesRead);
                }
                introAis.close();

                while (handle.running) {
                    AudioInputStream loopAis = AudioSystem.getAudioInputStream(new File(loopPath));
                    if (!loopAis.getFormat().matches(format)) {
                        System.err.println("BGMPlayer: formato diferente entre intro e loop");
                    }
                    while (handle.running && (bytesRead = loopAis.read(buf, 0, buf.length)) != -1) {
                        applyVolume(line, handle.volume);
                        line.write(buf, 0, bytesRead);
                    }
                    loopAis.close();
                }

                line.drain();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (line != null)
                    line.close();
            }
        }, "BGMPlayer");
        t.setDaemon(true);
        t.start();
        return t;
    }

    private void playFileOnce(String path, Handle handle) throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(path));
        AudioFormat format = ais.getFormat();
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format, BUFFER_SIZE);
        line.start();

        applyVolume(line, handle.volume);

        byte[] buf = new byte[BUFFER_SIZE];
        int bytesRead;
        while (handle.running && (bytesRead = ais.read(buf, 0, buf.length)) != -1) {
            applyVolume(line, handle.volume);
            line.write(buf, 0, bytesRead);
        }

        line.drain();
        line.close();
        ais.close();
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