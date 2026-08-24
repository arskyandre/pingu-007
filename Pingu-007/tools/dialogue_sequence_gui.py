"""Export a Java ``SoundManager.SFX[]`` dialogue sequence as a WAV file.

The tool intentionally mirrors ``SoundManager.playDialogue``: every entry starts
100 ms after the previous one, clips are allowed to overlap, and ``null`` takes
one timing slot without starting a sound.

Run from any directory with::

    python tools/dialogue_sequence_gui.py

The GUI and WAV mixer use only the Python standard library. Non-zero pitch uses
an FFmpeg build that includes the Rubber Band filter.
"""

from __future__ import annotations

import math
import os
import random
import re
import shutil
import subprocess
import sys
import tempfile
import wave
from array import array
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


OUTPUT_CHANNELS = 2
OUTPUT_SAMPLE_RATE = 48_000
OUTPUT_SAMPLE_WIDTH = 2
DEFAULT_INTERVAL_MS = 100
MIN_PITCH_SEMITONES = -12.0
MAX_PITCH_SEMITONES = 12.0
RADIO_PADDING_SECONDS = 0.3
BASE_HISS_LEVEL = 0.067
VOLUME_SLIDER_UNITY = 50.0
MAX_VOLUME_GAIN = 2.0

PROJECT_DIR = Path(__file__).resolve().parent.parent

# Kept here deliberately instead of being read from SoundManager.java. These
# legacy names remain valid input even after their Java enum entries are removed.
SFX_PATHS: dict[str, str] = {
    "CALL_RING": "sound/sfx/call_ring.wav",
    "NOOT_NOOT": "sound/sfx/noot_noot.wav",
    "SNOW_STEP_1": "sound/sfx/snow_footstep1.wav",
    "SNOW_STEP_2": "sound/sfx/snow_footstep2.wav",
    "SNOW_STEP_3": "sound/sfx/snow_footstep3.wav",
    "SNOW_STEP_4": "sound/sfx/snow_footstep4.wav",
    "ICE_STEP_1": "sound/sfx/ice_footstep1.wav",
    "ICE_STEP_2": "sound/sfx/ice_footstep2.wav",
    "ARENA_ENTER": "sound/sfx/arena_enter.wav",
    "PLAYER_DAMAGE": "sound/sfx/player_damage.wav",
    "PLAYER_HEAL": "sound/sfx/player_heal.wav",
    "KEY_SPAWN": "sound/sfx/key_spawn.wav",
    "LINE_CAST": "sound/sfx/line_cast.wav",
    "SPLASH": "sound/sfx/splash.wav",
    "GUNSHOT": "sound/sfx/gunshot.wav",
    "BOMBER_AVISO": "sound/sfx/bomber_aviso.wav",
    "EXPLOSION": "sound/sfx/bomber_explosion.wav",
    "SHOOTER_METRALHADA": "sound/sfx/shooter_metralhada.wav",
    "WOLF_DEATH": "sound/sfx/wolf_death.wav",
    "MORSA_ROAR": "sound/sfx/morsa_roar.wav",
    "HUD_CLICK": "sound/hud/click.wav",
    "DIALOGUE_SOUND_1": "sound/dialogue/dialogue_sound_1.wav",
    "DIALOGUE_SOUND_2": "sound/dialogue/dialogue_sound_2.wav",
    "DIALOGUE_SOUND_3": "sound/dialogue/dialogue_sound_3.wav",
    "DIALOGUE_QUESTION": "sound/dialogue/question.wav",
    "KATAKANA_A": "sound/dialogue/kata_a.wav",
    "KATAKANA_BA": "sound/dialogue/kata_ba.wav",
    "KATAKANA_BE": "sound/dialogue/kata_be.wav",
    "KATAKANA_BI": "sound/dialogue/kata_bi.wav",
    "KATAKANA_BO": "sound/dialogue/kata_bo.wav",
    "KATAKANA_BU": "sound/dialogue/kata_bu.wav",
    "KATAKANA_BYA": "sound/dialogue/kata_bya.wav",
    "KATAKANA_BYE": "sound/dialogue/kata_bye.wav",
    "KATAKANA_BYO": "sound/dialogue/kata_byo.wav",
    "KATAKANA_BYU": "sound/dialogue/kata_byu.wav",
    "KATAKANA_CHA": "sound/dialogue/kata_cha.wav",
    "KATAKANA_CHE": "sound/dialogue/kata_che.wav",
    "KATAKANA_CHI": "sound/dialogue/kata_chi.wav",
    "KATAKANA_CHO": "sound/dialogue/kata_cho.wav",
    "KATAKANA_CHU": "sound/dialogue/kata_chu.wav",
    "KATAKANA_DA": "sound/dialogue/kata_da.wav",
    "KATAKANA_DE": "sound/dialogue/kata_de.wav",
    "KATAKANA_DI": "sound/dialogue/kata_di.wav",
    "KATAKANA_DO": "sound/dialogue/kata_do.wav",
    "KATAKANA_DU": "sound/dialogue/kata_du.wav",
    "KATAKANA_DYU": "sound/dialogue/kata_dyu.wav",
    "KATAKANA_E": "sound/dialogue/kata_e.wav",
    "KATAKANA_FA": "sound/dialogue/kata_fa.wav",
    "KATAKANA_FE": "sound/dialogue/kata_fe.wav",
    "KATAKANA_FI": "sound/dialogue/kata_fi.wav",
    "KATAKANA_FO": "sound/dialogue/kata_fo.wav",
    "KATAKANA_FYO": "sound/dialogue/kata_fyo.wav",
    "KATAKANA_FYU": "sound/dialogue/kata_fyu.wav",
    "KATAKANA_GA": "sound/dialogue/kata_ga.wav",
    "KATAKANA_GE": "sound/dialogue/kata_ge.wav",
    "KATAKANA_GI": "sound/dialogue/kata_gi.wav",
    "KATAKANA_GO": "sound/dialogue/kata_go.wav",
    "KATAKANA_GU": "sound/dialogue/kata_gu.wav",
    "KATAKANA_GWA": "sound/dialogue/kata_gwa.wav",
    "KATAKANA_GWE": "sound/dialogue/kata_gwe.wav",
    "KATAKANA_GWI": "sound/dialogue/kata_gwi.wav",
    "KATAKANA_GWO": "sound/dialogue/kata_gwo.wav",
    "KATAKANA_GYA": "sound/dialogue/kata_gya.wav",
    "KATAKANA_GYE": "sound/dialogue/kata_gye.wav",
    "KATAKANA_GYO": "sound/dialogue/kata_gyo.wav",
    "KATAKANA_GYU": "sound/dialogue/kata_gyu.wav",
    "KATAKANA_HA": "sound/dialogue/kata_ha.wav",
    "KATAKANA_HE": "sound/dialogue/kata_he.wav",
    "KATAKANA_HI": "sound/dialogue/kata_hi.wav",
    "KATAKANA_HO": "sound/dialogue/kata_ho.wav",
    "KATAKANA_HU": "sound/dialogue/kata_hu.wav",
    "KATAKANA_HYA": "sound/dialogue/kata_hya.wav",
    "KATAKANA_HYE": "sound/dialogue/kata_hye.wav",
    "KATAKANA_HYO": "sound/dialogue/kata_hyo.wav",
    "KATAKANA_HYU": "sound/dialogue/kata_hyu.wav",
    "KATAKANA_I": "sound/dialogue/kata_i.wav",
    "KATAKANA_JA": "sound/dialogue/kata_ja.wav",
    "KATAKANA_JE": "sound/dialogue/kata_je.wav",
    "KATAKANA_JO": "sound/dialogue/kata_jo.wav",
    "KATAKANA_JU": "sound/dialogue/kata_ju.wav",
    "KATAKANA_KA": "sound/dialogue/kata_ka.wav",
    "KATAKANA_KE": "sound/dialogue/kata_ke.wav",
    "KATAKANA_KI": "sound/dialogue/kata_ki.wav",
    "KATAKANA_KO": "sound/dialogue/kata_ko.wav",
    "KATAKANA_KU": "sound/dialogue/kata_ku.wav",
    "KATAKANA_KWA": "sound/dialogue/kata_kwa.wav",
    "KATAKANA_KWE": "sound/dialogue/kata_kwe.wav",
    "KATAKANA_KWI": "sound/dialogue/kata_kwi.wav",
    "KATAKANA_KWO": "sound/dialogue/kata_kwo.wav",
    "KATAKANA_KYA": "sound/dialogue/kata_kya.wav",
    "KATAKANA_KYE": "sound/dialogue/kata_kye.wav",
    "KATAKANA_KYO": "sound/dialogue/kata_kyo.wav",
    "KATAKANA_KYU": "sound/dialogue/kata_kyu.wav",
    "KATAKANA_MA": "sound/dialogue/kata_ma.wav",
    "KATAKANA_ME": "sound/dialogue/kata_me.wav",
    "KATAKANA_MI": "sound/dialogue/kata_mi.wav",
    "KATAKANA_MO": "sound/dialogue/kata_mo.wav",
    "KATAKANA_MU": "sound/dialogue/kata_mu.wav",
    "KATAKANA_MYA": "sound/dialogue/kata_mya.wav",
    "KATAKANA_MYE": "sound/dialogue/kata_mye.wav",
    "KATAKANA_MYO": "sound/dialogue/kata_myo.wav",
    "KATAKANA_MYU": "sound/dialogue/kata_myu.wav",
    "KATAKANA_N": "sound/dialogue/kata_n.wav",
    "KATAKANA_NA": "sound/dialogue/kata_na.wav",
    "KATAKANA_NE": "sound/dialogue/kata_ne.wav",
    "KATAKANA_NI": "sound/dialogue/kata_ni.wav",
    "KATAKANA_NO": "sound/dialogue/kata_no.wav",
    "KATAKANA_NU": "sound/dialogue/kata_nu.wav",
    "KATAKANA_NYA": "sound/dialogue/kata_nya.wav",
    "KATAKANA_NYE": "sound/dialogue/kata_nye.wav",
    "KATAKANA_NYO": "sound/dialogue/kata_nyo.wav",
    "KATAKANA_NYU": "sound/dialogue/kata_nyu.wav",
    "KATAKANA_O": "sound/dialogue/kata_o.wav",
    "KATAKANA_PA": "sound/dialogue/kata_pa.wav",
    "KATAKANA_PE": "sound/dialogue/kata_pe.wav",
    "KATAKANA_PI": "sound/dialogue/kata_pi.wav",
    "KATAKANA_PO": "sound/dialogue/kata_po.wav",
    "KATAKANA_PU": "sound/dialogue/kata_pu.wav",
    "KATAKANA_PYA": "sound/dialogue/kata_pya.wav",
    "KATAKANA_PYE": "sound/dialogue/kata_pye.wav",
    "KATAKANA_PYO": "sound/dialogue/kata_pyo.wav",
    "KATAKANA_PYU": "sound/dialogue/kata_pyu.wav",
    "KATAKANA_RA": "sound/dialogue/kata_ra.wav",
    "KATAKANA_RE": "sound/dialogue/kata_re.wav",
    "KATAKANA_RI": "sound/dialogue/kata_ri.wav",
    "KATAKANA_RO": "sound/dialogue/kata_ro.wav",
    "KATAKANA_RU": "sound/dialogue/kata_ru.wav",
    "KATAKANA_RYA": "sound/dialogue/kata_rya.wav",
    "KATAKANA_RYE": "sound/dialogue/kata_rye.wav",
    "KATAKANA_RYO": "sound/dialogue/kata_ryo.wav",
    "KATAKANA_RYU": "sound/dialogue/kata_ryu.wav",
    "KATAKANA_SA": "sound/dialogue/kata_sa.wav",
    "KATAKANA_SE": "sound/dialogue/kata_se.wav",
    "KATAKANA_SHA": "sound/dialogue/kata_sha.wav",
    "KATAKANA_SHO": "sound/dialogue/kata_sho.wav",
    "KATAKANA_SHU": "sound/dialogue/kata_shu.wav",
    "KATAKANA_SI": "sound/dialogue/kata_si.wav",
    "KATAKANA_SO": "sound/dialogue/kata_so.wav",
    "KATAKANA_SU": "sound/dialogue/kata_su.wav",
    "KATAKANA_SWI": "sound/dialogue/kata_swi.wav",
    "KATAKANA_SYE": "sound/dialogue/kata_sye.wav",
    "KATAKANA_TA": "sound/dialogue/kata_ta.wav",
    "KATAKANA_TE": "sound/dialogue/kata_te.wav",
    "KATAKANA_TI": "sound/dialogue/kata_ti.wav",
    "KATAKANA_TO": "sound/dialogue/kata_to.wav",
    "KATAKANA_TSA": "sound/dialogue/kata_tsa.wav",
    "KATAKANA_TSE": "sound/dialogue/kata_tse.wav",
    "KATAKANA_TSO": "sound/dialogue/kata_tso.wav",
    "KATAKANA_TSU": "sound/dialogue/kata_tsu.wav",
    "KATAKANA_TSWI": "sound/dialogue/kata_tswi.wav",
    "KATAKANA_TU": "sound/dialogue/kata_tu.wav",
    "KATAKANA_TYU": "sound/dialogue/kata_tyu.wav",
    "KATAKANA_U": "sound/dialogue/kata_u.wav",
    "KATAKANA_WA": "sound/dialogue/kata_wa.wav",
    "KATAKANA_WE": "sound/dialogue/kata_we.wav",
    "KATAKANA_WI": "sound/dialogue/kata_wi.wav",
    "KATAKANA_WO": "sound/dialogue/kata_wo.wav",
    "KATAKANA_YA": "sound/dialogue/kata_ya.wav",
    "KATAKANA_YE": "sound/dialogue/kata_ye.wav",
    "KATAKANA_YO": "sound/dialogue/kata_yo.wav",
    "KATAKANA_YU": "sound/dialogue/kata_yu.wav",
    "KATAKANA_ZA": "sound/dialogue/kata_za.wav",
    "KATAKANA_ZE": "sound/dialogue/kata_ze.wav",
    "KATAKANA_ZI": "sound/dialogue/kata_zi.wav",
    "KATAKANA_ZO": "sound/dialogue/kata_zo.wav",
    "KATAKANA_ZU": "sound/dialogue/kata_zu.wav",
    "KATAKANA_ZWI": "sound/dialogue/kata_zwi.wav",
}


