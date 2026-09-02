# Content ledger

Every string ID this mod registers, and one line saying what it is
for a player. Enforced by `python3 tools/content_ledger.py --check`,
which reads the registrations out of the source rather than trusting
this file — so a new item cannot ship undescribed.

Add the row in the SAME COMMIT that adds the content. Quests and
sprites that carry no registration of their own go in the free-text
sections at the bottom.

| id | kind | what it is, in one line |
|---|---|---|

## Chapter 01 — the Skyreach residents

The mod's first settlers after the Warden. Each is a `HumanShop` with a
registered `Settler` type, hired through vanilla's own recruit page, found
standing at a derelict workshop in the Skyreach.

| id | kind | what it is, in one line |
|---|---|---|
| `magpiesettler` | mob | Magpie, the courier who kept the cargo: the only vendor who BUYS sky salvage in quantity, and stocks goods from biomes you are nowhere near. |
| `haldasettler` | mob | Halda the Cellarer: sells the worked goods of the three stations (skyweave, stormglass, stormsteel) and buys the raw end of every sky gathering loop. |
| `ossiansettler` | mob | Ossian Vane, the last reader: the only source of incursion-tier salvage without an incursion, and the Aether Forge's customer for high-tier bars. |

## Settlers with professions — the four who travel

The mod's first settlers that arrive at a settlement on their own, the way every
vanilla settler does, and the first that hold a real vanilla **profession** (the
`defaultDisabledBySettler` job types — see `settlement/SkyArrivals` and
`mobs/SkySettlerMob`). Each is a `HumanShop` with a registered `Settler`, wears
vanilla clothing items, and borrows a vanilla settler face for its settlement
icon — **no new art**; every borrowed path has a row in
`docs/VANILLA_ASSET_MAP.md`.

| id | kind | what it is, in one line |
|---|---|---|
| `eveleensettler` | mob | Eveleen the Eden Botanist: the settlement's **farmer** — the only settler who can fertilise — and the only shop in the game that sells Eden grass seed, alongside vanilla seeds, saplings and a queen bee. Travels to a settlement once Eden grass grows in it. |
| `mortimersettler` | mob | Mortimer the Undertaker: the settlement's **hunter**, who will haul and craft and refuses to farm or chop, and the only vendor of gravestones, the sarcophagus, black candles and the whole Bonewood furniture family. Travels to a settlement that has built a graveyard. |
| `caspernsettler` | mob | Caspern the Spirit Smith: a dedicated **crafter** who never leaves the forge, and the only source of Spiritsteel, Soul Thread and the spectral line. Travels to a settlement that has built an Aether Forge. |
| `eleanorsettler` | mob | Eleanor the Lost Soul: found in the Ghost Realm / Aftergarden beside a gravestone, with two endings — hire her and she becomes the settlement's **husbandry** settler, or hold twelve Veil Essence out to her and she lets go, leaving a Will-o'-Wisp Lantern behind. She never travels to a town; the choice is yours to go and make. |

## Mr. Knott, the Doorman

Crooked Beyond's own settler — found once per world at the Door Yard, and,
unlike the four above, no `SkyArrivals` gate makes him walk to a settlement:
`docs/WORLD_DESIGN.md` §15 names no such condition. See
`stairwaytoheaven/mobs/KnottMob.java` and `settlement/CrookedResidents`.

| id | kind | what it is, in one line |
|---|---|---|
| `knottsettler` | mob | Mr. Knott the Doorman: a **trading** settler who refuses farming and forestry, and the only vendor of Crooked Beyond's weird furniture and cosmetic masks, buying Warp Resin, Eye Seed and Reality Shard in turn. Found once per world at the Door Yard; does not travel to a settlement on its own. |

## The Beetle Outlands — the sky's wrong ground

The contrast the bright Skyreach was missing, gated by distance from the spire
instead of by a door. See `docs/CURRENT_STATE.md` and
`stairwaytoheaven/worldgen/SkyOutlands.java`.

