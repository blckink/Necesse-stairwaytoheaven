package stairwaytoheaven;

import necesse.engine.registries.MobRegistry;
import necesse.gfx.gameTexture.GameTexture;
import stairwaytoheaven.mobs.CloudLambMob;
import stairwaytoheaven.mobs.SkyCritterMob;
import stairwaytoheaven.mobs.SkyWardenMob;
import stairwaytoheaven.mobs.SkystoneGolemMob;
import stairwaytoheaven.mobs.SpireCatMob;
import stairwaytoheaven.mobs.StormWispMob;
import stairwaytoheaven.mobs.WardenSettlerMob;
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
        // v0.5: the recruited Warden as a real surface settler (HumanShop).
        // NOT registered with createSpawnItem — the 100,000-coin recruitment
        // transaction transfers him directly (see SkyWardenMob.tryRecruit);
        // there is no spawn-item purchase.
        MobRegistry.registerMob("wardensettler", WardenSettlerMob.class, false);
        // ...and the settlement-side type for that mob. Without it the mob is
        // not a settler at all (SettlerRegistry lookup returns null, and
        // LevelSettler requireNonNulls it), so he could never move in.
        necesse.engine.registries.SettlerRegistry.registerSettler("wardensettler",
                new stairwaytoheaven.settlement.WardenSettler());
        // ambient critters, one per sub-biome
        MobRegistry.registerMob("cloudlamb", stairwaytoheaven.mobs.CloudLambMob.class, false);
        MobRegistry.registerMob("glowmoth", SkyCritterMob.GlowMoth.class, false);
        MobRegistry.registerMob("sparkbeetle", SkyCritterMob.SparkBeetle.class, false);
        // v0.3: the Veil
        MobRegistry.registerMob("gloomshade", stairwaytoheaven.mobs.GloomShadeMob.class, true);
        // v0.4: The Living Sky
        MobRegistry.registerMob("galehound", stairwaytoheaven.mobs.GalehoundMob.class, true);
        MobRegistry.registerMob("dawnpiercer", stairwaytoheaven.mobs.DawnpiercerMob.class, true);
        MobRegistry.registerMob("zephyrfinch", SkyCritterMob.ZephyrFinch.class, false);
        MobRegistry.registerMob("dewsnail", SkyCritterMob.DewSnail.class, false);
        // v0.5.1: the Mistserpent — a worm chain that swims the cloud sea.
        // The body and tail are registered too: worm segments are real mobs and
        // the chain cannot be rebuilt on load without their IDs.
        MobRegistry.registerMob("mistserpent", stairwaytoheaven.mobs.MistserpentHead.class, true);
        MobRegistry.registerMob("mistserpentbody", stairwaytoheaven.mobs.MistserpentBody.class, false);
        MobRegistry.registerMob("mistserpenttail", stairwaytoheaven.mobs.MistserpentBody.Tail.class, false);
    }

    /** Called from initResources — runs on the client only, never on servers. */
    static void loadTextures() {
        stairwaytoheaven.mobs.MistserpentHead.texture = GameTexture.fromFile("mobs/mistserpent");
        stairwaytoheaven.mobs.MistserpentHead.maskTexture = GameTexture.fromFile("mobs/mistserpent_mask");
        stairwaytoheaven.mobs.MistserpentHead.shadowTexture = GameTexture.fromFile("mobs/mistserpent_shadow");
        ZephyrRayMob.texture = GameTexture.fromFile("mobs/zephyrray");
        StormWispMob.texture = GameTexture.fromFile("mobs/stormwisp");
        SkystoneGolemMob.texture = GameTexture.fromFile("mobs/skystonegolem");
        // No SkyWardenMob texture any more: he is a HumanMob and the human
        // renderer composes him from the body layers plus his armor items.
        SpireCatMob.blackTexture = GameTexture.fromFile("mobs/spirecatblack");
        SpireCatMob.tabbyTexture = GameTexture.fromFile("mobs/spirecattabby");
        stairwaytoheaven.mobs.CloudLambMob.texture = GameTexture.fromFile("mobs/cloudlamb");
        stairwaytoheaven.mobs.CloudLambMob.shearedTexture = GameTexture.fromFile("mobs/cloudlamb_sheared");
        SkyCritterMob.mothTexture = GameTexture.fromFile("mobs/glowmoth");
        SkyCritterMob.beetleTexture = GameTexture.fromFile("mobs/sparkbeetle");
        stairwaytoheaven.mobs.GloomShadeMob.texture = GameTexture.fromFile("mobs/gloomshade");
        stairwaytoheaven.mobs.GalehoundMob.texture = GameTexture.fromFile("mobs/galehound");
        stairwaytoheaven.mobs.DawnpiercerMob.texture = GameTexture.fromFile("mobs/dawnpiercer");
        SkyCritterMob.finchTexture = GameTexture.fromFile("mobs/zephyrfinch");
        SkyCritterMob.snailTexture = GameTexture.fromFile("mobs/dewsnail");
    }
}
