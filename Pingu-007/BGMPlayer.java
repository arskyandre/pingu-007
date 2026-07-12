import javax.sound.sampled.*;
import java.io.File;

public class BGMPlayer {

    private Thread thread;
    private Thread fadeThread;
    private volatile boolean running = false;
    private volatile float volume = 1.0f;
    private String currentPath = null;

    private static final int BUFFER_SIZE = 2048;

    public void play(String path) {
        stop();
        currentPath = path;
        running = true;
        thread = new Thread(() -> {
            while (running) {
                try {
                    playFileOnce(path);
                } catch (Exception e) {
                    e.printStackTrace();
                    running = false;
                }
            }
        }, "BGMPlayer");
        thread.setDaemon(true);
        thread.start();
    }

    public void playIntroThenLoop(String introPath, String loopPath) {
        stop();
        currentPath = loopPath;
        running = true;
        thread = new Thread(() -> {
            SourceDataLine line = null;
            try {
                AudioInputStream introAis = AudioSystem.getAudioInputStream(new File(introPath));
                AudioFormat format = introAis.getFormat();
                line = AudioSystem.getSourceDataLine(format);
                line.open(format, BUFFER_SIZE);
                line.start();
                applyVolume(line);

                byte[] buf = new byte[BUFFER_SIZE];
                int bytesRead;

                while (running && (bytesRead = introAis.read(buf, 0, buf.length)) != -1) {
                    applyVolume(line);
                    line.write(buf, 0, bytesRead);
                }
                introAis.close();

                while (running) {
                    AudioInputStream loopAis = AudioSystem.getAudioInputStream(new File(loopPath));
                    if (!loopAis.getFormat().matches(format)) {
                        System.err.println(
                                "BGMPlayer: formato diferente entre intro e loop");
                    }
                    while (running && (bytesRead = loopAis.read(buf, 0, buf.length)) != -1) {
                        applyVolume(line);
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
        thread.setDaemon(true);
        thread.start();
    }

    private void playFileOnce(String path) throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(path));
        AudioFormat format = ais.getFormat();
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format, BUFFER_SIZE);
        line.start();

        applyVolume(line);

        byte[] buf = new byte[BUFFER_SIZE];
        int bytesRead;
        while (running && (bytesRead = ais.read(buf, 0, buf.length)) != -1) {
            applyVolume(line);
            line.write(buf, 0, bytesRead);
        }

        line.drain();
        line.close();
        ais.close();
    }

    public void stop() {
        running = false;
        if (thread != null) {
            try {
                thread.join(500);
            } catch (InterruptedException ignored) {
            }
            thread = null;
        }
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public float getVolume() {
        return volume;
    }

    private void applyVolume(SourceDataLine line) {
        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            float curved = volume * volume;
            float dB = (float) (Math.log10(Math.max(curved, 0.0001)) * 20);
            gain.setValue(Math.clamp(dB, gain.getMinimum(), gain.getMaximum()));
        }
    }

    public void fadeOut(long durationMs) {
        if (fadeThread != null && fadeThread.isAlive()) {
            return; // ja tem um fade em andamento
        }

        float startVolume = volume;

        fadeThread = new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                while (running) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (elapsed >= durationMs) {
                        volume = 0f;
                        break;
                    }
                    float progress = elapsed / (float) durationMs;
                    volume = startVolume * (1f - progress);
                    Thread.sleep(16); // ~60 atualizacoes por segundo
                }
            } catch (InterruptedException ignored) {
            }
            stop();
            volume = startVolume; // restaura o volume para a proxima musica tocar normal
        }, "BGMFadeOut");
        fadeThread.setDaemon(true);
        fadeThread.start();
    }
}