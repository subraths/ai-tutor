# TutorAI — M.Tech thesis report

`thesis.tex` is the main file of the M.Tech thesis report for the TutorAI
system in this repository. It follows the required template:

| Item | Where |
|---|---|
| Title page, Certificate, Declaration | `frontmatter/` |
| Abstract, Acknowledgment, Abbreviations | `frontmatter/` |
| TOC, List of Tables, List of Figures | generated automatically |
| 1 Introduction (Overview, Problem Statement, Objectives, Motivation) | `chapters/1-introduction.tex` |
| 2 Literature Survey | `chapters/2-literature-survey.tex` |
| 3 Methodology | `chapters/3-methodology.tex` |
| 4 Design Implementation | `chapters/4-design-implementation.tex` |
| 5 Results & Discussion | `chapters/5-results-discussion.tex` |
| 6 Conclusion and Future Scope | `chapters/6-conclusion.tex` |
| References (embedded, no BibTeX needed) | `backmatter/references.tex` |
| Publication Details, Plagiarism Report | `backmatter/` |
| Appendix A (API contract), Appendix B (repo layout, tests, CI) | `backmatter/` |

## Compile

Run `pdflatex` twice so the table of contents and cross-references resolve:

```bash
pdflatex thesis.tex
pdflatex thesis.tex
```

Easiest path: upload the whole `thesis/` folder (as a zip) to **Overleaf**,
set the compiler to `pdfLaTeX`, and it builds as-is.

To install a toolchain locally on Arch:

```bash
sudo pacman -S texlive-basic texlive-latexextra texlive-fontsrecommended texlive-pictures
```

## Before you submit — fill the placeholders

Search for `\todoph` across the files. Every placeholder renders as bold
`[...]` in the PDF so it is impossible to miss. You need to fill:

- `thesis.tex` — your **USN**, guide's **designation**, the **academic year**
- `frontmatter/certificate.tex` / `acknowledgment.tex` — **HoD name**, **Principal name**
- `frontmatter/titlepage.tex` — university/college **logos** (optional)
- `backmatter/publication.tex` — conference name/status once the paper is submitted
- `backmatter/plagiarism.tex` — tool, date, similarity %, and the report page

## Screenshots (figures)

The thesis reuses the same four PNGs as the conference paper. It looks in
`thesis/fig/` first and then `../fig/` (the conference paper's folder), so if
you already added the screenshots for `tutorai.tex` there is nothing to do:

| File | Screenshot |
|---|---|
| `fig/home.png`    | Home / topic-entry screen |
| `fig/player.png`  | Player screen with highlight visible |
| `fig/library.png` | "Your library" with saved lessons |
| `fig/diagram.png` | Full-screen Photosynthesis diagram |

Until a file is present the thesis still compiles — a boxed placeholder
appears in its place.

## Notes

- The numbers in the Results chapter (48 hermetic tests, six-segment
  Photosynthesis lesson, ~2.0k/3.8k LoC) come from the verified state
  recorded in `docs/08-roadmap-and-sdlc.md` and the conference paper. If the
  code moves on, update Chapter 5 to match.
- Chapter/section numbering follows the template; the template's
  "7.1 Conclusion / 7.2 Future Scope" under Chapter 6 is treated as a typo
  and numbered 6.1 / 6.2.
