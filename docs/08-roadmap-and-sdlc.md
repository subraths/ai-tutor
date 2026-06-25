# 08 — Roadmap & SDLC

We build in vertical-ish phases, each with a clear **exit criterion**. Providers are
mockable from day one so backend phases don't depend on live AI keys.

## 8.1 Phases

| Phase | Name | Deliverables | Exit criterion |
|---|---|---|---|
| **0** | Vision & requirements | This decision set + [01](01-vision-and-requirements.md) | Requirements agreed ✅ |
| **1** | Architecture & design | This `docs/` set (UML) | Design reviewed & approved ⬅️ *here* |
| **2** | Backend skeleton | FastAPI app, `Settings`, schemas, error envelope, `/healthz`, provider **interfaces** + **mock** impls | `POST /lessons` returns a mock lesson end-to-end |
| **3** | AI pipeline | LangGraph nodes, agent state, validator + repair loop (mock LLM) | Graph produces a **valid** lesson from a mock; validator rejects bad IDs |
| **4** | Real generation | Gemini + TTS adapters, asset store, cache repo, async jobs | Real topic → cached lesson with audio; cache hit path works |
| **5** | Android skeleton | Compose nav, Topic screen, Retrofit client, job polling | App generates a lesson and shows its manifest |
| **6** | Player + highlight | SVG renderer, highlight engine, ExoPlayer segment loop | Narration plays with parts highlighting in sync |
| **7** | Offline & history | Room, file store, downloader, history screen, delete | Saved lesson replays with **airplane mode on** |
| **8** | Hardening | Tests (unit/contract/instrumentation), logging/metrics, retries, rate limits, CI | Green CI; error paths covered |
| **9** | Release prep | Packaging, perf pass, docs, deploy profile | Installable build + deployable backend |

## 8.2 Milestones

- **M1 (end P3):** Backend produces validated lessons from mocks — *de-risks the agent design*.
- **M2 (end P4):** First real Gemini-generated narrated lesson with audio.
- **M3 (end P6):** First synchronized highlight playback on device — *the product's "wow"*.
- **M4 (end P7):** Full offline replay + history — *feature-complete v1*.
- **M5 (end P9):** Shippable.

## 8.3 Testing strategy

| Level | Backend | Android |
|---|---|---|
| Unit | nodes, validator, builder, providers (mocked) | ViewModels, UseCases, mappers |
| Contract | API request/response schema tests | Repository ↔ API contract (MockWebServer) |
| Integration | graph end-to-end with mock providers; cache + asset store | Room DAO tests |
| E2E / instrumentation | golden-topic generation smoke | Compose UI + offline-playback instrumentation |

**Definition of Done (per phase):** code + tests + docs updated + diagrams still accurate.

## 8.4 Risks & mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| **SVG quality/consistency** from LLM | Poor diagrams | Plan→generate→validate→**repair loop**; low temperature; golden-topic eval set |
| **ID ↔ narration mismatch** | Broken highlighting | Hard validator gate: never cache if any referenced ID is missing |
| **Gemini model id drift** | Build breaks | Model name is **config**; verify against live API; provider adapter isolates it |
| **TTS sync drift** | Highlight desync | Segment-level clips + measured durations make sync deterministic (no streaming timing) |
| **Cold-start latency** | Slow first lesson | Async jobs + progress UI; cache; parallelize TTS per segment |
| **Cost** | Spend per generation | Shared cache by topic key; request coalescing; bounded retries |
| **SVG security** (injected script) | Client risk | Sanitize SVG; if WebView, disable JS-from-content / restrict to our own highlight script |
| **Renderer choice** (WebView vs native) | Rework | Phase-6 spike behind a `HighlightEngine` interface before committing |

## 8.5 Immediate next step

Proceed to **Phase 2 — Backend skeleton**:
1. Scaffold `backend/` (FastAPI, Pydantic schemas from [03](03-domain-model.md)/[06](06-api-contract.md), typed `Settings`, error envelope, `/healthz`).
2. Define `LLMProvider` / `TTSProvider` / repository **interfaces** + **mock** implementations.
3. Wire `POST /lessons` → facade → mock graph → manifest, with the async job model.

This delivers a runnable end-to-end backend with **no AI keys required**, ready for the
real graph in Phase 3.
