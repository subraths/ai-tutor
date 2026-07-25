# TutorAI — conference paper

`tutorai.tex` is a self-contained IEEE conference paper (two-column) about the
TutorAI system in this repository.

## Compile

The bibliography is **embedded** (no `.bib` / BibTeX needed). Run `pdflatex`
twice so the citation numbers resolve:

```bash
pdflatex tutorai.tex
pdflatex tutorai.tex
```

Easiest path: upload `tutorai.tex` to **Overleaf**, set the compiler to
`pdfLaTeX`, and it builds as-is (IEEEtran, TikZ and listings are all preinstalled
there).

To install a toolchain locally instead:
- Arch: `sudo pacman -S texlive-basic texlive-latexextra texlive-fontsrecommended texlive-pictures`

## Screenshots (figures)

The paper includes real app screenshots. Save the four PNGs into `paper/fig/`
with these exact names:

| File | Screenshot |
|---|---|
| `fig/home.png`    | Home / topic-entry screen ("TutorAI … What shall we learn today?") |
| `fig/player.png`  | Player screen, segment 1/6, diagram visible |
| `fig/library.png` | "Your library" — 3 lessons saved offline |
| `fig/diagram.png` | Full-screen Photosynthesis diagram (landscape) |

Until a file is present the paper still compiles — a boxed placeholder labelled
"add screenshot: fig/…png" appears in its place, so missing images never break the
build. (Optional: crop the CC / full-screen buttons out of `fig/diagram.png` for a
cleaner figure.)

## Before you submit

Edit the placeholders marked `<<...>>` in `tutorai.tex`:
- the second author's email (the only remaining `<<...>>`)
- optional acknowledgement / funding line

Everything in the body describes the **real** system in this repo. The Results
section reports only what was actually verified — no invented user-study numbers.
If you later run a user study or measure latency/diagram quality, add those
results there.

## Troubleshooting

There are TikZ figures (architecture, generation pipeline, sync timeline) and
image figures (the screenshots). If a TikZ error ever blocks your build on some
TeX setup, you can comment out that figure's `figure`/`figure*` block — the paper
still compiles and reads fine without it.
