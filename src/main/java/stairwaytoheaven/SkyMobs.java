package stairwaytoheaven;

import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.HumanTexture;
import necesse.gfx.gameTexture.GameTexture;
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
        // The three Skyreach residents: mob AND settler type together, because
        // a mob without a registered Settler can never move in.
        stairwaytoheaven.settlement.SkySettlers.register();
        // ambient critters, one per sub-biome
        MobRegistry.registerMob("glowmoth", SkyCritterMob.GlowMoth.class, false);
        MobRegistry.registerMob("sparkbeetle", SkyCritterMob.SparkBeetle.class, false);
        // v0.3: the Veil
        MobRegistry.registerMob("gloomshade", stairwaytoheaven.mobs.GloomShadeMob.class, true);
        // The Seance Circle's answer (docs/FOGKEY_AND_BOSSPORTALS.md A3).
        // countKillStat = false: he cannot be killed (canTakeDamage is false),
        // so a bestiary row for him would never fill in. No registerSettler
        // call either -- he is summoned, never hired.
        //
        // The ID is written as a LITERAL rather than as GhostGuideMob.STRING_ID
        // deliberately: tools/locale_audit.py's registered_ids() and
        // human_mob_ids() both match `registerMob("<id>", <Class>.class` with a
        // regex, so an ID handed over through a constant would be registered
        // without either of its two required locale keys being checked. The
        // constant exists for the Seance Circle's lookup and carries the same
        // string; GhostGuideMob.STRING_ID names this line in its own comment.
        MobRegistry.registerMob("ghostguide", stairwaytoheaven.mobs.GhostGuideMob.class, false);
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
        // The Beetle Outlands' own cast. Each subclasses the vanilla mob the
        // biome used to spawn by string ID, inherits every number and every
        // behaviour from it, and overrides addDrawables so the sheet is ours
        // rather than the crystal caves'. See the three class comments.
        MobRegistry.registerMob("crookedgolem", stairwaytoheaven.mobs.CrookedGolemMob.class, true);
        MobRegistry.registerMob("rarecrookedgolem", stairwaytoheaven.mobs.RareCrookedGolemMob.class, true);
        MobRegistry.registerMob("crookedarmadillo", stairwaytoheaven.mobs.CrookedArmadilloMob.class, true);
    }

    /** Called from initResources — runs on the client only, never on servers. */
    static void loadTextures() {
        stairwaytoheaven.mobs.MistserpentHead.texture = GameTexture.fromFile("mobs/mistserpent");
        stairwaytoheaven.mobs.MistserpentHead.headTexture = GameTexture.fromFile("mobs/mistserpent_head");
        stairwaytoheaven.mobs.MistserpentHead.shadowTexture = GameTexture.fromFile("mobs/mistserpent_shadow");
        ZephyrRayMob.texture = GameTexture.fromFile("mobs/zephyrray");
        StormWispMob.texture = GameTexture.fromFile("mobs/stormwisp");
        SkystoneGolemMob.texture = GameTexture.fromFile("mobs/skystonegolem");
        // No SkyWardenMob texture any more: he is a HumanMob and the human
        // renderer composes him from the body layers plus his armor items.
        SpireCatMob.blackTexture = GameTexture.fromFile("mobs/spirecatblack");
        SpireCatMob.tabbyTexture = GameTexture.fromFile("mobs/spirecattabby");
        SkyCritterMob.mothTexture = GameTexture.fromFile("mobs/glowmoth");
        SkyCritterMob.beetleTexture = GameTexture.fromFile("mobs/sparkbeetle");
        stairwaytoheaven.mobs.GloomShadeMob.texture = GameTexture.fromFile("mobs/gloomshade");
        stairwaytoheaven.mobs.GalehoundMob.texture = GameTexture.fromFile("mobs/galehound");
        stairwaytoheaven.mobs.DawnpiercerMob.texture = GameTexture.fromFile("mobs/dawnpiercer");
        SkyCritterMob.finchTexture = GameTexture.fromFile("mobs/zephyrfinch");
        SkyCritterMob.snailTexture = GameTexture.fromFile("mobs/dewsnail");
        stairwaytoheaven.arsenal.FenWraithMob.texture = GameTexture.fromFile("mobs/fenwraith");
        stairwaytoheaven.arsenal.AuroraFlakeMob.texture = GameTexture.fromFile("mobs/auroraflake");
        stairwaytoheaven.arsenal.RimeSentryMob.texture = GameTexture.fromFile("mobs/rimesentry");
        // The Cinder Cantor is drawn through HumanDrawOptions, which needs a
        // HumanTexture -- THREE sheets, not one: body, left arms, right arms
        // (VERIFIED [jar], MobRegistry.humanTexture at MobRegistry.java:1830-1836).
        // The player supplied all three matched sheets; keep them together so
        // no vanilla skeleton layer leaks into the Cantor in motion.
        stairwaytoheaven.arsenal.CinderCantorMob.texture = new HumanTexture(
                GameTexture.fromFile("mobs/cindercantor"),
                GameTexture.fromFile("mobs/cindercantorarms_left"),
                GameTexture.fromFile("mobs/cindercantorarms_right"));
        stairwaytoheaven.mobs.CrookedGolemMob.texture = GameTexture.fromFile("mobs/crookedgolem");
        stairwaytoheaven.mobs.RareCrookedGolemMob.texture = GameTexture.fromFile("mobs/rarecrookedgolem");
        stairwaytoheaven.mobs.CrookedArmadilloMob.texture = GameTexture.fromFile("mobs/crookedarmadillo");
    }
}
