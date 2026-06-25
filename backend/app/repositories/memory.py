"""In-memory repository implementations (Phase 2)."""
import asyncio

from app.core.ids import new_asset_id, new_job_id
from app.domain.models import AssetRef, Job, Lesson
from app.repositories.base import AssetRepository, JobRepository, LessonRepository

_ACTIVE = ("queued", "running")
_TERMINAL = ("succeeded", "failed")


class InMemoryLessonRepository(LessonRepository):
    def __init__(self) -> None:
        self._by_key: dict[str, Lesson] = {}
        self._by_id: dict[str, Lesson] = {}
        self._hits: dict[str, int] = {}

    async def get_by_topic_key(self, topic_key: str) -> Lesson | None:
        return self._by_key.get(topic_key)

    async def get(self, lesson_id: str) -> Lesson | None:
        return self._by_id.get(lesson_id)

    async def save(self, lesson: Lesson) -> None:
        self._by_key[lesson.topic_key] = lesson
        self._by_id[lesson.id] = lesson
        self._hits.setdefault(lesson.topic_key, 0)

    async def increment_hits(self, topic_key: str) -> None:
        self._hits[topic_key] = self._hits.get(topic_key, 0) + 1


class InMemoryAssetRepository(AssetRepository):
    def __init__(self, base_url: str) -> None:
        self._items: dict[str, tuple[bytes, str, str]] = {}
        self._base = base_url.rstrip("/")

    def url_for(self, asset_id: str) -> str:
        return f"{self._base}/{asset_id}"

    async def put(self, data: bytes, content_type: str, kind: str) -> AssetRef:
        asset_id = new_asset_id()
        self._items[asset_id] = (data, content_type, kind)
        return AssetRef(
            id=asset_id, kind=kind, content_type=content_type, url=self.url_for(asset_id)
        )

    async def get(self, asset_id: str) -> tuple[bytes, str] | None:
        item = self._items.get(asset_id)
        if item is None:
            return None
        data, content_type, _kind = item
        return data, content_type


class InMemoryJobRepository(JobRepository):
    def __init__(self) -> None:
        self._by_id: dict[str, Job] = {}
        self._active_by_key: dict[str, str] = {}
        self._lock = asyncio.Lock()

    async def get(self, job_id: str) -> Job | None:
        return self._by_id.get(job_id)

    async def get_or_create(self, topic_key: str) -> tuple[Job, bool]:
        async with self._lock:
            existing_id = self._active_by_key.get(topic_key)
            if existing_id:
                job = self._by_id.get(existing_id)
                if job and job.status in _ACTIVE:
                    return job, False
            job = Job(id=new_job_id(), topic_key=topic_key)
            self._by_id[job.id] = job
            self._active_by_key[topic_key] = job.id
            return job, True

    async def update(self, job_id: str, **changes) -> Job | None:
        job = self._by_id.get(job_id)
        if job is None:
            return None
        updated = job.model_copy(update=changes)
        self._by_id[job_id] = updated
        if updated.status in _TERMINAL and self._active_by_key.get(updated.topic_key) == job_id:
            self._active_by_key.pop(updated.topic_key, None)
        return updated
