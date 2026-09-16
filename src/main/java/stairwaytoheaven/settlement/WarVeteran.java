package stairwaytoheaven.settlement;

import java.awt.Color;

import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.ProjectileRegistry;
import necesse.engine.registries.SettlerRegistry;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.FenceObject;
import necesse.level.gameObject.WallObject;
import stairwaytoheaven.objects.CatapultStoneProjectile;
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
 * <p>{@code veteranbarricade} is a real {@link WallObject} (sandbags + heavy
 * timber + riveted iron plates, vanilla's 352x128 wall-sheet format) and
 * {@code barbedwirefence} a real {@link FenceObject} (vanilla's 160x64,
 * 5x32-column post/rail format) — both connect to neighbouring pieces and
 * get correct side/front views the way vanilla walls and fences do, instead
 * of the single static 32x48 sprite this pass shipped with before. Both are
 * six times a plain wall's {@code objectHealth} (100 -> 600); "barbed wire
 * damages hostiles on touch" is still cut — no time to read the on-touch
 * damage API this pass.
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

        // veteranbarricade is a real vanilla-style WallObject now (sandbags +
        // heavy timber + riveted iron plates), registered directly under its
        // existing ID (not via WallObject.registerWallObjects, which would
        // append "wall"/"door"/"window" suffixes to the ID prefix and break
        // save compatibility). No outline texture (null) and no separate
        // door/window pair — those weren't part of the original scope and
        // registerWallObjects's convenience wrapper is the only path that
        // wires them up automatically.
        WallObject barricade = new WallObject("veteranbarricade", null, MAP_BARRICADE, 2.0F, ToolType.ALL);
        barricade.objectHealth = DEFENSE_OBJECT_HEALTH;
        ObjectRegistry.registerObject("veteranbarricade", barricade, 40.0F, true);

        // barbedwirefence is a real vanilla-style FenceObject now: it
        // connects to neighbouring fences/walls/rocks (FenceObject.attachesToObject)
        // and draws post + rail pieces instead of one static sprite. Collision
        // box is narrower than a full wall — wire strands, not a solid plate.
        FenceObject wire = new FenceObject("barbedwirefence", MAP_WIRE, 24, 16);
        wire.objectHealth = DEFENSE_OBJECT_HEALTH;
        ObjectRegistry.registerObject("barbedwirefence", wire, 35.0F, true);

        // Auto Turret: 1x1, real ObjectEntity target-scan and fire loop —
        // see VeteranTurretObjectEntity. Catapult (veterancatapult) and the
        // ammo-upgrade dialogue (VeteranDefense.raiseLevel) were cut under
        // the session's time limit; see those classes' notes.
        ObjectRegistry.registerObject("veteranturret", new VeteranTurretObject(), 60.0F, true);

        // Catapult: registered 1x1 (see VeteranCatapultObject's class note
        // on the cut 3x3 MultiTile footprint), long range, splash.
        ObjectRegistry.registerObject("veterancatapult", new VeteranCatapultObject(), 150.0F, true);
        // Cosmetic lobbed stone the catapult fires on each shot — no shadow
        // texture shipped with this art drop, so registered with a null
        // shadow path; see CatapultStoneProjectile's own addDrawables note.
        // Fired via `new CatapultStoneProjectile(...)` directly (see
        // VeteranCatapultObjectEntity), so the returned int ID isn't needed
        // here: Projectile.init() resolves texture/id by class automatically.
        ProjectileRegistry.registerProjectile(
                "catapultstone", CatapultStoneProjectile.class, "catapultstone", null);
    }
}
