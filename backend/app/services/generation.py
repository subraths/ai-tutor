"""LessonGenerationFacade — one entry point over cache lookup + graph + persist."""
from app.agents.base import GenerationGraph, ProgressCb
from app.domain.generation import GenerationOptions
from app.domain.models import Lesson
from app.repositories.base import LessonRepository


class LessonGenerationFacade:
    def __init__(self, graph: GenerationGraph, lessons: LessonRepository) -> None:
        self._graph = graph
        self._lessons = lessons

    async def get_cached(self, topic_key: str) -> Lesson | None:
        return await self._lessons.get_by_topic_key(topic_key)

    async def generate(
        self,
        topic: str,
        topic_key: str,
        options: GenerationOptions,
        progress: ProgressCb = None,
    ) -> Lesson:
        lesson = await self._graph.generate(topic, topic_key, options, progress)
        await self._lessons.save(lesson)
        return lesson
