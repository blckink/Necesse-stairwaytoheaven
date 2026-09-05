# Swapping a borrowed vanilla sprite for one of ours

The mod was built by borrowing vanilla art and writing down every loan
(`docs/ASSET_REQUESTS.md`). This is how a loan gets paid back: one borrowed
sprite in, one mod sprite out, with the player looking at it before it ships.

**This page is a runbook.** A session with no history should be able to follow
it end to end without asking anything.

```sh
export PYTHONPATH=/home/blackoffset/dev/pylib     # every tool here needs Pillow
```

## What is automated and what is not

Automated: finding the next asset, resolving its vanilla original, writing the
brief, measuring what comes back, building the review image, asking the player,
checking the row off, integrating.

**Not automated: the drawing itself.** That is a finding, not an omission:

- The local ComfyUI carries only `flux1-schnell-fp8` and no pixel-art LoRA. A
  live run on 2026-09-01 fed it the labelled template and it drew the guide
  text and the grid into the picture; a second run through a corrected graph
  produced a 245-colour, fully opaque render. Useful concept art, not a sheet.
- There is no `codex` CLI on this machine.
- The output that *was* usable came from the player driving a chat model by
  hand — and even that ignored the canvas (1312–1536px wide, 70k–290k colours).

So the generator is a **drop-in step**, not a service call: the brief and the
vanilla original go out, a PNG comes back into `art-inbox/`. Everything on
either side of that is a command.

## The loop

### 1. What is still borrowed

```sh
python3 tools/asset_worklist.py                 # 119 open at the time of writing
python3 tools/asset_worklist.py --realm eden --limit 5
```

Reads `docs/ASSET_REQUESTS.md` and checks every row against reality: the
vanilla stand-in must exist in the sprite dump and its real size must match the
size the table promises. A row marked `DONE` is dropped. A row that fails its
own check is reported rather than skipped — the table is hand-maintained, and a
wrong number there becomes a wrongly-sized sprite three steps later.

The dump is `~/dev/Necesse sprites` (override with `NECESSE_VANILLA_SPRITES`).
The dedicated server ships **no PNGs at all**, so this client-side dump is the
only place an original can come from.

### 2. Prepare the brief

```sh
python3 tools/asset_brief.py edenserpent
python3 tools/asset_brief.py --realm eden --limit 5
```

Writes `build/asset-briefs/<id>/`:

| file | what it is |
|---|---|
| `<vanilla>.png` | the original — **this is what you attach** |
| `brief.md` | the text to send with it |
| `grid.png` | the original with its frame grid drawn on — a reading aid for a human, never the base to draw on |

The brief states the rules that make a returned PNG usable at all: the vanilla
file is the base and not an inspiration; frame position, alignment, spacing and
size stay exactly where vanilla put them; shape may deviate as long as it stays
inside its cell; **no icons**; true pixel art on a tight palette; transparent
background; exact canvas or an exact integer multiple.

**No icons is deliberate.** The inventory/bestiary icon is cut from the
finished sheet afterwards, so it always matches the sheet it represents. A
hand-drawn icon would not. (Check whether `Mob.getMobIcon()` already covers the
mob case before writing an extractor — `mobs/BorrowedMobIcon` on the
`pmiygp` branch does exactly this for borrowed art.)

### 3. Generate — the manual step

Send `brief.md` plus the vanilla PNG to the image model. Drop what comes back
into `art-inbox/`, named after the vanilla stand-in's basename, or
`<vanillaname>-new-<ourname>.png` for new art drawn on a vanilla layout.

### 4. Measure and conform

```sh
python3 tools/asset_intake.py                   # report only, writes nothing
python3 tools/asset_intake.py --apply           # ship what passed
```

It resolves the target, checks the size (exact, or an exact integer upscale it
can downsample by taking the most common colour per block), counts colours,
checks the background is really transparent, and writes a preview. It refuses
what it cannot fix and says why.

