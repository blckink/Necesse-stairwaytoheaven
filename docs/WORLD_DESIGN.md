# Stairway to Heaven — World, Progression & Content Design

**This file is the mod's constitution.** It was supplied by the player on
2026-08-31 as the final concept, and it OUTRANKS every other design document in
this repository. Where `docs/DESIGN.md`, `docs/ART_DIRECTION.md` or
`docs/assets-style-guide.md` disagree with it, they are stale and this wins.

It exists so the player never has to brief this again. Do not summarise it away,
do not "simplify" it, and do not quietly drop a section because it is large.

Sections 1–40 are the player's concept, reproduced faithfully.
**Section 41 onward is this repo's own work**: the review, the gaps found in the
concept, the migration path from what is already built, and the vanilla asset
map. Those are separable on purpose — the player's design is not edited, it is
annotated.

---

## 1. High-level concept

One connected overworld, seed-based island distribution, whose mood changes with
distance from the central Skyreach tower. The world tells one road:

**SKYREACH → GARDEN OF EDEN → STEINFELD → GEISTERNEBEL (the Veil) → GHOST REALM
→ CROOKED BEYOND → INFERNAL FRINGE → HELL ANTECHAMBER → HELL**

The biomes are **not concentric rings**. The generator turns distance from the
Skyreach origin into biome *weights*, and neighbouring progression steps overlap
heavily. Every seed differs; the world reads as naturally explorable.

There are only **two real progression gates**:

- **Gate 1 — The Veil / Geisternebel.** Separates the world of the living from
  the world of the dead. Opened by boss + séance quest.
- **Gate 2 — Hell Gate.** Separates the free afterlife from the actual endgame.
  Opened by boss + infernal questline.

Every other transition happens organically, through distance, landscape,
enemies, resources and worldgen.

## 2. Design philosophy

**The core rule.** The further the player is from the Skyreach tower, the more
the following decay: natural order, architecture, colour, perspective, social
order, physical logic. The world begins idealised and ends absurd.

| realm | what the visuals say |
|---|---|
| Skyreach | perfect order |
| Eden | perfect nature |
| Steinfeld | order decays |
| Ghost Realm | life is gone |
| Crooked Beyond | reality no longer works properly |
| Infernal Fringe | chaos suddenly becomes bureaucratic |
| Hell | organised absurdity + violence + black humour |

## 3. Worldgen system

Every world has a fixed **`SkyOrigin`** at the main tower / central Skyreach
region. For each generated island the generator computes
**`distanceFromSkyOrigin`**, from which comes:

```
realmDepth = normalizedDistance     // 0.0 – 1.0
```

Distance does **not** set a hard biome zone. It sets biome weights:

| realm depth | main biomes |
|---|---|
| 0.00–0.15 | Skyreach |
| 0.10–0.30 | Skyreach + Eden |
| 0.20–0.42 | Eden |
| 0.32–0.48 | Eden + Steinfeld |
| 0.42–0.58 | Steinfeld |
| 0.48–0.70 | Steinfeld + Ghost Realm |
| 0.60–0.80 | Ghost Realm |
| 0.70–0.88 | Ghost + Crooked Beyond |
| 0.80–0.94 | Crooked + Infernal Fringe |
| 0.90–1.00 | Infernal / Hell |

Every island additionally carries a **`distortion`** value, which selects
objects and variants *within* the same biome.

A Ghost biome at low distortion: classic graveyards, fog, old trees, ghosts.
At high distortion: floating gravestones, bent trees, green ectoplasm plants,
crooked buildings, first stripes, eyes, impossible geometry.

This is what dissolves the hard optical borders.

## 4. Tier 0 — Skyreach

**Role.** Spawn area. Main hub. Safest region. Home of the friendly NPCs. Centre
of the whole mod.

**Look.** White, cream, light blue, pink, warm gold, single pastel tones.
**No desaturated fantasy look.** The sky must read alive and friendly.

**Tiles.** Cloud Floor · Dense Cloud Floor · Pearl Cloud · Celestial Marble ·
Gold-Inlaid Marble · Pink Cloud Carpet · Skyglass Floor · Holy Garden Soil ·
Cloud Bridge Tile

**Vegetation.** Cloud Bush · Pink Heaven Flowers · Blue Heaven Flowers · Golden
Grass · Star Flowers · Angel Lilies · Cloud Fern · Halo Shrub · Sky Blossom Tree

**Trees.** Big clear silhouettes — *not vanilla trees painted white*.
- **Cloudwood Tree** — white trunk, fluffy pastel crown
- **Halo Tree** — golden branches, bright leaves
- **Seraph Tree** — tall bright trunk, feathered crown

**Resources.** Cloudwood (wood tier 1: furniture, early tools, floors, sky
architecture) · Sky Petal (alchemy / early healing) · Halo Dust (rare plant/mob
resource) · Celestial Stone (early building material) · Sun Gold Ore (rare sky
ore — **not** a real endgame metal).

**Passive animals.**
- **Cloud Sheep** — tameable, feed *Sky Wheat*, product *Cloud Wool* (beds,
  clothing, carpets, sky deco)
- **Halo Hen** — tameable, feed *Star Seeds*, product *Golden Egg*, ~1 per
  in-game day
- **Sky Bunny** — catchable pet, no husbandry resource needed
- **Cherub Finch** — critter, catchable with a special net, deco cage or pet

**Fish.** Cloudfin (common) · Halo Koi (rare) · Angel Minnow (alchemy)

**Enemies.** Near the tower mostly peaceful. Further out: **Fallen Wisp** (small
ranged) · **Jealous Cherub** (odd aggressive mini-angel) · **Cloud Mimic**
(disguised as harmless cloud deco).

**NPCs.**
- **The Caretaker** — first story NPC; tutorial, lore, quest giver.
  > "Welcome to Skyreach. Stay near the tower until you have found your footing.
  > The clouds beyond are less forgiving than they look."
