from app.core.audio import pcm_duration_ms, pcm_to_wav


def test_pcm_to_wav_has_riff_wave_header():
    wav = pcm_to_wav(b"\x00\x00" * 1000, 24000)
    assert wav[:4] == b"RIFF"
    assert wav[8:12] == b"WAVE"
    assert len(wav) > 2000  # header added on top of pcm


def test_pcm_duration_exact_one_second():
    # 24000 frames * 2 bytes at 24 kHz, 16-bit mono == 1.000 s
    assert pcm_duration_ms(b"\x00\x00" * 24000, 24000) == 1000


def test_pcm_duration_half_second():
    assert pcm_duration_ms(b"\x00\x00" * 12000, 24000) == 500


def test_pcm_duration_empty():
    assert pcm_duration_ms(b"", 24000) == 0
