package stairwaytoheaven;

import necesse.engine.registries.MobRegistry;
import necesse.gfx.gameTexture.GameTexture;
import stairwaytoheaven.mobs.SkystoneGolemMob;
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
    }

    /** Called from initResources — runs on the client only, never on servers. */
    static void loadTextures() {
        ZephyrRayMob.texture = GameTexture.fromFile("mobs/zephyrray");
        StormWispMob.texture = GameTexture.fromFile("mobs/stormwisp");
        SkystoneGolemMob.texture = GameTexture.fromFile("mobs/skystonegolem");
    }
}
