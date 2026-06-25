# TutorAI Backend

FastAPI backend for TutorAI. **Phases 2–4 done:** a real LangGraph agent graph
(plan → svg → narration → validate → repair loop → render → assemble) runs on
**mock** providers with no key, and on **real Gemini** (`google-genai`) when
configured — verified live end-to-end. Swap via env: `TUTOR_LLM_PROVIDER`,
`TUTOR_TTS_PROVIDER` (mock|gemini), `TUTOR_GENERATION_ENGINE` (langgraph|mock),
`TUTOR_STORAGE_BACKEND` (memory|filesystem). Confirm Gemini model ids with
`python -m app.tools.list_models`.

## Layout

```
app/
├── main.py            # composition root + app factory
├── core/              # config, logging, typed errors, id helpers
├── domain/            # entities, DTO schemas, generation value objects, mappers
├── providers/         # LLM + TTS ports (Strategy) + mock adapters + factory
├── repositories/      # lesson/asset/job ports + in-memory implementations
├── agents/            # generation graph port, deterministic validator, mock graph
├── services/          # LessonBuilder, generation facade, async job manager
└── api/               # routers, DI deps, error envelope
tests/                 # health, validator, generation, lessons API
```

Design patterns: Strategy, Abstract Factory, Adapter, Builder, Repository, Facade,
DTO, Dependency Injection — see [../docs/04-ai-agent-design.md](../docs/04-ai-agent-design.md).

## Setup & run

```bash
# from the backend/ directory
uv venv
uv pip install -r requirements-dev.txt
PYTHONPATH=. uv run uvicorn app.main:app --reload
```

## Test

```bash
PYTHONPATH=. uv run pytest
```

## Try it

```bash
# start generation (cache miss → 202 + job_id)
curl -s -X POST localhost:8000/api/v1/lessons \
  -H 'content-type: application/json' -d '{"topic":"Photosynthesis"}'

# poll the job
curl -s localhost:8000/api/v1/lessons/jobs/<job_id>

# fetch the manifest, then download assets
curl -s localhost:8000/api/v1/lessons/<lesson_id>
```

Interactive docs at `http://localhost:8000/docs`.