- **Seraphine — Celestial Healer.** Healing, potions, status cures, later
  revival items. Tier 0 store: Minor Celestial Remedy, Bandage, Sky Petal,
  Simple Revival Draught.
- **Aurelius — Heaven Smith.** First specialist forge: Cloudwood tools,
  Celestial Stone tools, decorative gold. After Eden: Eden Bronze. After Veil:
  Soulforged equipment.
- **Pip — Cherub Merchant.** Humour NPC: random furniture, plants, deco, dyes,
  small pets.

**Station — Celestial Workbench.** First mod crafting station. Recipes from
Cloudwood + Celestial Stone: mod building material, furniture, simple sky
weapons, farming goods.

## 5. Tier 1 — Garden of Eden

**Concept.** Not "green heaven". Eden is an exaggerated biological explosion:
big, lush, dense, warm, colourful, alive.

**Look.** Deep green ground, intense blue sky, turquoise water, white sand, big
colourful blooms, giant fruit.

**Tiles.** Eden Grass · Rich Eden Soil · White Paradise Sand · Turquoise Shallow
Water · Eden Moss · Flower Carpet · Root Floor

**Vegetation (extremely dense).** Paradise Fern · Giant Monstera · Eden Palm ·
Flowering Vine · Giant Lotus · Red Paradise Flower · Blue Paradise Flower ·
Golden Orchid · Adam's Vine · Serpent Grass

**Trees.** Tree of Plenty (several fruit kinds) · Giant Fig Tree · Pomegranate
Tree · Paradise Palm (Paradise Coconut) · **Knowledge Tree** (rare worldgen
object, not a normal farm plant).

**Farmable.** Paradise Wheat (high-value food) · Golden Carrot (animal feed +
cooking) · Eden Berry (sweets) · Moon Melon (juice + potion) · Sun Grape
(wine/juice system, no alcohol dependency required) · Paradise Pepper (buff
food).

**Food.** Eden is where real mod cooking starts: Eden Fruit Salad · Paradise Pie
· Golden Bread · Sun Grape Juice · Coconut Bowl · **Ambrosia** (only fully
craftable later).

**Animals.**
- **Eden Peacock** — feed *Paradise Seeds*, product *Paradise Feather* (ranger
  gear, furniture, feather deco)
- **Paradise Goat** — feed *Golden Carrot*, product *Paradise Milk* (cooking,
  healing recipes, Ambrosia)
- **Eden Bee** — beehive system, product *Paradise Honey*; requires flowers in
  radius, more flowers = more production
- **Fruit Bat** — catchable critter/pet, not livestock

**Dangers.** Beauty is no protection.
- **Eden Serpent** — poison attack; drops Serpent Scale, Venom Fang
- **Bloom Maw** — carnivorous flower
- **Jealous Vine** — attacks out of vegetation
- **Golden Hornet** — fast air enemy
- **Forbidden Serpent** — elite, near Knowledge Trees

**Mining.** Eden caves hold **Eden Copper / Verdant Ore** → **Eden Bronze**, the
first real mod combat metal.

**Station — Eden Press.** Mechanical fruit press. Fruit in; juices, extracts,
oils out. Sun Grape → Sun Juice · Moon Melon → Moon Nectar · Paradise Coconut →
Eden Oil.

**NPC — Eveleen, Eden Botanist.** Unlocked by discovering an Eden island +
collecting three Eden plants. Can join the settlement; works Farming and
Forestry. Store: seeds, saplings, rare plants, Bee Hive. Later: Knowledge
Cutting, Ambrosia recipe.

## 6. First main boss — the Keeper of the First Garden

> **RENAMED 2026-08-31, by the player.** This boss was called "The Garden
> Warden" in the original concept. That collides with the mod's existing **Sky
> Warden**, who is not a boss at all but the player's central companion from the
> first hour (see §41.7). Candidate names the player offered: **Keeper of the
> First Garden**, **The Serpent Crown**, **The First Thorn**. "Keeper of the
> First Garden" is used throughout this document until the player picks;
> whoever implements it should confirm the final name first.
>
> The drop keeps its role but not its name: **Warden Seed** should become
> **Keeper's Seed** (or the equivalent for the chosen name), because the séance
> quest hands it to the Ferryman and "Warden" there would read as the Sky
> Warden.

**Where.** A large Ancient Garden / Sanctuary deep in Eden, toward Steinfeld.

**Look.** A former paradise guardian. Large. Plants + golden armour + serpents.

**Phases.** (1) classic guardian, melee + vines. (2) arena overgrows, poison
plants appear. (3) a Forbidden Serpent merges with him.

**Drops.** Guaranteed: **Keeper's Seed** (progression item). Also: Warden Bark ·
Serpent Crown · Eden Core · Keeper's Weapon · Keeper's Armor Material.

**Quest.** The Caretaker, after the win:
> "The Garden has opened its roots to you. But what you woke beneath them was
> never meant for the living."

Player receives **WHISPERS BEYOND THE STONES** — go find Steinfeld.

## 7. Tier 2 — Steinfeld / The Quiet Reach

**Function.** The most important transition biome. Not simply a grey plain —
Steinfeld visualises the death of paradise.

Near Eden: green grass, bright stone, single Eden flowers, broken angel statues.
Further out: pale grass, big stone slabs, dead trees, gravestones, fog, ghost
apparitions.

**Tiles.** Pale Grass · Weathered Stone · Cracked Heaven Marble · Dead Soil ·
Ash Grass · Mist Stone · Grave Soil

**Plants.** Withered Grass · Pale Reed · Widow Flower · Dead Heaven Bloom ·
Ghost Mushroom

**Resources.** Pale Stone (building) · Grave Salt (alchemy) · Spirit Moss (later
séance) · Echo Shard (from ghost apparitions)

