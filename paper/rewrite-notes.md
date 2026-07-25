# Rewrite notes — raw material for writing tutorai.tex in your own voice

**How to use this file.** Write each section from these bullets, _without_ the
old PDF open. The bullets are deliberately not sentences — if you catch
yourself just adding "the" and "is" to a bullet, close the file, say the point
out loud, then type what you said. Plainer is fine; your natural phrasing is
the goal. Do **not** run your draft through any "humanizer"/paraphraser tool —
those are word spinners and Turnitin flags them as AI-paraphrased.

**What you do NOT need to rewrite** (not authored prose): the LaTeX preamble,
TikZ figure code (Fig. 1–3), Table I/II contents, Listing 1 (JSON), section
headings, and the 32 references. Keep the existing `tutorai.tex` skeleton and
replace only the running text between headings.

**Suggested order:** IV → V → VI → VII (most familiar, easiest to write) →
III → VIII → IX → II (hardest) → I → Abstract last.

Citation numbers below = the paper's existing reference order. Keep them.

---

## Abstract (write LAST)

Must contain, in some order:

- problem: learners stitch together text + static picture + separate video
- what TutorAI does: typed topic → lesson = named SVG + segmented narration +
  per-segment audio
- playback: current segment highlights its diagram parts while its clip plays
- core difficulty: LLM narration can reference diagram parts that don't exist
  → breaks highlighting
- solution: LangGraph pipeline (plan / draw / critique-refine / narrate /
  validate) + non-LLM validator as hard cache gate
- backend: stateless FastAPI, cache keyed by topic → popular topic generated once
- client: Android, Clean Architecture + MVVM, download once, fully offline replay
  , on-device history
- paper reports: architecture, pipeline, reliability mechanisms, engineering
  practice, verified behaviour, trade-offs

## I. Introduction

- teacher behaviour: point at drawing part + say what it does, simultaneously → listener never guesses
- digital tools break the link (text here, picture there, video elsewhere)
- lesson = 3 artifacts: (1) SVG, every meaningful part has stable human-readable id;
  (2) narration in ordered segments, each lists the ids it covers;
  (3) one TTS clip per segment, duration measured server-side
- sync mechanism: highlight driven by which clip is playing → no word timing, no network
- central challenge = consistency between artifacts, not fluency/beauty
  - example: narration says "chloroplast absorbs light", SVG has no id `chloroplast` → product promise broken
  - hallucination, cite [1]
  - one-shot mega-prompt gives no interception point
- answer: small pipeline of cooperating steps, LangGraph [2]
  - steps: plan → draw → critique+refine → narrate against finished drawing → validate
  - validation = ordinary code (XML parse, collect ids, check every reference)
  - fail → bounded repair loop with targeted fix prompt
  - only passing lessons stored; gate turns probabilistic model into dependable feature
- 4 contributions (bullets in paper):
  1. system design producing self-explaining lessons (SVG ids + segment narration + measured audio)
  2. pipeline w/ separate bounded critique-refine loop + bounded repair loop + non-LLM gate
  3. cost-aware backend: stateless, topic-keyed shared cache, request coalescing, provider adapters (model choice = config)
  4. offline-first client w/ deterministic sync + engineering scaffolding (hermetic tests, container, CI/CD)
- roadmap paragraph: II background, III overview, IV backend, V client, VI engineering, VII results, VIII limitations, IX conclusion

## II. Related Work

Fields: ITS, multimedia-learning psychology, LLMs, agents, diagram generation, TTS, offline-first mobile.

**A. ITS / AI in education**

- Bloom 2-sigma: 1-to-1 tutoring ≫ classroom; open problem = reach that at scale [3]
- cognitive tutors: model student's solution steps, feedback each step [4]
- VanLehn review: good step-based ITS ≈ human tutors [5]
- classic ITS weakness: expensive hand-built domain model per topic
- TutorAI's different trade: no learner model; fresh artifact for ANY topic on demand; breadth over depth
- LLMs-in-education surveys: promise + risks [6]

