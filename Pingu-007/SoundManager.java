
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
        DIALOGUE_QUESTION("sound/dialogue/question.wav", 3),
        // para o dialogo animal crossing
        KATAKANA_A("sound/dialogue/kata_a.wav", 3),
        KATAKANA_BA("sound/dialogue/kata_ba.wav", 3),
        KATAKANA_BE("sound/dialogue/kata_be.wav", 3),
        KATAKANA_BI("sound/dialogue/kata_bi.wav", 3),
        KATAKANA_BO("sound/dialogue/kata_bo.wav", 3),
        KATAKANA_BU("sound/dialogue/kata_bu.wav", 3),
        KATAKANA_BYA("sound/dialogue/kata_bya.wav", 3),
        KATAKANA_BYE("sound/dialogue/kata_bye.wav", 3),
        KATAKANA_BYO("sound/dialogue/kata_byo.wav", 3),
        KATAKANA_BYU("sound/dialogue/kata_byu.wav", 3),
        KATAKANA_CHA("sound/dialogue/kata_cha.wav", 3),
        KATAKANA_CHE("sound/dialogue/kata_che.wav", 3),
        KATAKANA_CHI("sound/dialogue/kata_chi.wav", 3),
        KATAKANA_CHO("sound/dialogue/kata_cho.wav", 3),
        KATAKANA_CHU("sound/dialogue/kata_chu.wav", 3),
        KATAKANA_DA("sound/dialogue/kata_da.wav", 3),
        KATAKANA_DE("sound/dialogue/kata_de.wav", 3),
        KATAKANA_DI("sound/dialogue/kata_di.wav", 3),
        KATAKANA_DO("sound/dialogue/kata_do.wav", 3),
        KATAKANA_DU("sound/dialogue/kata_du.wav", 3),
        KATAKANA_DYU("sound/dialogue/kata_dyu.wav", 3),
        KATAKANA_E("sound/dialogue/kata_e.wav", 3),
        KATAKANA_FA("sound/dialogue/kata_fa.wav", 3),
        KATAKANA_FE("sound/dialogue/kata_fe.wav", 3),
        KATAKANA_FI("sound/dialogue/kata_fi.wav", 3),
        KATAKANA_FO("sound/dialogue/kata_fo.wav", 3),
        KATAKANA_FYO("sound/dialogue/kata_fyo.wav", 3),
        KATAKANA_FYU("sound/dialogue/kata_fyu.wav", 3),
        KATAKANA_GA("sound/dialogue/kata_ga.wav", 3),
        KATAKANA_GE("sound/dialogue/kata_ge.wav", 3),
        KATAKANA_GI("sound/dialogue/kata_gi.wav", 3),
        KATAKANA_GO("sound/dialogue/kata_go.wav", 3),
        KATAKANA_GU("sound/dialogue/kata_gu.wav", 3),
        KATAKANA_GWA("sound/dialogue/kata_gwa.wav", 3),
        KATAKANA_GWE("sound/dialogue/kata_gwe.wav", 3),
        KATAKANA_GWI("sound/dialogue/kata_gwi.wav", 3),
        KATAKANA_GWO("sound/dialogue/kata_gwo.wav", 3),
        KATAKANA_GYA("sound/dialogue/kata_gya.wav", 3),
        KATAKANA_GYE("sound/dialogue/kata_gye.wav", 3),
        KATAKANA_GYO("sound/dialogue/kata_gyo.wav", 3),
        KATAKANA_GYU("sound/dialogue/kata_gyu.wav", 3),
        KATAKANA_HA("sound/dialogue/kata_ha.wav", 3),
        KATAKANA_HE("sound/dialogue/kata_he.wav", 3),
        KATAKANA_HI("sound/dialogue/kata_hi.wav", 3),
        KATAKANA_HO("sound/dialogue/kata_ho.wav", 3),
        KATAKANA_HU("sound/dialogue/kata_hu.wav", 3),
        KATAKANA_HYA("sound/dialogue/kata_hya.wav", 3),
        KATAKANA_HYE("sound/dialogue/kata_hye.wav", 3),
        KATAKANA_HYO("sound/dialogue/kata_hyo.wav", 3),
        KATAKANA_HYU("sound/dialogue/kata_hyu.wav", 3),
        KATAKANA_I("sound/dialogue/kata_i.wav", 3),
        KATAKANA_JA("sound/dialogue/kata_ja.wav", 3),
        KATAKANA_JE("sound/dialogue/kata_je.wav", 3),
        KATAKANA_JO("sound/dialogue/kata_jo.wav", 3),
        KATAKANA_JU("sound/dialogue/kata_ju.wav", 3),
        KATAKANA_KA("sound/dialogue/kata_ka.wav", 3),
        KATAKANA_KE("sound/dialogue/kata_ke.wav", 3),
        KATAKANA_KI("sound/dialogue/kata_ki.wav", 3),
        KATAKANA_KO("sound/dialogue/kata_ko.wav", 3),
        KATAKANA_KU("sound/dialogue/kata_ku.wav", 3),
        KATAKANA_KWA("sound/dialogue/kata_kwa.wav", 3),
        KATAKANA_KWE("sound/dialogue/kata_kwe.wav", 3),
        KATAKANA_KWI("sound/dialogue/kata_kwi.wav", 3),
        KATAKANA_KWO("sound/dialogue/kata_kwo.wav", 3),
        KATAKANA_KYA("sound/dialogue/kata_kya.wav", 3),
        KATAKANA_KYE("sound/dialogue/kata_kye.wav", 3),
        KATAKANA_KYO("sound/dialogue/kata_kyo.wav", 3),
        KATAKANA_KYU("sound/dialogue/kata_kyu.wav", 3),
        KATAKANA_MA("sound/dialogue/kata_ma.wav", 3),
        KATAKANA_ME("sound/dialogue/kata_me.wav", 3),
        KATAKANA_MI("sound/dialogue/kata_mi.wav", 3),
        KATAKANA_MO("sound/dialogue/kata_mo.wav", 3),
        KATAKANA_MU("sound/dialogue/kata_mu.wav", 3),
        KATAKANA_MYA("sound/dialogue/kata_mya.wav", 3),
        KATAKANA_MYE("sound/dialogue/kata_mye.wav", 3),
        KATAKANA_MYO("sound/dialogue/kata_myo.wav", 3),
        KATAKANA_MYU("sound/dialogue/kata_myu.wav", 3),
        KATAKANA_N("sound/dialogue/kata_n.wav", 3),
        KATAKANA_NA("sound/dialogue/kata_na.wav", 3),
        KATAKANA_NE("sound/dialogue/kata_ne.wav", 3),
        KATAKANA_NI("sound/dialogue/kata_ni.wav", 3),
        KATAKANA_NO("sound/dialogue/kata_no.wav", 3),
        KATAKANA_NU("sound/dialogue/kata_nu.wav", 3),
        KATAKANA_NYA("sound/dialogue/kata_nya.wav", 3),
        KATAKANA_NYE("sound/dialogue/kata_nye.wav", 3),
        KATAKANA_NYO("sound/dialogue/kata_nyo.wav", 3),
        KATAKANA_NYU("sound/dialogue/kata_nyu.wav", 3),
        KATAKANA_O("sound/dialogue/kata_o.wav", 3),
        KATAKANA_PA("sound/dialogue/kata_pa.wav", 3),
        KATAKANA_PE("sound/dialogue/kata_pe.wav", 3),
        KATAKANA_PI("sound/dialogue/kata_pi.wav", 3),
        KATAKANA_PO("sound/dialogue/kata_po.wav", 3),
        KATAKANA_PU("sound/dialogue/kata_pu.wav", 3),
        KATAKANA_PYA("sound/dialogue/kata_pya.wav", 3),
        KATAKANA_PYE("sound/dialogue/kata_pye.wav", 3),
        KATAKANA_PYO("sound/dialogue/kata_pyo.wav", 3),
        KATAKANA_PYU("sound/dialogue/kata_pyu.wav", 3),
        KATAKANA_RA("sound/dialogue/kata_ra.wav", 3),
        KATAKANA_RE("sound/dialogue/kata_re.wav", 3),
        KATAKANA_RI("sound/dialogue/kata_ri.wav", 3),
        KATAKANA_RO("sound/dialogue/kata_ro.wav", 3),
        KATAKANA_RU("sound/dialogue/kata_ru.wav", 3),
        KATAKANA_RYA("sound/dialogue/kata_rya.wav", 3),
        KATAKANA_RYE("sound/dialogue/kata_rye.wav", 3),
        KATAKANA_RYO("sound/dialogue/kata_ryo.wav", 3),
        KATAKANA_RYU("sound/dialogue/kata_ryu.wav", 3),
        KATAKANA_SA("sound/dialogue/kata_sa.wav", 3),
        KATAKANA_SE("sound/dialogue/kata_se.wav", 3),
        KATAKANA_SHA("sound/dialogue/kata_sha.wav", 3),
        KATAKANA_SHO("sound/dialogue/kata_sho.wav", 3),
        KATAKANA_SHU("sound/dialogue/kata_shu.wav", 3),
        KATAKANA_SI("sound/dialogue/kata_si.wav", 3),
        KATAKANA_SO("sound/dialogue/kata_so.wav", 3),
        KATAKANA_SU("sound/dialogue/kata_su.wav", 3),
        KATAKANA_SWI("sound/dialogue/kata_swi.wav", 3),
        KATAKANA_SYE("sound/dialogue/kata_sye.wav", 3),
        KATAKANA_TA("sound/dialogue/kata_ta.wav", 3),
        KATAKANA_TE("sound/dialogue/kata_te.wav", 3),
        KATAKANA_TI("sound/dialogue/kata_ti.wav", 3),
        KATAKANA_TO("sound/dialogue/kata_to.wav", 3),
        KATAKANA_TSA("sound/dialogue/kata_tsa.wav", 3),
        KATAKANA_TSE("sound/dialogue/kata_tse.wav", 3),
        KATAKANA_TSO("sound/dialogue/kata_tso.wav", 3),
        KATAKANA_TSU("sound/dialogue/kata_tsu.wav", 3),
        KATAKANA_TSWI("sound/dialogue/kata_tswi.wav", 3),
        KATAKANA_TU("sound/dialogue/kata_tu.wav", 3),
        KATAKANA_TYU("sound/dialogue/kata_tyu.wav", 3),
        KATAKANA_U("sound/dialogue/kata_u.wav", 3),
        KATAKANA_WA("sound/dialogue/kata_wa.wav", 3),
        KATAKANA_WE("sound/dialogue/kata_we.wav", 3),
        KATAKANA_WI("sound/dialogue/kata_wi.wav", 3),
        KATAKANA_WO("sound/dialogue/kata_wo.wav", 3),
        KATAKANA_YA("sound/dialogue/kata_ya.wav", 3),
        KATAKANA_YE("sound/dialogue/kata_ye.wav", 3),
        KATAKANA_YO("sound/dialogue/kata_yo.wav", 3),
        KATAKANA_YU("sound/dialogue/kata_yu.wav", 3),
        KATAKANA_ZA("sound/dialogue/kata_za.wav", 3),
        KATAKANA_ZE("sound/dialogue/kata_ze.wav", 3),
        KATAKANA_ZI("sound/dialogue/kata_zi.wav", 3),
        KATAKANA_ZO("sound/dialogue/kata_zo.wav", 3),
        KATAKANA_ZU("sound/dialogue/kata_zu.wav", 3),
        KATAKANA_ZWI("sound/dialogue/kata_zwi.wav", 3);

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
        loadSFX();
        setMusicVolume(musicVolume);
        setSfxVolume(sfxVolume);
    }

    public void BGMfadeOut() {
        BGMfadeOut(1000);
    }

    public void BGMfadeOut(int duration) {
        bgmPlayer.fadeOut(duration, () -> currentTrack = null);
    }

    private void loadSFX() {
        for (SFX sfx : SFX.values()) {
            sfxPools.put(sfx, new SoundPool(sfx.path, sfx.poolSize, sfxVolume));
        }
    }

    /**
     * @param sfx valor do enum SFX
     */
    public void playSFX(SFX sfx) {
        sfxPools.get(sfx).play();
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
        }
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
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float curved = volume * volume;
        float dB = (float) (Math.log10(Math.max(curved, 0.0001)) * 20);
        gain.setValue(dB);
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
