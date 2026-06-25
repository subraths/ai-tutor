from app.domain.models import Lesson, Segment
from app.repositories.filesystem import (
    FilesystemAssetRepository,
    FilesystemLessonRepository,
)


def _lesson() -> Lesson:
    return Lesson(
        id="les_x",
        topic="Photosynthesis",
        topic_key="photosynthesis|en",
        title="Title",
        svg='<svg viewBox="0 0 1 1"><rect id="a"/></svg>',
        svg_asset_id="ast_svg",
        segments=[
            Segment(
                index=0,
                text="hi",
                svg_element_ids=["a"],
                audio_asset_id="ast_a",
                duration_ms=100,
            )
        ],
        total_duration_ms=100,
    )


async def test_asset_roundtrip(tmp_path):
    repo = FilesystemAssetRepository("/api/v1/assets", str(tmp_path))
    ref = await repo.put(b"hello-bytes", "audio/wav", "audio")
    assert await repo.get(ref.id) == (b"hello-bytes", "audio/wav")
    assert repo.url_for(ref.id).endswith(ref.id)


async def test_asset_missing_returns_none(tmp_path):
    repo = FilesystemAssetRepository("/api/v1/assets", str(tmp_path))
    assert await repo.get("ast_missing") is None


async def test_lesson_roundtrip_and_persists_across_instances(tmp_path):
    repo = FilesystemLessonRepository(str(tmp_path))
    lesson = _lesson()
    await repo.save(lesson)

    assert (await repo.get(lesson.id)).id == lesson.id
    assert (await repo.get_by_topic_key("photosynthesis|en")).id == lesson.id
    await repo.increment_hits("photosynthesis|en")

    # A fresh instance over the same dir sees the saved data.
    repo2 = FilesystemLessonRepository(str(tmp_path))
    reloaded = await repo2.get_by_topic_key("photosynthesis|en")
    assert reloaded.title == "Title"
    assert reloaded.segments[0].svg_element_ids == ["a"]


async def test_lesson_missing_returns_none(tmp_path):
    repo = FilesystemLessonRepository(str(tmp_path))
    assert await repo.get("les_missing") is None
    assert await repo.get_by_topic_key("nope|en") is None
