"""Audio helpers — wrap raw PCM (as returned by Gemini TTS) into a WAV container
and derive an exact clip duration from the byte length.

Exact, byte-derived duration is what makes the "pre-rendered timed segments" sync
model deterministic (docs/01 §1.8): no streaming timestamps required.
"""
import io
import wave


def pcm_to_wav(
    pcm: bytes, sample_rate: int = 24000, channels: int = 1, sample_width: int = 2
) -> bytes:
    buf = io.BytesIO()
    with wave.open(buf, "wb") as w:
        w.setnchannels(channels)
        w.setsampwidth(sample_width)
        w.setframerate(sample_rate)
        w.writeframes(pcm)
    return buf.getvalue()


def pcm_duration_ms(
    pcm: bytes, sample_rate: int = 24000, channels: int = 1, sample_width: int = 2
) -> int:
    bytes_per_frame = channels * sample_width
    if bytes_per_frame <= 0 or sample_rate <= 0:
        return 0
    frames = len(pcm) / bytes_per_frame
    return int(round(frames / sample_rate * 1000))
