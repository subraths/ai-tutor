"""Repository ports (Repository pattern).

In-memory implementations live in memory.py (Phase 2). Phase 4 swaps in
PostgreSQL + object storage behind these same interfaces.
"""
from abc import ABC, abstractmethod

from app.domain.models import AssetRef, Job, Lesson


class LessonRepository(ABC):
    @abstractmethod
    async def get_by_topic_key(self, topic_key: str) -> Lesson | None:
        ...

    @abstractmethod
    async def get(self, lesson_id: str) -> Lesson | None:
        ...

    @abstractmethod
    async def save(self, lesson: Lesson) -> None:
        ...

    @abstractmethod
    async def increment_hits(self, topic_key: str) -> None:
        ...


class AssetRepository(ABC):
    @abstractmethod
    def url_for(self, asset_id: str) -> str:
        ...

    @abstractmethod
    async def put(self, data: bytes, content_type: str, kind: str) -> AssetRef:
        ...

    @abstractmethod
    async def get(self, asset_id: str) -> tuple[bytes, str] | None:
        """Return (data, content_type) or None."""
        ...


class JobRepository(ABC):
    @abstractmethod
    async def get(self, job_id: str) -> Job | None:
        ...

    @abstractmethod
    async def get_or_create(self, topic_key: str) -> tuple[Job, bool]:
        """Return (job, created). Reuses an in-flight job for the same topic_key."""
        ...

    @abstractmethod
    async def update(self, job_id: str, **changes) -> Job | None:
        ...
