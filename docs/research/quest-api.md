# Necesse 1.3.2 — Vanilla Quest API: HUD-Tracked Quests From a Mod

Source: decompiled sources at `/home/user/necesse-game/decompiled/necesse/` (paths below
relative to that root). Companion doc: `quest-npc-system.md` (NPC/mob side, dialogue
containers, spawn-once patterns). This doc covers the quest engine itself: definition,
registration, giving, objective tracking, HUD/journal display, persistence, multiplayer
sync — and how to layer it onto the existing Warden chain.

---

## Verdict

**A mod can register HUD-tracked quests: YES, fully.** `QuestRegistry` is an open
`registerCore`-pattern registry (`QuestRegistry.registerQuest(stringID, Class)`), open
during `ModEntry.init()` exactly like `MobRegistry`/`ItemRegistry`. Once a `Quest`
instance is added to the world's `QuestManager` and made active for a player, everything
else is automatic vanilla machinery: sync packets, the tracked-quest sidebar on the HUD
(new quests auto-track by default), the quest-log window on the quickbar, live progress
updates, team sharing, abandon, and world-save persistence. No container, no `HumanShop`,
no `QuestGiver` implementation is required — a plain `FriendlyMob.interact()` can give and
complete quests directly (this is exactly what the game's own F1 debug tool does).

Two caveats:
1. There is **no vanilla "reach location" or generic checklist objective** — the shipped
   bases are deliver-items and kill-mobs. Anything else (find-the-spire, bring-cats-home)
   is a small custom `Quest` subclass (~60 lines each; the base class does the heavy
   lifting).
2. Rewards are **not** part of the engine. `Quest` displays reward text
   (`getRewardType`), but granting items on completion is the giver's job — which the
   Warden's `interact()` already does.

The separate `StoryObjective` system (`engine/storyObjectives/`) is also mod-open but is a
**single global campaign line** — one "current objective" per player, sorted across all
registered objectives, with claim UI tied to settlements/the Elder. Inserting mod stages
there would displace vanilla progression display. Use the `Quest` system for a
self-contained chain; consider `StoryObjective` only for a single "reach the sky" pointer
appended after the vanilla line (see §8).

---

## 1. The three "goal" systems — pick the right one

| System | Package | Scope | HUD | Mod-open? |
|---|---|---|---|---|
| **Quests** | `engine/quest/` + `engine/registries/QuestRegistry` | Per-player instances, arbitrary count, given/removed at runtime | Tracked sidebar + quest-log window, automatic | Yes — `registerQuest` |
| Story objectives | `engine/storyObjectives/` + `StoryObjectiveRegistry` | One global ordered line, every player has every objective | Same sidebar (own section) + on-level arrows/text | Yes — `registerObjective`, but single-line semantics |
| Journal challenges | `engine/journal/` + `JournalChallengeRegistry` | Per-biome journal page hints with claimable reward | Journal UI only (no sidebar tracking) | Yes |

The player-facing ask — "story goals shown in the HUD like vanilla quests" — is the first
row.

---

## 2. Core classes and lifecycle

### 2.1 `Quest` (`engine/quest/Quest.java`) — abstract base

```java
public abstract class Quest implements IDDataContainer {
    public Quest()                                  // self-registers type ID via QuestRegistry.instance.applyIDData(getClass(), idData)
    public int getUniqueID()                        // per-INSTANCE id, lazily GameRandom.globalRandom.nextInt()
    public void makeActiveFor(Server, ServerClient) // adds client auth, client.addQuest(this,true), sends PacketQuest
    public void abandonFor(Server, ServerClient)    // reverse + PacketQuestRemove
    public boolean isActiveFor(long clientAuth)

    public abstract void tick(ServerClient var1);              // server, every tick, per active client
    public abstract boolean canComplete(NetworkClient var1);   // runs on BOTH sides (UI + server check)
    public void complete(ServerClient client)                  // base: stats increment; override to consume items etc.

    public abstract GameMessage getTitle();
    public abstract GameMessage getDescription();              // may return null
    public abstract DrawOptionsBox getProgressDrawBox(NetworkClient, int x, int y, int width, Color textColor, boolean outlined);
    public abstract FairType getRewardType(NetworkClient, boolean outlined);   // display-only; null OK
    public abstract FairType getHandInType(NetworkClient, boolean outlined);   // "return to X" line; null OK

    // sync: full state once vs delta on change
    public void setupSpawnPacket(PacketWriter) / applySpawnPacket(PacketReader)  // writes uniqueID + setupPacket()
    public void setupPacket(PacketWriter) / applyPacket(PacketReader)            // delta payload (empty by default)
    public void markDirty()                          // -> WorldEntity tick auto-sends PacketQuestUpdate to active clients

    // persistence
    public void addSaveData(SaveData) / applyLoadData(LoadData)   // base saves uniqueID + active-client auth set

    // sharing (team)
    public boolean canShare()                        // default true
    public boolean canShareWith(ServerClient me, ServerClient him)  // default same team
    public void onShared(Server, ServerClient from, ServerClient to)

    // optional world hooks while quest is active for a client
    public MobSpawnTable getExtraCritterSpawnTable(ServerClient, Level)
    public MobSpawnTable getExtraMobSpawnTable(ServerClient, Level)
    public FishingLootTable getExtraFishingLoot(ServerClient, FishingSpot)
    public LootTable getExtraMobDrops(ServerClient, Mob)         // e.g. quest-only drops
}
```

Note the two ID spaces: the **registry type ID** (`getID()`, per class, sent as
`putNextShortUnsigned` in packets, saved as stringID) and the **unique instance ID**
(`getUniqueID()`, random int, identifies a spawned quest across save/network).

### 2.2 `QuestManager` (`engine/quest/QuestManager.java`)

One instance server-side on the world, one client-side:

- Server: `server.world.getQuests()` (field `WorldEntity.quests`, created as
  `new QuestManager(server)`).
- Client: `Client.quests` (`engine/network/client/Client.java:123`), plus
  `Client.trackedQuests` (`HashSet<Integer>` of unique IDs, line 124).

```java
public void addQuest(Quest quest, boolean isNew)
    // server: sends PacketQuest to every client for whom quest.isActiveFor(auth)
    // client: if isNew && Settings.trackNewQuests (DEFAULT TRUE, Settings.java:87)
    //         -> client.trackedQuests.add(uniqueID); TrackedSidebarForm.updateTrackedList()
public Quest getQuest(int uniqueID)
public boolean removeQuest(int uniqueID)   // server: PacketQuestRemove to ALL clients
public boolean removeQuest(Quest quest)    // server: PacketQuestRemove to active clients
public Iterable<Quest> getQuests()
```

### 2.3 Per-player state on the server (`engine/network/server/ServerClient.java`)

```java
private final HashMap<Quest, Boolean> quests;        // value = tracked flag (line 182)
public void addQuest(Quest quest, boolean isNew)     // line 1777; tracked = isNew && client.trackNewQuests
public void removeQuest(Quest quest)
public Quest getQuest(int questUniqueID)
public boolean hasQuest(Quest quest)
public boolean setTrackedQuest(int questUniqueID, boolean tracked)
public HashMap<Quest, Boolean> getQuests()
```

Server tick (line ~795): iterates the map, drops entries whose quest `isRemoved()`,
otherwise calls `quest.tick(this)` — so `tick` runs once per tick **per active client**.

---

## 3. Shipped quest base classes (the "objective types")

| Class | Objective | State sync | Notes |
|---|---|---|---|
| `DeliverItemsQuest` (abstract) | N × `ItemObjective(itemStringID, amount)` | none needed — `canComplete` counts the player's own inventory live on both sides | `canComplete`: `getInv().main.getAmount(level, player, item, "deliverquest") >= amount` per objective. `complete(ServerClient)` **consumes** the items. Progress box auto-renders "Deliver X" + progress bar per item, plus the item's `ObtainTip` hint if the `Item` implements `inventory/item/ObtainTip`. |
| `KillMobsQuest` (abstract) | N × `KillObjective(mobStringID, mobsToKill)` | `currentKills` synced via `setupPacket`/`applyPacket` | Counts kills **from acceptance**: per-client delta of `client.characterStats().mob_kills` computed in `KillObjective.tick`; progress shared across all clients on the same instance. `markDirty()` on change → auto `PacketQuestUpdate`. |
| `HaveKilledMobsQuest` (abstract) | N × `HaveKilledObjective(mobStringID, mobsToKill)` | none — reads lifetime `mob_kills` stat on both sides | "Have ever killed" totals, not quest-scoped. |
| `KillMobsTitleQuest` (concrete) | inherits | inherits | Only core registration: `registerQuest("killmobstitle", KillMobsTitleQuest.class)`. Adds a saved/synced `GameMessage title`; `getRewardType`/`getHandInType` return null. |

Even in these bases, `getTitle()`/`getRewardType()`/`getHandInType()` stay abstract
(`getDescription()` returns null) — a subclass supplies them.

The vanilla progress-bar helper used by all of them (public, reusable):

```java
Achievement.getProgressbarTextDrawBox(x, y, width, 5, progress,
    Settings.UI.progressBarOutline, Settings.UI.progressBarFill,
    current + "/" + total, progressFontOptions)   // engine/achievements/Achievement.java:178
```

---

## 4. Registration — open registry, mod-facing

`engine/registries/QuestRegistry.java`:

```java
public class QuestRegistry extends EmptyConstructorGameRegistry<Quest> {
    public static final QuestRegistry instance = new QuestRegistry();   // "Quest", max 32762
    public static void registerQuest(String stringID, Class<? extends Quest> questClass)
    public static Quest getNewQuest(int id) / getNewQuest(String stringID)
    public static int getQuestID(String stringID) / getQuestID(Class)
}
```

- Registration window: `GlobalData` (lines 305–382) runs `registerCore()` on all
  registries (`QuestRegistry.instance` is in the array at line 347), **then**
  `mod.init()` for each enabled mod, **then** `closeRegistry()`. So
  `QuestRegistry.registerQuest(...)` belongs in `ModEntry.init()`, alongside the existing
  tile/object/mob registrations. After close, `register` throws `RegistryClosedException`.
- Requirement: the quest class needs a **public no-arg constructor**
  (`EmptyConstructorGameRegistry.registerClass` rejects it otherwise) — packets and saves
  instantiate via it, then call `applySpawnPacket`/`applyLoadData`.
- Client/server-only mods cannot register (`LoadedMod.isRunningModClientSide()` guard).
- Numeric IDs are assigned in registration order (`GameRegistry.registerObj`,
  `id = list.size()`); saves use the stringID (stable across updates), packets use the
  numeric ID (both sides must run the mod — Necesse's normal mod-parity requirement
  covers this).

---

## 5. Giving, completing, removing — the canonical flow

The only vanilla creation site, the F1 debug tool
(`gfx/forms/presets/debug/DebugToolsList.java:326-336`), shows the whole give sequence:

```java
KillMobsQuest quest = new KillMobsTitleQuest(new StaticMessage("TEST QUEST"),
        new KillMobsQuest.KillObjective("zombie", 5),
        new KillMobsQuest.KillObjective("zombiearcher", 2));
this.getServer().world.getQuests().addQuest(quest, true);   // 1. world-register (persist + tick + dirty-sync)
quest.makeActiveFor(this.getServer(), serverClient);        // 2. per-player activate (sends PacketQuest)
```

Both steps matter: `addQuest` puts the instance under world save/tick/dirty-broadcast;
`makeActiveFor` records the player auth and pushes it to that client (where it
auto-appears in the tracked sidebar, §6). Share-with-team, abandon and the quest-log
window then work with zero further code.

Completion is giver-driven. The generic NPC-dialogue implementation
(`inventory/container/mob/ShopContainer.java:294-334`) does exactly:

```java
// take:     cq.quest.makeActiveFor(server, serverClient);
// complete: if (cq.quest.canComplete(client)) { humanShop.completeQuest(serverClient, uniqueID); ... }
```

i.e. the pattern for any custom giver (e.g. inside a `FriendlyMob.interact()`):

```java
Quest q = client.getQuest(uniqueID);            // or find by type among client.getQuests()
if (q != null && q.canComplete(client)) {
    q.complete(client);                         // DeliverItemsQuest consumes the items here
    // ...grant rewards, advance world stage...
    server.world.getQuests().removeQuest(q);    // broadcasts PacketQuestRemove to active clients
}
```

`Quest.complete` does **not** remove the quest and does **not** grant rewards — the giver
does both. There is also no engine-side "completed history"; a chain keeps its own stage
state (the existing `SkywatchQuestData` already does this).

### Optional: `!`/`?` head markers — `QuestGiver` (`entity/mobs/QuestGiver.java`)

A complete but vanilla-unused interface: implement `getQuestGiverObject()` (a
`QuestGiver.QuestGiverObject(this, shouldSaveQuests)` field) + `getGivenQuests(ServerClient)`,
call `questGiverObject.serverTick()`/`clientTick()` from the mob's ticks, and send
`new PacketQuestGiverRequest(getUniqueID())` from client `init()` (the Elder does this at
`ElderHumanMob.java:233`). `QuestGiverObject.getMarkerOptions(perspective)` then yields
`!` (offer), `?` gray (in progress), `?` yellow (can complete). Marker **drawing** is only
wired into `HumanMob.addDrawables`/`drawOnMap` — a plain `Mob`/`FriendlyMob` must draw it
itself in `addDrawables` via the public static helper:

```java
QuestGiver.getMarkerDrawOptions(icons, color, x, y, light, camera, xOffset, yOffset)
// or QuestMarkerOptions.getDrawOptions(...)
```

Since the Warden already overrides `addDrawables`, drawing a marker from mod-known state
(world stage + per-client `canComplete`) is a few lines and does not require implementing
`QuestGiver` at all.

---

## 6. HUD and journal display — what is automatic

- **Tracked sidebar (HUD, right edge)**: `gfx/forms/presets/sidebar/TrackedSidebarForm`,
  installed unconditionally (`MainGameFormManager.java:880`,
  `addSidebar(new TrackedSidebarForm(this))`). Sections: story objectives (with collapse
  toggle), tracked achievements, then **"Quests"** — one
  `FormQuestTrackedComponent` per tracked quest. New quests auto-track
  (`Settings.trackNewQuests` default `true`; user-toggleable, synced to the server via
  `PacketTrackNewQuests`). The sidebar re-renders on quest add/remove/update.
- **Quest-log window**: a "Quests" quickbar button appears whenever
  `client.quests.getTotalQuests() > 0` (`MainGameFormManager.java:542-545`), sending
  `PacketOpenQuests` → server opens `ContainerRegistry.QUESTS_CONTAINER`
  (registered in core, `ContainerRegistry.java:531`) → `QuestsContainerForm`: full list
  with per-quest track checkbox (`PacketQuestTrack`), share dropdown
  (`PacketQuestShare`, team required) and abandon (`PacketQuestAbandon` + confirm).
- **Rendering contract**: both `FormQuestComponent` (log) and
  `FormQuestTrackedComponent` (sidebar) draw, in order: `getTitle()` (16pt, highlight
  color), `getDescription()` (12pt), `getProgressDrawBox(...)` (the quest draws its own
  objectives/progress bars), `getRewardType(...)`, `getHandInType(...)`. Rich text via
  `FairType`; item/mob icons by embedding parse strings and applying parsers, e.g.:

```java
// TypeParsers.getItemParseString("stormshard") -> "[item=stormshard]"
FairType type = new FairType().append(new FontOptions(12).outline(outlined),
        Localization.translate("quests", "handinwarden"));
type.applyParsers(TypeParsers.GAME_COLOR, TypeParsers.ItemIcon(12), TypeParsers.MobIcon(12));
```

- **NPC dialogue quest tab** (optional): only for `HumanShop`-derived mobs —
  `HumanShop.getQuests(ServerClient) -> ArrayList<ContainerQuest>` +
  `completeQuest`/`skipQuest` overrides, rendered by `ShopQuestsForm` with generic
  take/complete/skip buttons. Not needed for an interact-driven `FriendlyMob` giver.

---

## 7. Persistence and multiplayer sync

### Save/load

- **World save**: `WorldEntity.addSaveData` writes a `"QUESTS"` section, one
  `QuestSave.getSave(quest)` each — stringID + `quest.addSaveData` (lines 361-369);
  load re-instantiates via `QuestRegistry.getNewQuest(stringID)` +
  `applyLoadData`, re-adding with `addQuest(quest, false)` (lines 471-482).
  `QuestSave.REMOVED_STRING_IDS` silences load errors for retired quest types.
- **Per-player**: `ServerClient.addSaveData` writes `"quests"` and `"trackedQuests"` int
  arrays of unique IDs (lines 299-300); load resolves them against
  `server.world.getQuests()` (lines 561-571). The `Quest` itself also persists its
  active-auth set. Net effect: quests survive save/load with per-player activation and
  tracked flags intact, provided the instance is in the **world** manager.
- **Client cache**: tracked-quest choices also cache locally per world
  (`TrackedSidebarForm.loadTrackedQuests`/`saveTrackedQuests` via `GameCache`).

### Packets (all in `engine/network/packet/`, core-registered)

| Packet | Direction | Purpose |
|---|---|---|
| `PacketQuest` | S→C | Add one quest: registry ID + `setupSpawnPacket` payload + isNew flag |
| `PacketQuests` | S→C | Bulk sync on join (all of the player's quests + tracked flags); requested by the `ClientLoadingQuests` login phase via `PacketRequestQuests` |
| `PacketQuestUpdate` | S→C | Delta (`setupPacket`) for one quest; **sent automatically** by `WorldEntity` tick for every dirty quest (lines 602-615) — server code only needs `markDirty()` |
| `PacketQuestRemove` | S→C | Remove by unique ID |
| `PacketQuestRequest` | C→S | Client saw an update for an unknown unique ID → server re-sends full `PacketQuest` |
| `PacketQuestTrack` / `PacketQuestAbandon` | C→S | Sidebar tracking toggle / abandon (server validates ownership) |
| `PacketQuestShare` → `PacketQuestShareReceive` → `PacketQuestShareReply` | C→S→C→S | Team share invite flow; accept calls `quest.onShared` → `makeActiveFor` |
| `PacketOpenQuests` | C→S | Open the quest-log container |
| `PacketQuestGiverRequest` / `PacketQuestGiverUpdate` | C→S / S→C | `QuestGiver` marker state sync (only if that interface is used) |

A mod never touches these directly — they fire from `addQuest`/`makeActiveFor`/
`markDirty`/`removeQuest` and the vanilla UI.

---

## 8. Story objectives — when (not) to use them

`engine/storyObjectives/StoryObjective` + `StoryObjectiveManager` (per player:
`Client.storyManager` / `ServerClient.storyManager`) + `StoryObjectiveRegistry`:

- Open registration: `StoryObjectiveRegistry.registerObjective(stringID, Class, isMajorObjective)`
  (constructor must take `StoryObjectiveManager`); the returned element supports
  `.showBeforeObjective(id)` / `.showAfterObjective(id)` ordering.
- Every player's manager instantiates **every** registered objective into one ordered
  line (`createObjectiveLine`); the **first** uncompleted-or-unclaimed one is the single
  "current objective" (`updateCurrentAndClaimableObjectives`) — shown in the sidebar and
  allowed to draw on-level guidance (`getCurrentObjectiveLevelHudDrawOptions`, arrow/text
  helpers in `StoryObjective.getLevelTextDrawOptions`).
- Completion is server-side `markCompleted()`; progress events flow through listener
  interfaces registered in the public `StoryObjectiveManager.LISTENER_CLASSES`
  (mob-killed, object-placed/interacted, container-opened, …). Reward claiming runs
  through the Elder/settlement UI (`claimRewards(ServerSettlementData)`).
- **Why not for this chain**: inserting objectives mid-line hijacks the global "current
  objective" slot until done (blocking vanilla display), and appending at the end means
  they only surface after the entire vanilla campaign. Acceptable use: at most one
  appended, self-completing "reach the sky" breadcrumb; the actual chain belongs in the
  quest system.

---

## 9. Mapping the Warden chain onto the quest system

World state stays where it is: `SkywatchQuestData` (LevelData) remains authoritative for
stage/flags; `SkyWardenMob.interact()` remains the state machine. The quest layer is a
per-player HUD mirror created/completed at the same points. Recommended registrations
(`ModEntry.init()`):

```java
QuestRegistry.registerQuest("swh_findspire",   FindSpireQuest.class);
QuestRegistry.registerQuest("swh_beacon",      BeaconDeliveryQuest.class);
QuestRegistry.registerQuest("swh_cats",        SpireCatsQuest.class);
QuestRegistry.registerQuest("swh_anchor",      AnchorDeliveryQuest.class);
```

| Chain step | Base class | Objective mechanics |
|---|---|---|
| Stage 0 — find the Warden's Spire | custom `Quest` (no vanilla reach-location base) | `tick` no-op; `canComplete` → true (or "is on the sky level"); `getProgressDrawBox` renders a hint line (optionally direction/distance from the known spire coords). Completed by talking to the Warden. |
| Stage 1 — light the beacon (12 stormshard + 8 windsilk) | `DeliverItemsQuest` subclass | `new ItemObjective("stormshard", 12), new ItemObjective("windsilk", 8)` in the no-arg ctor; `canComplete`/`complete` (count + consume) inherited — replaces the hand-rolled `tryTurnIn` for this stage. Progress bars per item are free; implement `getTitle`/`getRewardType`/`getHandInType`. |
| Cats — bring 2 cats home | custom `Quest` | Two booleans (`blackHome`,`tabbyHome`) synced via `setupPacket`/`applyPacket`; progress box = two check lines + 0–2/2 bar via `Achievement.getProgressbarTextDrawBox`; `canComplete` = both true. When a cat reaches home, mod code updates `SkywatchQuestData` **and** the live instance (find it in `server.world.getQuests().getQuests()`, set flags, `markDirty()` → auto `PacketQuestUpdate`). |
| Anchor finale (5 aetheriumbar + 20 skystone) | `DeliverItemsQuest` subclass | Same as beacon stage. |

### Giving

- `FindSpireQuest`: on a player's first arrival in the Skyreach while `stage == 0`
  (stairway arrival hook), if the client doesn't already have one:
  `world.getQuests().addQuest(q, true); q.makeActiveFor(server, client);`
- `BeaconDeliveryQuest`: in `interact()` when stage becomes 1 (and for players who talk
  to the Warden later while stage == 1 and don't have it yet).
- `SpireCatsQuest` / `AnchorDeliveryQuest`: at the respective intro lines
  (`catsIntroShown` / `anchorIntroShown` — per-player giving can simply be "on talk while
  stage ≥ 2 and quest not held/`stage` flag not done").

### Completing (inside `interact()`, per stage)

```java
BeaconDeliveryQuest q = findActive(client, BeaconDeliveryQuest.class);
if (q != null && q.canComplete(client)) {
    q.complete(client);                       // consumes 12 stormshard + 8 windsilk
    quest.stage = 2; igniteBeacon(...);       // existing world-state advance + rewards via give(...)
    server.world.getQuests().removeQuest(q);  // clears it from every active client's HUD
}
```

Because the chain state is world-shared, on every stage advance also sweep obsolete
instances of that quest type from `world.getQuests()` so other players' HUDs clear
(`removeQuest` broadcasts the removal; a fresh talk gives them the next stage's quest).

### Sketch of the smallest custom subclass

```java
public class FindSpireQuest extends Quest {
    public FindSpireQuest() {}                                    // required no-arg ctor

    @Override public void tick(ServerClient client) {}
    @Override public boolean canComplete(NetworkClient client) { return true; } // completes on talk

    @Override public GameMessage getTitle()       { return new LocalMessage("quests", "swhfindspiretitle"); }
    @Override public GameMessage getDescription() { return new LocalMessage("quests", "swhfindspiredesc"); }

    @Override
    public DrawOptionsBox getProgressDrawBox(NetworkClient client, int x, int y, int width,
                                             Color textColor, boolean outlined) {
        FontOptions fo = new FontOptions(16).outline(outlined);
        if (textColor != null) fo.color(textColor);
        StringDrawOptions line = new StringDrawOptions(fo,
                Localization.translate("quests", "swhfindspireobj"));
        // wrap in a DrawOptionsBox with bounding box (x, y, width, 16) as the vanilla bases do
        ...
    }
    @Override public FairType getRewardType(NetworkClient c, boolean o) { return null; }
    @Override public FairType getHandInType(NetworkClient c, boolean o) {
        return new FairType().append(new FontOptions(12).outline(o),
                Localization.translate("quests", "swhreturnwarden"));
    }
}
```

Locale: add a `[quests]` section to the mod `.lang` files for titles/descriptions/
objective lines (vanilla already provides the generic keys `deliveritem`, `killmob`,
`track`, `abandon`, share/abandon confirmations).

Optional polish, each independent:
- `!`/`?` marker over the Warden via `QuestGiver.getMarkerDrawOptions` in `addDrawables`
  (§5), driven by world stage + `canComplete`.
- `ObtainTip` on `stormshard` (interface `inventory/item/ObtainTip`, one method
  `getObtainTip()`) so the beacon quest's progress box shows a where-to-find hint —
  requires the item class to implement it (`MatItem` does not; a tiny subclass would).
- One appended `StoryObjective` ("build the stairway / reach the sky") after
  `"defeatascendedwizard"` via `registerObjective(...).showAfterObjective(...)` — only if
  main-line visibility is wanted; skippable.

---

## 10. Gotchas

1. **No-arg constructor** is mandatory for registered quest classes; all real state must
   round-trip through `addSaveData`/`applyLoadData` **and** `setupSpawnPacket`/
   `applySpawnPacket` (packets/saves instantiate empty, then apply).
2. **`KillMobsQuest` spawn-packet quirk**: its `setupSpawnPacket` writes objectives, then
   `super.setupSpawnPacket` (which already appends `setupPacket()`), then calls
   `setupPacket()` **again** — the delta payload appears twice, mirrored in
   `applySpawnPacket`. When subclassing it with extra fields, keep the exact same
   ordering/symmetry or the stream desyncs. `DeliverItemsQuest` does not have this quirk.
3. `addQuest` alone shows nothing — without `makeActiveFor` no client receives it; and
   `makeActiveFor` without world `addQuest` breaks persistence (per-player save stores
   only unique IDs resolved against the world manager on load).
4. `canComplete` runs client-side too (UI coloring/buttons) — it must only read
   client-visible state (inventory, synced quest fields), never server-only data.
5. The engine keeps **no completion history** — "already did this stage" lives in mod
   state (the existing `SkywatchQuestData` stage/flags already cover it).
6. Rewards: engine-free. Grant items in the giver at completion; `getRewardType` is text.
7. Registry numeric IDs are order-of-registration; never persist them — persist
   stringIDs (the save layer already does). Renaming a stringID orphans saved quests
   (add old IDs to `QuestSave.REMOVED_STRING_IDS` to silence load warnings).
8. Both sides must run the mod (packets carry registry IDs); vanilla's mod-parity check
   on connect handles this.
9. `Quest.tick` runs per active client per tick — keep it cheap; don't force-load levels
   from it (push updates into the quest from world events instead, then `markDirty()`).