**Enemies.** Lost Pilgrim (ghost fragment) · Stone Mourner (statue that wakes) ·
Hollow Angel (broken heaven guardian) · Grave Crow (flying)

**World events.** Transparent ghosts appear occasionally. Not attackable. They
walk to a grave, to a door, or to the map edge — visually steering the player
toward the Veil.

## 8. The Veil — Geisternebel

Past a defined realm depth a permanent fog effect exists.

Before unlock, a debuff — **Soul Exposure** — stacks in the fog:

| seconds | effect |
|---|---|
| 0–3 | slight vision reduction |
| 4–7 | slow |
| 8–12 | health drain |
| 12+ | massive damage |

So a short step in is possible; running through is not.

**Teleport / movement abuse must be handled.** Do not merely block tiles — the
effect is checked against the **world region**.

## 9. Séance questline

After the Keeper of the First Garden and first contact with the Veil, **Madame Orla** appears
— an eccentric medium, friendly-strange rather than grim-evil, living in a small
house in Skyreach.

> — "The fog nearly killed me."
> — "Of course it did. You are breathing."
> — "That is usually considered a good thing."
> — "Not where you are going."

**Quest — A CALL TO THE OTHER SIDE.** Needs 5 Spirit Moss · 3 Echo Shards ·
1 Keeper's Seed · 1 Golden Candle · 1 Personal Offering.

Orla crafts the **Séance Table** (placeable station). On placement: light dims,
candles react, a spirit appears — **The Ferryman / Elias**.

> — "You knock loudly for someone who is not dead."
> — "I need to cross." / "I am looking for someone." / "Never mind."
> — "The Veil does not care what you need. It cares what you are."

The player hands over the Keeper's Seed.

> — "Life touched by death. That might confuse it."

**Reward — VEIL MARK.** A permanent character unlock, **not** a losable
inventory item. Optional cosmetic item: *Veil Sigil*.

**Effect.** Soul Exposure is disabled. The fog stays visible, and parts locally
around the player when crossing — so the border stays legible.

## 10. Tier 3 — Ghost Realm / Aftergarden

**Concept.** Tim-Burton-like. Spooky but **not grey**.
Palette: petrol, turquoise, violet, poison green, black, cold white.

**Tiles.** Haunted Grass · Graveyard Soil · Spirit Stone · Ghost Moss ·
Ectoplasm Puddle · Black Cobble · Violet Dirt

**Trees.** Crooked Dead Tree · Spirit Willow · Lantern Tree · Bonewood

**Plants.** Ghost Lily · Ectoplasm Fern · Mourning Rose · Spirit Mushroom ·
Widow Vine

**World objects.** Gravestones · mausoleums · coffins · sarcophagi · Spirit Basin
· Void Flame · crooked fences · lanterns · floating candles · urns · broken
statues

**Resources.** Ectoplasm (universal ghost resource) · Soul Thread (textile) ·
Bonewood (wood tier) · Spectral Ore → **Spiritsteel Bar**

**Machines.**
- **Soul Loom** — Soul Thread + Cloud Wool etc. → Ghost Cloth, Spectral Armor,
  curtains, haunted furniture
- **Spirit Forge** — Spectral Ore + Ectoplasm → Spiritsteel equipment. Special
  mechanic: certain weapons gain a **Ghost Modifier**.

**Enemies.** Drifter (standard) · Headless Butler (melee) · Possessed Chair
(deco wakes) · Lantern Widow (ranged) · Coffin Crawler (coffin with legs) ·
Soul Hound (fast) · Mourning Bride (elite)

**Humour.** Not every ghost is hostile. Some enemies may say on death: *"Rude."*

## 11. Ghost NPCs

- **Mortimer — Undertaker.** Shop: coffin, grave decorations, black candles,
  urns, Bonewood furniture. Recruit: after building a graveyard in the
  settlement.
- **Caspern — Spirit Smith.** No direct Casper copy; own name/design. Shop:
  Spiritsteel, spectral weapon recipes, Soul Thread. Recruit: build the Spirit
  Forge.
- **Eleanor — Lost Soul.** Quest NPC. The player helps her find out why she
  still exists. Two endings: **Pass on** (reward: strong trinket) or **Stay**
  (Eleanor becomes a recruitable settler). This is what gives the Ghost Realm
  emotional content.

## 12. Ghost animals

- **Spirit Sheep** — feed *Ghost Wheat*, product *Soul Wool* (Ghost Cloth)
- **Grave Chicken** — feed *Ghost Seeds*, product *Hollow Egg* (cooking +
  alchemy)
- **Ecto Slug** — terrarium critter, can produce *Ectoplasm Droplet*
- **Ghost Koi** — catchable fish, potions + food
- **Soul Moth** — net-catchable, deco or ingredient

## 13. Tier 4 — Crooked Beyond

Now the world physically breaks apart.

**Look.** Black-and-white stripes · neon green · violet · red · cyan ·
checkerboard · spirals. **Still Necesse pixel art** — no chaotic random
textures. All assets share one deliberately defined palette.

**Tiles.** Crooked Stripe · Spiral Soil · Checker Stone · Violet Mud · Eye Floor
· Bent Grass · Wrong-Way Tile

**Decoration.** Doors without a house · windows in the ground · eyes in plants ·
teeth-rocks · bent lanterns · crooked clocks · giant keys · absurdly long chairs
· living carpets

**Plants.** Eyeball Shrub · Spiral Tree · Screaming Flower · Striped Mushroom ·
Tongue Plant

**Resources.** Oddwood · Warp Resin · Reality Shard · Eye Seed · Strange Fabric

**Station — Reality Stitcher.** Built from Spiritsteel + Reality Shards + Soul
Thread. Joins materials that normally cannot combine. Produces Crooked weapons,
morphing furniture, high-tier trinkets, Pocket Door, warped building materials.

## 14. Crooked fauna