class SequenceError(ValueError):
    """Raised when the pasted Java sequence is not valid for this project."""


@dataclass(frozen=True)
class AudioClip:
    """Stereo, signed 16-bit PCM samples at ``OUTPUT_SAMPLE_RATE``."""

    samples: array

    @property
    def frame_count(self) -> int:
        return len(self.samples) // OUTPUT_CHANNELS


def _strip_java_comments(source: str) -> str:
    """Remove Java comments so their words, commas, and SFX are ignored."""

    def replace_comment(match: re.Match[str]) -> str:
        comment = match.group(0)
        if comment.startswith("//"):
            return ""  # The line ending is outside this match and is preserved.
        # Preserve block-comment line endings and use spaces to avoid joining the
        # tokens on either side of a comment into one accidental entry.
        return re.sub(r"[^\r\n]", " ", comment)

    return re.sub(r"//[^\r\n]*|/\*.*?\*/", replace_comment, source, flags=re.DOTALL)


def load_sfx_catalog() -> dict[str, Path]:
    """Return the embedded SFX mapping, rooted at the project directory."""

    return {name: PROJECT_DIR / relative_path for name, relative_path in SFX_PATHS.items()}


def parse_sequence(text: str, known_sfx: Iterable[str]) -> list[str | None]:
    """Parse a comma-separated list of SFX references, syllables, and ``null``.

    Both ``SoundManager.SFX.NAME`` and the shorter ``SFX.NAME`` are accepted.
    Bare syllables are case-insensitive, so ``a`` and ``ME`` resolve to
    ``KATAKANA_A`` and ``KATAKANA_ME``. Java array wrappers are optional, and
    both ``// line`` and ``/* block */`` comments are ignored.
    """

    known = set(known_sfx)
    clean_text = _strip_java_comments(text).strip()

    opening_brace = clean_text.find("{")
    closing_brace = clean_text.rfind("}")
    if opening_brace >= 0 or closing_brace >= 0:
        if opening_brace < 0 or closing_brace < opening_brace:
            raise SequenceError("The Java array wrapper has unmatched braces.")
        suffix = clean_text[closing_brace + 1 :].strip()
        if suffix not in ("", ";"):
            raise SequenceError(f"Unexpected text after the array: {suffix}")
        clean_text = clean_text[opening_brace + 1 : closing_brace].strip()
    elif clean_text.endswith(";"):
        clean_text = clean_text[:-1].rstrip()

    if not clean_text:
        raise SequenceError("No SFX entries found. Try: a, O, me")

    parts = clean_text.split(",")
    if parts[-1].strip() == "":
        parts.pop()  # A trailing comma is valid Java array syntax.
    if not parts:
        raise SequenceError("No SFX entries found. Try: a, O, me")

    full_reference = re.compile(
        r"(?:(?:SoundManager\s*\.\s*)?SFX\s*\.\s*)"
        r"([A-Za-z_][A-Za-z0-9_]*)"
    )
    bare_name = re.compile(r"[A-Za-z_][A-Za-z0-9_]*")
    tokens: list[str | None] = []

    for index, part in enumerate(parts, start=1):
        entry = part.strip()
        if not entry:
            raise SequenceError(f"Entry #{index} is empty; remove the extra comma.")
        if entry == "null":
            tokens.append(None)
            continue

        reference_match = full_reference.fullmatch(entry)
        if reference_match:
            enum_name = reference_match.group(1).upper()
            if enum_name not in known:
                raise SequenceError(f"Unknown SFX entry #{index}: {entry}")
            tokens.append(enum_name)
            continue

        if bare_name.fullmatch(entry):
            upper_name = entry.upper()
            if upper_name in known:
                tokens.append(upper_name)
                continue
            katakana_name = f"KATAKANA_{upper_name}"
            if katakana_name in known:
                tokens.append(katakana_name)
                continue
            raise SequenceError(f"Unknown Katakana syllable at entry #{index}: {entry}")

        raise SequenceError(
            f"Invalid entry #{index}: {entry}. Entries must be separated by commas."
        )

    return tokens


