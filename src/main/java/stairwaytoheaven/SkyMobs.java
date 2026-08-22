package stairwaytoheaven;

import necesse.engine.registries.MobRegistry;
import necesse.gfx.gameTexture.GameTexture;
import stairwaytoheaven.mobs.SkyWardenMob;
import stairwaytoheaven.mobs.SkystoneGolemMob;
import stairwaytoheaven.mobs.SpireCatMob;
import stairwaytoheaven.mobs.StormWispMob;
import stairwaytoheaven.mobs.ZephyrRayMob;

/**
 * Mob registration and (client-only) texture loading for the Skyreach roster.
 */
final class SkyMobs {

    private SkyMobs() {
    }

    static void register() {
        MobRegistry.registerMob("zephyrray", ZephyrRayMob.class, true);
        MobRegistry.registerMob("stormwisp", StormWispMob.class, true);
        MobRegistry.registerMob("skystonegolem", SkystoneGolemMob.class, true);
        // v0.2: quest NPCs (no kill stats — they cannot die)
        MobRegistry.registerMob("skywarden", SkyWardenMob.class, false);
        MobRegistry.registerMob("spirecatblack", SpireCatMob.Black.class, false);
        MobRegistry.registerMob("spirecattabby", SpireCatMob.Tabby.class, false);
    }

    /** Called from initResources — runs on the client only, never on servers. */
    static void loadTextures() {
        ZephyrRayMob.texture = GameTexture.fromFile("mobs/zephyrray");
        StormWispMob.texture = GameTexture.fromFile("mobs/stormwisp");
        SkystoneGolemMob.texture = GameTexture.fromFile("mobs/skystonegolem");
        SkyWardenMob.texture = GameTexture.fromFile("mobs/skywarden");
        SpireCatMob.blackTexture = GameTexture.fromFile("mobs/spirecatblack");
        SpireCatMob.tabbyTexture = GameTexture.fromFile("mobs/spirecattabby");
    }
}
