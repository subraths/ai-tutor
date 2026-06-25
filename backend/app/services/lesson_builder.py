"""LessonBuilder (Builder pattern) — assemble a valid Lesson incrementally."""
from app.core.errors import GenerationFailedError
from app.core.ids import new_lesson_id
from app.domain.generation import DEFAULT_VOICE
from app.domain.models import Lesson, Segment


class LessonBuilder:
    def __init__(
        self,
        topic: str,
        topic_key: str,
        language: str = "en",
        voice: str = DEFAULT_VOICE,
    ) -> None:
        self._topic = topic
        self._topic_key = topic_key
        self._language = language
        self._voice = voice
        self._title: str | None = None
        self._svg: str | None = None
        self._svg_asset_id: str | None = None
        self._segments: list[Segment] = []

    def with_title(self, title: str) -> "LessonBuilder":
        self._title = title
        return self

    def with_svg(self, svg: str, svg_asset_id: str) -> "LessonBuilder":
        self._svg = svg
        self._svg_asset_id = svg_asset_id
        return self

    def add_segment(
        self,
        index: int,
        text: str,
        svg_element_ids: list[str],
        audio_asset_id: str,
        duration_ms: int,
    ) -> "LessonBuilder":
        self._segments.append(
            Segment(
                index=index,
                text=text,
                svg_element_ids=svg_element_ids,
                audio_asset_id=audio_asset_id,
                duration_ms=duration_ms,
            )
        )
        return self

    def build(self) -> Lesson:
        if self._svg is None or self._svg_asset_id is None:
            raise GenerationFailedError("Cannot build lesson: SVG not set")
        if not self._segments:
            raise GenerationFailedError("Cannot build lesson: no segments")
        return Lesson(
            id=new_lesson_id(),
            topic=self._topic,
            topic_key=self._topic_key,
            title=self._title or self._topic,
            language=self._language,
            voice=self._voice,
            svg=self._svg,
            svg_asset_id=self._svg_asset_id,
            segments=self._segments,
            total_duration_ms=sum(s.duration_ms for s in self._segments),
        )