**B. Words + pictures (why the product form works)**

- dual coding: separate verbal/visual channels, both together aid memory [7]
- cognitive load: small working memory, don't waste it [8]
- Mayer multimedia theory → design rules; spatial/temporal contiguity principle [9], [10]
- TutorAI = direct application: lighting parts during the matching sentence removes the search step
- segment-level (not word-level) sync = deliberate balance: strong contiguity + deterministic + offline

**C. LLMs for content generation**

- transformer [11]; scaling → instruction following, few-shot [12]; Gemini multimodal family (used here) [13]
- two weaknesses that matter: hallucination [1]; free text hard to consume in software
- mitigation used: structured output against schema + validate before trusting

**D. Agents / self-correction**

- decomposition helps: CoT [14], ReAct [15]
- most relevant: Self-Refine [16], Reflexion [17], self-debugging from error messages [18]
  - repair loop = same shape as self-debugging, but errors come from deterministic validator, not runtime
- LangGraph = agent as state graph, nodes/conditional edges/cycles, on LangChain [2], [19]
- contrast w/ open-ended agents: fixed nodes, every loop capped → predictable cost/latency
- prompting practice survey [20]

**E. Diagram generation**

- two styles: pixels (diffusion [21]) vs structured (SVG)
- pixels opaque: can't name a region or attach behaviour
- SVG: W3C standard, any element can carry id [22] → chosen because highlighting needs addressable parts; vector scales
- learned SVG models: DeepSVG [23], StarVector-style code generation [24]
- TutorAI doesn't train a model: prompts a code-capable LLM to emit SVG (common now [25]) — but quality/self-consistency vary → hence critique-refine + structural check

**F. TTS**

- WaveNet [26], Tacotron 2 [27] → natural hosted TTS services
- key detail for sync: duration; one clip per segment, exact length measured from returned bytes
- → client drives highlight purely from clip boundaries; no server word-timing, no streaming alignment (usual drift sources)

**G. Offline-first mobile**

- offline-first: local device = source of truth, network = enhancement [28]
- Android guidance: layered, MVVM over repositories [29]; Clean Architecture: dependencies point inward [30]
- once downloaded: diagram+audio in app-private files, metadata in local DB, replay touches no server

**H. Positioning**

- not a new model, not new learning theory → SYSTEMS contribution
- combination: agentic pipeline + deterministic consistency gate + cost-aware caching backend + offline-first client, in one working product

## III. System Overview

**A. Requirements** (write as flowing prose, not a list, or reformat as you like)

- functional: free-text topic; SVG w/ stable semantic ids; ordered segments referencing ids; one measured clip per segment; assemble self-contained lesson; cache repeated topics; device: download+persist, synced playback, play/pause/resume/seek-by-segment/replay, history, offline replay; clear failures + retry
- non-functional: cache hit feels instant; saved lesson = zero network; validate before caching; identical topics generated once (cost); providers swappable; core unit-testable; history never leaves device (no accounts)

**B. Architecture** (Fig. 1)

- thin Android client ↔ stateless FastAPI server ↔ hosted Gemini text + speech models via adapters

**C. Key decisions (3)**

1. sync by pre-rendered timed segments: server splits narration, renders clip per segment, measures duration; device plays in order + highlights → deterministic, no network at playback
2. stateless backend + shared cache: no accounts; normalized topic key; generate once, serve everyone; cheaper, faster, easy to scale/reason
3. provider adapters: narrow interfaces; pipeline never touches vendor SDK; model/vendor swap = config change

## IV. Backend Implementation

- Python FastAPI, ~2.0k LoC; layers: API / domain (typed entities+schemas) / providers / agent graph / services / repositories; single composition root

**A. Config over hard-coding**

- 12-factor env config through one typed settings object [31]
- covers: LLM+TTS provider choice, generation engine, model ids, storage backend, loop budgets
- model ids = config not code (hosted names drift); helper lists live models to confirm pinned name
- pinned at time of writing: Flash-class Gemini text (e.g. `gemini-2.5-flash`) + Flash-class Gemini speech
- same codebase runs mock or real providers by settings alone → tests fast + key-free

