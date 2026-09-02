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
