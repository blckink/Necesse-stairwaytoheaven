# Implementation rules

These are repository-wide production rules for every agent and every new piece of content. They exist to prevent half-finished assets, non-native behaviour, sparse procedural filler, and repeated re-discovery of Necesse conventions.

## 1. Build content as complete Necesse-native families

A new visual or gameplay idea is not finished when a PNG exists or a class compiles. Every new thing must be traced end to end through the engine and receive every required companion asset/registration.

Before calling an item/object/mob/content family complete, verify all applicable parts:

- registry entry and correct registry/category type
- world/object sprite or mob sheet
- inventory/item icon
- placement/object item when placeable
- menu/crafting visibility when intended
- English and German display names
- tooltip/description where useful
- crafting recipe and workstation when intended craftable
- correct tool interaction and tool tier
- correct HP/break speed
- correct drops / captured item / loot behaviour
- correct light, collision, shore/water, solidity and interaction flags where relevant
- save/persistence/despawn semantics for mobs/critters/NPCs
- animation/frame layout expected by the native renderer
- bestiary/journal/icon support when the category uses it
- generator source updated first for generated assets
- gallery/QA regenerated and visually inspected

Do not ship placeholder registrations or art-only content that cannot behave like its nearest vanilla equivalent.

## 2. Choose the native archetype first

Before implementing any new content, identify the closest real Necesse archetype and inspect its decompiled implementation and assets. Reuse native systems rather than inventing parallel frameworks.

Examples:

- **settler / NPC** -> `HumanMob` / `HumanShop` / settlement patterns
- **pet / companion** -> native companion/pet pattern if the desired behaviour truly matches
- **farm animal** -> native husbandry/farm-animal behaviour
- **critter** -> native `CritterMob` and net-catching/release pattern when appropriate
- **enemy** -> nearest combat mob AI/animation/drops pattern
- **tree / woody plant** -> axe/tool behaviour of the nearest vanilla tree/shrub
- **rock / ore / crystal** -> pickaxe/tool tier, ore masks and `RockObject` conventions
- **flower / grass / soft flora** -> nearest vanilla plant break/interaction pattern
- **furniture / placeable decor** -> corresponding vanilla object/item/collision pattern
- **floor / terrain tile** -> nearest safe tile/splat implementation

Record verified API findings in `docs/TECHNICAL_LEARNINGS.md`.

## 3. Human settlers should remain human-native

The recruited Surface Warden is a real settlement NPC. Prefer the same layered/native human rendering approach used by Elder/settlers/player-like humans rather than replacing him with a bespoke full-body mob renderer.

Use native human appearance/layers for clothing, head/hair/beard, footwear, accessories, colours and animation wherever the API allows. His identity should come from a coherent Skywatch outfit and face, not from fighting the renderer.

The sky-side/lore Warden may use more bespoke presentation if needed, but the two versions must still read as the same character.

Do not alter the verified recruitment/settler architecture merely to simplify art production.

## 4. Tool and interaction behaviour must make intuitive vanilla sense

Do not default custom objects to pickaxe interaction.

Audit object by object against the closest vanilla analogue:

- trees and woody trunks -> axe
- woody shrubs/saplings -> axe where vanilla does so
- rock / ore / mineral / crystal formations -> pickaxe
- flowers / grasses / soft plants -> corresponding vanilla plant behaviour
- catchable harmless critters -> net if the native critter pattern supports it
- furniture/building pieces -> nearest vanilla furnishing/building behaviour

Also verify tool tier, HP, break time, drops and placement/pickup behaviour. A correct sprite with the wrong interaction is still unfinished content.

## 5. Every player-facing registration needs a usable UI representation

For every registered item/object/tile/mob where the engine expects one, verify the correct icon and locale keys exist. Never assume the world sprite doubles as the inventory icon unless the vanilla archetype does exactly that.

Run `python3 tools/locale_audit.py` and the relevant asset audits. Error textures, internal string IDs and missing names are release blockers.

## 6. Generated art is changed through generators

Assets under `src/main/resources/` that are generator-controlled must be changed by editing `tools/asset_generator/` first, then regenerating. Do not hand-edit only the emitted PNG.

`vanilla-sprites/` is a local technical reference only and must never be committed.

## 7. In-game readability outranks asset metrics

