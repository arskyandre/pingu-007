
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.sound.sampled.*;

public class SoundManager {

    /**
     * @param path caminho para o arquivo de som, WAV 16-bit PCM
     */
    public enum BGM {
        MAIN_MENU("sound/bgm/main_menu.wav"),
        LEVEL_1_DAY_INTRO("sound/bgm/level_1_day_intro.wav"),
        LEVEL_1_DAY_LOOP("sound/bgm/level_1_day_loop.wav"),
        LEVEL_1_NIGHT_INTRO("sound/bgm/level_1_night_intro.wav"),
        LEVEL_1_NIGHT_LOOP("sound/bgm/level_1_night_loop.wav"),
        ARENA_INTRO("sound/bgm/arena_intro.wav"),
        ARENA_LOOP("sound/bgm/arena_loop.wav"),
        INSIDE_INTRO("sound/bgm/inside_intro.wav"),
        INSIDE_LOOP("sound/bgm/inside_loop.wav"),
        BOSS_INTRO("sound/bgm/boss_intro.wav"),
        BOSS_LOOP("sound/bgm/boss_loop.wav");

        public final String path;

        BGM(String path) {
            this.path = path;
        }
    }

    /**
     * @param path     caminho para o arquivo de som, WAV 16-bit PCM nao funciona
     *                 mp3
     * @param poolSize quantidade maxima de copias simultaneas desse som(quantas
     *                 explosoes podem tocar ao mesmo tempo, por exemplo)
     */
    public enum SFX {
        CALL_RING("sound/sfx/call_ring.wav", 2),
        NOOT_NOOT("sound/sfx/noot_noot.wav", 3),
        SNOW_STEP_1("sound/sfx/snow_footstep1.wav", 6),
        SNOW_STEP_2("sound/sfx/snow_footstep2.wav", 6),
        SNOW_STEP_3("sound/sfx/snow_footstep3.wav", 6),
        SNOW_STEP_4("sound/sfx/snow_footstep4.wav", 6),
        ICE_STEP_1("sound/sfx/ice_footstep1.wav", 6),
        ICE_STEP_2("sound/sfx/ice_footstep2.wav", 6),
        ARENA_ENTER("sound/sfx/arena_enter.wav", 2),
        PLAYER_DAMAGE("sound/sfx/player_damage.wav", 8),
        PLAYER_HEAL("sound/sfx/player_heal.wav", 4),
        KEY_SPAWN("sound/sfx/key_spawn.wav",1),
        // GET_AMMO("sound/sfx/get_ammo.wav", 4),
        LINE_CAST("sound/sfx/line_cast.wav", 3),
        SPLASH("sound/sfx/splash.wav", 3),
        // FISHING_FISH_FOUND("sound/sfx/fishing_fish_found.wav", 2),
        // FISHING_CAUGHT("sound/sfx/fishing_caught.wav", 2),
        // FISHING_LOST("sound/sfx/fishing_lost.wav", 2),
        GUNSHOT("sound/sfx/gunshot.wav", 16),
        BOMBER_AVISO("sound/sfx/bomber_aviso.wav", 6),
        EXPLOSION("sound/sfx/bomber_explosion.wav", 6),
        SHOOTER_METRALHADA("sound/sfx/shooter_metralhada.wav", 8),
        WOLF_DEATH("sound/sfx/wolf_death.wav", 8),
        MORSA_ROAR("sound/sfx/morsa_roar.wav", 2),
        HUD_CLICK("sound/hud/click.wav", 4),
        DIALOGUE_SOUND_1("sound/dialogue/dialogue_sound_1.wav", 9),
        DIALOGUE_SOUND_2("sound/dialogue/dialogue_sound_2.wav", 9),
        DIALOGUE_SOUND_3("sound/dialogue/dialogue_sound_3.wav", 9),
        DIALOGUE_QUESTION("sound/dialogue/question.wav", 3);
        // para o dialogo animal crossing

        public final String path;
        public final int poolSize;

        SFX(String path, int poolSize) {
            this.path = path;
            this.poolSize = poolSize;
        }
    }

    private final Map<SFX, SoundPool> sfxPools = new HashMap<>();
    private final Random random = new Random();
    private final BGMPlayer bgmPlayer = new BGMPlayer();
    private BGM currentTrack = null;
    // private float musicVolume = 0f;
    private float musicVolume = 0.45f;
    private float sfxVolume = 0.5f;

