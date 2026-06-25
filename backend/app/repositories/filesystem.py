"""Filesystem-backed repositories (dev/single-node persistence).

Real audio bytes and generated lessons survive restarts. Same interfaces as the
in-memory repos, so this is a drop-in via settings.storage_backend. A production
deployment would swap these for object storage + PostgreSQL behind the same ports.
"""
import asyncio
import json
from pathlib import Path

from app.core.ids import new_asset_id
from app.domain.models import AssetRef, Lesson
from app.repositories.base import AssetRepository, LessonRepository


class FilesystemAssetRepository(AssetRepository):
    def __init__(self, base_url: str, data_dir: str) -> None:
        self._base = base_url.rstrip("/")
        self._dir = Path(data_dir) / "assets"
        self._dir.mkdir(parents=True, exist_ok=True)

    def url_for(self, asset_id: str) -> str:
        return f"{self._base}/{asset_id}"

    async def put(self, data: bytes, content_type: str, kind: str) -> AssetRef:
        asset_id = new_asset_id()
        (self._dir / asset_id).write_bytes(data)
        (self._dir / f"{asset_id}.meta").write_text(
            json.dumps({"content_type": content_type, "kind": kind})
        )
        return AssetRef(
            id=asset_id, kind=kind, content_type=content_type, url=self.url_for(asset_id)
        )

    async def get(self, asset_id: str) -> tuple[bytes, str] | None:
        blob = self._dir / asset_id
        meta = self._dir / f"{asset_id}.meta"
        if not blob.exists() or not meta.exists():
            return None
        content_type = json.loads(meta.read_text())["content_type"]
        return blob.read_bytes(), content_type


class FilesystemLessonRepository(LessonRepository):
    def __init__(self, data_dir: str) -> None:
        self._dir = Path(data_dir) / "lessons"
        self._dir.mkdir(parents=True, exist_ok=True)
        self._index_path = self._dir / "_index.json"
        self._lock = asyncio.Lock()

    def _read_index(self) -> dict[str, dict]:
        if not self._index_path.exists():
            return {}
        return json.loads(self._index_path.read_text())

    def _write_index(self, index: dict[str, dict]) -> None:
        self._index_path.write_text(json.dumps(index, indent=2))

    def _load(self, lesson_id: str) -> Lesson | None:
        path = self._dir / f"{lesson_id}.json"
        if not path.exists():
            return None
        return Lesson.model_validate(json.loads(path.read_text()))

    async def get_by_topic_key(self, topic_key: str) -> Lesson | None:
        entry = self._read_index().get(topic_key)
        return self._load(entry["lesson_id"]) if entry else None

    async def get(self, lesson_id: str) -> Lesson | None:
        return self._load(lesson_id)

    async def save(self, lesson: Lesson) -> None:
        async with self._lock:
            (self._dir / f"{lesson.id}.json").write_text(
                json.dumps(lesson.model_dump(mode="json"), indent=2)
            )
            index = self._read_index()
            index[lesson.topic_key] = {"lesson_id": lesson.id, "hits": 0}
            self._write_index(index)

    async def increment_hits(self, topic_key: str) -> None:
        async with self._lock:
            index = self._read_index()
            if topic_key in index:
                index[topic_key]["hits"] = index[topic_key].get("hits", 0) + 1
                self._write_index(index)
