import javax.sound.sampled.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SoundManager {

    public enum BGM {
        MAIN_MENU("sound/bgm/main_menu.wav"),
        LEVEL_1("sound/bgm/level_1.wav"),
        OS_CRIA("sound/bgm/os_cria.wav");

        public final String path;

        BGM(String path) {
            this.path = path;
        }
    }

    public enum SFX {
        SNOW_STEP_1("sound/sfx/snow_footstep1.wav", 3),
        SNOW_STEP_2("sound/sfx/snow_footstep2.wav", 3),
        SNOW_STEP_3("sound/sfx/snow_footstep3.wav", 3),
        SNOW_STEP_4("sound/sfx/snow_footstep4.wav", 3),
        GUNSHOT("sound/sfx/gunshot.wav", 16),
        WOLF_DEATH("sound/sfx/wolf_death.wav", 8),
        HUD_CLICK("sound/hud/click.wav", 2);

        public final String path;
        public final int poolSize;

        SFX(String path, int poolSize) {
            this.path = path;
            this.poolSize = poolSize;
        }
    }

    private final Map<SFX, SoundPool> sfxPools = new HashMap<>();
    private final Random random = new Random();
    private Clip current_BGM = null;
    private BGM currentTrack = null;
    private float musicVolume = 0.05f;
    private float sfxVolume = 0.4f;

    public SoundManager() {
        loadSFX();
        setMusicVolume(musicVolume);
        setSfxVolume(sfxVolume);
    }

    private void loadSFX() {
        for (SFX sfx : SFX.values()) {
            sfxPools.put(sfx, new SoundPool(sfx.path, sfx.poolSize, sfxVolume));
        }
    }

    public void playSFX(SFX sfx) {
        sfxPools.get(sfx).play();
    }

    public void playRandomSnowStep() {
        SFX[] steps = { SFX.SNOW_STEP_1, SFX.SNOW_STEP_2, SFX.SNOW_STEP_3, SFX.SNOW_STEP_4 };
        playSFX(steps[random.nextInt(steps.length)]);
    }

    public void playGunshot() {
        playSFX(SFX.GUNSHOT);
    }

    public void playBGM(BGM track) {
        if (current_BGM != null) {
            current_BGM.stop();
        }
        try {
            currentTrack = track;
            current_BGM = loadClip(track.path);
            setVolume(current_BGM, musicVolume);
            current_BGM.setFramePosition(0);
            if (currentTrack != BGM.MAIN_MENU)
                current_BGM.loop(Clip.LOOP_CONTINUOUSLY);
            else
                current_BGM.loop(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopMusic() {
        if (current_BGM != null) {
            current_BGM.stop();
        }
    }

    public static void setVolume(Clip clip, float volume) {
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float dB = (float) (Math.log10(Math.max(volume, 0.0001)) * 20);
        gain.setValue(dB);
    }

    public void setMusicVolume(float volume) {
        musicVolume = volume;
        if (current_BGM != null) {
            setVolume(current_BGM, volume);
        }
    }

    public void setSfxVolume(float volume) {
        sfxVolume = volume;
        for (SoundPool pool : sfxPools.values()) {
            pool.setVolume(volume);
        }
    }

    public static Clip loadClip(String path) throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(path));
        Clip clip = AudioSystem.getClip();
        clip.open(ais);
        return clip;
    }

    public BGM currentSong() {
        return currentTrack;
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public float getSfxVolume() {
        return sfxVolume;
    }
}