Automated mass, saturation, dimensions and alpha checks are guardrails. They are not a substitute for art judgement.

A major visual asset is complete only after:

1. engine structure/frame layout is valid
2. generator reproduces the PNG byte-identically where applicable
3. gallery/contact-sheet output is regenerated
4. the output is actually inspected at 1x/2x and on representative terrain
5. obvious issues receive a correction pass
6. final acceptance remains **awaiting player confirmation** until it is seen in the real client

Do not redesign assets explicitly marked KEEP from player testing merely because a metric can be improved.

## 8. Worldgen should compose scenes, not scatter objects

The stronger green/turquoise Skyreach areas are the density/readability benchmark. Stone/Stormveil/Aurora areas must feel equally authored and worth exploring, but not by uniformly increasing random density.

Prefer composition systems that create:

- clusters and families
- pockets of density
- meaningful empty space
- corridors / transitions
- small clearings
- rock outcrops and veins
- local prop relationships
- rare highlights
- visible landmarks / POIs
- biome-specific combinations

Avoid independent uniform per-tile scatter for content that should form a natural or designed formation.

A player should repeatedly have a reason to think: **“What is that over there?”**

## 9. Biome depth requires gameplay hooks, not decoration alone

When enriching a sparse biome, add a mixture of visual, resource and encounter reasons to explore. New props should support at least one of:

- navigation/landmark value
- harvest/resource value
- loot
- combat encounter
- critter/collection behaviour
- environmental storytelling
- quest/progression relevance
- rare humour/oddity discovery

Do not create dozens of decorative PNGs that never affect the player experience.

## 10. Humour is part of the identity, but use restraint

Skyreach can contain dry, strange and surprising humour: lost aeronaut equipment, improbable sky debris, zeppelin/balloon ideas, storks, odd historical flying attempts, suspiciously modern fragments, etc.

Use original designs and subtle absurdity. Do not turn every screen into a joke, meme dump or direct copyrighted-game reference. The core world must remain coherent and beautiful enough that the humour lands as discovery.

The later perfect-Heaven concept and the Veil's gothic/morbid comedy belong in their own biome/realm language; do not dilute the current core biomes by mixing every idea everywhere.

## 11. Preserve KEEP content and verified architecture

Do not silently redo working systems or player-approved content. In particular, read `docs/CURRENT_STATE.md`, `docs/PLAYTEST_LOG.md` and `docs/DESIGN_DECISIONS.md` before touching:

- portal/SkyOrigin routing
- save schema/migration
- Warden recruitment architecture
- Siggi/Peanut persistence and immortality
- Marble Checker fix
- player-approved Ray/Wisp/birds/snails/small vegetation

If a task appears to require reversing a recorded decision, stop and surface the conflict instead of implementing around it.

## 12. Add everything needed when adding a content family

Before handing off new content, explicitly answer this checklist for each new family:

- What is it in engine terms?
- Where/how does it spawn or become obtainable?
- What does the player do with it?
- What tool/action interacts with it?
- What does it drop / reward / unlock?
- Does it need an item icon?
- Does it need an object item?
- Does it need locale keys?
- Does it need crafting?
- Does it need bestiary/journal support?
- Does it save/despawn correctly?
- Does it need animation or directional frames?
- Is it represented in QA/gallery output?
- Is it tested only headlessly, or actually player-confirmed?

If those questions are not answered, the content is not done.

## 13. Standard verification gates

Run the applicable gates before handoff:

```bash
./gradlew buildModJar
scripts/integration_test.sh
scripts/tile_sprite_check.sh
python3 tools/size_audit.py
python3 tools/locale_audit.py
```

Also run generator reproducibility and gallery/visual QA tools relevant to the changed domain.

A dedicated-server pass does **not** prove client rendering or player UX. State the verification level accurately.

## 14. Handoff language

Use these states consistently:

- **KEEP — player confirmed**
- **FIXED — awaiting player confirmation**
- **IMPLEMENTED — awaiting player confirmation**
- **VERIFIED [jar]** — proven from decompiled/source implementation
- **VERIFIED [run]** — observed in automated execution
- **VERIFIED [game]** — observed in the real client/playtest
- **HYPOTHESIS** — not yet proven

Never upgrade an automated result to player-confirmed status.
