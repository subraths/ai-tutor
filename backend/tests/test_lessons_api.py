"""End-to-end API tests over the async job flow."""
from tests.helpers import wait_for_job


async def test_generate_then_fetch_manifest_and_assets(client):
    r = await client.post("/api/v1/lessons", json={"topic": "Photosynthesis"})
    assert r.status_code == 202, r.text
    job_id = r.json()["job_id"]

    job = await wait_for_job(client, job_id)
    assert job["status"] == "succeeded", job
    lesson_id = job["lesson_id"]

    r2 = await client.get(f"/api/v1/lessons/{lesson_id}")
    assert r2.status_code == 200
    manifest = r2.json()
    assert manifest["topic"] == "Photosynthesis"
    assert manifest["segments"]
    assert manifest["total_duration_ms"] > 0

    # every referenced asset downloads
    svg = await client.get(manifest["svg"]["url"])
    assert svg.status_code == 200
    assert svg.headers["content-type"].startswith("image/svg+xml")
    for seg in manifest["segments"]:
        audio = await client.get(seg["audio"]["url"])
        assert audio.status_code == 200
        assert audio.headers["content-type"].startswith("audio/")


async def test_cache_hit_returns_200_after_generation(client):
    r1 = await client.post("/api/v1/lessons", json={"topic": "Gravity"})
    assert r1.status_code == 202
    await wait_for_job(client, r1.json()["job_id"])

    r2 = await client.post("/api/v1/lessons", json={"topic": "gravity"})  # normalized
    assert r2.status_code == 200
    assert r2.json()["topic"] == "Gravity"


async def test_empty_topic_is_rejected(client):
    r = await client.post("/api/v1/lessons", json={"topic": "   "})
    assert r.status_code == 400
    assert r.json()["error"]["code"] == "INVALID_TOPIC"


async def test_missing_topic_is_422(client):
    r = await client.post("/api/v1/lessons", json={})
    assert r.status_code == 422
    assert r.json()["error"]["code"] == "VALIDATION_ERROR"


async def test_job_not_found(client):
    r = await client.get("/api/v1/lessons/jobs/job_missing")
    assert r.status_code == 404
    assert r.json()["error"]["code"] == "JOB_NOT_FOUND"


async def test_lesson_not_found(client):
    r = await client.get("/api/v1/lessons/les_missing")
    assert r.status_code == 404
    assert r.json()["error"]["code"] == "LESSON_NOT_FOUND"


async def test_request_id_header_present(client):
    r = await client.get("/healthz")
    assert r.headers.get("X-Request-ID", "").startswith("req_")