| id | kind | what it is, in one line |
|---|---|---|
| `outlands` | biome | The Beetle Outlands: striped violet ground, dead trees and crystal massifs, impossible within 900 tiles of the spire and ordinary in the far sky — the sky's own dark half, no second dimension required. |
| `evilwall` | object | A crystal massif of the Outlands: a mineable rock wall on vanilla's `RockObject`, drawn on the game's own `crystalwall` sheet, that builds the region's ridges and dead ends and drops crystalstone, the same material vanilla's own crystal rock gives. |
| `crookedgolem` | mob | Crooked Golem: the Outlands' slow bruiser, a ringed floating eye on candy-striped hooks that plants itself, charges for two seconds and fires a 130-damage beam down the line it warned you about. |
| `rarecrookedgolem` | mob | Rare Crooked Golem: the crimson wall, 1000 HP on Classic and the rarest thing in the mix — and, like the ascended body it inherits, it walks off again after twenty seconds if you leave it alone. |
| `crookedarmadillo` | mob | Crooked Armadillo: a bone-white plate dome that plods at armour 60 until it sees you, then rolls up and comes at speed 200 for 90 damage on contact. |

## The Garden of Eden — first brick

The player supplied the ground pair on 2026-09-01
(`kk-sprites/overgrowngrass_splat-overgrowneden_splatt.png` + seed icon,
drawn on vanilla's overgrowngrass). The realm ships later as a complete
chapter; the ground is real now.

| id | kind | what it is, in one line |
|---|---|---|
| `overgrownedentile` | tile | Eden grass: deep lush ground that sprouts grass tufts on its own, spreads to dirt, and seeds itself back at 4% when mined — the Garden of Eden's first ground, plantable today. |
| `overgrownedenseed` | item | Eden grass seeds: plant on dirt or Cloudturf to start an Eden patch; found rarely in sky crates, then self-renewing. |

## The Garden of Eden — generated core

| id | kind | what it is, in one line |
|---|---|---|
| `eden2` | level | The infinite Garden of Eden level at dimension +2. |
| `edengarden` | biome | Eden's dense common garden of fruit, flowers and concealed predators. |
| `edencanopy` | biome | Rooted deep growth around Knowledge Trees and Eden Copper. |
| `edenshallows` | biome | White beaches and quiet shallow lagoons. |
| `paradisesandtile` | tile | White lagoon sand borrowed from vanilla's native sand terrain. |
| `edenshallowstile` | tile | Eden's shallow lagoon liquid. |
| `edenmosstile` | tile | Thin organic green between patches of supplied Eden Grass. |
| `edensoiltile` | tile | Rich organic soil beneath the canopy. |
| `edenrootfloortile` | tile | Ancient-root ground under Eden's largest trees. |
| `edenwood` | item | Common wood material recovered from Eden vegetation and enemies. |
| `edensap` | item | Living plant resin used in Eden metalworking. |
| `paradiseapple` | item | Eden fruit dropped by Bloom Maws and garden caches. |
| `serpentscale` | item | Common combat material from Eden's serpents. |
| `venomfang` | item | Rare poison material from Eden's serpents and hornets. |
| `goldenpollen` | item | Bright material carried by Golden Hornets. |
| `knowledgecutting` | item | Rare living cutting recovered near Knowledge Trees. |
| `paradisecoconut` | item | Lagoon-shore fruit and cache reward. |
| `edenberry` | item | Common Eden berry and cache reward. |
| `moonmelon` | item | Cool garden fruit and cache reward. |
| `sungrape` | item | Warm shore fruit intended for the future Eden Press. |
| `edencopperore` | item | Verdant ore mined beneath Eden's canopy. |
| `edenbronzebar` | item | Eden's first combat metal, made from ore and living sap. |
| `edenserpent` | mob | Standard poisonous ground predator hidden in dense growth. |
| `bloommaw` | mob | Stationary carnivorous flower guarding garden caches. |
| `jealousvine` | mob | Slow, heavy canopy predator emerging from vegetation. |
| `goldenhornet` | mob | Fast Eden flier, tightly capped by biome spawn rules. |
| `forbiddenserpent` | mob | Persistent elite associated with Knowledge Trees. |
| `edenseedbasin` | object | Craftable gate focus that consumes six Eden Grass Seeds to open the Eden Gate. |
| `edengatedown` | object | Unobtainable living-side portal created by an activated Eden Threshold. |
| `edengateup` | object | Persistent return portal automatically placed in the Garden of Eden. |

## The Veil — Geisternebel, and the gate it is

`docs/WORLD_DESIGN.md` §8: past a defined realm depth a permanent fog stands,
and until the player carries §9's Veil Mark it stacks **Soul Exposure** on
them — vision at 0-3 seconds, slow at 4-7, health drain at 8-12, massive
damage past 12. Built as ONE gate mechanic (§42.4) so the Infernal Visa is a
second configuration rather than a second code path. See
`stairwaytoheaven/veil/`.

| id | kind | what it is, in one line |
|---|---|---|
| `soulexposure` | buff | Soul Exposure: what the Veil's fog does to anyone crossing it without the Veil Mark — one stack per second in the fog, dimming the view, then slowing you, then draining life, then killing you outright at thirteen seconds, and giving a stack back per second once you are out. |
| `veilfog` | buff | The Veil's fog itself: an invisible marker the server puts on anyone standing past the fog line, which draws the drifting mist on their screen. It stays after the Veil Mark is earned, so the border between Steinfeld and the Ghost Realm remains something you can see rather than something you only remember. |

## Crooked Beyond realm

| id | kind | what it is, in one line |
|---|---|---|
| `crooked2` | level | The separate Crooked Beyond level reached through its paired wrong-way doors. |
| `stripedwaste` | biome | The striped outer band, carrying Crooked veterans and sparse broken vegetation. |
| `spiralfields` | biome | A living spiral field guarded by waking Tongue Plants. |
| `checkerworks` | biome | The built, checker-patterned band containing authored door-and-table POIs. |
| `crookedstripetile` | tile | Existing Beetlefreak stripes reused as the realm's outer terrain. |
| `spiralsoiltile` | tile | Organic purple terrain borrowed from vanilla ascended growth. |
| `violetmudtile` | tile | Corrupted violet terrain for wet low ground. |
| `checkerstonetile` | tile | Dark tiled terrain beneath the Checkerworks. |
| `wrongwaytile` | tile | Near-black ascended-void terrain used where geometry stops making sense. |
| `spilltile` | tile | The Crooked realm's separating liquid. |
| `spiraltree` | object | A harvestable black tangle yielding Oddwood and occasional Warp Resin. |
| `eyeballshrub` | object | Soft eye-like growth and the renewable source of Eye Seeds. |
| `screamingflower` | object | A faintly glowing plant that yields Warp Resin. |
| `stripedmushroom` | object | Fibre-like fungus harvested for Strange Fabric. |
| `bentgrass` | object | Non-paying ground cover that gives the Spiral Fields their broken direction. |
| `bentlantern` | object | A natural violet light standing in for a lantern grown from the ground. |
| `crookedclock` | object | Breakable built debris yielding Strange Fabric and occasional Reality Shards. |
| `longchair` | object | Unsittable authored-set-piece furniture from the Long Table POI. |
| `groundwindow` | object | A luminous window-like object lying where a floor should be. |
| `teethrock` | object | The realm's pickaxe node and reliable Reality Shard source. |
| `crookedcrate` | object | The realm crate whose contents come from the current Crooked biome. |
| `crookeddoordown` | object | The entry door from the Outlands rim into Crooked Beyond. |
| `crookeddoorup` | object | The persistent return door created inside Crooked Beyond. |
| `oddwood` | item | Common Crooked wood and future Reality Stitcher building material. |
| `warpresin` | item | Resin harvested from living Crooked flora. |
| `strangefabric` | item | Fibre from fungi and broken Crooked furnishings. |
| `eyeseed` | item | Seed-like material harvested from watching shrubs. |
| `stripedshell` | item | The catch reward from a Stripe Beetle. |
| `realityshard` | item | Rare tier-10 material mined from Teeth-Rocks and guarded sites. |
| `doormimic` | mob | Elite disguised Crooked guard using vanilla Mimic behaviour at the realm tier. |
| `tongueplant` | mob | A dormant plant guard that wakes when a player approaches. |
| `stripebeetle` | mob | Net-catchable Crooked critter dropping a Striped Shell. |

## Ghost Realm / Aftergarden

| id | kind | what it is, in one line |
|---|---|---|
| `ghost2` | level | The persistent dimension-4 Aftergarden generated from three dead sub-biomes. |
| `aftergarden` | biome | Poison-green common ground with mixed ghost pressure and dead groves. |
| `boneorchard` | biome | Violet resource ground where bonewood and Spectral Ore concentrate. |
| `ectomarsh` | biome | Turquoise wetland dominated by ambushers and glowing ectoplasm. |
| `hauntedgrasstile` | tile | Organic Aftergarden terrain borrowing the existing Murkmoss sheet. |
| `ghostmosstile` | tile | Organic marsh terrain borrowing vanilla swamp grass. |
| `violetdirttile` | tile | Violet orchard terrain borrowing vanilla crypt ash. |
| `spiritstonetile` | tile | Petrol hard ground borrowing vanilla stone-brick floor art. |
| `blackcobbletile` | tile | Cold dark hard ground borrowing vanilla Raven floor art. |
| `graveyardsoiltile` | tile | Wet cemetery ground borrowing vanilla swamp-rock terrain. |
| `ectoplasmtile` | tile | Swimmable turquoise liquid using vanilla swamp-water atlases. |
| `crookeddeadtree` | object | Unobtainable dead scenery yielding Bonewood. |
| `bonewoodtree` | object | The orchard's renewable Bonewood source. |
| `spiritwillow` | object | A cold willow yielding Bonewood. |
| `lanterntree` | object | A luminous grove tree yielding Bonewood and occasional Ectoplasm. |
| `ghostlily` | object | Soft pale flora yielding Soul Thread. |
| `mourningrose` | object | Violet soft flora yielding Soul Thread. |
| `ectoplasmfern` | object | Turquoise marsh flora yielding Ectoplasm. |
| `widowvine` | object | Dark soft flora yielding Soul Thread. |
| `spiritmushroom` | object | Glowing soft fungus yielding Ectoplasm. |
| `ghostrock` | object | Breakable dead stone with a small Bonewood salvage drop. |
| `spectralorerock` | object | The realm's mineable source of Spectral Ore. |
| `ghostgravestone` | object | Unobtainable cemetery scenery used by worldgen and POIs. |
| `soulbasin` | object | Craftable gate focus that consumes twelve Ectoplasm to open the Ghost Gate. |
| `soulloom` | object | Settler-compatible station that spins Soul Thread. |
| `spiritforge` | object | Settler-compatible station that smelts and forges Spiritsteel without fuel. |
| `ghostgatedown` | object | Unobtainable living-side portal created by an activated Soul Basin. |
| `ghostgateup` | object | Persistent return portal automatically placed in the Aftergarden. |
| `bonewood` | item | Common realm timber and authored POI loot. |
| `soulthread` | item | Ghost textile spun from Ectoplasm and Aurora Fleece. |
| `spectralore` | item | Rare ore mined in the Bone Orchard. |
| `spiritsteelbar` | item | Forged Ghost-tier bar consumed by the Spiritsteel set. |
| `spiritsteelhelmet` | item | Tier-7 melee crown and head of the Spiritsteel set. |
| `spiritsteelchestplate` | item | Tier-7 Spiritsteel chest armour. |
| `spiritsteelboots` | item | Tier-7 Spiritsteel greaves. |
| `drifter` | mob | Flying standard ghost using the realm's common pressure table. |
| `headlessbutler` | mob | Melee manor servant with persistent authored-site use. |
| `lanternwidow` | mob | Ranged Ghost-tier caster concentrated in the Bone Orchard. |
| `mourningbride` | mob | Elite anchor for all three authored-site guard packs. |
| `possessedchair` | mob | Mimic-style furniture ambusher used in marsh guard packs. |
| `soulhound` | mob | Fast melee pursuer used where sightlines are short. |
| `coffincrawler` | mob | Buried ambusher concentrated in the Ectomarsh. |

## Baseline — registered before the ledger existed

These predate the ledger and are described in `CHANGELOG.md`,
`docs/CURRENT_STATE.md` and the design documents. `--check` exempts
them. Anything registered from now on does not get that exemption.

- `aeronautwreck`
- `aetherforge`
- `aetheriumbar`
- `aetheriumore`
- `aetheriumrock`
- `ashbones`
- `ashenreach`
- `ashsandtile`
- `aurorabloom`
- `aurorabloomr`
- `auroraflake`
- `aurorafleece`
- `auroralily`
- `auroralocket`
- `aurorapetal`
- `aurorashards`
- `aurorashoals`
- `beetledoor`
- `beetledoorlocked`
- `beetlefreakhollow`
- `beetlefreaktile`
- `beetlewall`
- `beetlewindow`
- `blackpeattile`
- `catbasket`
- `charfloortile`
- `chargecrystal`
- `charwood`
- `cindercantor`
- `cinderpearl`
- `cloudbell`
- `cloudberry`
- `cloudberrybush`
- `cloudberrysapling`
- `cloudcustard`
- `cloudlamb`
- `cloudmarbledoor`
- `cloudmarbledoorlocked`
- `cloudmarblefence`
- `cloudmarblefencegate`
- `cloudmarblewall`
- `cloudmarblewindow`
- `cloudpufftreat`
- `cloudsapling`
- `cloudtree`
- `aurorashoaltile`
- `skycache`
- `skycrate`
- `cloudturftile`
- `cloudwood`
- `cragbloom`
- `dawnpiercer`
- `deadtree`
- `dewsnail`
- `driftlands`
- `fenwraith`
- `flickerlightgarland`
- `fulgurite`
- `fulguriterock`
- `fulgurpine`
- `fulgursapling`
- `galehound`
- `galehowl`
- `ghostlantern`
- `glimmergoat`
- `glimmerstrides`
- `gloomfen`
- `gloomravenstatue`
- `gloomshade`
- `gloomshroom`
- `gloomwillow`
- `gloomwoodfloortile`
- `glowfern`
- `glowmoth`
- `marblecheckertile`
- `mistglasslantern`
- `mistseatile`
- `mistserpent`
- `mistserpentbody`
- `mistserpenttail`
- `murkmosstile`
- `murkwatertile`
- `nightfelldoor`
- `nightfelldoorlocked`
- `nightfellwall`
- `nightfellwindow`
- `nimbusdraught`
- `nimbusfloortile`
- `nimbusmilk`
- `nimbussapling`
- `nimbuswillow`
- `nimbuswood`
- `nimbusyak`
- `pottedcloudberry`
- `prismabirch`
- `prismasapling`
- `prismcaller`
- `prismfloortile`
- `prismgrass`
- `prismshard`
- `prismshardrock`
- `prismwood`
- `rimesentry`
- `seancecircle`
- `seraphstatue`
- `seraphwood`
- `silverbell`
- `skyanchor`
- `skyballoon`
- `skycurd`
- `skyfallshard`
- `skyironfence`
- `skyironfencegate`
- `skylichen`
- `skyparcel`
- `skyreach2`
- `skyreave`
- `skyreeds`
- `skyscree`
- `skyseraphsapling`
- `skyseraphtree`
- `skystairwaydown`
- `skystairwayup`
- `skystone`
- `skystonebrickdoor`
- `skystonebrickdoorlocked`
- `skystonebrickwall`
- `skystonebrickwindow`
- `skystonegolem`
- `skystonerock`
- `skystonetile`
- `skytulip`
- `skywarden`
- `skywatchastrolabe`
- `skywatchbanner`
- `skywatchbookshelf`
- `skywatchcabinet`
- `skywatchcandelabra`
- `skywatchcandle`
- `skywatchcarpet`
- `skywatchchair`
- `skywatchchalice`
- `skywatchclock`
- `skywatchdesk`
- `skywatchdisplay`
- `skywatchdresser`
- `skywatchhood`
- `skywatchmodulartable`
- `skywatchrubble`
- `skywatchtelescope`
- `skywatchtome`
- `skywatchwhistle`
- `skyway`
- `skywaytile`
- `skyweave`
- `sparkbeetle`
- `spirecatblack`
- `spirecattabby`
- `starfall`
- `staticmoss`
- `stormcrystal`
- `stormcrystalr`
- `stormdisc`
- `stormglass`
- `stormglasskiln`
- `stormscreed`
- `stormsedge`
- `stormshard`
- `stormslatetile`
- `stormsteelbar`
- `stormsteelboots`
- `stormsteelchestplate`
- `stormsteelhelmet`
- `stormsteelvambrace`
- `stormveil`
- `stormwisp`
- `tallcloudgrass`
- `tempestedge`
- `thunderbloom`
- `thunderhead`
- `veil2`
- `veilessence`
- `veilriftdown`
- `veilriftup`
- `veilrock`
- `wardenbeaconoff`
- `wardenbeaconon`
- `wardenboots`
- `wardencandelabra`
- `wardenmantle`
- `wardensettler`
- `watchmote`
- `whisperreeds`
- `windsilk`
- `windsilkloom`
- `windwheat`
- `withershrub`
- `zephyrfinch`
- `zephyrharness`
- `zephyrray`