- **Stripe Beetle** — catchable, drops Striped Shell
- **Long-Legged Chicken** — tameable, deliberately silly; feed *Eye Seeds*,
  product *Odd Egg*
- **Crooked Goat** — feed *Spiral Fruit*, product *Questionable Milk*
- **Door Mimic** — not livestock; looks like a door; enemy
- **Carpet Worm** — enemy/critter, rarely drops a pet egg

## 15. Crooked NPC — The Doorman

**Mr. Knott**, standing at a free-standing red door.

> — "Going somewhere?"
> — "I think so."
> — "That makes one of us."

Store: keys, doors, portals, weird furniture, cosmetic masks. Important later
for the Hell quest.

## 16. Crooked boss — The Architect

Boss of an impossible house. A tall narrow figure whose body parts read as
building elements; he can move rooms.

**Mechanic.** The arena appears to change geometry during the fight —
technically, tile/obstacle presets swap.
(1) walls move. (2) doors spawn enemies. (3) checker/stripe attacks.

**Drop.** **Architect's Key** (central progression item). Also: Reality Core ·
Crooked Weapon · Architect Mask · Impossible Furniture recipes.

Mr. Knott recognises the key:
> — "Oh. *That* key." / "You really should not have that."
> — "What does it open?"
> — "Yes."

## 17. Infernal Fringe

A transition. Still Crooked Beyond, with the first hell elements.

**Look.** Brass · red · black · burnt beige · pipes · signs · numbering systems.

**Enemies.** Infernal Clerk · Ticket Imp · Boiler Hound · Ash Spirit

**Resources.** Infernal Brass · Ash Coal · Bureaucratic Seal · Furnace Core

## 18. Second gate — Infernal Processing

Not a magic barrier. **Hell begins with an authority.**

**Building — Department of Eternal Processing.** Waiting room, counters, ticket
machines, filing cabinets, broken fans, pipes, hell elevators.

**NPC — Clerk 666-B.**
> — "Purpose of damnation?"
> — "I'm not damned."
> — "Then you are in the wrong queue."
> — "I need to enter."
> — "Form?"
> — "What form?"
> — "Exactly."

**Quest — INFERNAL PAPERWORK.** Three absurdly literal quest objects:
**Proof of Death** (Ghost Realm) · **Proof of Life** (Eden) · **Proof of
Unreasonable Intent** (the Architect). The three represent the whole journey.

> — "Alive, dead and unreasonable. Everything appears to be in order."

**Reward — INFERNAL VISA.** Permanent unlock; the Hell Gate can be opened.

## 19. Tier 5 — Hell Antechamber

Not yet pure fire. A bizarre **city**: crooked tenements, demon shops, forges,
bars, prisons, offices, markets.

**Friendly demons.** Not everything attacks — that is what makes hell read as a
society.

- **Brim — Infernal Blacksmith.** Infernal Brass, Hellsteel, weapon enhancements
- **Moxie — Demon Cook.** Absurd food
- **Vex — Contraband Merchant.** Stolen heaven items, crooked artefacts, rare
  pets, unusual furniture

## 20. Hell resources

Hellsteel Ore (endgame ore, Infernal Forge) · Brimstone (alchemy + building) ·
Infernal Brass (machines + deco) · Demon Hide (armor) · Hellglass (windows +
magic items) · Furnace Heart (elite/boss drop)

## 21. Hell machines

- **Infernal Forge** — highest standard crafting tier: Hellsteel armor, tools,
  hell weapons, endgame components
- **Soul Boiler** — Ectoplasm + Brimstone → Condensed Soul Fuel
- **Hellglass Furnace** — Paradise Sand + Brimstone → Hellglass *(deliberately
  makes an early resource relevant again)*
- **Celestial-Infernal Crucible** — late special station combining heaven + hell
  materials. Sun Gold + Hellsteel + Reality Core → **Ascendant Alloy**

## 22. Hell fauna

- **Ember Pig** — feed *Ash Root*, product *Ember Truffle*
- **Hell Goat** — feed *Brimberry*, product *Infernal Milk*
- **Fire Hen** — feed *Charred Seeds*, product *Ember Egg*
- **Lava Snail** — critter, produces Molten Slime
- **Imp** — **not** tameable, but rarely obtainable as a pet egg

## 23. Hell crops

Ash Root (grows on Ash Soil) · Brimberry · Ember Pepper · Devil Lettuce
(deliberately silly name allowed) · Coal Mushroom

**Endgame cooking** must need materials from several worlds:
- **Divine Ambrosia** = Paradise Milk + Paradise Honey + Golden Egg + Sky Petal
- **Forbidden Ambrosia** = Divine Ambrosia + Questionable Milk + Brimberry
  (large buff)

## 24. Endgame — celestial/infernal hybrid system

The strongest items must **not** be hell drops alone, so old regions stay
relevant. Endgame needs Heaven + Eden + Ghost + Crooked + Hell material.

**Ascendant Blade** = Sun Gold + Warden Bark + Spiritsteel + Reality Shard +
Hellsteel. The end gear represents the entire journey.

## 25. End boss — The Auditor

Hell should not simply use Satan. **The Auditor** is an ancient infernal
instance overseeing the order between life, death and hell.

**Design.** Huge slender demonic figure · robe · brass · several arms · books /
files · burning seal · partial angelic symbolism — making clear that heaven and
hell may be more connected than assumed.

**Phases.** (1) ORDER — precise geometric attacks, gold/white. (2) DEATH — ghost
attacks, summons. (3) CHAOS — crooked arena, perspective/tile attacks.
(4) INFERNO — classic hell escalates. **Final:** all four styles at once.

**Drops.** Seal of the Auditor (opens the endgame system) · Auditor Core
(crafting) · Ledger of Eternity (trinket) · boss weapons, one each for melee,
ranged, magic, summon.

## 26. Postgame

After the Auditor, Skyreach changes slightly: new NPC dialogue, new
travels/missions, new elite islands.

