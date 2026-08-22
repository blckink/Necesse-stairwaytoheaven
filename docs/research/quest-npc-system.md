# Necesse 1.3.2 — Quest-Giving Friendly NPC: Reference

Source: decompiled `necesse/` sources at `/home/user/necesse-game/decompiled/necesse/`.
All paths below are relative to that root unless given in full. Signatures and constants
are quoted; multi-line method bodies are described in prose instead of pasted.

---

## 0. Headline correction to the brief

The Elder (`entity/mobs/friendly/human/ElderHumanMob.java`) does **not** give quests in
vanilla 1.3.2. It extends `HumanShop`, which declares the quest-giving hook
(`getQuests(ServerClient)`), but `ElderHumanMob` overrides it to return `null` (line 391),
same as the `HumanShop` default (line 222). No shipped `HumanShop` subclass (Alchemist,
Blacksmith, Trader, Mage, Stylist, PawnBroker, Pirate, Explorer, Hunter, Miner, Farmer,
Gunsmith, Angler, ExoticMerchant, Elder…) overrides it with real content either. Likewise
`entity/mobs/QuestGiver.java` (the mob-side quest interface) has **zero implementers** in
the decompiled sources — confirmed three ways:

1. `grep "implements QuestGiver"` → no hits anywhere.
2. `PacketQuestGiverRequest.processServer` does `if (mob instanceof QuestGiver) …` — a
   dead branch today.
3. `ElderHumanMob.init()` still unconditionally sends `new PacketQuestGiverRequest(this.getUniqueID())`
   to the server — leftover wiring for a feature that was never finished on the Elder.

