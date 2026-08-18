import javax.sound.sampled.*;

public class SoundPool {
    private final Clip[] pool;
    private final String path;
    private int index = 0;
    private float volume;

    public SoundPool(String path, int size, float volume) {
        this.path = path;
        this.volume = volume;
        this.pool = new Clip[Math.max(1, size)];
    }

    public synchronized void play() {
        if (pool.length == 0) {
            return;
        }

        Clip current = pool[index];

        // ALTERADO (Lazy Loading):
        // Carrega somente o Clip desta posição quando ela for usada pela primeira vez.
        if (current == null || !current.isOpen()) {
            try {
                current = SoundManager.loadClip(path);
                pool[index] = current;

                if (current != null) {
                    SoundManager.setVolume(current, volume);
                }
            } catch (Exception e) {
                System.err.println("Erro ao carregar SFX [" + path + "]: " + e.getMessage());
                current = null;
                pool[index] = null;
            }
        }

        if (current != null && current.isOpen()) {
            current.stop();
            current.setFramePosition(0);
            current.start();
        }

        index = (index + 1) % pool.length;
    }

    public synchronized void setVolume(float volume) {
        this.volume = volume;

        for (Clip clip : pool) {
            if (clip != null && clip.isOpen()) {
                SoundManager.setVolume(clip, volume);
            }
        }
    }
}