**Realm Incursions.** A random island gets a modifier: **Overgrown** (Eden takes
the sky) · **Haunted** (Ghost Realm infects another biome) · **Crooked**
(reality distorts) · **Infernal** (hell creatures appear). Old biomes become
postgame-relevant.

## 27. Settlement integration

New NPCs use vanilla settlement logic. Necesse already has work for hauling,
crafting, forestry, farming, fertilizing, husbandry, fishing and hunting, plus
specialised settlers. **Do not invent an own AI unless necessary.**

| NPC | jobs |
|---|---|
| Eveleen | Farming + Forestry |
| Seraphine | Crafting |
| Aurelius | Crafting |
| Mortimer | Hauling + Crafting |
| Caspern | Crafting |
| Spirit Shepherd | Husbandry |
| Heaven Angler | Fishing |
| Vex | Trading |
| Brim | Crafting |

## 28. Husbandry

Vanilla animals are moved by rope and tamed/bred with feed; husbandry zones let
settlers collect products automatically. Mod animals should adopt this
behaviour.

| realm | animal | feed | product |
|---|---|---|---|
| Heaven | Cloud Sheep | Sky Wheat | Cloud Wool |
| Heaven | Halo Hen | Star Seeds | Golden Egg |
| Eden | Paradise Goat | Golden Carrot | Paradise Milk |
| Eden | Eden Bee | Flowers | Paradise Honey |
| Eden | Peacock | Paradise Seeds | Paradise Feather |
| Ghost | Spirit Sheep | Ghost Wheat | Soul Wool |
| Ghost | Grave Chicken | Ghost Seeds | Hollow Egg |
| Crooked | Long-Legged Chicken | Eye Seeds | Odd Egg |
| Crooked | Crooked Goat | Spiral Fruit | Questionable Milk |
| Hell | Ember Pig | Ash Root | Ember Truffle |
| Hell | Hell Goat | Brimberry | Infernal Milk |
| Hell | Fire Hen | Charred Seeds | Ember Egg |

**No animal may produce the same resource in a different colour.** Every animal
needs an economic reason.

## 29. Fishing progression

Heaven: Cloudfin · Angel Minnow · Halo Koi
Eden: Paradise Bass · Sunscale · Lagoon Ray
Steinfeld: Pale Carp
Ghost: Ghost Koi · Bonefish · Widow Eel
Crooked: Wrongfish · Eye Fish · Doorfin
Hell: Ash Eel · Brimstone Bass · Lavafin

Rare fish serve cooking, alchemy, quests, aquariums and traders. Some may unlock
extremely rare deco.

## 30. Capturable critters

Heaven: Cherub Finch · Sky Butterfly — Eden: Fruit Bat · Paradise Butterfly ·
Golden Beetle — Ghost: Soul Moth · Ecto Slug — Crooked: Stripe Beetle · Carpet
Worm — Hell: Lava Snail · Ash Moth

Each must enable at least one of: terrarium, cage, pet, alchemy, decoration.

## 31. Store progression

NPC offers are not static; they react to quest tier.

**Seraphine, by stage:** initial → Minor Celestial Remedy. The Keeper
defeated → Eden Remedy, Antivenom. Veil unlocked → Spirit Cleanse, Soul Ward
Potion. Architect defeated → Reality Stabilizer. Hell unlocked → Infernal
Resistance Potion. Auditor defeated → Ascendant Elixir.

## 32. Mission system

Necesse already has repeatable missions/expeditions via settlers and the mission
board. Extend that structure.

- **Botanist Expedition** (Eden) → seeds, fruit, flowers
- **Spirit Expedition** (Ghost) → ectoplasm, soul thread, rare furniture
- **Crooked Expedition** (high risk) → reality shards, oddwood, strange furniture
- **Infernal Expedition** (very high risk) → brimstone, infernal brass, hellglass
- **Cross-Realm Expedition** (postgame) → random endgame resources

## 33. Happiness objects / decoration

Every realm needs characteristic happiness deco.

- **Heaven** — Angel Statue · Cloud Bench · Halo Lamp · Golden Fountain · Sky
  Harp · Cherub Painting
- **Eden** — Fruit Basket · Paradise Fountain · Giant Flower Pot · Peacock
  Statue · Garden Swing
- **Steinfeld** — Weathered Angel · Stone Memorial · Mossy Bench
- **Ghost** — Sarcophagus · Spirit Basin · Coffin Shelf · Haunted Mirror · Ghost
  Portrait · Void Flame · Mourning Clock
- **Crooked** — Slime Bench · Sheep Chair · bizarre Wooden Duck variant · Eye
  Lamp · Long Chair · Impossible Cabinet · Stripe Carpet · Door Painting
- **Hell** — Scrap Lamp · Scrap Heap · Infernal Throne · Demon Bust · Boiler ·
  Hell Clock · Burning Filing Cabinet

## 34. Resource relevance rule

**Critical.** New tiers must not make old resources worthless. Every new
crafting tier requires at least one resource from an earlier area.

Ghost gear = Spiritsteel + Cloud Wool. Crooked gear = Reality Shard +
Spiritsteel. Hell gear = Hellsteel + Reality Shard. Ascendant gear = materials
from all realms.

## 35. Asset systematics per biome

Every complete biome needs at least:

- **Terrain** — base ground, alternative ground, transition/splat, sand/stone,
  water if present
- **Natural objects** — 3–5 plants, 2–4 bushes, 3+ trees, 2 rocks, 1–2 rare
  large objects
- **Resources** — ore, wood, plant material, mob material, rare material
- **Architecture** — wall, floor, door, fence, lamp, chest, table, chair, bed,
  storage, crafting station
- **Decoration** — at least 10 objects
- **Fauna** — 1–3 critters, 1 fish group, 1–3 passive animals
- **Enemies** — 3 standard, 1 ranged, 1 fast, 1 elite
- **Dungeon/structure** — at least one characteristic structure