So the engine's generic `Quest`/`QuestRegistry`/`QuestGiver` system is real, functional,
networked and save-capable (it's exercised by an F1 dev-tool, see §1.3), but it ships
**dormant** — no vanilla NPC currently uses it. This matters for the recommendation in §9.

The mob-icon-suggested "CavelingElderMob" also doesn't exist as a class. `cavelingelder.png`
is a plain `GameTexture` (`MobRegistry.Textures.cavelingElder`, `engine/registries/MobRegistry.java:1179,1615`)
used by exactly two things: `ElderHumanMob.addDrawables` (an April Fools cosmetic swap) and
`PetCavelingElder` (a follower-pet mob). See §3.4 for the sprite-sheet analysis.

---

## 1. Quest system

### 1.1 Class hierarchy (`engine/quest/`)

| Class | Extends | Role |
|---|---|---|
| `Quest` (abstract) | `IDDataContainer` | Base: unique ID, per-client active-set, save, network spawn/update packets, dirty-tracking |
| `DeliverItemsQuest` (abstract) | `Quest` | N objectives of `(Item, amount)`; checks/consumes from the player's own inventory |
| `KillMobsQuest` (abstract) | `Quest` | N objectives of `(mobID, mobsToKill)`; tracks kills via `ServerClient.characterStats().mob_kills` deltas |
| `KillMobsTitleQuest` | `KillMobsQuest` | Adds a settable title `GameMessage`; the only concrete class registered in core |
| `HaveKilledMobsQuest` (abstract) | `Quest` | Like KillMobsQuest but checks lifetime kill totals, not quest-scoped counting |

`Quest` key members (`engine/quest/Quest.java`):
- `public Quest()` — self-registers its ID data via `QuestRegistry.instance.applyIDData(...)`.
- `int getUniqueID()` — lazily assigned via `GameRandom.globalRandom.nextInt()`; this is the
  per-*instance* ID (a spawned quest), distinct from the registry's per-*class* type ID.
- `void makeActiveFor(Server, ServerClient)` / `abandonFor(...)` / `isActiveFor(long clientAuth)`
  — per-player activation tracked in a `HashSet<Long> clients` keyed by `client.authentication`.
- `abstract void tick(ServerClient)`, `abstract boolean canComplete(NetworkClient)`,
  `void complete(ServerClient)` (base impl just does `client.newStats.quests_completed.increment(1)`).
- `boolean canShare()`, `canShareWith(me, him)` (default: same team), `onShared(...)` — built-in
  "share this quest with a party member" flow.
- Save: `addSaveData`/`applyLoadData` persist `uniqueID` and the `clients` auth set.
- Network: `setupSpawnPacket`/`applySpawnPacket` (full state, sent once) vs
  `setupPacket`/`applyPacket` (delta payload reused by spawn and by live updates).

`DeliverItemsQuest.canComplete`/`complete` are the direct vanilla precedent for "hand N
items to an NPC" — see §5.

### 1.2 Registry

`engine/registries/QuestRegistry.java` — `EmptyConstructorGameRegistry<Quest>`, id cap 32762.
```java
registerQuest("killmobstitle", KillMobsTitleQuest.class);
```
is the **only** core registration. API: `registerQuest(String, Class<? extends Quest>)`,
`getNewQuest(int|String id)`, `getQuestID(String|Class)`. A mod calls `registerQuest` the
same way to add its own `Quest` subclasses (each needs a public no-arg constructor per
`EmptyConstructorGameRegistry`).

### 1.3 Where quests actually run today

`gfx/forms/presets/debug/DebugToolsList.java:329-336` is the only place a `Quest` is ever
created and added in the decompiled sources (an F1 debug-menu "add test quest" tool):
`new KillMobsTitleQuest(new StaticMessage("TEST QUEST"), new KillMobsQuest.KillObjective("zombie", 5), …)`
added via `server.world.getQuests().addQuest(quest, true)`. This confirms the pipeline
(`QuestManager` → `PacketQuest` → tracked sidebar) works end-to-end; it's just unused by
shipped content.

`QuestManager` (`engine/quest/QuestManager.java`) is the container: one instance lives on
`Server` (`world.getQuests()`) and one on `Client`. `addQuest(Quest, boolean isNew)` replaces
any quest with the same unique ID, calls `quest.init(this)`, and (server-side) broadcasts
`PacketQuest` to clients for whom `quest.isActiveFor(auth)`; (client-side, if `isNew` and
`Settings.trackNewQuests`) adds it to `client.trackedQuests` and refreshes `TrackedSidebarForm`.

### 1.4 `QuestGiver` interface — the mob-side wiring point (unused, but complete)

`entity/mobs/QuestGiver.java` declares exactly two methods a mob must implement:
`getQuestGiverObject()` (returning a `QuestGiver.QuestGiverObject`) and
`getGivenQuests(ServerClient client)` (returning `List<Quest>` — whatever quest(s) this mob
currently offers that client). A mob implementing this needs only those two methods plus a `QuestGiverObject` field
(`new QuestGiver.QuestGiverObject(this, shouldSaveQuests)`, which throws if `this` isn't a
`QuestGiver`). Everything else is generic and networked automatically:

- **Client → server**: on mob `init()`, send `PacketQuestGiverRequest(mobUniqueID)`. Server
  handler (`engine/network/packet/PacketQuestGiverRequest.java`) looks the mob up by unique
  ID and, if it's a `QuestGiver`, replies with `PacketQuestGiverUpdate`.
- **Server tick**: `QuestGiverObject.serverTick()` (called once per interval from the mob's
  own `serverTick()`) re-evaluates `getGivenQuests(client)` per active player-auth, diffs
  against last-sent state, and pushes `PacketQuestGiverUpdate` on change.
- **Client tick**: `QuestGiverObject.clientTick()` refreshes each `GivenQuest` from the local
  `QuestManager` cache.
- **Markers**: `QuestGiverObject.getMarkerOptions(PlayerMob perspective)` returns a `!`/`?`
  icon (`QuestMarkerOptions`) — new-quest vs has-quest vs can-complete (color-coded).
- **Save**: `QuestGiverObject.addSaveData`/`applyLoadData`, gated by the `shouldSaveQuests`
  constructor flag, persist a per-player-auth list of given-quest unique IDs and re-resolve
  them against `mob.getLevel().getServer().world.getQuests()` on load.

Important: this only tracks **which quest instances are currently offered**, not "has this
player ever completed quest type X". A multi-stage chain still needs the mod's own
stage/progress tracking (see §9) — the vanilla system supplies transport, save, sharing and
UI chrome around whatever quest object your own logic decides to hand out next.

### 1.5 UI wiring for quests inside a shop container

`inventory/container/mob/ContainerQuest.java` is a small data wrapper — `introMessage`,
`Quest quest`, `canSkip`, `skipError` — with its own packet (de)serialization that resolves
the `Quest` via `QuestRegistry.getNewQuest(questID)` then `quest.applySpawnPacket(reader)`.
`HumanShop.getShopContainerData` passes `ArrayList<ContainerQuest> quests` (from
`getQuests(client)`) into `ShopContainer.getContainerContent(...)`, rendered client-side by
`gfx/forms/presets/containerComponent/mob/ShopQuestsForm.java`. There's also a standalone
`QUESTS_CONTAINER` (`ContainerRegistry`) backed by `QuestsContainerForm.java` for a
general quest-log view, and `FormQuestComponent`/`FormQuestTrackedComponent`
(`gfx/forms/components/`) render individual quest entries. This machinery is only worth
adopting if you specifically want your quest to appear inside the standard shop-dialogue
/ quest-log UI (see §9).

---

## 2. Journal / hint systems — three separate registries, not one

| Registry | File | What it actually is |
|---|---|---|
| `JournalRegistry` | `engine/registries/JournalRegistry.java` | Per-biome/level "bestiary" entries: `new JournalEntry(biome, levelIdentifier)`, then `.addBiomeLootEntry(...)`, `.addMobEntries(...)`, `.addTreasureEntry(...)`. **Not** a hint or quest system — it's the discoverable compendium page. |
| `JournalChallengeRegistry` | `engine/registries/JournalChallengeRegistry.java` | Discrete "mini-challenge" hints with completion tracking + a `LootTable` reward, each attached to exactly one `JournalEntry`. **This is the real analog for "find the Sky Warden".** |
| `StoryObjective` / `StoryObjectiveManager` / `StoryObjectiveRegistry` | `engine/storyObjectives/` | The main campaign progression track (the `!`/`?` marker sequence over the Elder's head: mine ore, craft tools, defeat bosses…). Heavier-weight, tied to core progression UI. |

**Correction on the brief's example**: "craft a cave ladder" is a `StoryObjective`
(`engine/storyObjectives/objectives/GoMiningStoryObjective.java:186`, message key
`"objectives"/"craftanduseladder"`), *not* a `JournalChallengeRegistry` entry. The
`JournalChallengeRegistry` equivalent pattern for "obtain/defeat X" hints looks like:
```java
FIND_WET_ICICLE_ID = registerChallenge("findweticicle", new ItemObtainedJournalChallenge("weticicle"));
DEFEAT_CAVELING_ID = registerChallenge("defeatcaveling", new DefeatMobJournalChallenge("stonecaveling"));
```
(`JournalChallengeRegistry.java:209,246`). Other ready-made `JournalChallenge` subclasses:
`CraftItemJournalChallenge`, `ObjectPlacedJournalChallenge`, `ObjectsDestroyedJournalChallenge`,
`PickupItemsJournalChallenge`, `LevelVisitedJournalChallenge`, `SimpleJournalChallenge`
(manual complete/claim, no auto-trigger).

`JournalChallenge` (abstract, `engine/journal/JournalChallenge.java`) contract: `getName()`,
`isCompleted`/`markCompleted(ServerClient)`, `isClaimed`/`markClaimed(ServerClient)`,
`getReward()` (a `LootTable`), `addJournalFormContent(Client, FormContentBox, FormFlow)` for
custom journal-page rendering. `setAttachedJournal(JournalEntry)` **throws if called twice**
— a challenge belongs to exactly one journal entry.

**Can a mod add "find the Sky Warden"?** Yes, via `JournalChallengeRegistry`: register a
`DefeatMobJournalChallenge("skywarden")` (or a custom `JournalChallenge` subclass) and attach
it to an existing `JournalEntry` (`JournalRegistry.getJournalEntry(...)`) or to a new one the
mod registers itself with `JournalRegistry.registerJournalEntry`. This is self-contained and
doesn't touch the main `StoryObjectiveManager` progression track.

---

## 3. Friendly NPC mob — base classes and fields

### 3.1 Hierarchy

```
Mob (entity/mobs/Mob.java, extends Entity implements Attacker)
 └─ AttackAnimMob (entity/mobs/AttackAnimMob.java)
     └─ FriendlyMob (entity/mobs/friendly/FriendlyMob.java) — sets isHostile = false
         └─ CritterMob (entity/mobs/friendly/critters/CritterMob.java)
             └─ PeacefulCritterMob — manageHuntJob() is a no-op (never turns aggressive)
```
`FriendlyMob(int health)` is the whole class — 5 lines, just flips `isHostile`. This, not
`HumanMob`/`HumanShop`, is the right base for a non-human quest-giving critter.

`HumanMob` (`entity/mobs/friendly/human/HumanMob.java:206`) is a different, much heavier
branch: it extends `ItemAttackerMob` and implements eleven separate mob-role interfaces
(`HumanSettlerMob`, `CommandMob`, `EntityJobWorker`, `FishingMob`, `HungerMob`,
`ObjectUserMob`, `ActivityDescriptionMob`, `MobInventory`, `ClientInteractMob`,
`AmmoUserMob`, `RopeClearerMob`, `JobWorkerChatter`). `HumanShop extends
HumanMob` adds the whole shop/dialogue/recruit machinery. `SettlerMob`
(`level/maps/levelData/settlementData/settler/SettlerMob.java`) is an **interface**, not a
base class — a large settlement-bed contract (`assignBed`, `moveOut`, `settlementTick`,
`makeSettler`, `onRoomStatsCalculated`, …). None of this is needed for a standalone
quest-giving critter, and adopting it pulls in settlement/room/bed/happiness systems that
have nothing to do with the goal.

`CritterMob` is tuned for *wild wandering wildlife*, not a stationary unique NPC:
`this.canDespawn = true` in its constructor, and it overrides `shouldSave()` to
`return this.shouldSave && !this.canDespawn();` — i.e. despawnable critters are also not
saved (they're expected to just re-spawn ambiently). A quest NPC should **not** inherit this;
either subclass `FriendlyMob` directly, or subclass `CritterMob`/`PeacefulCritterMob` and
reset `canDespawn = false` in the constructor.

### 3.2 Fields/hooks that answer the brief's specific asks

All in `entity/mobs/Mob.java` unless noted:

| Need | Mechanism |
|---|---|
| Save persistence | `public boolean shouldSave = true;` (line 266, default already on) + `boolean shouldSave()` getter (line 4028) — override only if you need conditional logic |
| Not a "summon" | `public boolean isSummoned = false;` (line 267) — leave false; vanilla only sets this true on ephemeral combat summons (bosses' adds, minions), affects loot/retreat/respawn logic, not relevant here |
| Doesn't despawn | `public boolean canDespawn;` (line 264) — **defaults to `false`** for a plain `Mob`; only `CritterMob` and similar opt in. Leave untouched. |
| Immortal | Override `canTakeDamage()` → `return false;` (simplest — the base `AttackAnimMob`/`FriendlyMob` don't block this, only `isStatic` mobs do it by default). Alternative idiom used by `TrainingDummyMob` (`entity/mobs/TrainingDummyMob.java`): keep `canTakeDamage()==true` so hits still register visually/sound-wise, but force-reset health every tick via `setHealthHidden(this.getMaxHealth())` — lets it react to hits without ever dying. |
| Not a valid raid/hostile target | Nothing extra needed beyond `FriendlyMob`'s `isHostile = false`: `canBeTargetedByHumans(HumanMob)` defaults to `return this.isHostile;` (line 1857) and `canBeTargeted(Mob attacker, NetworkClient)` (line 2110) additionally excludes same-team and several other cases. Raider AI (`HumanRaiderMob`, `PirateRaiderMob`, etc.) targets via these same hooks, so a non-hostile mob is already excluded by default. |
| Can't be shoved around | `canBePushed(Mob)` / `canPushMob(Mob)` → `return false;` (both used by `TrainingDummyMob`) |
| Stands still / doesn't wander | Simplest robust option: `setSpeed(0.0F)` and don't assign any `BehaviourTreeAI` in `init()` at all — `TrainingDummyMob` does exactly this (no AI field ever set) and is a fully valid, supported mob configuration. `HumanMob.setHome(Point)`/`getHomeX()`/`getHomeY()` (line 2056) exists for a "leash to a point while still walking around" behavior, but it's **HumanMob-specific** — a plain `Mob` subclass doesn't get it for free and would need its own small `BehaviourTreeAI` leaf comparing position to a stored home tile if roaming-with-a-leash is wanted instead of fully stationary. |

### 3.3 Registration

`engine/registries/MobRegistry.java` has several `registerMob` overloads, all taking
`(String stringID, Class<? extends Mob> mobClass, boolean countKillStat, ...)`; the extra
trailing parameters (in various overloads) add `isBossMob`, `createSpawnItem`, or a
`GameMessage killHint`. The minimal overload for a plain friendly NPC is just
`registerMob(stringID, mobClass, countKillStat)`. Runtime lookup:
`MobRegistry.getMob(String stringID, Level level)` (used throughout preset code, e.g.
`MobRegistry.getMob("elderhuman", level)`).

### 3.4 `cavelingelder.png` — sprite sheet layout (visually confirmed)

The file is 384×320 px = 6 columns × 5 rows of 64 px cells. The base animation-frame
selector is `Mob.getAnimSprite(int x, int y, int dir)` (`Mob.java:3931`), which returns a
`Point(col, dir)` where:
- **row = `dir`**, and `dir` only ever ranges **0–3** (`this.dir = GameRandom.globalRandom.nextInt(4)` at `Mob.java:337`; `getDirVector()` maps 0→up, 1→right, 2→down/inferred, 3→left).
- **col = 0** when stationary, **col = 1..4** cycling through the walk animation
  (`(distanceRan / rockSpeed) % 4 + 1`), **col = 5** when `inLiquid` (the swim pose).

Visually, the sheet's rows 0–3 each show: column 0 = the caveling "disguised as a rock/boulder"
idle pose (matching Necesse's caveling gimmick of hiding as ore/stone until approached),
columns 1–4 = the bearded elder-hatted character walking in that row's direction, column 5 =
a smaller top-down "swimming" pose. **Row 4 (the 5th row) contains only a single extra
boulder/moss sprite in column 0 and is otherwise empty** — it is never read by either
consumer of this texture (`ElderHumanMob.addDrawables`'s April Fools branch, or
`PetCavelingElder.addDrawables`), since both rely on the stock `getAnimSprite`, which never
returns a row outside 0–3. Treat it as inert bonus art / template padding, not a functional
frame. **Takeaway for drawing a new simple (non-`HumanTexture`) mob sprite**: 6 columns × 4
rows (384×256) is the complete, actually-used template; a 5th row is not required.

---

## 4. Dialogue / interact UI

### 4.1 The container pipeline (this is how every shop/trader NPC opens its UI)

1. Player interacts → engine calls `Mob.interact(PlayerMob player)` (`Mob.java:2811`) on the
   server. Override this; the base implementation is a no-op hook. (`HumanShop.interact`,
   `FriendlyRopableMob.interact`, boss-portal mobs, etc. all just override this directly —
   no special packet class needs to be written by the mob author.)
2. `HumanShop.interact` (`entity/mobs/friendly/human/humanShop/HumanShop.java:162-178`)
   pattern: compute an error message (`getInteractError`); if none, build
   `PacketOpenContainer openShopPacket = this.getOpenShopPacket(server, client)` and call
   `ContainerRegistry.openAndSendContainer(client, openShopPacket)`.
3. `getOpenShopPacket` → `this.getShopContainerData(client).getPacket(ContainerRegistry.SHOP_CONTAINER, this)`
   — i.e. "build my container's content payload, tag it with a registered container-type ID
   and my own mob unique-ID."
4. `ContainerRegistry.openAndSendContainer(ServerClient, PacketOpenContainer)`
   (`engine/registries/ContainerRegistry.java:831`) both sends the packet to the client *and*
   calls `openContainer(...)` server-side immediately, so server and client open in lock-step.
5. Client receives `PacketOpenContainer` → `ContainerRegistry.openContainer(containerID, …)`
   looks up the registered `ContainerRegistryElement` and calls its `clientHandler`, which
   constructs a client-side `Container` + wraps it in a `ContainerForm<T>`.

### 4.2 Registering a new mob-bound container type

`ContainerRegistry.registerMobContainer(ClientExtraContainerHandler<Mob>, ServerExtraContainerHandler<Mob>)`
(`ContainerRegistry.java:773`) is a thin wrapper over the generic `registerContainer` that
automatically resolves the target `Mob` by unique ID (`level.entityManager.mobs.get(mobUniqueID, false)`)
on both ends — this is exactly how `SHOP_CONTAINER`, `ELDER_CONTAINER`, `MAGE_CONTAINER`,
`STYLIST_CONTAINER` etc. are all registered (`ContainerRegistry.java:411-439`): each passes a
client-side lambda that builds a `new XyzContainerForm<>(client, new XyzContainer(...))` and a
server-side lambda that builds the matching `new XyzContainer(...)` from the resolved `Mob`
and the packet content. A mod does the same with its own `MyQuestContainer`/`MyQuestContainerForm` pair — no need to
touch `ShopContainer`/`HumanShop` at all. Related sibling helpers: `registerOEContainer`
(keyed to an `ObjectEntity`), `registerLOContainer` (a `LevelObject`), `registerLevelContainer`
(no target, just the level) — pick whichever matches what your UI is "about."

### 4.3 Base `Container` — client/server event system for buttons

`inventory/container/Container.java` (1138 lines) provides a generic typed pub/sub between
the paired client and server `Container` instances:
```java
<T extends ContainerEvent> ContainerEventHandler<T> onEvent(Class<T> eventClass, Consumer<T> handler)
```
plus `transferToSlots`/`transferFromSlots` helpers for real inventory-slot UIs. For a simple
dialogue box you don't need slots at all — just define a small `ContainerEvent` subclass
(e.g. `RequestTurnInEvent`) sent client → server when a "Turn In" button is clicked, register
an `onEvent` handler server-side that re-validates and advances state, and (if needed) an
`onEvent` handler client-side for the server's acknowledgement/refresh event. This is the
same mechanism vanilla uses for e.g. `ShopContainerQuestUpdateEvent`
(`inventory/container/events/ShopContainerQuestUpdateEvent.java`) and the various
`inventory/container/settlement/actions/Request*Action.java` classes.

### 4.4 "FormContainer" — actual class is `ContainerForm<T>`

`gfx/forms/presets/containerComponent/ContainerForm.java`:
```java
public class ContainerForm<T extends Container> extends Form implements ContainerComponent<T>
```
Mods subclass this as `MyContainerForm<T extends MyContainer> extends ContainerForm<T>` and
lay out ordinary `Form*` components inside (`FormButton`, `FormLabel`, `FormContentBox`,
`FormFairTypeLabel`, etc. — the same building blocks `JournalChallenge.addJournalFormContent`
and `SimpleJournalChallenge` use). This is the simplest robust path to a custom "portrait +
text + turn-in button" dialogue window; it reuses the exact opening pipeline in §4.1.

### 4.5 Simple chat-bubble alternative

If you don't need any button/UI at all — just a line of flavor text — mobs can talk without
opening a container: see `HumanMobChatterHandler`/`getMessages`/`getRandomMessage` on
`HumanMob`/`HumanShop` (float-text style speech), and `ElderHumanMob.getMessages` for the
localization-key pattern (`getLocalMessages("eldertalk", 6)`). This alone is not sufficient
for a turn-in flow (no button), only for ambient flavor lines. There is no separate
"SettlerDialogueRegistry" class in the decompiled sources — settler dialogue lives in
`level/maps/levelData/settlementData/settler/dialogues/SettlerDialogue` and is assembled per
personality (`SettlerPersonality.getDialogues`), which is settlement-specific machinery, not
a generic registry a standalone mob would hook into.

---

## 5. Item turn-in mechanism

The vanilla precedent is `DeliverItemsQuest` (§1.1), and it is refreshingly simple — **no
special slots, no depositing items into a container at all**:

- `canComplete(NetworkClient client)`: for every objective, checks
  `client.playerMob.getInv().main.getAmount(level, playerMob, objective.item, "deliverquest")`
  is `>=` the required amount — i.e. it just counts what's already in the player's own
  inventory.
- `complete(ServerClient client)`: calls
  `client.playerMob.getInv().main.removeItems(level, playerMob, objective.item, objective.itemsAmount, "deliverquest")`
  for each objective — removes the items directly from the player's inventory server-side.

Both calls take a free-text "purpose" string (`"deliverquest"`) — this is just a logging/UI
tag, not a registry key. **This exact two-call idiom (`getAmount` check, then `removeItems`
on turn-in-button-click) is the recommended pattern to copy** for a custom turn-in — it's
proven, requires no client-side item drag/drop UI, and is trivially server-authoritative (the
server re-checks the inventory itself when the button event arrives; it never trusts a
client-reported "I have the items" claim).

Other candidates checked and ruled out as *not* being this mechanic:
- `SettlementRequestInventory`/`SettlementRequestOptions`
  (`level/maps/levelData/settlementData/`) — this is the automated settler
  hauler/logistics system (a storage container that generates `HaulFromLevelJob`s to keep
  itself stocked), not a player-facing "give an NPC items" interaction.
- Incursion "gateway tablets" are crafting-material items consumed at a portal object, not a
  quest-style NPC turn-in — not investigated further as it's a different subsystem (object
  crafting/activation, not `Quest`/`Container` dialogue).

---

## 6. Spawning a persistent unique mob at a fixed tile

### 6.1 The core spawn call

`entity/manager/EntityManager.java:173`, signature `public void addMob(Mob mob, float x, float y)`
— internally it just positions the mob (`mob.setPos(x, y, true)`) and adds it to the level's
mob list. Called as `level.entityManager.addMob(mob, worldX, worldY)`. This is the one API
every placement path funnels through.

### 6.2 Concrete vanilla example: the Elder's own placement

`level/maps/presets/ElderHousePreset.java` places the Elder during world generation (when
this preset structure is rolled), and it's a complete worked example of the flow: a preset
callback (`addCustomApply`, at the tile coordinate of the Elder's desk) first resolves the mob
via `MobRegistry.getMob("elderhuman", level)`, then delegates to a static
`createAndAddElder(HumanMob mob, Level level, int tileX, int tileY)` helper which, in order:
calls `mob.setHome(tileX, tileY)`; nudges the spawn point off any solid tile via
`Waystone.findTeleportLocation(level, tileX, tileY, mob)`; calls
`level.entityManager.addMob(mob, x, y)` at that resolved point; and then, server-side only,
registers a settlement bed for the Elder via `ServerSettlementData`/`SettlementBed` —
purely a settlement-specific step, not part of the general spawn recipe. The preset callback
also returns a cleanup lambda that calls `mob.remove()` if the preset is ever undone.

For a non-settler NPC, skip the settlement-bed step entirely and keep just:
`MobRegistry.getMob("yourmobid", level)` (or `new YourMob()`), optionally
`Waystone.findTeleportLocation` to avoid solid tiles, then
`level.entityManager.addMob(mob, x, y)`.

### 6.3 "Exactly once", including on saves that predate the mod

A world-gen preset only fires for **newly generated** worlds/structures — it won't retroactively
place your NPC into a save that already exists when the mod is added. The robust,
save-portable pattern is a custom persistent flag checked on server world/level load:

- `engine/registries/WorldDataRegistry.java` + `engine/world/worldData/WorldData.java` —
  register a `WorldData` subclass (`registerWorldData(stringID, MyWorldData.class)`, needs a
  public no-arg constructor) that stores e.g. a `boolean spawned` and the mob's unique ID.
  Retrieve it via `WorldEntity.getWorldData(String key)` (mirror the accessor pattern
  `SettlementsWorldData.getSettlementsData(WorldEntity)` uses). This is genuinely how
  `SettlementsWorldData`/`SettlersWorldData` are wired (the only two core registrations).
- `engine/registries/LevelDataRegistry.java` + `level/maps/levelData/LevelData` is the
  per-`Level` equivalent, more appropriate if the NPC belongs to one specific level (e.g. the
  starting island) rather than being a world-global singleton.
- On server level load, check the flag; if unset, spawn via §6.1/6.2, store the mob's
  `getUniqueID()` and flip the flag, so a restart or an old save picking up the mod spawns it
  exactly once, and subsequent loads just re-find the already-saved `Mob` instance the normal
  way (mobs `shouldSave()` by default — see §3.2 — so it round-trips through the level save
  automatically; you don't need to re-add it yourself after the first time).

### 6.4 Unkillable vs. respawning

Two valid vanilla-precedented options (see §3.2 for the immortal idioms):
- **Invulnerable** (`canTakeDamage()` → `false`, or the `TrainingDummyMob` health-reset
  trick) — simplest, and means "respawn if killed" is moot because it can never die.
- **Respawning boss-style**: `isSummoned` + `getRespawnTime()` overrides
  (`BossMob.getBossRespawnTime(Mob)`, `entity/mobs/hostile/bosses/BossMob.java:13`) are the
  vanilla idiom for "this mob comes back after a delay if defeated," but that whole system is
  built for hostile bosses (arena/portal respawn points) and would need adapting; for a
  *friendly* NPC, invulnerability is simpler and avoids "the quest-giver is temporarily
  missing" edge cases entirely. Recommended: go invulnerable.

---

## 7. Pets / companions / ambient critters

`entity/mobs/summon/summonFollowingMob/petFollowingMob/` (`PetParrotMob`, `PetPugMob`,
`PetDragonWhelpMob`, `PetCavelingElder`, `PetGrizzlyBearCub`, `PetPenguinMob`,
`PetThrumboMob`, `PetIcicleArmadilloMob`, `PetWalkingTorchMob`, `WillOWispMob`,
`GhostlyBowFollowingMob`, `PetEvilMinion`) all extend `PetFollowingMob` →
`SummonedFollowingMob` → `SummonedMob`. Important: **these are not persistent adopted
creatures** — `SummonedFollowingMob`'s constructor sets `shouldSave = false`, `isStatic = true`,
and its `serverTick()` removes itself if `!isFollowing()` or if a bound
`removeWhenNotInInventoryItem` is no longer carried by the owner. In other words, vanilla
"pets" are ephemeral companions summoned by an equipped/held item (like a minion), tied to
the owning player, using `PlayerFollowerAINode` (`entity/mobs/ai/behaviourTree/leaves/`) for
the following behavior, and they vanish on disconnect/region-unload/item-removal. This is
**not** a good model for a persistent world-fixed quest NPC.

For "an ambient critter that stays near a point," the closer fits are (in order of
simplicity):
1. A plain stationary `FriendlyMob` (speed 0, no AI) — see §3.2 — if it truly never needs to
   move.
2. `CritterMob`/`PeacefulCritterMob` with `canDespawn` reset to `false` in the constructor, if
   some ambient wandering is wanted; `CritterAI` (`entity/mobs/ai/behaviourTree/trees/CritterAI.java`)
   is its default behavior tree.

---

## 8. Multiplayer sync & persistence toolkit (for whichever design you pick)

| Concern | Mechanism |
|---|---|
| Live field sync (server↔client, while both loaded) | `Mob.registerNetworkField(new IntNetworkField(start))` (and `BooleanNetworkField`/`ByteNetworkField`/`FloatNetworkField`/`LongNetworkField`/`ShortNetworkField`/`StringNetworkField`/`UnsignedByteNetworkField`/`UnsignedShortNetworkField` in `entity/mobs/networkField/`). Call `.set(value)` to change it — dirty-tracking and delta packets are automatic via the mob's internal `MobNetworkFieldRegistry`. Used pervasively, e.g. `HumanMob.team = registerNetworkField(new IntNetworkField(-10) {...})`. **Does not persist to save by itself** — it's sync only. |
| Full state at spawn | Override `setupSpawnPacket(PacketWriter)`/`applySpawnPacket(PacketReader)` (base `Mob` calls through to `networkFields.writeSpawnPacket`/`readSpawnPacket` automatically, plus whatever you add — see `TrainingDummyMob`'s `isSnowman` boolean for a manual example) |
| Save persistence | Override `addSaveData(SaveData)`/`applyLoadData(LoadData)` — always call `super` first. This is separate from network sync; write your stage/progress fields here explicitly. |
| Per-player (not global) progress | Key a `HashMap<Long, …>` by `ServerClient.authentication`, exactly like `QuestGiver.QuestGiverObject.serverQuests` and `KillMobsQuest.KillObjective.prevClientKills` do. |

---

## 9. Recommendation: 4-stage fetch-quest chain, multiplayer-safe, save-persistent

**Recommendation: (b) hand-rolled state machine on the Mob, with a custom `Container`/`ContainerForm` UI.** Concrete classes to use:

- Base: your own `class MyQuestNpcMob extends FriendlyMob` (not `HumanMob`/`HumanShop`/`SettlerMob` — see §3.1).
- Registration: `MobRegistry.registerMob("myquestnpc", MyQuestNpcMob.class, false)`.
- Stage state: one `IntNetworkField questStage = registerNetworkField(new IntNetworkField(0))` on the mob (global chain) — or, if progress must be per-player, a `HashMap<Long,Integer>` plus your own small sync packet/event, mirroring `QuestGiverObject.serverQuests`/`KillMobsQuest.KillObjective.prevClientKills` (§8).
- Persistence: override `addSaveData`/`applyLoadData` to write/read `questStage.get()`/`.set()`. Placement persistence via §6.3's `WorldData`/`LevelData` "spawned-once" flag.
- Immortality: `canTakeDamage()` → `false` (§3.2/§6.4).
- Interact/UI: override `interact(PlayerMob)`, open via `ContainerRegistry.registerMobContainer` + your own `MyQuestContainer extends Container` / `MyQuestContainerForm<T> extends ContainerForm<T>`, following the exact `HumanShop`/`SHOP_CONTAINER` pipeline in §4.1–4.4. A "Turn In" button fires a custom `ContainerEvent` (§4.3); the server handler re-checks and removes items using the `DeliverItemsQuest` idiom (§5: `getInv().main.getAmount(...)` then `.removeItems(...)`), then increments `questStage` and advances to the next stage's required items/dialogue text (all just data in your own class — no need for 4 separate `Quest`/`Container` classes, one mob + one container handles all 4 stages by branching on `questStage`).

**Why not (a), the vanilla `QuestRegistry`/`QuestGiver` route:** it is real and would work,
but buys less than it appears to for this specific shape of task:
- It ships **dormant** (§0) — there is no working vanilla example to model the *mob* side on
  (only a debug-menu example that never touches `QuestGiver`), so implementing `QuestGiver`
  correctly is higher-risk, first-of-its-kind integration work in this codebase.
- It does **not** natively model a "chain" — `getGivenQuests(ServerClient)` just returns
  "what's on offer right now"; deciding *which* of your 4 stages is next is still 100% your
  own logic and your own persisted state (§1.4), so you don't actually avoid writing a stage
  tracker — you'd be writing one *underneath* the Quest system as well as inside it.
  Overriding `HumanShop.getQuests()`/`ContainerQuest` (piggy-backing on the shop dialogue) is
  the only way to get the vanilla quest UI on-screen, and pulling in `HumanShop` re-introduces
  all the `HumanMob` weight §3.1 says to avoid for a non-human critter.
- It requires registering each stage as its own `Quest` subclass in `QuestRegistry` (a global,
  save-versioned ID registry you now own compatibility for across mod updates), for a benefit
  (tracked-quest sidebar entry, `!`/`?` head markers, party quest-sharing) that's cosmetic
  polish rather than something your fetch chain strictly needs.
- `DeliverItemsQuest`'s item-checking logic is trivially reusable **without** adopting the
  rest of the Quest engine (§5) — you get the one part that's actually valuable (the
  proven inventory-check-then-consume idiom) for free either way.

Use route (a) instead only if: you specifically want your NPC's quest to show up in the
standard tracked-quest sidebar / head markers / be nominally shareable with party members via
the existing UI chrome, and are fine with either accepting `HumanShop`'s weight or building a
new `QuestGiver` integration mostly from scratch (no working precedent to copy from).

**ObjectEntity alternative (also asked about):** appropriate only if the "NPC" is really
meant to be an inanimate turn-in fixture (a shrine/altar/mailbox) rather than a character —
`entity/objectEntity/ObjectEntity` has the same save/network/content-packet shape as `Mob`
(`addSaveData`/`applyLoadData`, `setupContentPacket`/`applyContentPacket`,
`runNetworkFieldUpdate`, `runEvent`, `implementsOEUsers()`) and would work equally well for
the state-machine + `Container` parts of this design. For a *friendly critter* NPC
specifically (the brief's ask), `Mob` is the right base — it gets you facing/animation,
`interact` from adjacent tiles, and correct exclusion from raid/hostile targeting (§3.2) for
free, none of which `ObjectEntity` provides.
