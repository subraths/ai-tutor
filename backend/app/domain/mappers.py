"""Domain entity -> API DTO mappers."""
from app.domain.models import Lesson
from app.domain.schemas import AssetRefOut, LessonManifest, SegmentOut
from app.repositories.base import AssetRepository


def to_manifest(lesson: Lesson, assets: AssetRepository) -> LessonManifest:
    return LessonManifest(
        id=lesson.id,
        topic=lesson.topic,
        title=lesson.title,
        language=lesson.language,
        voice=lesson.voice,
        total_duration_ms=lesson.total_duration_ms,
        created_at=lesson.created_at,
        svg=AssetRefOut(
            asset_id=lesson.svg_asset_id,
            url=assets.url_for(lesson.svg_asset_id),
        ),
        segments=[
            SegmentOut(
                index=seg.index,
                text=seg.text,
                svg_element_ids=seg.svg_element_ids,
                audio=AssetRefOut(
                    asset_id=seg.audio_asset_id,
                    url=assets.url_for(seg.audio_asset_id),
                ),
                duration_ms=seg.duration_ms,
            )
            for seg in lesson.segments
        ],
    )
