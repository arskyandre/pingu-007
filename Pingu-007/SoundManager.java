
import games.rednblack.miniaudio.MAGroup;
import games.rednblack.miniaudio.MASound;
import games.rednblack.miniaudio.MiniAudio;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SoundManager {

    /**
     * @param path caminho para o arquivo de som, WAV 16-bit PCM
     * 
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
     * @param path     caminho para o arquivo de som, WAV 16-bit,nao funciona MP3
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
        KEY_SPAWN("sound/sfx/key_spawn.wav", 1),
        // GET_AMMO("sound/sfx/get_ammo.wav", 4),
        AAAHHHH("sound/sfx/AAAHHHH.wav", 4),
        CLICK("sound/sfx/click.wav", 4),
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
        DIALOGUE_SOUND_1("sound/dialogue/dialogue_sound_1.wav", 4),
        DIALOGUE_SOUND_2("sound/dialogue/dialogue_sound_2.wav", 4),
        DIALOGUE_SOUND_3("sound/dialogue/dialogue_sound_3.wav", 4),
        DIALOGUE_QUESTION("sound/dialogue/question.wav", 3),
        SCREAM("sound/sfx/chicken2.wav", 4),
        SEM_AURA("sound/sfx/sem-aura.wav", 1),
        SIX_SEVEN("sound/sfx/sixseben.wav", 1),
        HONK("sound/sfx/honk.wav", 2),
        HONK3("sound/sfx/honk3.wav", 2),
        BONK("sound/sfx/bonk.wav", 2),
        PLANE("sound/sfx/plane.wav", 2),

        // falas completas do dialogo animal crossing
        PESCADOR_FALA1_PART1_1("sound/dialogue/pescador_fala1_part1_1.wav", 1),
        PESCADOR_FALA1_PART1_2("sound/dialogue/pescador_fala1_part1_2.wav", 1),
        PESCADOR_FALA1_PART2_1("sound/dialogue/pescador_fala1_part2_1.wav", 1),
        PESCADOR_FALA1_PART2_2("sound/dialogue/pescador_fala1_part2_2.wav", 1),
        PESCADOR_FALA1_PART2_3("sound/dialogue/pescador_fala1_part2_3.wav", 1),
        PESCADOR_FALA1_PART2_4("sound/dialogue/pescador_fala1_part2_4.wav", 1),
        PESCADOR_FALA1_PART2_5("sound/dialogue/pescador_fala1_part2_5.wav", 1),
        PESCADOR_FALA1_PART2_6("sound/dialogue/pescador_fala1_part2_6.wav", 1),
        PESCADOR_FALA2_HASKEY_1("sound/dialogue/pescador_fala2_haskey_1.wav", 1),
        PESCADOR_FALA2_HASKEY_2("sound/dialogue/pescador_fala2_haskey_2.wav", 1),
        PESCADOR_FALA2_HASKEY_3("sound/dialogue/pescador_fala2_haskey_3.wav", 1),
        PESCADOR_FALA1_NOKEY_1("sound/dialogue/pescador_fala2_nokey_1.wav", 1),
        PESCADOR_FALA1_NOKEY_2("sound/dialogue/pescador_fala2_nokey_2.wav", 1),
        PESCADOR_PERGUNTA("sound/dialogue/pescador_pergunta.wav", 1),
        PORTAO_ABRIU("sound/dialogue/portao_abriu.wav", 1),
        VENDEDOR_ALGO_A_MAIS("sound/dialogue/vendedor_algo_a_mais.wav", 1),
        VENDEDOR_FALA1_1("sound/dialogue/vendedor_fala1_1.wav", 1),
        VENDEDOR_FALA1_2("sound/dialogue/vendedor_fala1_2.wav", 1),
        VENDEDOR_FALA1_3("sound/dialogue/vendedor_fala1_3.wav", 1),
        VENDEDOR_RECOMPENSA_AVISO_1("sound/dialogue/vendedor_recompensa_aviso_1.wav", 1),
        VENDEDOR_RECOMPENSA_AVISO_2("sound/dialogue/vendedor_recompensa_aviso_2.wav", 1),
        VENDEDOR_MISSAO_ATIVA("sound/dialogue/vendedor_missao_ativa.wav", 1),
        VENDEDOR_PROCURANDO_SERVICO("sound/dialogue/vendedor_procurando_servico.wav", 1),
        VENDEDOR_DAR_SINALIZADOR("sound/dialogue/vendedor_dar_sinalizador.wav", 1),
        VENDEDOR_EXPLICAR_MISSAO("sound/dialogue/vendedor_explicar_missao.wav", 1),
        VENDEDOR_MISSAO_RECOMPENSA_1("sound/dialogue/vendedor_missao_recompensa_1.wav", 1),
        VENDEDOR_MISSAO_RECOMPENSA_2("sound/dialogue/vendedor_missao_recompensa_2.wav", 1),
        VENDEDOR_INSUFICIENTE_RECOMPENSA("sound/dialogue/vendedor_insuficiente_recompensa.wav", 1),
        VENDEDOR_O_QUE_DESEJA("sound/dialogue/vendedor_o_que_deseja.wav", 1),
        VENDEDOR_TCHAU("sound/dialogue/vendedor_tchau.wav", 1);

        public final String path;
        public final int poolSize;

        SFX(String path, int poolSize) {
            this.path = path;
            this.poolSize = poolSize;
        }
    }

    private final Map<SFX, SoundPool> sfxPools = new HashMap<>();
    private final Random random = new Random();
    private final MiniAudio miniAudio;
    private final MAGroup musicGroup;
    private final MAGroup sfxGroup;
    private final MAGroup dialogueGroup;
    private final BGMPlayer bgmPlayer;
    private BGM currentTrack = null;
    private float musicVolume = 0.45f;
    private float sfxVolume = 0.5f;
    private double spatialViewLeft;
    private double spatialViewWidth;

    private MASound dialogueSound;
    private boolean disposed;

    public SoundManager() {
        MiniAudio engine = null;
        MAGroup music = null;
        MAGroup sfx = null;
        MAGroup dialogue = null;
        try {
            engine = new MiniAudio();
            music = engine.createGroup((short) (
                    MASound.Flags.MA_SOUND_FLAG_NO_SPATIALIZATION
                            | MASound.Flags.MA_SOUND_FLAG_NO_PITCH), null);
            sfx = engine.createGroup((short) (
                    MASound.Flags.MA_SOUND_FLAG_NO_SPATIALIZATION
                            | MASound.Flags.MA_SOUND_FLAG_NO_PITCH), null);
            dialogue = engine.createGroup((short) (
                    MASound.Flags.MA_SOUND_FLAG_NO_SPATIALIZATION
                            | MASound.Flags.MA_SOUND_FLAG_NO_PITCH), null);
        } catch (RuntimeException | LinkageError e) {
            // Audio is optional for headless machines and CI; game logic still runs.
            warnAudioUnavailable(e);
            disposeGroup(dialogue);
            disposeGroup(sfx);
            disposeGroup(music);
            if (engine != null) {
                try {
                    engine.dispose();
                } catch (RuntimeException ignored) {
                    // Native initialization may have failed before full setup.
                }
            }
            engine = null;
            music = null;
            sfx = null;
            dialogue = null;
        }

        miniAudio = engine;
        musicGroup = music;
        sfxGroup = sfx;
        dialogueGroup = dialogue;
        bgmPlayer = new BGMPlayer(engine, music);
        loadSFX();
        if (musicGroup != null) {
            musicGroup.setVolume(1f);
        }
        if (sfxGroup != null) {
            sfxGroup.setVolume(1f);
        }
        if (dialogueGroup != null) {
            dialogueGroup.setVolume(1f);
        }
        setMusicVolume(musicVolume);
        setSfxVolume(sfxVolume);
    }

    public void BGMfadeOut() {
        BGMfadeOut(1000);
    }

    public void BGMfadeOut(int duration) {
        bgmPlayer.fadeOut(duration, () -> {
            if (!bgmPlayer.isPlaying()) {
                currentTrack = null;
            }
        });
    }

    private void loadSFX() {
        for (SFX sfx : SFX.values()) {
            boolean dialogue = sfx.path.startsWith("sound/dialogue/");
            MAGroup group = dialogue ? dialogueGroup : sfxGroup;
            short flags = dialogue
                    ? (short) (MASound.Flags.MA_SOUND_FLAG_STREAM
                            | MASound.Flags.MA_SOUND_FLAG_NO_SPATIALIZATION
                            | MASound.Flags.MA_SOUND_FLAG_NO_PITCH)
                    : (short) (MASound.Flags.MA_SOUND_FLAG_DECODE
                            | MASound.Flags.MA_SOUND_FLAG_NO_SPATIALIZATION
                            | MASound.Flags.MA_SOUND_FLAG_NO_PITCH);
            sfxPools.put(sfx, new SoundPool(
                    miniAudio, sfx.path, sfx.poolSize, sfxVolume, group, flags));
        }
    }

    /**
     * @param sfx valor do enum SFX
     */
    public void playSFX(SFX sfx) {
        if (disposed || sfx == null) {
            return;
        }
        SoundPool pool = sfxPools.get(sfx);
        if (pool != null) {
            pool.play();
        }
    }

    /**
     * Reproduz um efeito com pan estereo: -1 esquerda, 0 centro, 1 direita.
     */
    public void playSpatialSFX(SFX sfx, float pan) {
        if (disposed || sfx == null) {
            return;
        }
        SoundPool pool = sfxPools.get(sfx);
        if (pool != null) {
            pool.play(pan);
        }
    }

    /** Reproduz um efeito usando a posicao horizontal da fonte no mundo. */
    public void playSpatialSFX(SFX sfx, double sourceWorldX) {
        float pan = 0f;
        if (Double.isFinite(sourceWorldX) && spatialViewWidth > 0.0) {
            double viewCenter = spatialViewLeft + spatialViewWidth / 2.0;
            pan = (float) Math.clamp(
                    (sourceWorldX - viewCenter) / (spatialViewWidth / 2.0),
                    -1.0,
                    1.0);
        }
        playSpatialSFX(sfx, pan);
    }

    public void setSpatialViewport(double viewLeft, double viewWidth) {
        if (Double.isFinite(viewLeft) && Double.isFinite(viewWidth) && viewWidth > 0.0) {
            spatialViewLeft = viewLeft;
            spatialViewWidth = viewWidth;
        }
    }

    /** Alias mantido para o nome solicitado. */
    public void playSpatialSFXX(SFX sfx, float pan) {
        playSpatialSFX(sfx, pan);
    }

    public void playDialogue(SFX som) {
        stopDialogue(); // corta a fala anterior antes de comecar a nova

        if (!disposed && som != null) {
            SoundPool pool = sfxPools.get(som);
            if (pool != null) {
                dialogueSound = pool.play();
            }
        }
    }

    public void stopDialogue() {
        if (dialogueSound != null) {
            try {
                dialogueSound.stop();
            } catch (RuntimeException ignored) {
                // The native engine may already be stopping.
            }
            dialogueSound = null;
        }
    }

    public void playRandomSnowStep() {
        SFX[] steps = { SFX.SNOW_STEP_1, SFX.SNOW_STEP_2, SFX.SNOW_STEP_3, SFX.SNOW_STEP_4 };
        playSFX(steps[random.nextInt(steps.length)]);
    }

    public void playRandomSnowStep(double sourceWorldX) {
        SFX[] steps = { SFX.SNOW_STEP_1, SFX.SNOW_STEP_2, SFX.SNOW_STEP_3, SFX.SNOW_STEP_4 };
        playSpatialSFX(steps[random.nextInt(steps.length)], sourceWorldX);
    }

    public void playRandomDialogueSound() {
        SFX[] sounds = { SFX.DIALOGUE_SOUND_1, SFX.DIALOGUE_SOUND_2, SFX.DIALOGUE_SOUND_3 };
        playSFX(sounds[random.nextInt(sounds.length)]);
    }

    public void playRandomIceStep() {
        SFX[] steps = { SFX.ICE_STEP_1, SFX.ICE_STEP_2 };
        playSFX(steps[random.nextInt(steps.length)]);
    }

    public void playRandomIceStep(double sourceWorldX) {
        SFX[] steps = { SFX.ICE_STEP_1, SFX.ICE_STEP_2 };
        playSpatialSFX(steps[random.nextInt(steps.length)], sourceWorldX);
    }

    public void playGunshot() {
        playSFX(SFX.GUNSHOT);
    }

    public void playGunshot(double sourceWorldX) {
        playSpatialSFX(SFX.GUNSHOT, sourceWorldX);
    }

    public void playBGM(BGM track) {
        if (disposed || track == null) {
            return;
        }
        currentTrack = track;
        bgmPlayer.play(track.path);
        bgmPlayer.setVolume(musicVolume);
    }

    public void playBGM(BGM intro, BGM loop) {
        if (disposed || intro == null || loop == null) {
            return;
        }
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
        if (disposed || track == null) {
            return;
        }
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

        if (disposed || track == null) {
            return;
        }
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

        if (disposed || track == null) {
            return;
        }
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

        if (disposed || intro == null || loop == null) {
            return;
        }
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

        if (disposed || intro == null || loop == null) {
            return;
        }
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

        if (disposed || intro == null || loop == null) {
            return;
        }
        bgmPlayer.crossfadeToIntroThenLoop(
                intro.path,
                loop.path,
                musicVolume,
                durationMs,
                delay,
                fade_in,
                timestampInicial);
        // Delayed transitions are intentionally considered current as soon as
        // they are scheduled; the target voice starts after the requested delay.
        currentTrack = loop;
    }

    public void stopMusic() {
        bgmPlayer.stop();
        currentTrack = null;
    }

    public static void setVolume(MASound sound, float volume) {
        if (sound == null) {
            return;
        }
        try {
            sound.setVolume(Math.max(0f, volume));
        } catch (RuntimeException ignored) {
            // The native engine may be unavailable during shutdown.
        }
    }

    public void setMusicVolume(float volume) {
        musicVolume = Math.max(0f, volume);
        // Keep the routing group at unity; the per-voice volume is the public
        // music-volume control, avoiding accidental double attenuation.
        bgmPlayer.setVolume(volume);
    }

    public void setSfxVolume(float volume) {
        sfxVolume = Math.max(0f, volume);
        for (SoundPool pool : sfxPools.values()) {
            if (pool != null) {
                pool.setVolume(volume);
            }
        }
    }

    /**
     * Releases voices/pools/groups before the shared native MiniAudio engine.
     * Calling dispose more than once is safe.
     */
    public synchronized void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        stopDialogue();
        bgmPlayer.dispose();
        for (SoundPool pool : sfxPools.values()) {
            if (pool != null) {
                pool.dispose();
            }
        }
        sfxPools.clear();
        disposeGroup(dialogueGroup);
        disposeGroup(sfxGroup);
        disposeGroup(musicGroup);
        if (miniAudio != null) {
            miniAudio.dispose();
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

    private static void disposeGroup(MAGroup group) {
        if (group != null) {
            try {
                group.dispose();
            } catch (RuntimeException ignored) {
                // Best effort while unwinding failed/native shutdown.
            }
        }
    }

    private static void warnAudioUnavailable(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getClass().getSimpleName();
        }
        System.err.println("SoundManager warning: audio disabled (" + message + ")");
    }
}
