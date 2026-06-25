import asyncio
import time


async def wait_for_job(client, job_id: str, timeout: float = 5.0) -> dict:
    """Poll a job until it reaches a terminal state; let the background task run."""
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        r = await client.get(f"/api/v1/lessons/jobs/{job_id}")
        assert r.status_code == 200, r.text
        data = r.json()
        if data["status"] in ("succeeded", "failed"):
            return data
        await asyncio.sleep(0.02)
    raise AssertionError(f"job {job_id} did not finish within {timeout}s")