def _decode_pcm_samples(raw: bytes, sample_width: int) -> array:
    """Decode little-endian PCM of common widths into signed 16-bit samples."""

    if sample_width == 1:
        return array("h", ((value - 128) << 8 for value in raw))

    if sample_width == 2:
        samples = array("h")
        samples.frombytes(raw)
        if sys.byteorder != "little":
            samples.byteswap()
        return samples

    samples = array("h")
    if sample_width == 3:
        for offset in range(0, len(raw), 3):
            value = int.from_bytes(raw[offset : offset + 3], "little", signed=True)
            samples.append(value >> 8)
        return samples

    if sample_width == 4:
        for offset in range(0, len(raw), 4):
            value = int.from_bytes(raw[offset : offset + 4], "little", signed=True)
            samples.append(value >> 16)
        return samples

    raise SequenceError(f"Unsupported WAV sample width: {sample_width * 8} bits.")


def _stereo_frame(samples: array, channels: int, frame_index: int) -> tuple[int, int]:
    start = frame_index * channels
    if channels == 1:
        value = samples[start]
        return value, value
    if channels == 2:
        return samples[start], samples[start + 1]

    # This project currently uses mono and stereo WAVs, but averaging additional
    # channels gives a sensible result if a future SFX uses a multichannel file.
    frame = samples[start : start + channels]
    value = round(sum(frame) / channels)
    return value, value


def read_audio_clip(path: Path) -> AudioClip:
    """Load a PCM WAV and normalize it to stereo 48 kHz, signed 16-bit PCM."""

    if not path.is_file():
        raise SequenceError(f"Audio file not found: {path}")

    try:
        with wave.open(str(path), "rb") as wav_file:
            if wav_file.getcomptype() != "NONE":
                raise SequenceError(
                    f"Compressed WAV is not supported ({wav_file.getcomptype()}): {path.name}"
                )
            channels = wav_file.getnchannels()
            sample_width = wav_file.getsampwidth()
            sample_rate = wav_file.getframerate()
            frame_count = wav_file.getnframes()
            raw = wav_file.readframes(frame_count)
    except (OSError, EOFError, wave.Error) as exc:
        raise SequenceError(f"Could not read WAV file {path.name}: {exc}") from exc

    if channels < 1 or sample_rate < 1 or frame_count < 1:
        raise SequenceError(f"Invalid or empty WAV file: {path.name}")

    decoded = _decode_pcm_samples(raw, sample_width)
    expected_samples = frame_count * channels
    if len(decoded) != expected_samples:
        raise SequenceError(f"Incomplete PCM data in WAV file: {path.name}")

    output_frames = max(1, round(frame_count * OUTPUT_SAMPLE_RATE / sample_rate))
    output = array("h")
    for output_index in range(output_frames):
        source_position = output_index * sample_rate / OUTPUT_SAMPLE_RATE
        left_index = min(int(source_position), frame_count - 1)
        right_index = min(left_index + 1, frame_count - 1)
        fraction = source_position - left_index

        left_a, right_a = _stereo_frame(decoded, channels, left_index)
        if fraction and right_index != left_index:
            left_b, right_b = _stereo_frame(decoded, channels, right_index)
            left_a = round(left_a + (left_b - left_a) * fraction)
            right_a = round(right_a + (right_b - right_a) * fraction)

        output.append(left_a)
        output.append(right_a)

    return AudioClip(output)


def _apply_pitch_shift(samples: array, semitones: float) -> array:
    """Pitch the finished mix with FFmpeg Rubber Band, preserving duration."""

    if abs(semitones) < 1e-9:
        return samples

    ffmpeg_path = shutil.which("ffmpeg")
    if ffmpeg_path is None:
        raise SequenceError(
            "FFmpeg was not found. Install a full FFmpeg build with the "
            "rubberband filter, or export with pitch set to 0."
        )

    input_pcm = array("h")
    input_pcm.extend(max(-32_768, min(32_767, sample)) for sample in samples)
    if sys.byteorder != "little":
        input_pcm.byteswap()

    pitch_ratio = 2.0 ** (semitones / 12.0)
    command = (
        ffmpeg_path,
        "-hide_banner",
        "-loglevel",
        "error",
        "-nostdin",
        "-f",
        "s16le",
        "-ar",
        str(OUTPUT_SAMPLE_RATE),
        "-ac",
        str(OUTPUT_CHANNELS),
        "-i",
        "pipe:0",
        "-af",
        f"rubberband=pitch={pitch_ratio:.12f}:tempo=1.0",
        "-f",
        "s16le",
        "-acodec",
        "pcm_s16le",
        "-ar",
        str(OUTPUT_SAMPLE_RATE),
        "-ac",
        str(OUTPUT_CHANNELS),
        "pipe:1",
    )
    creation_flags = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0
    try:
        result = subprocess.run(
            command,
            input=input_pcm.tobytes(),
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
            creationflags=creation_flags,
        )
    except OSError as exc:
        raise SequenceError(f"Could not run FFmpeg: {exc}") from exc

    if result.returncode != 0:
        detail = result.stderr.decode("utf-8", errors="replace").strip()
        if "No such filter" in detail or "rubberband" in detail.lower():
            detail = (
                "This FFmpeg installation does not provide the rubberband filter. "
                "Install a full build with librubberband support."
            )
        raise SequenceError(f"Pitch shifting failed. {detail}")

    shifted_pcm = array("h")
    shifted_pcm.frombytes(result.stdout)
    if sys.byteorder != "little":
        shifted_pcm.byteswap()
    shifted = array("i", shifted_pcm)

    # Rubber Band may return a few frames more or less because of its analysis
    # window. Pin the result to the original length so timing is exact.
    target_samples = len(samples)
    if len(shifted) > target_samples:
        del shifted[target_samples:]
    elif len(shifted) < target_samples:
        shifted.extend([0] * (target_samples - len(shifted)))
    return shifted


class _Biquad:
    """Small transposed-direct-form-II filter used by the radio treatment."""

    def __init__(
        self,
        b0: float,
        b1: float,
        b2: float,
        a0: float,
        a1: float,
        a2: float,
    ) -> None:
        self.b0 = b0 / a0
        self.b1 = b1 / a0
        self.b2 = b2 / a0
        self.a1 = a1 / a0
        self.a2 = a2 / a0
        self.z1 = 0.0
        self.z2 = 0.0

    def process(self, sample: float) -> float:
        output = self.b0 * sample + self.z1
        self.z1 = self.b1 * sample - self.a1 * output + self.z2
        self.z2 = self.b2 * sample - self.a2 * output
        return output


def _butterworth_filter(kind: str, cutoff_hz: float) -> _Biquad:
    omega = 2.0 * math.pi * cutoff_hz / OUTPUT_SAMPLE_RATE
    cosine = math.cos(omega)
    alpha = math.sin(omega) / (2.0 * math.sqrt(0.5))
    if kind == "highpass":
        b0 = (1.0 + cosine) / 2.0
        b1 = -(1.0 + cosine)
        b2 = b0
    else:
        b0 = (1.0 - cosine) / 2.0
        b1 = 1.0 - cosine
        b2 = b0
    return _Biquad(b0, b1, b2, 1.0 + alpha, -2.0 * cosine, 1.0 - alpha)


def _presence_filter(frequency_hz: float, gain_db: float, quality: float) -> _Biquad:
    amplitude = 10.0 ** (gain_db / 40.0)
    omega = 2.0 * math.pi * frequency_hz / OUTPUT_SAMPLE_RATE
    cosine = math.cos(omega)
    alpha = math.sin(omega) / (2.0 * quality)
    return _Biquad(
        1.0 + alpha * amplitude,
        -2.0 * cosine,
        1.0 - alpha * amplitude,
        1.0 + alpha / amplitude,
        -2.0 * cosine,
        1.0 - alpha / amplitude,
    )


def _soft_limit(sample: float) -> float:
    """Leave normal levels untouched and smoothly contain only high peaks."""

    magnitude = abs(sample)
    if magnitude <= 0.9:
        return sample
    limited = 0.9 + 0.1 * math.tanh((magnitude - 0.9) / 0.1)
    return math.copysign(limited, sample)


def _apply_radio_effect(samples: array, hiss_volume: float = 1.0) -> array:
    """Apply an obvious band-limited radio color with controlled audible hiss.

    The voice is mostly mono, loses substantial bass, and gains focused high mids.
    Deterministic band-limited noise makes the radio character audible even in
    pauses without using piercing full-band white noise or random crackles. Radio
    mode also adds a 0.3-second hiss-only fade before and after the dialogue.
    """

    highpass = _butterworth_filter("highpass", 480.0)
    presence = _presence_filter(2_800.0, 4.5, 0.85)
    lowpass = _butterworth_filter("lowpass", 6_500.0)
    noise_highpass = _butterworth_filter("highpass", 500.0)
    noise_lowpass = _butterworth_filter("lowpass", 5_200.0)
    noise_random = random.Random(0x50494E4755)
    wet_amount = 0.95
    dry_amount = 1.0 - wet_amount
    drive = 1.22
    output_gain = 1.02
    noise_level = BASE_HISS_LEVEL * hiss_volume
    effected = array("i")
    dialogue_frames = len(samples) // OUTPUT_CHANNELS
    padding_frames = round(OUTPUT_SAMPLE_RATE * RADIO_PADDING_SECONDS)
    frame_count = dialogue_frames + (padding_frames * 2)

    for frame in range(frame_count):
        dialogue_frame = frame - padding_frames
        if 0 <= dialogue_frame < dialogue_frames:
            source_index = dialogue_frame * OUTPUT_CHANNELS
            left = samples[source_index] / 32_768.0
            right = samples[source_index + 1] / 32_768.0
            mono = (left + right) * 0.5
            wet = lowpass.process(presence.process(highpass.process(mono)))
            wet = math.tanh(wet * drive) / drive
        else:
            left = 0.0
            right = 0.0
            wet = 0.0

        if frame < padding_frames:
            fade_position = frame / padding_frames
            noise_envelope = 0.5 - (0.5 * math.cos(math.pi * fade_position))
        elif frame >= padding_frames + dialogue_frames:
            remaining = (frame_count - 1 - frame) / padding_frames
            noise_envelope = 0.5 - (0.5 * math.cos(math.pi * remaining))
        else:
            noise_envelope = 1.0

        noise = noise_lowpass.process(
            noise_highpass.process(noise_random.uniform(-1.0, 1.0))
        )
        noise *= noise_level * noise_envelope

        output_left = _soft_limit(
            ((dry_amount * left + wet_amount * wet) * output_gain) + noise
        )
        output_right = _soft_limit(
            ((dry_amount * right + wet_amount * wet) * output_gain) + noise
        )
        effected.append(round(output_left * 32_767.0))
        effected.append(round(output_right * 32_767.0))

    return effected