    private Thread dialogueThread;
    private volatile boolean dialogueAtiva = false;
    private static final long INTERVALO_SILABA_MS = 100;

    public SoundManager() {
        // ALTERADO (Lazy Loading):
        // Não abre dezenas de Clips durante a construção do GameCore.
        // Cada SFX cria seu SoundPool somente quando for usado pela primeira vez.
        setMusicVolume(musicVolume);
        setSfxVolume(sfxVolume);
    }

    public void BGMfadeOut() {
        BGMfadeOut(1000);
    }

    public void BGMfadeOut(int duration) {
        bgmPlayer.fadeOut(duration, () -> currentTrack = null);
    }

    // ALTERADO (Katakana assíncrono):
    // O carregamento/conversão/Clip.open() não acontece mais na thread do jogo.
    // Se várias letras chegarem enquanto o áudio está ocupado, guardamos apenas
    // a mais recente para não criar uma fila de sons atrasados.
    private Clip katanaClip;
    private final Object katanaLock = new Object();
    private String katanaPendente;
    private Thread katanaThread;

    public void playKatana(String silaba) {
        if (silaba == null || silaba.isBlank()) {
            return;
        }

        String normalizada = silaba.toLowerCase();
        String fileName = "sound/dialogue/kata_" + normalizada + ".wav";

        if (!new File(fileName).isFile()) {
            return;
        }

        synchronized (katanaLock) {
            katanaPendente = normalizada;

            if (katanaThread == null || !katanaThread.isAlive()) {
                katanaThread = new Thread(this::processarKatana, "KatakanaAudioThread");
                katanaThread.setDaemon(true);
                katanaThread.start();
            }

            katanaLock.notifyAll();
        }
    }

