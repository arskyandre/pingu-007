import games.rednblack.miniaudio.MAGroup;
import games.rednblack.miniaudio.MASound;
import games.rednblack.miniaudio.MiniAudio;

/**
 * Small round-robin pool of independent MiniAudio voices.
 *
 * A MASound is stateful and cannot be played concurrently with itself, so a
 * separate voice is kept for every possible overlap. Sounds are created lazily
 * to keep the SoundManager constructor cheap (there are many dialogue assets).
 */
public final class SoundPool {
    private static final int MAX_POOL_SIZE = 16;

    private static final class StandaloneAudio {
        final MiniAudio miniAudio;
        final MAGroup group;

        StandaloneAudio(MiniAudio miniAudio, MAGroup group) {
            this.miniAudio = miniAudio;
            this.group = group;
        }
    }

    private final MiniAudio miniAudio;
    private final MAGroup group;
    private final boolean ownsEngine;
    private final String path;
    private final short flags;
    private final MASound[] pool;
    private final boolean[] loadFailed;
    private int index;
    private float volume;
    private boolean warnedAboutAudio;
    private boolean disposed;

    /**
     * Constructor retained for source compatibility. New code should inject
     * the shared engine through the other constructor.
     */
    public SoundPool(String path, int size, float volume) {
        this(createStandaloneAudio(), path, size, volume,
                (short) (MASound.Flags.MA_SOUND_FLAG_DECODE
                        | MASound.Flags.MA_SOUND_FLAG_NO_SPATIALIZATION
                        | MASound.Flags.MA_SOUND_FLAG_NO_PITCH));
    }

    private SoundPool(
            StandaloneAudio audio,
            String path,
            int size,
            float volume,
            short flags) {
        this(audio == null ? null : audio.miniAudio,
                path,
                size,
                volume,
                audio == null ? null : audio.group,
                flags,
                audio != null);
    }

    public SoundPool(
            MiniAudio miniAudio,
            String path,
            int size,
            float volume,
            MAGroup group,
            short flags) {
        this(miniAudio, path, size, volume, group, flags, false);
    }

    private SoundPool(
            MiniAudio miniAudio,
            String path,
            int size,
            float volume,
            MAGroup group,
            short flags,
            boolean ownsEngine) {
        this.miniAudio = miniAudio;
        this.path = path;
        this.group = group;
        this.ownsEngine = ownsEngine;
        this.flags = flags;
        int cappedSize = Math.max(1, Math.min(size, MAX_POOL_SIZE));
        this.pool = new MASound[cappedSize];
        this.loadFailed = new boolean[cappedSize];
        this.volume = volume;
    }

    /**
     * Plays one voice. A free voice is preferred; when all voices overlap,
     * round-robin selection stops the oldest slot so effects remain responsive.
     */
    public synchronized MASound play() {
        return play(0f);
    }

    /**
     * Plays one voice with stereo pan: -1 left, 0 center, 1 right.
     */
    public synchronized MASound play(float pan) {
        if (disposed || miniAudio == null || pool.length == 0) {
            return null;
        }

        int slot = findFreeSlot();
        MASound sound = ensureSound(slot);
        if (sound == null) {
            return null;
        }

        index = (slot + 1) % pool.length;
        try {
            sound.stop();
            sound.seekTo(0f);
            sound.setVolume(volume);
            sound.setPan(Math.clamp(pan, -1f, 1f));
            sound.play();
            return sound;
        } catch (RuntimeException e) {
            warnOnce("playback failed for " + path + ": " + conciseMessage(e));
            return null;
        }
    }

    public synchronized void setVolume(float volume) {
        this.volume = Math.max(0f, volume);
        for (MASound sound : pool) {
            if (sound != null) {
                try {
                    sound.setVolume(this.volume);
                } catch (RuntimeException e) {
                    warnOnce("volume update failed for " + path + ": " + conciseMessage(e));
                }
            }
        }
    }

    public synchronized void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (int i = 0; i < pool.length; i++) {
            MASound sound = pool[i];
            pool[i] = null;
            if (sound == null) {
                continue;
            }
            try {
                sound.stop();
            } catch (RuntimeException ignored) {
                // The native engine may already be stopping during shutdown.
            }
            try {
                sound.dispose();
            } catch (RuntimeException ignored) {
                // Best effort: continue disposing every voice.
            }
        }
        if (ownsEngine) {
            if (group != null) {
                try {
                    group.dispose();
                } catch (RuntimeException ignored) {
                    // Continue shutting down the standalone engine.
                }
            }
            if (miniAudio != null) {
                try {
                    miniAudio.dispose();
                } catch (RuntimeException ignored) {
                    // Native shutdown is best effort.
                }
            }
        }
    }

    private int findFreeSlot() {
        for (int offset = 0; offset < pool.length; offset++) {
            int candidate = (index + offset) % pool.length;
            MASound sound = pool[candidate];
            if (sound == null || !sound.isPlaying()) {
                return candidate;
            }
        }
        return index;
    }

    private MASound ensureSound(int slot) {
        MASound sound = pool[slot];
        if (sound != null || loadFailed[slot]) {
            return sound;
        }

        try {
            sound = miniAudio.createSound(path, flags, group);
            sound.setVolume(volume);
            pool[slot] = sound;
            return sound;
        } catch (RuntimeException e) {
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

    private String conciseMessage(RuntimeException e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return e.getClass().getSimpleName() + " - " + message;
    }

    private static StandaloneAudio createStandaloneAudio() {
        MiniAudio engine = null;
        MAGroup group = null;
        try {
            engine = new MiniAudio();
            group = engine.createGroup((short) (
                    MASound.Flags.MA_SOUND_FLAG_NO_SPATIALIZATION
                            | MASound.Flags.MA_SOUND_FLAG_NO_PITCH), null);
            return new StandaloneAudio(engine, group);
        } catch (RuntimeException | LinkageError e) {
            if (group != null) {
                try {
                    group.dispose();
                } catch (RuntimeException ignored) {
                    // Native initialization may have failed part-way through.
                }
            }
            if (engine != null) {
                try {
                    engine.dispose();
                } catch (RuntimeException ignored) {
                    // Native initialization may have failed part-way through.
                }
            }
            String message = e.getMessage();
            if (message == null || message.isBlank()) {
                message = e.getClass().getSimpleName();
            }
            System.err.println("SoundPool warning: standalone audio disabled (" + message + ")");
            return null;
        }
    }
}
