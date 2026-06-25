"""Generation graph port.

Phase 2 ships MockGenerationGraph. Phase 3 adds a LangGraph-backed implementation
of this same interface; the facade and job manager don't change.
"""
from abc import ABC, abstractmethod
from typing import Awaitable, Callable, Optional

from app.domain.generation import GenerationOptions
from app.domain.models import Lesson

# (stage, progress_pct) -> awaitable
ProgressCb = Optional[Callable[[str, int], Awaitable[None]]]


class GenerationGraph(ABC):
    @abstractmethod
    async def generate(
        self,
        topic: str,
        topic_key: str,
        options: GenerationOptions,
        progress: ProgressCb = None,
    ) -> Lesson:
        ...