    private void processarKatana() {
        while (true) {
            String silaba;

            synchronized (katanaLock) {
                while (katanaPendente == null) {
                    try {
                        katanaLock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                silaba = katanaPendente;
                katanaPendente = null;
            }

            tocarKatanaInterno(silaba);
        }
    }

    private void tocarKatanaInterno(String silaba) {
        String fileName = "sound/dialogue/kata_" + silaba + ".wav";
        File file = new File(fileName);

        try {
            fecharKatanaClip();

            try (AudioInputStream original = AudioSystem.getAudioInputStream(file)) {
                AudioFormat sourceFormat = original.getFormat();

                int channels = Math.max(1, sourceFormat.getChannels());
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        44100.0f,
                        16,
                        channels,
                        channels * 2,
                        44100.0f,
                        false);

                AudioInputStream pcmStream = original;

                if (!sourceFormat.matches(targetFormat)) {
                    if (!AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
                        return;
                    }

                    pcmStream = AudioSystem.getAudioInputStream(targetFormat, original);
                }

                try (AudioInputStream converted = pcmStream) {
                    Clip novoClip = AudioSystem.getClip();
                    novoClip.open(converted);
                    setVolume(novoClip, sfxVolume);

                    synchronized (katanaLock) {
                        katanaClip = novoClip;
                    }

                    novoClip.setFramePosition(0);
                    novoClip.start();
                }
            }
        } catch (Exception e) {
            fecharKatanaClip();
            System.err.println("Erro ao tocar Katakana [" + fileName + "]: " + e.getMessage());
        }
    }

    private void fecharKatanaClip() {
        Clip clip;

        synchronized (katanaLock) {
            clip = katanaClip;
            katanaClip = null;
        }

        if (clip == null) {
            return;
        }

        try {
            clip.stop();
            clip.flush();
            clip.close();
        } catch (Exception ignored) {
        }
    }

    /**
     * @param sfx valor do enum SFX
     */
    public synchronized void playSFX(SFX sfx) {
      if (sfx == null) {
        return;
      }

      SoundPool pool = sfxPools.get(sfx);

      if (pool == null) {
        // ALTERADO (Lazy Loading):
        // O pool só passa a existir quando este SFX é realmente solicitado.
        pool = new SoundPool(sfx.path, sfx.poolSize, sfxVolume);
        sfxPools.put(sfx, pool);
      }

      pool.play();
    }

    public void playDialogue(SFX[] sons) {
        stopDialogue(); // corta a fala anterior antes de comecar a nova

        if (sons == null) {
            return;
        }

        dialogueAtiva = true;
        dialogueThread = new Thread(() -> {
            try {
                for (SFX som : sons) {
                    if (!dialogueAtiva) {
                        return;
                    }
                    if (som != null) {
                        playSFX(som);
                    }
                    Thread.sleep(INTERVALO_SILABA_MS);
                }
            } catch (InterruptedException ignored) {
            }
        }, "DialogueSoundThread");
        dialogueThread.setDaemon(true);
        dialogueThread.start();
    }

    public void stopDialogue() {
        dialogueAtiva = false;
        if (dialogueThread != null) {
            dialogueThread.interrupt();
            dialogueThread = null;
        }

        synchronized (katanaLock) {
            katanaPendente = null;
        }

        fecharKatanaClip();
    }

    public void playRandomSnowStep() {
        SFX[] steps = { SFX.SNOW_STEP_1, SFX.SNOW_STEP_2, SFX.SNOW_STEP_3, SFX.SNOW_STEP_4 };
        playSFX(steps[random.nextInt(steps.length)]);
    }

    public void playRandomDialogueSound() {
        SFX[] sounds = { SFX.DIALOGUE_SOUND_1, SFX.DIALOGUE_SOUND_2, SFX.DIALOGUE_SOUND_3 };
        playSFX(sounds[random.nextInt(sounds.length)]);
    }

    public void playRandomIceStep() {
        SFX[] steps = { SFX.ICE_STEP_1, SFX.ICE_STEP_2 };
        playSFX(steps[random.nextInt(steps.length)]);
    }

    public void playGunshot() {
        playSFX(SFX.GUNSHOT);
    }

    public void playBGM(BGM track) {
        currentTrack = track;
        bgmPlayer.play(track.path);
        bgmPlayer.setVolume(musicVolume);
    }

    public void playBGM(BGM intro, BGM loop) {
        currentTrack = loop;
        bgmPlayer.playIntroThenLoop(intro.path, loop.path);
        bgmPlayer.setVolume(musicVolume);
    }

    public void crossfadeBGM(BGM track) {
        crossfadeBGM(track, 1000, true);
    }

    public void crossfadeBGM(BGM track, boolean fade_in) {
        crossfadeBGM(track, 1000, fade_in);
    }

    public void crossfadeBGM(BGM track, long durationMs) {
        crossfadeBGM(track, durationMs, true);
    }

    public void crossfadeBGM(BGM track, long durationMs, boolean fade_in) {
        currentTrack = track;
        bgmPlayer.crossfadeTo(
                track.path,
                musicVolume,
                durationMs,
                fade_in);
    }

    public void crossfadeBGM(BGM track, double delay) {
        crossfadeBGM(track, 1000, delay, false);
    }

    public void crossfadeBGM(BGM track, double delay, boolean fade_in) {
        crossfadeBGM(track, 1000, delay, fade_in);
    }

    public void crossfadeBGM(BGM track, long durationMs, double delay) {
        crossfadeBGM(track, durationMs, delay, false);
    }

    public void crossfadeBGM(
            BGM track,
            long durationMs,
            double delay,
            boolean fade_in) {

        currentTrack = track;
        bgmPlayer.crossfadeTo(
                track.path,
                musicVolume,
                durationMs,
                delay,
                fade_in);
    }

    public void crossfadeBGM(
            BGM track,
            long durationMs,
            double delay,
            double timestampInicial) {

        crossfadeBGM(
                track,
                durationMs,
                delay,
                false,
                timestampInicial);
    }

    public void crossfadeBGM(
            BGM track,
            long durationMs,
            double delay,
            boolean fade_in,
            double timestampInicial) {

        currentTrack = track;
        bgmPlayer.crossfadeTo(
                track.path,
                musicVolume,
                durationMs,
                delay,
                fade_in,
                timestampInicial);
    }

    public void crossfadeBGM(BGM intro, BGM loop) {
        crossfadeBGM(intro, loop, 1000, true);
    }

    public void crossfadeBGM(BGM intro, BGM loop, boolean fade_in) {
        crossfadeBGM(intro, loop, 1000, fade_in);
    }

    public void crossfadeBGM(BGM intro, BGM loop, long durationMs) {
        crossfadeBGM(intro, loop, durationMs, true);
    }

    public void crossfadeBGM(
            BGM intro,
            BGM loop,
            long durationMs,
            boolean fade_in) {

        currentTrack = loop;
        bgmPlayer.crossfadeToIntroThenLoop(
                intro.path,
                loop.path,
                musicVolume,
                durationMs,
                fade_in);
    }

    public void crossfadeBGM(BGM intro, BGM loop, double delay) {
        crossfadeBGM(intro, loop, 1000, delay, false);
    }

    public void crossfadeBGM(
            BGM intro,
            BGM loop,
            double delay,
            boolean fade_in) {

        crossfadeBGM(intro, loop, 1000, delay, fade_in);
    }

    public void crossfadeBGM(
            BGM intro,
            BGM loop,
            long durationMs,
            double delay) {

        crossfadeBGM(intro, loop, durationMs, delay, false);
    }

    public void crossfadeBGM(
            BGM intro,
            BGM loop,
            long durationMs,
            double delay,
            boolean fade_in) {

        currentTrack = loop;
        bgmPlayer.crossfadeToIntroThenLoop(
                intro.path,
                loop.path,
                musicVolume,
                durationMs,
                delay,
                fade_in);
    }

    public void crossfadeBGM(
            BGM intro,
            BGM loop,
            double delay,
            double timestampInicial) {

        crossfadeBGM(
                intro,
                loop,
                1000,
                delay,
                false,
                timestampInicial);
    }

    public void crossfadeBGM(
            BGM intro,
            BGM loop,
            long durationMs,
            double delay,
            double timestampInicial) {

        crossfadeBGM(
                intro,
                loop,
                durationMs,
                delay,
                false,
                timestampInicial);
    }

    public void crossfadeBGM(
            BGM intro,
            BGM loop,
            long durationMs,
            double delay,
            boolean fade_in,
            double timestampInicial) {

        bgmPlayer.crossfadeToIntroThenLoop(
                intro.path,
                loop.path,
                musicVolume,
                durationMs,
                delay,
                fade_in,
                timestampInicial);

        currentTrack = bgmPlayer.isPlaying() ? loop : null;
    }

    public void stopMusic() {
        bgmPlayer.stop();
        currentTrack = null;
    }

    public static void setVolume(Clip clip, float volume) {
      if(clip == null || !clip.isOpen())return;

      if(clip.isControlSupported(FloatControl.Type.MASTER_GAIN)){
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float curved = volume * volume;
        float dB = (float) (Math.log10(Math.max(curved, 0.0001)) * 20);
        gain.setValue(dB);
      }
    }

    public void setMusicVolume(float volume) {
        musicVolume = volume;
        bgmPlayer.setVolume(volume);
    }

    public void setSfxVolume(float volume) {
        sfxVolume = volume;
        for (SoundPool pool : sfxPools.values()) {
            pool.setVolume(volume);
        }

        // ALTERADO (Linux/Katakana): atualiza também a voz em execução.
        if (katanaClip != null && katanaClip.isOpen()) {
            setVolume(katanaClip, volume);
        }
    }

    public static Clip loadClip(String path) throws Exception {
      File file = new File(path);
      if(!file.exists()) return null;

      try (AudioInputStream original = AudioSystem.getAudioInputStream(file)) {
        AudioFormat sourceFormat = original.getFormat();

        int channels = Math.max(1, sourceFormat.getChannels());
        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100.0f,
                16,
                channels,
                channels * 2,
                44100.0f,
                false);

        AudioInputStream pcmStream = original;

        if (!sourceFormat.matches(targetFormat)) {
          if (!AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
            System.err.println("Formato de audio nao suportado [" + path + "]: " + sourceFormat);
            return null;
          }

          pcmStream = AudioSystem.getAudioInputStream(targetFormat, original);
        }

        try (AudioInputStream converted = pcmStream) {
          Clip clip = AudioSystem.getClip();
          clip.open(converted);
          return clip;
        }
      } catch(Exception e){
        System.err.println("Erro ao carregar audio [" + path + "]: " + e.getMessage());
        return null;
      }
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