So no realm feels like a single tileset.

## 36. Pixel-art identity

- **Heaven** — round/fluffy shapes, golden highlights, very bright palette
- **Eden** — big organic clusters, high saturation, many leaf sizes
- **Steinfeld** — more negative space, broken shapes
- **Ghost** — long crooked silhouettes; petrol/violet/green
- **Crooked** — extreme clear patterns; black-white + neon accents; **no random
  pixel noise**
- **Hell** — thick heavy silhouettes; brass, black, red; fire only as a
  component, never as the whole design

## 37. Story summary

The player wakes in Skyreach. The sky seems perfect. In the Garden of Eden he
discovers even paradise holds danger. The Keeper of the First Garden falls. Behind it
lies Steinfeld, where life slowly disappears. The Geisternebel blocks passage.
Through Madame Orla the player holds a séance and receives the Ferryman's
permission to cross the Veil.

The Ghost Realm first shows a classic afterlife, but with distance the world
loses its physical order. The player reaches Crooked Beyond, defeats the
Architect and receives a key nobody will explain. The key leads to the infernal
administration. After obtaining absurdly bureaucratic proofs he receives his
Infernal Visa.

Hell turns out not to be a fire landscape but a bizarre functioning society.
There the player finally discovers that heaven, death and hell are parts of the
same cosmic administrative system. The Auditor ends the main progression. After
his death the border between realms becomes unstable — the postgame begins.

## 38. Main quest order

01 Welcome to Skyreach · 02 A Garden Below · 03 Fruit of Paradise · 04 The
Watcher in the Garden · 05 The Keeper of the First Garden · 06 Whispers Beyond the Stones ·
07 Into the Mist · 08 A Call to the Other Side · 09 Preparations for a Séance ·
10 The Ferryman · 11 Marked for Passage · 12 Beyond the Veil · 13 The Dead Have
Problems Too · 14 Something Is Wrong With Reality · 15 Every Door Leads
Somewhere · 16 The Impossible House · 17 The Architect · 18 That Key ·
19 Infernal Processing · 20 Form 666-B · 21 Approved for Damnation · 22 Welcome
to Hell · 23 Business as Usual · 24 The Ledger · 25 The Auditor · 26 An
Administrative Error (postgame begins)

## 39. Unlock matrix

| event | unlock |
|---|---|
| game start | Skyreach |
| Eden discovered | Botanist can appear |
| Eden farming started | Eden Press |
| The Keeper defeated | Spirit quest, new stores |
| Séance completed | Veil Mark |
| Veil Mark | Ghost Realm |
| Ghost Realm entered | Spirit Forge |
| Ghost quest completed | Soul Loom |
| Crooked discovered | Reality recipes |
| Architect defeated | Architect Key + Reality Stitcher |
| Infernal Paperwork | Infernal Visa |
| Hell entered | Infernal Forge |
| Hell City quest | Celestial-Infernal Crucible |
| Auditor defeated | Ascendant equipment + Realm Incursions |

## 40. The central rule for the whole mod

**No biome may be only a visual skin.** Every realm needs its own reason why the
player explores, fights, farms, fishes, mines, keeps animals, recruits NPCs,
crafts, builds/decorates, and **later returns**.

If a region is visited only for a boss and is irrelevant afterwards, its economy
and resource design is not finished.

The goal is a world where, at the end of hell, the player still has a reason to
travel back to Eden, keep Cloud Sheep, fish ghost fish or gather crooked
material. Then Stairway to Heaven does not feel like seven biomes in a row, but
like one connected world with a working economy, progression and society.

---

---

# Part A2 — the Sky Warden and his house (player amendment, 2026-08-31)

This section was added by the player after the concept above, and it changes two
things in it: the Eden boss's name (§6) and the answer to the travel problem
(§42.2). It is part of the constitution, not a review note.

## A2.1 The Warden is a companion, not a gate

> *"dann darf der Warden nicht als späterer Boss/Unlock-NPC gedacht werden. Er
> ist von Anfang an dein zentraler Begleiter in der Stadt, und seine Progression
> sollte über sein Haus bzw. zusätzliche Räume laufen. Das passt viel besser zu
> dem, was schon existiert."*

- He is recruited **very early** and moves into the settlement.
- His first quest stays the **two cats**, who then live in the placeable Cat
  Basket. That establishes him immediately as an odd but friendly quest NPC.
- **After that the NPC does not grow — his BUILDING does.**

## A2.2 The house grows with the story

Each chapter adds a room. The world's progression becomes physically visible in
the player's own town, which is the point.

| room | unlocked by |
|---|---|
| Living quarters | from the start |
| **Cat room** | the cat quest |
| **Observatory / map room** | Eden discovered |
| **Séance room** | contact with the Geisternebel |
| **Relic chamber** | first major Ghost / Crooked progress |
| **Infernal Archive** | hell artefacts appear |
| **Final archive / portal room** | endgame |

The Warden asks for space after each chapter, and either triggers the extension
himself or **requires the player to build it** to a spec: a room of a minimum
size holding named furniture and station objects. The room becomes functional
only once it is built.

Worked example, the séance:

> *"Der Nebel reagiert nicht auf Gewalt. Wir brauchen jemanden, der mit ihm
> sprechen kann. Und dafür brauche ich einen Raum, in dem die Toten glauben, sie
> seien eingeladen."*

Requirements: a room of a minimum size · a **Séance Table** · **4 Spirit
Candles** · a **Spirit Basin** · possibly a **Veil Bell**. The séance quest then
starts in that room.

And the hell equivalent, when the player brings back an **Infernal Seal**:

> — *"Bitte sag mir, dass du das nicht im Haus öffnen willst."*
> — *"..."*
> — *"Ich brauche einen neuen Raum."*

That produces the **Relic chamber**, where dangerous items sit on pedestals.
Those pedestals should carry real function later rather than staying quest
markers.

