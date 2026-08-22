package stairwaytoheaven.worldgen;

import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.mobs.SkyWardenMob;
import stairwaytoheaven.quest.SkywatchQuestData;

/**
 * The Warden's Spire: a 15x15 half-collapsed Skywatch tower. Authored
 * imperatively; the Sky Warden is spawned through a custom-apply hook and the
 * quest anchor points (warden, beacon, basket spot) are recorded in
 * {@link SkywatchQuestData} the moment the preset is stamped.
 *
 * Local coordinates: (0,0) top-left; the tower ring spans (3..11)x(3..11)
 * with the door in the south wall and two storm-torn breaches.
 */
public class WardenSpirePreset extends Preset {

    public static final int SIZE = 15;
    public static final int WARDEN_X = 7, WARDEN_Y = 6;
    public static final int BEACON_X = 7, BEACON_Y = 4;
    public static final int BASKET_X = 5, BASKET_Y = 6;

    public WardenSpirePreset() {
        super(SIZE, SIZE);

        int checker = SkyRegistry.marbleCheckerID;
        int planks = SkyRegistry.gloomwoodFloorID;
        int wall = SkyRegistry.skystoneBrickWallID;
        int door = ObjectRegistry.getObjectID("skystonebrickdoor");
        int fence = SkyRegistry.skyironFenceID;
        int candelabra = SkyRegistry.wardenCandelabraID;
        int lantern = ObjectRegistry.getObjectID("mistglasslantern");
        int statue = ObjectRegistry.getObjectID("gloomravenstatue");
        int willow = ObjectRegistry.getObjectID("gloomwillow");

        // Any Mistsea under the footprint becomes solid ground first (the
        // ElderHousePreset liquid-fill idiom), so the ruin never half-floats
        this.addCustomPreApplyRectEach(0, 0, SIZE, SIZE, 0, (level, levelX, levelY, dir, blackboard) -> {
            if (level.getTile(levelX, levelY).isLiquid) {
                level.setTile(levelX, levelY, SkyRegistry.cloudturfID);
                level.setObject(levelX, levelY, 0);
            }
            return null;
        });

        // Floors: gloomwood ring with a checkered marble core
        this.fillTile(3, 3, 9, 9, planks);
        this.fillTile(5, 5, 5, 5, checker);
        // Approach path to the door
        this.fillTile(7, 12, 1, 3, planks);

        // Tower ring with a south door and storm breaches (missing segments)
        for (int i = 3; i <= 11; i++) {
            this.setObject(i, 3, wall);   // north
            this.setObject(i, 11, wall);  // south
            this.setObject(3, i, wall);   // west
            this.setObject(11, i, wall);  // east
        }
        this.setObject(7, 11, door);
        // breaches — the storm took these
        this.setObject(11, 6, 0);
        this.setObject(4, 3, 0);
        this.setObject(3, 8, 0);

        // Interior: the dark beacon, one working candelabra, a wall lantern
        this.setObject(BEACON_X, BEACON_Y, SkyRegistry.wardenBeaconOffID);
        this.setObject(5, 9, candelabra);
        this.setObject(10, 4, lantern);

        // Grounds: raven statue, two gloomwillows, fence stubs by the path
        this.setObject(1, 10, statue);
        this.setObject(13, 2, willow);
        this.setObject(0, 5, willow);
        this.setObject(5, 12, fence);
        this.setObject(6, 12, fence);
        this.setObject(8, 12, fence);
        this.setObject(9, 12, fence);

        // The Warden himself + quest bookkeeping, at stamp time. The lambda
        // receives the anchor's WORLD tile coordinates; the other quest
        // points are recorded via their fixed offsets from the warden tile.
        this.addCustomApply(WARDEN_X, WARDEN_Y, 0, (level, levelX, levelY, dir, blackboard) -> {
            if (level.isServer()) {
                SkywatchQuestData quest = SkywatchQuestData.get(level);
                quest.spireX = levelX;
                quest.spireY = levelY;
                quest.beaconX = levelX + (BEACON_X - WARDEN_X);
                quest.beaconY = levelY + (BEACON_Y - WARDEN_Y);
                quest.basketX = levelX + (BASKET_X - WARDEN_X);
                quest.basketY = levelY + (BASKET_Y - WARDEN_Y);
                quest.spirePlaced = true;

                SkyWardenMob warden = (SkyWardenMob) MobRegistry.getMob("skywarden", level);
                level.entityManager.addMob(warden, levelX * 32 + 16, levelY * 32 + 16);
            }
            return null;
        });
    }
}