def render_sequence(
    tokens: list[str | None],
    catalog: dict[str, Path],
    output_path: Path,
    interval_ms: int = DEFAULT_INTERVAL_MS,
    pitch_semitones: float = 0.0,
    radio_effect: bool = False,
    dialogue_volume: float = 1.0,
    hiss_volume: float = 1.0,
) -> float:
    """Mix the timed SFX events and write a Java-compatible PCM WAV.

    Returns the rendered duration in seconds.
    """

    if not tokens:
        raise SequenceError("The sequence is empty.")
    if not 1 <= interval_ms <= 10_000:
        raise SequenceError("The interval must be between 1 and 10000 ms.")
    if not MIN_PITCH_SEMITONES <= pitch_semitones <= MAX_PITCH_SEMITONES:
        raise SequenceError("Pitch must be between -12 and +12 semitones.")
    if not 0.0 <= dialogue_volume <= MAX_VOLUME_GAIN:
        raise SequenceError("Dialogue volume must be between 0% and 100%.")
    if not 0.0 <= hiss_volume <= MAX_VOLUME_GAIN:
        raise SequenceError("Radio hiss volume must be between 0% and 100%.")

    clip_cache: dict[str, AudioClip] = {}
    for token in tokens:
        if token is not None and token not in clip_cache:
            clip_cache[token] = read_audio_clip(catalog[token])

    interval_frames = round(OUTPUT_SAMPLE_RATE * interval_ms / 1000)
    total_frames = len(tokens) * interval_frames
    for index, token in enumerate(tokens):
        if token is not None:
            total_frames = max(
                total_frames,
                index * interval_frames + clip_cache[token].frame_count,
            )

    mixed = array("i", [0]) * (total_frames * OUTPUT_CHANNELS)
    for index, token in enumerate(tokens):
        if token is None:
            continue
        destination = index * interval_frames * OUTPUT_CHANNELS
        for sample_index, sample in enumerate(clip_cache[token].samples):
            mixed[destination + sample_index] += sample

    processed = _apply_pitch_shift(mixed, pitch_semitones)
    if dialogue_volume != 1.0:
        processed = array("i", (round(sample * dialogue_volume) for sample in processed))
    if radio_effect:
        processed = _apply_radio_effect(processed, hiss_volume)

    pcm = array("h")
    pcm.extend(max(-32_768, min(32_767, sample)) for sample in processed)
    if sys.byteorder != "little":
        pcm.byteswap()

    output_path = output_path.resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    temporary_file = tempfile.NamedTemporaryFile(
        mode="wb",
        prefix=f".{output_path.stem}-",
        suffix=".tmp",
        dir=output_path.parent,
        delete=False,
    )
    temporary_path = Path(temporary_file.name)
    temporary_file.close()
    try:
        with wave.open(str(temporary_path), "wb") as wav_file:
            wav_file.setnchannels(OUTPUT_CHANNELS)
            wav_file.setsampwidth(OUTPUT_SAMPLE_WIDTH)
            wav_file.setframerate(OUTPUT_SAMPLE_RATE)
            wav_file.writeframes(pcm.tobytes())
        os.replace(temporary_path, output_path)
    finally:
        if temporary_path.exists():
            temporary_path.unlink()

    return (len(processed) // OUTPUT_CHANNELS) / OUTPUT_SAMPLE_RATE


class AudioPreviewPlayer:
    """Own and play one temporary simulation WAV at a time."""

    def __init__(self) -> None:
        self.preview_path: Path | None = None
        self.process: subprocess.Popen[bytes] | None = None

    def prepare_path(self) -> Path:
        self.stop()
        descriptor, filename = tempfile.mkstemp(
            prefix="pingu-dialogue-preview-",
            suffix=".wav",
        )
        os.close(descriptor)
        self.preview_path = Path(filename)
        return self.preview_path

    def play(self) -> None:
        if self.preview_path is None or not self.preview_path.is_file():
            raise SequenceError("The temporary simulation WAV was not created.")

        if os.name == "nt":
            import winsound

            try:
                winsound.PlaySound(
                    str(self.preview_path),
                    winsound.SND_FILENAME | winsound.SND_ASYNC | winsound.SND_NODEFAULT,
                )
            except RuntimeError as exc:
                raise SequenceError(f"Could not play the audio simulation: {exc}") from exc
            return

        ffplay_path = shutil.which("ffplay")
        if ffplay_path is None:
            raise SequenceError("ffplay was not found; audio simulation cannot be played.")
        self.process = subprocess.Popen(
            (
                ffplay_path,
                "-hide_banner",
                "-loglevel",
                "error",
                "-nodisp",
                "-autoexit",
                str(self.preview_path),
            ),
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )

    def stop(self) -> None:
        if os.name == "nt":
            try:
                import winsound

                winsound.PlaySound(None, 0)
            except RuntimeError:
                pass
        elif self.process is not None and self.process.poll() is None:
            self.process.terminate()
        self.process = None

        if self.preview_path is not None:
            try:
                self.preview_path.unlink(missing_ok=True)
            except OSError:
                # Some audio backends briefly retain the file after stopping.
                pass
            self.preview_path = None


class DialogueSequenceApp:
    """Tkinter front end for parsing and exporting an SFX sequence."""

    EXAMPLE = "bo, se, ko, n, se, gi, u, null, o, po, ru, ta, o, a, bu, ri, u"

    def __init__(self) -> None:
        import tkinter as tk
        from tkinter import ttk

        self.tk = tk
        self.ttk = ttk
        self.catalog = load_sfx_catalog()
        self.preview_player = AudioPreviewPlayer()

        self.root = tk.Tk()
        self.root.title("Pingu Dialogue WAV Exporter")
        self.root.geometry("760x660")
        self.root.minsize(620, 540)
        self.root.protocol("WM_DELETE_WINDOW", self._close)

        self.interval_value = tk.StringVar(value=str(DEFAULT_INTERVAL_MS))
        self.pitch_value = tk.StringVar(value="0")
        self.radio_value = tk.BooleanVar(value=False)
        self.dialogue_volume_value = tk.DoubleVar(value=VOLUME_SLIDER_UNITY)
        self.hiss_volume_value = tk.DoubleVar(value=VOLUME_SLIDER_UNITY)
        self.dialogue_volume_label = tk.StringVar(value="50%")
        self.hiss_volume_label = tk.StringVar(value="50%")
        self.status_value = tk.StringVar(value="Paste a sequence to begin.")

        outer = ttk.Frame(self.root, padding=16)
        outer.grid(row=0, column=0, sticky="nsew")
        self.root.rowconfigure(0, weight=1)
        self.root.columnconfigure(0, weight=1)
        outer.rowconfigure(2, weight=1)
        outer.columnconfigure(0, weight=1)

        ttk.Label(
            outer,
            text="Dialogue sound sequence",
            font=("Segoe UI", 15, "bold"),
        ).grid(row=0, column=0, sticky="w")
        ttk.Label(
            outer,
            text=(
                "Comma-separate entries. Use full SFX names or syllables like a and ME. "
                "null adds a pause; Java // comments are ignored."
            ),
            wraplength=700,
        ).grid(row=1, column=0, sticky="ew", pady=(4, 10))

        editor_frame = ttk.Frame(outer)
        editor_frame.grid(row=2, column=0, sticky="nsew")
        editor_frame.rowconfigure(0, weight=1)
        editor_frame.columnconfigure(0, weight=1)

        self.editor = tk.Text(
            editor_frame,
            wrap="none",
            undo=True,
            font=("Consolas", 10),
            padx=8,
            pady=8,
        )
        y_scroll = ttk.Scrollbar(editor_frame, orient="vertical", command=self.editor.yview)
        x_scroll = ttk.Scrollbar(editor_frame, orient="horizontal", command=self.editor.xview)
        self.editor.configure(yscrollcommand=y_scroll.set, xscrollcommand=x_scroll.set)
        self.editor.grid(row=0, column=0, sticky="nsew")
        y_scroll.grid(row=0, column=1, sticky="ns")
        x_scroll.grid(row=1, column=0, sticky="ew")
        self.editor.insert("1.0", self.EXAMPLE)
        self.editor.edit_modified(False)
        self.editor.bind("<<Modified>>", self._on_text_modified)

        options = ttk.Frame(outer)
        options.grid(row=3, column=0, sticky="ew", pady=(12, 8))
        options.columnconfigure(2, weight=1)
        ttk.Label(options, text="Start interval (ms):").grid(row=0, column=0, sticky="w")
        interval_entry = ttk.Entry(options, textvariable=self.interval_value, width=8)
        interval_entry.grid(row=0, column=1, sticky="w", padx=(6, 14))
        ttk.Label(options, text="The game currently uses 100 ms.").grid(
            row=0, column=2, columnspan=2, sticky="w"
        )

        ttk.Label(options, text="Pitch (semitones):").grid(
            row=1, column=0, sticky="w", pady=(8, 0)
        )
        pitch_entry = ttk.Spinbox(
            options,
            from_=MIN_PITCH_SEMITONES,
            to=MAX_PITCH_SEMITONES,
            increment=0.5,
            textvariable=self.pitch_value,
            width=8,
            wrap=False,
        )
        pitch_entry.grid(row=1, column=1, sticky="w", padx=(6, 14), pady=(8, 0))
        ttk.Label(options, text="-12 to +12; duration is preserved.").grid(
            row=1, column=2, sticky="w", pady=(8, 0)
        )
        ttk.Checkbutton(
            options,
            text="Radio effect + hiss",
            variable=self.radio_value,
            command=self._update_radio_controls,
        ).grid(row=1, column=3, sticky="e", padx=(16, 0), pady=(8, 0))

        ttk.Label(options, text="Dialogue volume:").grid(
            row=2, column=0, sticky="w", pady=(10, 0)
        )
        self.dialogue_volume_scale = ttk.Scale(
            options,
            from_=0,
            to=100,
            variable=self.dialogue_volume_value,
            command=lambda value: self._update_volume_label(
                value, self.dialogue_volume_label
            ),
        )
        self.dialogue_volume_scale.grid(
            row=2, column=1, columnspan=2, sticky="ew", padx=(6, 14), pady=(10, 0)
        )
        ttk.Label(options, textvariable=self.dialogue_volume_label, width=5).grid(
            row=2, column=3, sticky="e", pady=(10, 0)
        )

        ttk.Label(options, text="Radio hiss volume:").grid(
            row=3, column=0, sticky="w", pady=(8, 0)
        )
        self.hiss_volume_scale = ttk.Scale(
            options,
            from_=0,
            to=100,
            variable=self.hiss_volume_value,
            command=lambda value: self._update_volume_label(value, self.hiss_volume_label),
        )
        self.hiss_volume_scale.grid(
            row=3, column=1, columnspan=2, sticky="ew", padx=(6, 14), pady=(8, 0)
        )
        ttk.Label(options, textvariable=self.hiss_volume_label, width=5).grid(
            row=3, column=3, sticky="e", pady=(8, 0)
        )
        self._update_radio_controls()

        actions = ttk.Frame(outer)
        actions.grid(row=4, column=0, sticky="ew")
        ttk.Button(actions, text="Load example", command=self._load_example).pack(side="left")
        ttk.Button(actions, text="Clear", command=self._clear).pack(side="left", padx=8)
        self.simulate_button = ttk.Button(
            actions, text="Simulate audio", command=self._simulate
        )
        self.simulate_button.pack(side="left", padx=(8, 0))
        ttk.Button(actions, text="Stop audio", command=self._stop_preview).pack(
            side="left", padx=8
        )
        self.export_button = ttk.Button(actions, text="Export WAV…", command=self._export)
        self.export_button.pack(side="right")

        ttk.Separator(outer).grid(row=5, column=0, sticky="ew", pady=(12, 8))
        ttk.Label(outer, textvariable=self.status_value, wraplength=700).grid(
            row=6, column=0, sticky="w"
        )
        self._refresh_status()

    def _current_tokens(self) -> list[str | None]:
        return parse_sequence(self.editor.get("1.0", "end-1c"), self.catalog)

    def _current_interval(self) -> int:
        try:
            interval = int(self.interval_value.get().strip())
        except ValueError as exc:
            raise SequenceError("The interval must be a whole number of milliseconds.") from exc
        if not 1 <= interval <= 10_000:
            raise SequenceError("The interval must be between 1 and 10000 ms.")
        return interval

    def _current_pitch(self) -> float:
        try:
            pitch = float(self.pitch_value.get().strip())
        except ValueError as exc:
            raise SequenceError("Pitch must be a number of semitones.") from exc
        if not MIN_PITCH_SEMITONES <= pitch <= MAX_PITCH_SEMITONES:
            raise SequenceError("Pitch must be between -12 and +12 semitones.")
        return pitch

    def _current_dialogue_volume(self) -> float:
        return max(
            0.0,
            min(MAX_VOLUME_GAIN, self.dialogue_volume_value.get() / VOLUME_SLIDER_UNITY),
        )

    def _current_hiss_volume(self) -> float:
        return max(
            0.0,
            min(MAX_VOLUME_GAIN, self.hiss_volume_value.get() / VOLUME_SLIDER_UNITY),
        )

    def _update_volume_label(self, value: str, label) -> None:
        label.set(f"{round(float(value))}%")

    def _update_radio_controls(self) -> None:
        state = "normal" if self.radio_value.get() else "disabled"
        if hasattr(self, "hiss_volume_scale"):
            self.hiss_volume_scale.configure(state=state)

    def _on_text_modified(self, _event: object) -> None:
        if self.editor.edit_modified():
            self.editor.edit_modified(False)
            self._refresh_status()

    def _refresh_status(self) -> None:
        try:
            tokens = self._current_tokens()
            sounds = sum(token is not None for token in tokens)
            pauses = len(tokens) - sounds
            self.status_value.set(
                f"Valid sequence: {len(tokens)} slots ({sounds} sounds, {pauses} null pauses)."
            )
        except SequenceError as exc:
            self.status_value.set(str(exc))

    def _load_example(self) -> None:
        self.editor.delete("1.0", "end")
        self.editor.insert("1.0", self.EXAMPLE)

    def _clear(self) -> None:
        self.editor.delete("1.0", "end")
        self.editor.focus_set()

    def _simulate(self) -> None:
        from tkinter import messagebox

        try:
            tokens = self._current_tokens()
            interval = self._current_interval()
            pitch = self._current_pitch()
        except SequenceError as exc:
            messagebox.showerror("Cannot simulate audio", str(exc), parent=self.root)
            return

        self.simulate_button.configure(state="disabled")
        self.status_value.set("Rendering audio simulation…")
        self.root.update_idletasks()
        try:
            preview_path = self.preview_player.prepare_path()
            duration = render_sequence(
                tokens,
                self.catalog,
                preview_path,
                interval,
                pitch,
                self.radio_value.get(),
                self._current_dialogue_volume(),
                self._current_hiss_volume(),
            )
            self.preview_player.play()
        except (OSError, SequenceError) as exc:
            self.preview_player.stop()
            self.status_value.set("Audio simulation failed.")
            messagebox.showerror("Simulation failed", str(exc), parent=self.root)
        else:
            self.status_value.set(f"Simulating current sequence ({duration:.2f} s).")
        finally:
            self.simulate_button.configure(state="normal")

    def _stop_preview(self) -> None:
        self.preview_player.stop()
        self.status_value.set("Audio simulation stopped.")

    def _close(self) -> None:
        self.preview_player.stop()
        self.root.destroy()

    def _export(self) -> None:
        from tkinter import filedialog, messagebox

        try:
            tokens = self._current_tokens()
            interval = self._current_interval()
            pitch = self._current_pitch()
        except SequenceError as exc:
            messagebox.showerror("Cannot export sequence", str(exc), parent=self.root)
            return

        selected = filedialog.asksaveasfilename(
            parent=self.root,
            title="Save dialogue sequence",
            defaultextension=".wav",
            filetypes=(("WAV audio", "*.wav"), ("All files", "*.*")),
            initialfile="dialogue_sequence.wav",
        )
        if not selected:
            return

        self.export_button.configure(state="disabled")
        self.status_value.set("Rendering WAV…")
        self.root.update_idletasks()
        try:
            duration = render_sequence(
                tokens,
                self.catalog,
                Path(selected),
                interval,
                pitch,
                self.radio_value.get(),
                self._current_dialogue_volume(),
                self._current_hiss_volume(),
            )
        except (OSError, SequenceError) as exc:
            messagebox.showerror("Export failed", str(exc), parent=self.root)
            self.status_value.set("Export failed.")
        else:
            self.status_value.set(
                f"Saved {len(tokens)} slots ({duration:.2f} s) to {selected}"
            )
            messagebox.showinfo(
                "WAV exported",
                f"The dialogue sequence was saved successfully.\n\n{selected}",
                parent=self.root,
            )
        finally:
            self.export_button.configure(state="normal")

    def run(self) -> None:
        self.root.mainloop()


class NativeWindowsDialogueSequenceApp:
    """Dependency-free Win32 fallback for Python installs without Tcl/Tk."""

    EXAMPLE = DialogueSequenceApp.EXAMPLE.replace("\n", "\r\n")

    def __init__(self) -> None:
        if os.name != "nt":
            raise SequenceError("The native fallback is only available on Windows.")

        import ctypes
        from ctypes import wintypes

        self.ctypes = ctypes
        self.wintypes = wintypes
        self.catalog = load_sfx_catalog()
        self.preview_player = AudioPreviewPlayer()
        self._configure_api()
        self._create_window()

    def _configure_api(self) -> None:
        ctypes = self.ctypes
        wintypes = self.wintypes

        self.user32 = ctypes.WinDLL("user32", use_last_error=True)
        self.kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)
        self.comdlg32 = ctypes.WinDLL("comdlg32", use_last_error=True)
        self.comctl32 = ctypes.WinDLL("comctl32", use_last_error=True)
        self.gdi32 = ctypes.WinDLL("gdi32", use_last_error=True)
        self.comctl32.InitCommonControls.argtypes = ()
        self.comctl32.InitCommonControls.restype = None
        self.comctl32.InitCommonControls()

        self.WNDPROC = ctypes.WINFUNCTYPE(
            ctypes.c_ssize_t,
            ctypes.c_void_p,
            wintypes.UINT,
            ctypes.c_size_t,
            ctypes.c_ssize_t,
        )

        class WNDCLASSW(ctypes.Structure):
            _fields_ = (
                ("style", wintypes.UINT),
                ("lpfnWndProc", self.WNDPROC),
                ("cbClsExtra", ctypes.c_int),
                ("cbWndExtra", ctypes.c_int),
                ("hInstance", ctypes.c_void_p),
                ("hIcon", ctypes.c_void_p),
                ("hCursor", ctypes.c_void_p),
                ("hbrBackground", ctypes.c_void_p),
                ("lpszMenuName", wintypes.LPCWSTR),
                ("lpszClassName", wintypes.LPCWSTR),
            )

        class MSG(ctypes.Structure):
            _fields_ = (
                ("hwnd", ctypes.c_void_p),
                ("message", wintypes.UINT),
                ("wParam", ctypes.c_size_t),
                ("lParam", ctypes.c_ssize_t),
                ("time", wintypes.DWORD),
                ("pt", wintypes.POINT),
            )

        class OPENFILENAMEW(ctypes.Structure):
            _fields_ = (
                ("lStructSize", wintypes.DWORD),
                ("hwndOwner", ctypes.c_void_p),
                ("hInstance", ctypes.c_void_p),
                ("lpstrFilter", wintypes.LPCWSTR),
                ("lpstrCustomFilter", wintypes.LPWSTR),
                ("nMaxCustFilter", wintypes.DWORD),
                ("nFilterIndex", wintypes.DWORD),
                ("lpstrFile", wintypes.LPWSTR),
                ("nMaxFile", wintypes.DWORD),
                ("lpstrFileTitle", wintypes.LPWSTR),
                ("nMaxFileTitle", wintypes.DWORD),
                ("lpstrInitialDir", wintypes.LPCWSTR),
                ("lpstrTitle", wintypes.LPCWSTR),
                ("Flags", wintypes.DWORD),
                ("nFileOffset", wintypes.WORD),
                ("nFileExtension", wintypes.WORD),
                ("lpstrDefExt", wintypes.LPCWSTR),
                ("lCustData", ctypes.c_ssize_t),
                ("lpfnHook", ctypes.c_void_p),
                ("lpTemplateName", wintypes.LPCWSTR),
                ("pvReserved", ctypes.c_void_p),
                ("dwReserved", wintypes.DWORD),
                ("FlagsEx", wintypes.DWORD),
            )

        self.WNDCLASSW = WNDCLASSW
        self.MSG = MSG
        self.OPENFILENAMEW = OPENFILENAMEW

        self.kernel32.GetModuleHandleW.argtypes = (wintypes.LPCWSTR,)
        self.kernel32.GetModuleHandleW.restype = ctypes.c_void_p
        self.user32.LoadCursorW.argtypes = (ctypes.c_void_p, ctypes.c_void_p)
        self.user32.LoadCursorW.restype = ctypes.c_void_p
        self.user32.RegisterClassW.argtypes = (ctypes.POINTER(WNDCLASSW),)
        self.user32.RegisterClassW.restype = wintypes.ATOM
        self.user32.CreateWindowExW.argtypes = (
            wintypes.DWORD,
            wintypes.LPCWSTR,
            wintypes.LPCWSTR,
            wintypes.DWORD,
            ctypes.c_int,
            ctypes.c_int,
            ctypes.c_int,
            ctypes.c_int,
            ctypes.c_void_p,
            ctypes.c_void_p,
            ctypes.c_void_p,
            ctypes.c_void_p,
        )
        self.user32.CreateWindowExW.restype = ctypes.c_void_p
        self.user32.DefWindowProcW.argtypes = (
            ctypes.c_void_p,
            wintypes.UINT,
            ctypes.c_size_t,
            ctypes.c_ssize_t,
        )
        self.user32.DefWindowProcW.restype = ctypes.c_ssize_t
        self.user32.SendMessageW.argtypes = (
            ctypes.c_void_p,
            wintypes.UINT,
            ctypes.c_size_t,
            ctypes.c_ssize_t,
        )
        self.user32.SendMessageW.restype = ctypes.c_ssize_t
        self.user32.GetWindowTextLengthW.argtypes = (ctypes.c_void_p,)
        self.user32.GetWindowTextLengthW.restype = ctypes.c_int
        self.user32.GetWindowTextW.argtypes = (
            ctypes.c_void_p,
            wintypes.LPWSTR,
            ctypes.c_int,
        )
        self.user32.GetWindowTextW.restype = ctypes.c_int
        self.user32.MoveWindow.argtypes = (
            ctypes.c_void_p,
            ctypes.c_int,
            ctypes.c_int,
            ctypes.c_int,
            ctypes.c_int,
            wintypes.BOOL,
        )
        self.user32.MoveWindow.restype = wintypes.BOOL
        self.user32.SetWindowTextW.argtypes = (ctypes.c_void_p, wintypes.LPCWSTR)
        self.user32.SetWindowTextW.restype = wintypes.BOOL
        self.user32.SetFocus.argtypes = (ctypes.c_void_p,)
        self.user32.SetFocus.restype = ctypes.c_void_p
        self.user32.MessageBoxW.argtypes = (
            ctypes.c_void_p,
            wintypes.LPCWSTR,
            wintypes.LPCWSTR,
            wintypes.UINT,
        )
        self.user32.MessageBoxW.restype = ctypes.c_int
        self.user32.EnableWindow.argtypes = (ctypes.c_void_p, wintypes.BOOL)
        self.user32.EnableWindow.restype = wintypes.BOOL
        self.user32.IsWindowEnabled.argtypes = (ctypes.c_void_p,)
        self.user32.IsWindowEnabled.restype = wintypes.BOOL
        self.user32.UpdateWindow.argtypes = (ctypes.c_void_p,)
        self.user32.UpdateWindow.restype = wintypes.BOOL
        self.user32.PostQuitMessage.argtypes = (ctypes.c_int,)
        self.user32.PostQuitMessage.restype = None
        self.user32.GetMessageW.argtypes = (
            ctypes.POINTER(MSG),
            ctypes.c_void_p,
            wintypes.UINT,
            wintypes.UINT,
        )
        self.user32.GetMessageW.restype = wintypes.BOOL
        self.user32.TranslateMessage.argtypes = (ctypes.POINTER(MSG),)
        self.user32.TranslateMessage.restype = wintypes.BOOL
        self.user32.DispatchMessageW.argtypes = (ctypes.POINTER(MSG),)
        self.user32.DispatchMessageW.restype = ctypes.c_ssize_t
        self.gdi32.GetStockObject.argtypes = (ctypes.c_int,)
        self.gdi32.GetStockObject.restype = ctypes.c_void_p
        self.comdlg32.GetSaveFileNameW.argtypes = (ctypes.POINTER(OPENFILENAMEW),)
        self.comdlg32.GetSaveFileNameW.restype = wintypes.BOOL

    def _create_window(self) -> None:
        ctypes = self.ctypes

        self.WM_COMMAND = 0x0111
        self.WM_DESTROY = 0x0002
        self.WM_SIZE = 0x0005
        self.WM_HSCROLL = 0x0114
        self.WM_SETFONT = 0x0030
        self.EN_CHANGE = 0x0300
        self.BN_CLICKED = 0
        self.BM_GETCHECK = 0x00F0
        self.BST_CHECKED = 1
        self.TBM_GETPOS = 0x0400
        self.TBM_SETPOS = 0x0405
        self.TBM_SETRANGE = 0x0406
        self.ID_EDITOR = 101
        self.ID_INTERVAL = 102
        self.ID_PITCH = 103
        self.ID_RADIO = 104
        self.ID_PITCH_UP = 105
        self.ID_PITCH_DOWN = 106
        self.ID_DIALOGUE_VOLUME = 107
        self.ID_HISS_VOLUME = 108
        self.ID_EXAMPLE = 201
        self.ID_CLEAR = 202
        self.ID_EXPORT = 203
        self.ID_SIMULATE = 204
        self.ID_STOP_PREVIEW = 205

        self._window_proc_callback = self.WNDPROC(self._window_proc)
        self.instance = self.kernel32.GetModuleHandleW(None)
        self.class_name = f"PinguDialogueExporter_{os.getpid()}"
        window_class = self.WNDCLASSW(
            0,
            self._window_proc_callback,
            0,
            0,
            self.instance,
            None,
            self.user32.LoadCursorW(None, self.ctypes.c_void_p(32512)),
            self.ctypes.c_void_p(6),  # COLOR_WINDOW + 1
            None,
            self.class_name,
        )
        if not self.user32.RegisterClassW(self.ctypes.byref(window_class)):
            raise self.ctypes.WinError(self.ctypes.get_last_error())

        styles = 0x00CF0000 | 0x10000000  # WS_OVERLAPPEDWINDOW | WS_VISIBLE
        self.hwnd = self.user32.CreateWindowExW(
            0,
            self.class_name,
            "Pingu Dialogue WAV Exporter",
            styles,
            100,
            80,
            780,
            700,
            None,
            None,
            self.instance,
            None,
        )
        if not self.hwnd:
            raise self.ctypes.WinError(self.ctypes.get_last_error())

    def _create_control(
        self,
        class_name: str,
        text: str,
        style: int,
        control_id: int,
        x: int = 0,
        y: int = 0,
        width: int = 10,
        height: int = 10,
        extended_style: int = 0,
    ) -> int:
        handle = self.user32.CreateWindowExW(
            extended_style,
            class_name,
            text,
            style | 0x40000000 | 0x10000000,  # WS_CHILD | WS_VISIBLE
            x,
            y,
            width,
            height,
            self.hwnd,
            self.ctypes.c_void_p(control_id),
            self.instance,
            None,
        )
        if not handle:
            raise self.ctypes.WinError(self.ctypes.get_last_error())
        return handle

    def _create_controls(self) -> None:
        self.title_handle = self._create_control(
            "STATIC", "Dialogue sound sequence", 0, 0
        )
        self.description_handle = self._create_control(
            "STATIC",
            "Comma-separate entries. Use full SFX names or syllables like a and ME. "
            "null adds a pause; Java // comments are ignored.",
            0,
            0,
        )
        edit_style = 0x0004 | 0x0040 | 0x0080 | 0x00100000 | 0x00200000
        self.editor_handle = self._create_control(
            "EDIT", self.EXAMPLE, edit_style, self.ID_EDITOR, extended_style=0x00000200
        )
        self.interval_label_handle = self._create_control(
            "STATIC", "Start interval (ms):", 0, 0
        )
        self.interval_handle = self._create_control(
            "EDIT", str(DEFAULT_INTERVAL_MS), 0x0080, self.ID_INTERVAL, extended_style=0x00000200
        )
        self.interval_note_handle = self._create_control(
            "STATIC", "The game currently uses 100 ms.", 0, 0
        )
        self.pitch_label_handle = self._create_control(
            "STATIC", "Pitch (semitones):", 0, 0
        )
        self.pitch_handle = self._create_control(
            "EDIT", "0", 0x0080, self.ID_PITCH, extended_style=0x00000200
        )
        self.pitch_up_handle = self._create_control(
            "BUTTON", "▲", 0, self.ID_PITCH_UP
        )
        self.pitch_down_handle = self._create_control(
            "BUTTON", "▼", 0, self.ID_PITCH_DOWN
        )
        self.pitch_note_handle = self._create_control(
            "STATIC", "-12 to +12; same duration.", 0, 0
        )
        self.radio_handle = self._create_control(
            "BUTTON", "Radio effect + hiss", 0x00000003, self.ID_RADIO
        )
        self.dialogue_volume_label_handle = self._create_control(
            "STATIC", "Dialogue volume:", 0, 0
        )
        self.dialogue_volume_handle = self._create_control(
            "msctls_trackbar32", "", 0x00000001 | 0x00010000, self.ID_DIALOGUE_VOLUME
        )
        self.dialogue_volume_value_handle = self._create_control(
            "STATIC", "50%", 0, 0
        )
        self.hiss_volume_label_handle = self._create_control(
            "STATIC", "Radio hiss volume:", 0, 0
        )
        self.hiss_volume_handle = self._create_control(
            "msctls_trackbar32", "", 0x00000001 | 0x00010000, self.ID_HISS_VOLUME
        )
        self.hiss_volume_value_handle = self._create_control(
            "STATIC", "50%", 0, 0
        )
        slider_range = 100 << 16
        for slider in (self.dialogue_volume_handle, self.hiss_volume_handle):
            self.user32.SendMessageW(slider, self.TBM_SETRANGE, 1, slider_range)
            self.user32.SendMessageW(
                slider, self.TBM_SETPOS, 1, round(VOLUME_SLIDER_UNITY)
            )
        self.example_handle = self._create_control(
            "BUTTON", "Load example", 0, self.ID_EXAMPLE
        )
        self.clear_handle = self._create_control("BUTTON", "Clear", 0, self.ID_CLEAR)
        self.simulate_handle = self._create_control(
            "BUTTON", "Simulate audio", 0, self.ID_SIMULATE
        )
        self.stop_preview_handle = self._create_control(
            "BUTTON", "Stop audio", 0, self.ID_STOP_PREVIEW
        )
        self.export_handle = self._create_control(
            "BUTTON", "Export WAV...", 0x00000001, self.ID_EXPORT
        )
        self.status_handle = self._create_control("STATIC", "", 0, 0)

        font = self.gdi32.GetStockObject(17)  # DEFAULT_GUI_FONT
        for handle in (
            self.title_handle,
            self.description_handle,
            self.editor_handle,
            self.interval_label_handle,
            self.interval_handle,
            self.interval_note_handle,
            self.pitch_label_handle,
            self.pitch_handle,
            self.pitch_up_handle,
            self.pitch_down_handle,
            self.pitch_note_handle,
            self.radio_handle,
            self.dialogue_volume_label_handle,
            self.dialogue_volume_handle,
            self.dialogue_volume_value_handle,
            self.hiss_volume_label_handle,
            self.hiss_volume_handle,
            self.hiss_volume_value_handle,
            self.example_handle,
            self.clear_handle,
            self.simulate_handle,
            self.stop_preview_handle,
            self.export_handle,
            self.status_handle,
        ):
            self.user32.SendMessageW(handle, self.WM_SETFONT, font, 1)

        self._layout(760, 660)
        self._update_native_volume_labels()
        self._update_native_radio_state()
        self._refresh_native_status()

    def _layout(self, width: int, height: int) -> None:
        if not hasattr(self, "editor_handle"):
            return
        move = self.user32.MoveWindow
        editor_height = max(130, height - 310)
        move(self.title_handle, 16, 14, max(100, width - 32), 26, True)
        move(self.description_handle, 16, 44, max(100, width - 32), 38, True)
        move(self.editor_handle, 16, 86, max(100, width - 32), editor_height, True)
        options_y = 98 + editor_height
        move(self.interval_label_handle, 16, options_y, 115, 24, True)
        move(self.interval_handle, 134, options_y - 2, 70, 25, True)
        move(self.interval_note_handle, 218, options_y, 240, 24, True)
        pitch_y = options_y + 30
        move(self.pitch_label_handle, 16, pitch_y, 115, 24, True)
        move(self.pitch_handle, 134, pitch_y - 4, 70, 30, True)
        move(self.pitch_up_handle, 204, pitch_y - 4, 22, 15, True)
        move(self.pitch_down_handle, 204, pitch_y + 11, 22, 15, True)
        move(self.pitch_note_handle, 234, pitch_y, 150, 24, True)
        move(self.radio_handle, max(392, width - 174), pitch_y - 2, 158, 25, True)
        dialogue_volume_y = options_y + 62
        move(self.dialogue_volume_label_handle, 16, dialogue_volume_y + 4, 115, 24, True)
        move(
            self.dialogue_volume_handle,
            134,
            dialogue_volume_y,
            max(120, width - 222),
            30,
            True,
        )
        move(
            self.dialogue_volume_value_handle,
            max(270, width - 78),
            dialogue_volume_y + 4,
            62,
            24,
            True,
        )
        hiss_volume_y = options_y + 94
        move(self.hiss_volume_label_handle, 16, hiss_volume_y + 4, 115, 24, True)
        move(
            self.hiss_volume_handle,
            134,
            hiss_volume_y,
            max(120, width - 222),
            30,
            True,
        )
        move(
            self.hiss_volume_value_handle,
            max(270, width - 78),
            hiss_volume_y + 4,
            62,
            24,
            True,
        )
        buttons_y = options_y + 130
        move(self.example_handle, 16, buttons_y, 105, 30, True)
        move(self.clear_handle, 129, buttons_y, 75, 30, True)
        move(self.simulate_handle, 212, buttons_y, 118, 30, True)
        move(self.stop_preview_handle, 338, buttons_y, 82, 30, True)
        move(self.export_handle, max(220, width - 136), buttons_y, 120, 30, True)
        move(self.status_handle, 16, buttons_y + 42, max(100, width - 32), 36, True)

    def _window_proc(self, hwnd: int, message: int, wparam: int, lparam: int) -> int:
        if message == 0x0001:  # WM_CREATE
            self.hwnd = hwnd
            self._create_controls()
            return 0
        if message == self.WM_SIZE:
            width = lparam & 0xFFFF
            height = (lparam >> 16) & 0xFFFF
            self._layout(width, height)
            return 0
        if message == self.WM_HSCROLL:
            self._update_native_volume_labels()
            return 0
        if message == self.WM_COMMAND:
            control_id = wparam & 0xFFFF
            notification = (wparam >> 16) & 0xFFFF
            if control_id == self.ID_EDITOR and notification == self.EN_CHANGE:
                self._refresh_native_status()
                return 0
            if notification == self.BN_CLICKED:
                if control_id == self.ID_EXAMPLE:
                    self.user32.SetWindowTextW(self.editor_handle, self.EXAMPLE)
                elif control_id == self.ID_CLEAR:
                    self.user32.SetWindowTextW(self.editor_handle, "")
                    self.user32.SetFocus(self.editor_handle)
                elif control_id == self.ID_PITCH_UP:
                    self._adjust_native_pitch(0.5)
                elif control_id == self.ID_PITCH_DOWN:
                    self._adjust_native_pitch(-0.5)
                elif control_id == self.ID_RADIO:
                    self._update_native_radio_state()
                elif control_id == self.ID_SIMULATE:
                    self._native_simulate()
                elif control_id == self.ID_STOP_PREVIEW:
                    self._native_stop_preview()
                elif control_id == self.ID_EXPORT:
                    self._native_export()
                return 0
        if message == self.WM_DESTROY:
            self.preview_player.stop()
            self.user32.PostQuitMessage(0)
            return 0
        return self.user32.DefWindowProcW(hwnd, message, wparam, lparam)

    def _get_text(self, handle: int) -> str:
        length = self.user32.GetWindowTextLengthW(handle)
        buffer = self.ctypes.create_unicode_buffer(length + 1)
        self.user32.GetWindowTextW(handle, buffer, len(buffer))
        return buffer.value

    def _set_status(self, text: str) -> None:
        if hasattr(self, "status_handle"):
            self.user32.SetWindowTextW(self.status_handle, text)

    def _native_tokens(self) -> list[str | None]:
        return parse_sequence(self._get_text(self.editor_handle), self.catalog)

    def _native_interval(self) -> int:
        try:
            interval = int(self._get_text(self.interval_handle).strip())
        except ValueError as exc:
            raise SequenceError("The interval must be a whole number of milliseconds.") from exc
        if not 1 <= interval <= 10_000:
            raise SequenceError("The interval must be between 1 and 10000 ms.")
        return interval

    def _adjust_native_pitch(self, amount: float) -> None:
        try:
            current = float(self._get_text(self.pitch_handle).strip())
            if not math.isfinite(current):
                current = 0.0
        except ValueError:
            current = 0.0
        adjusted = max(
            MIN_PITCH_SEMITONES,
            min(MAX_PITCH_SEMITONES, current + amount),
        )
        self.user32.SetWindowTextW(self.pitch_handle, f"{adjusted:g}")
        self.user32.SetFocus(self.pitch_handle)

    def _native_pitch(self) -> float:
        try:
            pitch = float(self._get_text(self.pitch_handle).strip())
        except ValueError as exc:
            raise SequenceError("Pitch must be a number of semitones.") from exc
        if not MIN_PITCH_SEMITONES <= pitch <= MAX_PITCH_SEMITONES:
            raise SequenceError("Pitch must be between -12 and +12 semitones.")
        return pitch

    def _native_radio_effect(self) -> bool:
        state = self.user32.SendMessageW(self.radio_handle, self.BM_GETCHECK, 0, 0)
        return state == self.BST_CHECKED

    def _native_dialogue_volume(self) -> float:
        position = self.user32.SendMessageW(
            self.dialogue_volume_handle, self.TBM_GETPOS, 0, 0
        )
        return max(0.0, min(MAX_VOLUME_GAIN, position / VOLUME_SLIDER_UNITY))

    def _native_hiss_volume(self) -> float:
        position = self.user32.SendMessageW(
            self.hiss_volume_handle, self.TBM_GETPOS, 0, 0
        )
        return max(0.0, min(MAX_VOLUME_GAIN, position / VOLUME_SLIDER_UNITY))

    def _update_native_volume_labels(self) -> None:
        if not hasattr(self, "dialogue_volume_handle"):
            return
        dialogue_percent = self.user32.SendMessageW(
            self.dialogue_volume_handle, self.TBM_GETPOS, 0, 0
        )
        hiss_percent = self.user32.SendMessageW(
            self.hiss_volume_handle, self.TBM_GETPOS, 0, 0
        )
        self.user32.SetWindowTextW(
            self.dialogue_volume_value_handle, f"{dialogue_percent}%"
        )
        self.user32.SetWindowTextW(self.hiss_volume_value_handle, f"{hiss_percent}%")

    def _update_native_radio_state(self) -> None:
        if hasattr(self, "hiss_volume_handle"):
            self.user32.EnableWindow(
                self.hiss_volume_handle, self._native_radio_effect()
            )

    def _refresh_native_status(self) -> None:
        if not hasattr(self, "editor_handle"):
            return
        try:
            tokens = self._native_tokens()
            sounds = sum(token is not None for token in tokens)
            pauses = len(tokens) - sounds
            self._set_status(
                f"Valid sequence: {len(tokens)} slots ({sounds} sounds, {pauses} null pauses)."
            )
        except SequenceError as exc:
            self._set_status(str(exc))

    def _message(self, title: str, text: str, error: bool = False) -> None:
        icon = 0x00000010 if error else 0x00000040
        self.user32.MessageBoxW(self.hwnd, text, title, icon)

    def _choose_output_path(self) -> Path | None:
        buffer = self.ctypes.create_unicode_buffer(32_768)
        buffer.value = "dialogue_sequence.wav"
        dialog = self.OPENFILENAMEW()
        dialog.lStructSize = self.ctypes.sizeof(self.OPENFILENAMEW)
        dialog.hwndOwner = self.hwnd
        dialog.lpstrFilter = "WAV audio (*.wav)\0*.wav\0All files (*.*)\0*.*\0\0"
        dialog.nFilterIndex = 1
        dialog.lpstrFile = self.ctypes.cast(buffer, self.wintypes.LPWSTR)
        dialog.nMaxFile = len(buffer)
        dialog.lpstrTitle = "Save dialogue sequence"
        dialog.Flags = 0x00000002 | 0x00000800 | 0x00080000
        dialog.lpstrDefExt = "wav"
        if not self.comdlg32.GetSaveFileNameW(self.ctypes.byref(dialog)):
            return None
        return Path(buffer.value)

    def _native_simulate(self) -> None:
        try:
            tokens = self._native_tokens()
            interval = self._native_interval()
            pitch = self._native_pitch()
        except SequenceError as exc:
            self._message("Cannot simulate audio", str(exc), error=True)
            return

        self.user32.EnableWindow(self.simulate_handle, False)
        self._set_status("Rendering audio simulation...")
        self.user32.UpdateWindow(self.hwnd)
        try:
            preview_path = self.preview_player.prepare_path()
            duration = render_sequence(
                tokens,
                self.catalog,
                preview_path,
                interval,
                pitch,
                self._native_radio_effect(),
                self._native_dialogue_volume(),
                self._native_hiss_volume(),
            )
            self.preview_player.play()
        except (OSError, SequenceError) as exc:
            self.preview_player.stop()
            self._set_status("Audio simulation failed.")
            self._message("Simulation failed", str(exc), error=True)
        else:
            self._set_status(f"Simulating current sequence ({duration:.2f} s).")
        finally:
            self.user32.EnableWindow(self.simulate_handle, True)

    def _native_stop_preview(self) -> None:
        self.preview_player.stop()
        self._set_status("Audio simulation stopped.")

    def _native_export(self) -> None:
        try:
            tokens = self._native_tokens()
            interval = self._native_interval()
            pitch = self._native_pitch()
        except SequenceError as exc:
            self._message("Cannot export sequence", str(exc), error=True)
            return

        output_path = self._choose_output_path()
        if output_path is None:
            return

        self.user32.EnableWindow(self.export_handle, False)
        self._set_status("Rendering WAV...")
        self.user32.UpdateWindow(self.hwnd)
        try:
            duration = render_sequence(
                tokens,
                self.catalog,
                output_path,
                interval,
                pitch,
                self._native_radio_effect(),
                self._native_dialogue_volume(),
                self._native_hiss_volume(),
            )
        except (OSError, SequenceError) as exc:
            self._set_status("Export failed.")
            self._message("Export failed", str(exc), error=True)
        else:
            self._set_status(
                f"Saved {len(tokens)} slots ({duration:.2f} s) to {output_path}"
            )
            self._message(
                "WAV exported",
                f"The dialogue sequence was saved successfully.\n\n{output_path}",
            )
        finally:
            self.user32.EnableWindow(self.export_handle, True)

    def run(self) -> None:
        message = self.MSG()
        while True:
            result = self.user32.GetMessageW(self.ctypes.byref(message), None, 0, 0)
            if result == 0:
                return
            if result == -1:
                raise self.ctypes.WinError(self.ctypes.get_last_error())
            self.user32.TranslateMessage(self.ctypes.byref(message))
            self.user32.DispatchMessageW(self.ctypes.byref(message))


def main() -> None:
    try:
        app = DialogueSequenceApp()
        print("[Dialogue GUI] Interface: Tkinter", flush=True)
        app.run()
    except Exception as gui_error:
        if os.name == "nt":
            try:
                native_app = NativeWindowsDialogueSequenceApp()
                print("[Dialogue GUI] Interface: native Windows fallback", flush=True)
                native_app.run()
                return
            except Exception as native_error:
                print(f"GUI error: {gui_error}", file=sys.stderr)
                print(f"Native GUI error: {native_error}", file=sys.stderr)
                raise SystemExit(1) from native_error
        if not isinstance(gui_error, SequenceError):
            raise
        # On non-Windows systems this reports catalog or audio asset errors.
        print(f"Error: {gui_error}", file=sys.stderr)
        raise SystemExit(1) from gui_error


if __name__ == "__main__":
    main()
