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

## 8.5 Progress

- **Phase 2 ✅** — Backend skeleton: FastAPI + mock providers, async job model,
  in-memory repos, deterministic validator gate, error envelope. 16 tests.
- **Phase 3 ✅** — Real LangGraph `StateGraph` (`app/agents/langgraph_graph.py`)
  implementing the `GenerationGraph` port: planner → svg → narration → validate
  with a conditional **repair loop** (bounded by `max_repair_retries`) → render →
  assemble. Drives the same provider ports, so still runs on mocks. 21 tests
  (incl. repair-recovery and exhausted-retries failure).
- **Phase 4 ✅** — Real Gemini providers (`app/providers/gemini.py`) via google-genai:
  structured-output planning/narration/repair, SVG generation, and TTS (PCM→WAV with
  exact byte-derived duration), with transient-error retry/backoff. Filesystem asset +
  lesson repositories. `python -m app.tools.list_models` confirms model ids.
  **Verified live (2026-06-25):** a real Photosynthesis lesson — 7 segments, valid SVG
  with semantic ids, narration consistent (validator passed), real WAV audio. 43 tests
  (Gemini adapters covered via a faked SDK client, so the suite stays key-free).
  Verified models: text `gemini-2.5-flash` / `gemini-2.5-flash-lite`; TTS
  `gemini-3.1-flash-tts-preview`.

- **Phase 5 ✅** — Android skeleton (`android/`): Kotlin + Compose, MVVM + Clean
  Architecture (domain/data/ui), Retrofit `TutorApi`, `LessonRepositoryImpl` that does
  the **async job polling**, `TopicScreen` that generates a lesson and renders its
  manifest (title, segments, highlight ids, durations). Manual `AppContainer` DI.
  **Verified:** `:app:assembleDebug` builds `app-debug.apk` (AGP 8.9.1 / Gradle 8.13 /
  Kotlin 2.1.0 / compileSdk 35). Decision: chose manual DI over Hilt for build
  reliability (deviates from [07](07-android-architecture.md) §7.1; Hilt swappable later).

- **Phase 6 ✅** — Player + highlight sync (`android/.../ui/player/`): SVG rendered in a
  WebView (inline SVG + `highlight()`/`clearHighlight()` JS, glow+scale CSS); ExoPlayer
  plays one clip per segment as a playlist, and `currentMediaItemIndex` drives the
  WebView highlight of that segment's `svg_element_ids`. Transport: Prev/Play-Pause/
  Next/Replay + live caption. Asset URLs resolved to absolute against the base URL.
  **Build verified** (`:app:assembleDebug`). Not yet run on a device (no AVD here), and
  real audio playback needs the **gemini** backend (mock audio bytes won't decode).

- **Phase 7 ✅** — Offline & history (`android/.../data/local/`, `ui/history/`): Room
  (`LessonEntity`/`SegmentEntity` + DAO, SQL compile-checked by KSP) + `LessonFileStore`.
  "Save offline" downloads the svg + every audio clip to app-private files and persists
  rows; History screen lists saved lessons (newest first) with delete; the player is
  source-agnostic (a `loader` lambda) so saved lessons load from **local file URIs** and
  replay with **no network**. **Build verified** (KSP generated `LessonDao_Impl`).

> **Original product spec is now feature-complete** (Phases 1–7): topic → narrated,
> highlight-synced SVG lesson, with on-device history and offline replay.

- **Phase 8 ✅** — Hardening & CI/CD (see [09](09-cicd.md)). Backend wired to real
  providers/storage by config (`generation_engine=langgraph` default; gemini LLM/TTS;
  memory|filesystem storage). GitHub Actions: `backend-ci` (pytest + `docker build`),
  `backend-deploy` (GHCR → SSH deploy to EC2), `android-ci` (`assembleDebug` artifact),
  `android-release` (signed APK → GitHub Release, debug-key fallback). Containerized
  backend (`Dockerfile`/`docker-compose.yml`) with `/healthz` healthcheck. The test
  suite is **hermetic** — a session fixture disables `.env` loading and strips `TUTOR_*`
  vars so the 43 tests stay key-free and never touch the live API. **Verified locally:**
  43 tests green (0 live API calls); `docker build` + container smoke (`/healthz` 200,
  `POST /api/v1/lessons` 202 on mock providers, no key); Android release signing/BASE_URL
  wiring consumes the workflow env vars.

### Remaining (release)

- **Phase 9** — release prep: app icon, signing, perf pass, deploy profile for the backend.

Run on a device to exercise runtime (no AVD in the build env yet). Optional backend
add-on (deepagents topic *researcher/planner* behind `LLMProvider.plan_concepts`)
remains deferred — would not change the deterministic core.
