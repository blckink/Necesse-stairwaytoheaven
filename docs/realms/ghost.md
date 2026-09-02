# Ghost Realm / Aftergarden

Verification state: **built**. The classes compile against Necesse 1.3.2 and
the repository audits cover registration, locale, assets and tile behaviour.
Runtime world generation remains to be proven by the integration test.

## Playable core

- Level `ghostlevel`, identifier `ghost2`, dimension `+4`.
- Biomes: Aftergarden, Bone Orchard and Ectomarsh.
- Seven hostile roles: Drifter, Headless Butler, Lantern Widow, Mourning Bride,
  Possessed Chair, Soul Hound and Coffin Crawler.
- Authored sites: Mausoleum, Haunted Manor and Sunken Graveyard. Their lattices
  also drive spawn pressure and persistent guard placement.
- Entry: craft a Soul Basin and feed it twelve Ectoplasm. It becomes a Ghost
  Gate; the far side creates a persistent return gate.
- Economy: Bonewood, Soul Thread, Spectral Ore, Spiritsteel Bars, Soul Loom,
  Spirit Forge and the three-piece Spiritsteel armour set.

Both stations extend the engine's settlement-workstation crafting archetype,
so assigned settlers can run their recipe tech without a custom job type.

## Borrowed assets

No image is generated or recoloured. These names are literal engine resource
paths and are also recorded in `docs/VANILLA_ASSET_MAP.md`.

| content | borrowed path |
|---|---|
| Ghost grass / moss / dirt / stone | `tiles/murkmoss_splat`, `tiles/swampgrass_splat`, `tiles/cryptash_splat`, `tiles/stonebrickfloor_splat`, `tiles/ravenfloor_splat`, `tiles/swamprock_splat` |
| Ectoplasm | `tiles/swampfreshwater_shallow_splat`, `tiles/swampfreshwater_deep_splat` |
| Trees and plants | `objects/deadtree`, `objects/deadwood`, `objects/willowtree`, `objects/gloomwillow`, `objects/aurorabloom`, `objects/cragbloom`, `objects/gloomshroom`, `objects/withershrub` |
| Rocks and gravestone | `objects/veilrock`, `objects/cryptorerock_nightsteelore`, `objects/cryptgravestone1` |
| Basin and stations | `objects/spiritbasin`, `objects/windsilkloom`, `objects/aetherforge`; icons `items/spiritbasin`, `items/caveglowalchemytable`, `items/forge` |
| Materials | `items/deadwoodlog`, `items/clothscraps`, `items/nightsteelore`, `items/nightsteelbar` |
| Spiritsteel armour | `items/soulseedcrown`, `items/soulseedchestplate`, `items/soulseedboots` and matching `player/armor/soulseed*` sheets |
| Portal pair | existing mod sheets `objects/veilriftdown`, `objects/veilriftup` |

## Settlers and quest

The three named Ghost NPCs now stand beside a gravestone, one per world:
Mortimer the Undertaker (`mortimersettler`, hunter), Caspern the Spirit Smith
(`caspernsettler`, dedicated crafter) and Eleanor the Lost Soul
(`eleanorsettler`, husbandry). Mortimer and Caspern also travel to a
settlement once it has built a graveyard or an Aether Forge, respectively;
Eleanor never travels — `EleanorQuest` wraps her two endings (PASS ON for a
Will-o'-Wisp Lantern plus Spiritsteel bars, or STAY as a settler) in one
journal entry. See `docs/settlers.md` and `docs/quests.md`.

## Deferred

The five Ghost animals (§12), the spectral weapon family and the realm-wide
walking-ghost event are separate content families. They are not claimed by
this implementation.