## A2.3 The house IS the travel system

**This is the answer to the travel problem** that Part B §42.2 records as the
concept's largest hole. It is not a generic teleporter network: each anchor is a
themed room in the Warden's house, and each is earned.

| stage | what opens |
|---|---|
| early game | ordinary travel between Skyreach, Eden and Steinfeld |
| after the séance | Warden's house ↔ **Ghost Realm** anchor |
| after Crooked | a second anchor in **Crooked Beyond** |
| after the Hell unlock | the **Infernal** anchor |

**The rule that makes it work: a route only becomes fast travel after the player
has physically made it once.** Exploration stays mandatory on first contact; the
grind of the return trip does not.

The anchors are world-flavoured, never generic pads:

- **Ghost** — a séance mirror, or a spirit door
- **Crooked** — an absurd red door (Mr. Knott's, §15)
- **Hell** — an infernal elevator

Each stands in a different room, so the house reads as increasingly absurd:
living room and cats at the start; later a cat room, a map room, a séance
chamber full of candles, sealed ghost relics, a red door that should not fit
anywhere, and a demonic lift.

## A2.4 What this overturns

- **§6's boss name.** Retired — see the note there.
- **§42.2 (travel).** Answered. The gap entry stays for its reasoning, but the
  solution is this section.
- **The mod's shipped Warden content is now on-plan rather than legacy.** The
  recruitment path, the cat quest, the Cat Basket and the Warden's Spire preset
  were all built before this concept and looked like orphans under it. They are
  the foundation of A2.1 and A2.2 instead. Do not retire them.
- **`docs/DESIGN_DECISIONS.md`'s 30,000 coin recruit price** now has a second
  reason to stay low: he is a first-hour companion, not an endgame unlock.

# Part B — this repo's review of the concept

Everything from here is **not** the player's text. It is the analysis this
repository owes the concept before building it.

## 41. What this concept OVERTURNS in the existing repo

These are live contradictions. Where they are not yet fixed in code, the concept
wins and the code is wrong.

### 41.1 The palette instruction was wrong and is retired

`docs/assets-style-guide.md` said *"Muted bases … 'Cool, not kitschy': weathered
stone, cold air, electric light — no rainbows, no gold-trimmed clouds"*, and
`docs/DESIGN.md` said *"cool, muted"*. **§4 of this document says the opposite
for Skyreach**: white, cream, light blue, pink, warm gold, pastels, explicitly
*"keine entsättigte Fantasy-Optik"* — gold-trimmed clouds are now the brief, not
the thing to avoid.

The player named this directly: *"ich habe festgestellt dass viele Vorgaben für
Skyreach nicht passen zb alles nur entsättigt sein soll.. das ist falsch"*.

Those lines are corrected in place. **"Muted" is dead as a global rule.**
Saturation is now per realm (§36): Heaven bright, Eden highly saturated, Ghost
petrol/violet/poison-green, Crooked neon-on-monochrome, Hell brass/red.
The one thing that survives from the old rule is *coherence* — each realm has a
defined palette and stays inside it.

### 41.2 Two dimensions become one world — already half done

The Veil is no longer a dimension you reach through a door; §1 makes it a **fog
gate inside the one world**. This repo already began that move on 2026-08-31
(the Beetle Outlands), for the player's earlier reason ("wir machen nur sky
region"). That direction is confirmed and extended by this concept.

The `veil2` dimension stays registered so existing saves load. It takes no new
content, and the Séance Circle's role changes as §9 describes.

### 41.3 The existing sky sub-biomes are not in the new list

The mod ships Driftlands, Stormveil, Skyway Passages and Aurora Shoals. The
concept's Tier 0 is one "Skyreach". **Do not delete them** — they become
*variants inside Tier 0*, which is exactly what §3's `distortion` value is for.
Driftlands is the default; Stormveil, Skyway and Aurora become distortion or
sub-noise variants of Skyreach. That keeps four finished biomes' worth of art
and worldgen.

### 41.4 The Beetle Outlands ARE Crooked Beyond

This is the cleanest mapping in the whole migration, and it means Tier 4 is
partly built already. The Outlands are striped, violet, wrong, Beetlejuice-based
— that is §13 exactly. What exists and carries over: the `beetlefreak` striped
ground, `evilwall` crystal massifs, the Crooked House preset, the
`beetlewall`/door/window set, and the distance-gated placement machinery in
`SkyOutlands`.

**Rename, do not rebuild.** The `outlands` biome becomes Crooked Beyond, and its
900-tile floor becomes the realmDepth band for tier 4.

### 41.5 The Veil's existing content is Tier 3

Gloomfen and Ashen Reach, with murkwater, blackpeat, murkmoss, ashsand,
deadtree, ashbones, gloomshroom, whisperreeds and the Gloom Shade, are a Ghost
Realm in everything but name (§10). They move from the `veil2` dimension into
the one world at the Ghost Realm's realmDepth band.

### 41.6 Existing NPCs vs. the new cast

The concept names a new cast and does not mention the shipped one. Proposed
mapping, for the player to confirm:

| shipped | proposal |
|---|---|
| Sky Warden | **is** The Caretaker (§4) — same role: first story NPC, tutorial, lore, quest giver, already recruitable |
| Halda the Cellarer | fits Tier 0/1 food-and-brewing; keep, she is the Eden Press's natural owner |
| Ossian Vane | the "sells what nothing else sells" role — closest to **Vex** (§19), or keep both |
| Magpie | courier/buyer — no equivalent named; keep |
| Siggi & Peanut (cats) | no equivalent; keep, they are player-approved |

## 42. Gaps in the concept — things it does not answer

These are not criticisms; they are the questions that will block implementation.

### 42.1 What distance is realmDepth 1.0? — **must be decided first**

§3 normalises distance but never says by what. Everything downstream depends on
it. The shipped world already has scales to anchor to: the spire hub is 56
tiles, `CORE_RADIUS` 700, `MID_RADIUS` 1600, and the Outlands ramp runs
900 → 3000.

**Proposal: realmDepth = clamp(distance / 12000).** That puts the bands at:

| realm | depth | tiles from origin |
|---|---|---|
| Skyreach | 0.00–0.15 | 0 – 1,800 |
| Eden | 0.20–0.42 | 2,400 – 5,040 |
| Steinfeld | 0.42–0.58 | 5,040 – 6,960 |
| Ghost Realm | 0.60–0.80 | 7,200 – 9,600 |
| Crooked Beyond | 0.70–0.88 | 8,400 – 10,560 |
| Hell | 0.90–1.00 | 10,800 – 12,000+ |

**This makes §42.2 unavoidable**, which is why it is the next entry.

### 42.2 TRAVEL — the largest unsolved problem in the concept

At any sane normalisation, hell is thousands of tiles from the hub, and §40
requires the player to **keep going back to Eden** for materials. Walking
10,000 tiles each way, repeatedly, over an island world separated by open
Mistsea, is not a loop anyone will run twice.

The concept has no travel system. `ROADMAP.md` already flagged the smaller
version of this ("the traverse problem the Mistsea creates still has no
answer"), and nine tiers make it nine times worse.

**ANSWERED 2026-08-31 by Part A2.3** — the Warden's house becomes the travel
hub, one themed anchor per realm, each unlocked only after the player has made
that journey once. The analysis below stands as the reasoning; A2.3 is the
design.

**This must be designed before the outer tiers are built, not after.**
Recommended shape: a waypoint network, one anchor per realm, unlocked by that
realm's gate quest, on vanilla's own portal/teleport pattern. The gates in §1
then double as travel unlocks, which also gives Gate 1 and Gate 2 a second
reason to exist.

### 42.3 Realm gating vs. an infinite streamed world

Both gates are described as *region* checks (§8 explicitly). Necesse streams
regions lazily and the player can build, rope, teleport and place beds. A gate
that is a fog debuff keyed to realmDepth is robust; a gate that is a wall is
not. §8 gets this right for the Veil — **the Hell Gate needs the same treatment
and does not currently have it.**

### 42.4 The two gates are one mechanic described twice

Soul Exposure (§8) and the Infernal Visa (§18) are the same thing: a realmDepth
debuff switched off by a permanent character unlock. Build **one** system with
two configurations. One implementation, two quests, no second code path.

### 42.5 Boss arenas are unspecified

Three bosses (the Keeper of the First Garden, the Architect, the Auditor) need places to be fought. The
Architect's needs geometry that changes mid-fight (§16). Nothing says whether
these are worldgen structures, instanced arenas, or summoned encounters.

The mod already has the machinery for the summoned answer: Séance Circles stand
at fixed hashed sites and already say nothing answers yet. **Recommend: bosses
are summoned at fixed per-realm sites**, which reuses what exists and avoids
instancing.

### 42.6 Difficulty is never stated in numbers

The concept has five tiers of gear and no target values. The repo has the
measurement: vanilla's *ordinary* ascended mobs carry 1000 HP (Classic) and
130 damage behind 40 armour, and the mod's own content tops out at 520/70 —
which is why the player reports everything as too easy. Each tier needs a
stated HP/damage/armour band against a named vanilla analogue before its
enemies are built.

### 42.7 Missing: what the player does with a settlement in the outer realms

§27 assigns settler jobs across all realms, but settlements need safety,
happiness and pathing. A Hell settlement is a different proposition from a
Skyreach one. Unspecified.

### 42.8 Minor gaps

- **Steinfeld has no NPC, no boss and no station.** §7 gives it resources and
  enemies only. It is a whole tier with nothing to come back for, which §40
  forbids.
- **Steinfeld and Infernal Fringe have no husbandry animal** (§28 skips both).
- **The Knowledge Tree** (§5) is named as a rare object with no stated function.
- **"Personal Offering"** (§9) is a quest ingredient with no definition.
- **Sky Bunny and the special net** (§4) — vanilla's net exists; whether a new
  net item is needed is unstated.

## 43. Build order this repo recommends

Derived from what is already standing, cheapest real progress first:

1. **Rename and rebase what exists.** Outlands → Crooked Beyond; Veil biomes →
   Ghost Realm; the four sky sub-biomes → Skyreach variants. Pure refactor, no
   new art, and it makes the world match the concept's spine immediately.
2. **Build the realmDepth field** (§3, §42.1) as one function, replacing the
   ad-hoc distance rules now spread across `SkyOutlands` and `SkyOrigin`.
3. **Build the one gate mechanic** (§42.4) with Soul Exposure as its first
   configuration.
4. **Solve travel** (§42.2) before any tier past Ghost is built.
5. **Then** Eden and Steinfeld, which are the two missing *early* tiers and the
   ones the player will actually reach first.
6. Bosses, then Hell.

Eden before Hell, even though Hell is more exciting: the concept's road is only
believable if its first half exists.

---

# Part C — the vanilla asset map

The working method the player set out: **build every realm now from mod assets +
suitable vanilla assets; the player then replaces all used assets in one pass and
the style swaps out wholesale.**

So every vanilla asset used must be recorded here, per realm, with what it
stands in for. `docs/VANILLA_ASSET_MAP.md` holds the live table and is the file
the player works from. Rules:

1. **Every vanilla asset used in worldgen or a preset gets a row** in that file,
   in the same commit that uses it.
2. A row names: the vanilla asset, the realm, what it stands in for, and where it
   is referenced.
3. Vanilla assets are referenced **by string ID** wherever possible (spawn
   tables, object IDs), never copied into `src/main/resources/` — a copy is a
   fork that will not swap out cleanly.
4. When a mod asset replaces a vanilla stand-in, the row moves to the "replaced"
   table rather than being deleted, so the history of the swap survives.
