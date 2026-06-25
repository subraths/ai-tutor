"""Async JobManager — runs generation in the background and tracks status.

Identical concurrent topics coalesce onto a single job via the JobRepository, so a
popular topic is generated once even under a thundering herd.
"""
import asyncio
import logging

from app.core.errors import DomainError
from app.domain.generation import GenerationOptions
from app.domain.models import Job
from app.repositories.base import JobRepository
from app.services.generation import LessonGenerationFacade

log = logging.getLogger("tutor.jobs")


class JobManager:
    def __init__(self, jobs: JobRepository, facade: LessonGenerationFacade) -> None:
        self._jobs = jobs
        self._facade = facade
        self._tasks: set[asyncio.Task] = set()

    async def submit(
        self, topic: str, topic_key: str, options: GenerationOptions
    ) -> tuple[Job, bool]:
        job, created = await self._jobs.get_or_create(topic_key)
        if created:
            task = asyncio.create_task(self._run(job.id, topic, topic_key, options))
            self._tasks.add(task)
            task.add_done_callback(self._tasks.discard)
        return job, created

    async def get(self, job_id: str) -> Job:
        job = await self._jobs.get(job_id)
        if job is None:
            from app.core.errors import JobNotFoundError

            raise JobNotFoundError(f"Job {job_id} not found")
        return job

    async def _run(
        self, job_id: str, topic: str, topic_key: str, options: GenerationOptions
    ) -> None:
        await self._jobs.update(job_id, status="running", stage="planning", progress=1)

        async def progress(stage: str, pct: int) -> None:
            await self._jobs.update(job_id, stage=stage, progress=pct)

        try:
            lesson = await self._facade.generate(topic, topic_key, options, progress)
            await self._jobs.update(
                job_id,
                status="succeeded",
                stage="done",
                progress=100,
                lesson_id=lesson.id,
            )
        except DomainError as exc:
            log.warning("generation failed for %s: %s", topic_key, exc.message)
            await self._jobs.update(job_id, status="failed", error=exc.message)
        except Exception as exc:  # noqa: BLE001 - last-resort guard for the task
            log.exception("unexpected generation error for %s", topic_key)
            await self._jobs.update(job_id, status="failed", error=str(exc))
