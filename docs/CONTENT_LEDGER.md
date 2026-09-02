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
- `stormdown`
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
- `thunderplume`
- `thunderquill`
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