**B. Provider adapters**

- LLMProvider: plan concepts / draw SVG / critique+refine SVG / narration as structured data / repair given error list
- TTSProvider: text → audio clip + duration
- mock impls: deterministic, no network (tests, local dev)
- Gemini impls: call hosted models; TTS receives raw PCM → wrap as WAV → duration from byte count + sample rate (measured fact, not estimate); retry transient errors w/ exponential backoff
- factory builds matching provider set from settings (abstract factory)

**C. Pipeline nodes** (Fig. 2)

- planner: concepts + visual plan; planning once keeps drawing & narration on same concept set
- svg_generator: draw from plan; semantic id per meaningful part (`sun`, `leaf`, `chloroplast`)
- critique+refine: score + concrete problems; below threshold & budget left → redraw w/ feedback → re-critique; capped passes; budget out → accept best so far
- narration: against the finished drawing; structured data (text + id list per segment); steers toward existing ids
- validator: NO model; parse XML; well-formed + viewBox; collect real ids; every referenced id exists; reject empty narration/segment text; result = pass/fail + error list
- repair: gets exact offending names + parse errors; minimal correction; re-validate; bounded; exhausted → job fails cleanly, nothing cached
- renderer: per segment TTS, store asset, record measured duration
- assembler: builder assembles title + SVG asset + segments (text, ids, audio asset, duration) → cache

**D. Deterministic gate** (short section, emphasize)

- model probabilistic; invariant binary (every referenced part exists or not)
- enforce invariant in plain testable code + refuse to cache failures → unreliable generator becomes dependable source
- repair loop = bridge (bounded chances to satisfy invariant)
- = practical answer to hallucination risk from II

**E. Caching, jobs, coalescing** (Listing 1)

- cold generation = seconds → async endpoint
- topic → normalized key; hit → manifest immediately; miss → job, client polls status (stage + %) until terminal
- jobs keyed by topic key → concurrent same-topic devices share one job/generation
- errors: one envelope (code, message, retryable flag, request id)
- manifest = wire + storage contract; fields: id, topic, title, language, voice, total_duration_ms, svg asset ref, segments (index, text, svg_element_ids, audio asset ref, duration_ms)

**F. Persistence**

- lesson repo hides cache; asset repo hides object store
- dev: memory or filesystem; same interfaces allow DB + cloud object store later, no call-site changes

## V. Client Implementation

- Kotlin ~3.8k LoC ("roughly four thousand" in the submitted version — pick one and stay consistent w/ Table II ≈3.8k), Jetpack Compose, Clean Architecture + MVVM
- presentation: screens + viewmodels / domain: pure entities, use cases, repo interfaces, no Android deps / data: Retrofit + Room + file store
- dependencies point inward; manual DI container instead of framework (traded boilerplate for build simplicity)

**A. Generation flow**

- topic screen → repository does the async job dance (post topic, poll job, progress as StateFlow) → screen renders state → ready: play, optionally save offline

**B. Synchronized playback** (Fig. 3)

- SVG inline in WebView + two small JS functions: highlight(ids) / clear
- highlight = CSS glow + slight scale on matching ids
- ExoPlayer playlist, one media item per segment; current index k → highlight segment k's ids
- clip start/end = highlight window → exact sync, no word timing
- transport = small state machine: idle/loading/ready/playing/paused/seeking/completed; seek = stop clip, clear highlight, start chosen segment

**C. Offline & history**

- save = download SVG + every clip into app-private files + write lesson+segments to Room
- player source-agnostic (loads via lambda) → saved lesson loads from local paths, replays w/ zero network
- history from Room newest-first; delete removes rows + files
- no accounts + local-only history → privacy by construction

## VI. Engineering for Reliability & Reproducibility

- framing: prototype that only runs on author's laptop is hard to trust/extend

**A. Patterns** (Table I) [32]

