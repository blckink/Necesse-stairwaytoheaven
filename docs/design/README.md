# Chapter design documents

One folder per loop iteration of `docs/WORLDBUILDING_LOOP.md`. Two files per
chapter, both written before any pixel or any Java:

```
chapter-01-<slug>.md        the brief          (agent: biome-designer)
chapter-01-pois.md          the POI dossier    (agent: poi-architect)
```

These are **design intent, not a record of what shipped**. When a chapter is
integrated, what actually exists goes into `docs/CURRENT_STATE.md` and
`CHANGELOG.md`; the brief stays here as the reason it looks the way it does, and
is not rewritten to match the outcome. If the build deviated from the design,
the integrator's chapter report says so.

Nothing here is read by the game. Vanilla preset references used for
proportions live in `docs/references/presets/`.
