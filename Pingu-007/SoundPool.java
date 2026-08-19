import javax.sound.sampled.*;

public class SoundPool {
    private final Clip[] pool;
    private int index = 0;

    public SoundPool(String path, int size, float volume) {
        pool = new Clip[size];
        for (int i = 0; i < size; i++) {
            try {
                pool[i] = SoundManager.loadClip(path);
                SoundManager.setVolume(pool[i], volume);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Clip play() {
        Clip clip = pool[index];
        clip.setFramePosition(0);
        clip.start();
        index = (index + 1) % pool.length;
        return clip;
    }

    public void setVolume(float volume) {
        for (Clip clip : pool) {
            SoundManager.setVolume(clip, volume);
        }
    }
}
