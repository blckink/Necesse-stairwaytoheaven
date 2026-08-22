package stairwaytoheaven.quest;

import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.LevelData;

/**
 * Server-side world state of "The Warden's Call" quest chain, persisted with
 * the Skyreach level. One shared progression per world (like vanilla world
 * events); rewards go to the delivering player at turn-in time.
 *
 * Stages: 0 = Warden not yet met · 1 = beacon delivery open ·
 * 2+ = beacon lit (shop open; cats & anchor quests both available).
 */
public class SkywatchQuestData extends LevelData {

    public static final String KEY = "skywatchquest";

    public int stage = 0;

    // Warden's Spire (set once when the structure is stamped)
    public boolean spirePlaced = false;
    public int spireX, spireY;          // warden tile
    public int beaconX, beaconY;        // beacon object tile
    public int basketX, basketY;        // cat basket / cat home tile

    // Cats
    public boolean catsSpawned = false;
    public int blackLairX, blackLairY;  // Siggi (Stormveil)
    public int tabbyLairX, tabbyLairY;  // Peanut (Aurora Shoals)
    public boolean blackHome = false;
    public boolean tabbyHome = false;
    public boolean catsIntroShown = false;
    public boolean catsRewardGiven = false;

    // Anchor finale
    public boolean anchorIntroShown = false;
    public boolean anchorDone = false;
    public boolean finaleShown = false;

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        save.addInt("stage", this.stage);
        save.addBoolean("spirePlaced", this.spirePlaced);
        save.addInt("spireX", this.spireX);
        save.addInt("spireY", this.spireY);
        save.addInt("beaconX", this.beaconX);
        save.addInt("beaconY", this.beaconY);
        save.addInt("basketX", this.basketX);
        save.addInt("basketY", this.basketY);
        save.addBoolean("catsSpawned", this.catsSpawned);
        save.addInt("blackLairX", this.blackLairX);
        save.addInt("blackLairY", this.blackLairY);
        save.addInt("tabbyLairX", this.tabbyLairX);
        save.addInt("tabbyLairY", this.tabbyLairY);
        save.addBoolean("blackHome", this.blackHome);
        save.addBoolean("tabbyHome", this.tabbyHome);
        save.addBoolean("catsIntroShown", this.catsIntroShown);
        save.addBoolean("catsRewardGiven", this.catsRewardGiven);
        save.addBoolean("anchorIntroShown", this.anchorIntroShown);
        save.addBoolean("anchorDone", this.anchorDone);
        save.addBoolean("finaleShown", this.finaleShown);
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.stage = save.getInt("stage", 0, false);
        this.spirePlaced = save.getBoolean("spirePlaced", false, false);
        this.spireX = save.getInt("spireX", 0, false);
        this.spireY = save.getInt("spireY", 0, false);
        this.beaconX = save.getInt("beaconX", 0, false);
        this.beaconY = save.getInt("beaconY", 0, false);
        this.basketX = save.getInt("basketX", 0, false);
        this.basketY = save.getInt("basketY", 0, false);
        this.catsSpawned = save.getBoolean("catsSpawned", false, false);
        this.blackLairX = save.getInt("blackLairX", 0, false);
        this.blackLairY = save.getInt("blackLairY", 0, false);
        this.tabbyLairX = save.getInt("tabbyLairX", 0, false);
        this.tabbyLairY = save.getInt("tabbyLairY", 0, false);
        this.blackHome = save.getBoolean("blackHome", false, false);
        this.tabbyHome = save.getBoolean("tabbyHome", false, false);
        this.catsIntroShown = save.getBoolean("catsIntroShown", false, false);
        this.catsRewardGiven = save.getBoolean("catsRewardGiven", false, false);
        this.anchorIntroShown = save.getBoolean("anchorIntroShown", false, false);
        this.anchorDone = save.getBoolean("anchorDone", false, false);
        this.finaleShown = save.getBoolean("finaleShown", false, false);
    }

    /** Fetches the quest data of a level, creating and attaching it if absent. */
    public static SkywatchQuestData get(Level level) {
        LevelData data = level.getLevelData(KEY);
        if (data instanceof SkywatchQuestData) {
            return (SkywatchQuestData) data;
        }
        SkywatchQuestData created = new SkywatchQuestData();
        level.addLevelData(KEY, created);
        return created;
    }
}
