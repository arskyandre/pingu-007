import javax.sound.sampled.*;
import java.io.File;

public class BGMPlayer {

    private Thread thread;
    private volatile boolean running = false;
    private volatile float volume = 1.0f;
    private String currentPath = null;

    private static final int BUFFER_SIZE = 2048; // small = faster volume response

    public void play(String path) {
        stop();
        currentPath = path;
        running = true;
        thread = new Thread(() -> {
            while (running) {
                try {
                    AudioInputStream ais = AudioSystem.getAudioInputStream(new File(path));
                    AudioFormat format = ais.getFormat();
                    SourceDataLine line = AudioSystem.getSourceDataLine(format);
                    line.open(format, BUFFER_SIZE);
                    line.start();

                    applyVolume(line);

                    byte[] buf = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while (running && (bytesRead = ais.read(buf, 0, buf.length)) != -1) {
                        applyVolume(line); // applied every chunk — near-instant response
                        line.write(buf, 0, bytesRead);
                    }

                    line.drain();
                    line.close();
                    ais.close();
                    // loop: while(running) restarts the file automatically

                } catch (Exception e) {
                    e.printStackTrace();
                    running = false;
                }
            }
        }, "BGMPlayer");
        thread.setDaemon(true);
        thread.start();
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
        this.volume = volume; // picked up on next chunk write
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
}