- patterns are the seams, not decoration; provider interfaces = single point where mock and Gemini differ → key-free suite, same code live in prod

**B. Hermetic key-free testing**

- 48 tests: validator, provider factory, filesystem repo, graph end-to-end incl. repair-recovery AND exhausted-retries, lessons API, audio handling, Gemini adapters
- Gemini adapters tested vs faked SDK client → zero live API calls, no key
- session fixture disables env loading + strips app env vars → same result on any machine/CI
- Android: repository interfaces as test seams

**C. Container & CD**

- container + health endpoint
- CI: backend changes → tests + container build check; client changes → debug APK
- delivery: build image → registry → deploy to cloud host; reverse proxy auto-obtains/renews TLS → always HTTPS, no cleartext exception
- version tag → signed release APK attached to release
- path filters: backend jobs vs client jobs
- net: reproducible commit → running secured service + installable app

## VII. Implementation Status & Verification (report ONLY these, nothing invented)

- framing sentence: implementation paper → report what runs + what's verified + what's NOT measured
- end-to-end live run (real Gemini): complete Photosynthesis lesson — narration segments (**check count: submitted PDF says seven, docs/08 says 7, current tex says six w/ ~43 s audio — verify against your actual saved lesson and use one number everywhere**), well-formed SVG w/ semantic ids, all references passed validator, real audio per segment; exercises every graph node against live models
- gate both directions (graph tests): deliberately broken reference → rejected → repair loop; unfixable within budget → clean failure, nothing cached
- reproducibility: 48 tests, zero live calls; container builds + health endpoint; mock-provider generation returns async response w/ no key; client builds installable APK; player + offline paths build vs Room/WebView
- NOT measured (say plainly): no user study on learning outcomes; no large diagram-quality study; no fleet latency/battery numbers → future work, not claimed
- Table II summarizes

## VIII. Discussion & Limitations (5, name plainly)

1. diagram quality ceiling: LLM good at well-known concepts, weaker at dense/unusual; refine raises floor, no beauty guarantee; gate = consistency, not aesthetics
2. segment-level sync: group of parts per sentence, not per word; deliberate (determinism + offline); word-level = more precise but reintroduces timing data + fragility
3. cold topic cost/latency: first request pays several model calls + TTS, takes seconds; cache hides repeats; bounded loops cap worst case; cold path inherently slow
4. generated markup security: model SVG in WebView = untrusted; restrict to own highlight script, no content-supplied script execution; ongoing care
5. evaluation breadth: claims about system behaviour, not learning gains; controlled study vs static diagrams / plain text = most important next step

## IX. Conclusion & Future Work

- restate: typed topic → named vector diagram + segment narration tied to names + per-segment audio → synced highlighting, fully offline
- core insight = architectural, not any single model: small graph + bounded refine + bounded repair + deterministic non-LLM validator in front of cache → probabilistic generator reliable enough for exact part-to-word correspondence
- around the core: stateless caching backend (cost), swappable adapters (vendor softness), offline-first client (no network), hermetic tests + container + CD (reproducibility)
- future work, 3 groups:
  1. evaluation: controlled learning study; large diagram-quality study; on-device latency/energy
  2. capability: optional word-level highlighting where timing allows; richer diagram styles; research/planning step before drawing for hard topics; multi-language
  3. scale: in-process worker + filesystem store → separate worker + DB + cloud object store behind same interfaces
- closing: none change the central design; they extend it

---

## Consistency checklist before you re-submit anywhere

- [ ] segment count for the Photosynthesis run: one number everywhere (paper, thesis ch. 5, screenshots)
- [ ] LoC figures match Table II (≈2.0k Python / ≈3.8k Kotlin)
- [ ] test count 48 everywhere
- [ ] voice name in Listing 1 ("Kore") matches what your backend actually uses
- [ ] screenshots added to `paper/fig/` before the camera-ready
- [ ] if your venue/university permits disclosed AI assistance, still consider the one-line disclosure — it costs nothing and removes all ambiguity
