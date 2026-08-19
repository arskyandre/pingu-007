import javax.sound.sampled.*;

public class SoundPool {
    private static final int MAX_POOL_SIZE = 6;

    private final String path;
    private final Clip[] pool;
    private final boolean[] loadFailed;
    private int index = 0;
    private float volume;
    private boolean warnedAboutAudio = false;

    public SoundPool(String path, int size, float volume) {
        this.path = path;
        
        int cappedSize = Math.max(1, Math.min(size, MAX_POOL_SIZE));
        pool = new Clip[cappedSize];
        loadFailed = new boolean[cappedSize];
        this.volume = volume;
    }

    public Clip play() {
        if (pool.length == 0) {
            return null;
        }

        int current = index;
        index = (index + 1) % pool.length;

        Clip clip = ensureClip(current);
        if (clip == null) {
            return null;
        }

        try {
            clip.stop();
            clip.setFramePosition(0);
            SoundManager.setVolume(clip, volume);
            clip.start();
            return clip;
        } catch (Exception e) {
            warnOnce("playback failed for " + path + ": " + conciseMessage(e));
            return null;
        }
    }

    public void setVolume(float volume) {
        this.volume = volume;

        for (Clip clip : pool) {
            SoundManager.setVolume(clip, volume);
        }
    }

    private Clip ensureClip(int slot) {
        Clip clip = pool[slot];
        if (clip != null || loadFailed[slot]) {
            return clip;
        }

        try {
            clip = SoundManager.loadClip(path);
            SoundManager.setVolume(clip, volume);
            pool[slot] = clip;
            return clip;
        } catch (Exception e) {
            loadFailed[slot] = true;
            warnOnce("audio unavailable for " + path + ": " + conciseMessage(e));
            return null;
        }
    }

    private void warnOnce(String message) {
        if (warnedAboutAudio) {
            return;
        }
        warnedAboutAudio = true;
        System.err.println("SoundPool warning: " + message);
    }

    private String conciseMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return e.getClass().getSimpleName() + " - " + message;
    }
}
