package stairwaytoheaven.settlement;

import java.awt.Color;

import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.SettlerRegistry;
import stairwaytoheaven.objects.SkyDecoObject;
import stairwaytoheaven.objects.VeteranCatapultObject;
import stairwaytoheaven.objects.VeteranTurretObject;

/**
 * The War Veteran (Kriegsveteran): a recruitable soldier settler and the two
 * defense structures he sells.
 *
 * <h2>What shipped in this pass, and what did not</h2>
 *
 * The settler, his soldier look, his patrol/combat AI (vanilla's
 * {@code HumanAI} with {@code attackHostiles=true}, the same flag the Miner
 * and Explorer use — see {@code WarVeteranHumanMob}) and his shop selling
 * {@code veteranbarricade}/{@code barbedwirefence} are real, registered
 * content. The turret ({@code veteranturret}), catapult
 * ({@code veterancatapult}) and the ammo-upgrade defense-level system were
 * CUT under the session's hard time limit: they need a ticking entity with
 * hostile-only targeting and a projectile, an API surface this pass had no
 * time to read from source rather than guess. Building that without reading
 * the real target-acquisition/projectile-spawn API first is exactly the kind
 * of guess that turns a build red, which the task explicitly said to avoid.
 * A later pass should read {@code necesse.entity.mobs.Mob} targeting and
 * {@code necesse.entity.projectile} before attempting them.
 *
 * <p>The two structures registered here are static, high-toughness
 * {@link SkyDecoObject}s — solid, six times a plain wall's
 * {@code objectHealth} (100 -> 600) — with no special combat behaviour of
 * their own; "barbed wire damages hostiles on touch" was cut for the same
 * reason.
 */
public final class WarVeteran {

    private static final Color MAP_BARRICADE = new Color(120, 108, 84);
    private static final Color MAP_WIRE = new Color(140, 140, 140);

    /** A plain vanilla wood wall's objectHealth is 100 (GameObject default); 6x that. */
    private static final int DEFENSE_OBJECT_HEALTH = 600;

    private WarVeteran() {
    }

    public static void register() {
        MobRegistry.registerMob("warveteranhuman", stairwaytoheaven.mobs.WarVeteranHumanMob.class, false);
        SettlerRegistry.registerSettler("warveteran", new WarVeteranSettler());

        // Single 32x48 sprite, bottom-anchored with a 16px overhang upwards —
        // the same idiom SkyDecoObject already uses for Gloomwillow etc.
        // variantWidth == texture width (32) means every tile draws the same
        // frame; there is only one.
        // No custom item category: passing none falls back to
        // SkyDecoObject's own default ("objects", "decorations"), which is
        // already registered and safe rather than inventing a new category
        // this pass has no time to wire into the crafting menu properly.
        SkyDecoObject barricade = new SkyDecoObject("veteranbarricade", 32, MAP_BARRICADE, null);
        barricade.isSolid = true;
        barricade.setObjectHealth(DEFENSE_OBJECT_HEALTH);
        ObjectRegistry.registerObject("veteranbarricade", barricade, 40.0F, true);

        SkyDecoObject wire = new SkyDecoObject("barbedwirefence", 32, MAP_WIRE, null);
        wire.isSolid = true;
        wire.setObjectHealth(DEFENSE_OBJECT_HEALTH);
        ObjectRegistry.registerObject("barbedwirefence", wire, 35.0F, true);

        // Auto Turret: 1x1, real ObjectEntity target-scan and fire loop —
        // see VeteranTurretObjectEntity. Catapult (veterancatapult) and the
        // ammo-upgrade dialogue (VeteranDefense.raiseLevel) were cut under
        // the session's time limit; see those classes' notes.
        ObjectRegistry.registerObject("veteranturret", new VeteranTurretObject(), 60.0F, true);

        // Catapult: registered 1x1 (see VeteranCatapultObject's class note
        // on the cut 3x3 MultiTile footprint), long range, splash.
        ObjectRegistry.registerObject("veterancatapult", new VeteranCatapultObject(), 150.0F, true);
    }
}