**A generated sheet usually will not pass on the first try**, because
generators do not honour cell geometry. The stage that fixes that — segment
each frame, normalise it into its cell on a shared ground line, then
modal-downsample — is **specified but not built**; the spec is in this repo's
history (`docs/HANDOFF_LOCAL_PIPELINE.md` as of 2026-09-02, restorable from
`backup-20260905-lokalstand/`). Until it exists, a sheet whose frames drifted
has to go back to the generator rather than be rescaled as a whole: X and Y
scales differ per sheet and a whole-image resize is not recoverable.

### 5. Show the player, and ask

```sh
python3 tools/asset_review.py edenserpent art-inbox/crocodile.png            # look only
python3 tools/asset_review.py edenserpent art-inbox/crocodile.png --ask --job <jobid>
```

Builds `build/qa/review/<id>_review.png`: vanilla and candidate side by side,
same scale, on a dark and a light ground, with a 1× strip beneath. Side by side
is what makes "did the frames stay put" visible instead of asserted; the 1×
strip is the honest one — if it only reads at 3×, it is not finished.

`--ask` pushes that image to the phone (`bin/artifact.sh`) and blocks on a
verdict (`bin/approve`):

| exit | meaning | what to do |
|---|---|---|
| 0 | approved | `asset_intake --apply`, check the row off, integrate |
| 1 | rejected | drop it, next asset |
| 3 | parked (no answer in time) | write the handover and stop |
| 4 | the player wrote a correction | re-brief with that text, generate again |

**One approval can be pending per job at a time** — `bin/approve` keeps a
single `approval` slot in the job's `status.json`, so a second concurrent ask
would overwrite the first. Batch of five means five sequential asks, not five
at once. Use `--risiko hoch`: `niedrig` self-approves after 15 minutes, which
would ship art nobody looked at.

Without `--ask` the tool only writes files and prints the context. That is what
a remote session must do — it has no phone and no aethergate — so the pipeline
degrades to useful rather than failing.

### 6. Check it off, and integrate

1. In `docs/ASSET_REQUESTS.md`, strike the row through and stamp it
   `**DONE <date>**`. `asset_worklist.py` then drops it for good.
2. In `docs/VANILLA_ASSET_MAP.md`, move the row from §1.x to §4 — the map keeps
   the swap's history rather than deleting it.
3. Point the code at the new sheet. For a mob that already subclasses a vanilla
   mob (`VANILLA_ASSET_MAP.md` §1.2) this is swapping the texture reference;
   the behaviour stays.
4. Add the file to `generate_assets.py`'s `CONVERTED` guard, or the next
   generator run overwrites the art. That mistake has shipped once.
5. Gates: `./gradlew buildModJar`, `python3 tools/size_audit.py`,
   `tools/sheet_format_audit.py`, `tools/locale_audit.py`,
   `tools/content_ledger.py --check`.

## Running it from Archon

Archon dispatches with `run_coding_agent(agent="claude", prompt=...)`. The
prompt only has to point here:

> Folge `~/projekte/necesse-mod/ablage/docs/ASSET_SWAP_PIPELINE.md`. Nimm die
> nächsten fünf offenen Assets aus `tools/asset_worklist.py`, erzeuge die
> Briefs, und melde dich mit den Briefs und den Vanilla-Originalen zur
> Bildgenerierung zurück. Für jedes fertige Sheet: `asset_review.py --ask`,
> und erst nach Freigabe abhaken und integrieren.

Batch size is a token-economy call, not a correctness one: briefs are cheap and
independent, the approvals are serial either way. Five is a reasonable default;
**the first run of a changed pipeline should be one**, because each of the six
stages has a failure mode worth seeing alone.

## Verification language

Say which one you have (`docs/IMPLEMENTATION_RULES.md` §14). For art the
distinction bites hardest: `asset_intake` passing is **VERIFIED [run]** — the
file is on-format. Only the player seeing it in game is **VERIFIED [game]**,
and only that can say the art is any good.
