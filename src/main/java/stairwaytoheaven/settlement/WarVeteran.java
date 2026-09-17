package stairwaytoheaven.settlement;

import java.awt.Color;

import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.ProjectileRegistry;
import necesse.engine.registries.SettlerDialogueRegistry;
import necesse.engine.registries.SettlerRegistry;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.WallObject;
import stairwaytoheaven.objects.BarbedWireFenceObject;
import stairwaytoheaven.objects.CatapultStoneProjectile;
import stairwaytoheaven.objects.VeteranCatapultObject;
import stairwaytoheaven.objects.VeteranTurretObject;

/**
 * The War Veteran (Kriegsveteran): a recruitable soldier settler and the two
 * defense structures he sells.
 *
 * <ul>
 * <li>{@code warveteranhuman}/{@code warveteran}: always in iron armour, patrols
 * with vanilla's {@code HumanAI} ({@code attackHostiles=true}); settlement
 * raiders extend {@code HostileItemAttackerMob}, which sets
 * {@code isHostile}, so he and every defense below engage them.</li>
 * <li>{@code veteranturret} (1x1) and {@code veterancatapult} (3x3
 * multi-tile): hostile-only targeting, damage scaled by the ammo level
 * ({@link VeteranDefense}, raised in {@link VeteranAmmoDialogue}); firing
 * frames follow the server's shots.</li>
 * <li>{@code veteranbarricade}: a vanilla {@link WallObject};
 * {@code barbedwirefence}: a vanilla fence that cuts hostiles touching it.
 * Both six times a plain wall's
 * {@code objectHealth} (100 -> 600).</li>
 * </ul>
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
        // The "hand over ammunition" talk, the same way SkyDoctor registers
        // its own: a SettlerDialogue resolves its registry ID in its
        // constructor, so an unregistered one throws the moment the shop
        // builds this settler's dialogue list. VeteranDefense (WorldData) is
        // registered in StairwayToHeavenMod for the same reason.
        SettlerDialogueRegistry.registerSettlerDialogue("swh_veteranammo", VeteranAmmoDialogue.class);

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
        // BarbedWireFenceObject adds the entity that cuts hostiles touching it.
        BarbedWireFenceObject wire = new BarbedWireFenceObject("barbedwirefence", MAP_WIRE, 24, 16);
        wire.objectHealth = DEFENSE_OBJECT_HEALTH;
        // No gate: barbed wire is not something you swing open.
        ObjectRegistry.registerObject("barbedwirefence", wire, 35.0F, true);

        // Auto Turret: 1x1, ObjectEntity target-scan and fire loop — see
        // VeteranTurretObjectEntity.
        ObjectRegistry.registerObject("veteranturret", new VeteranTurretObject(), 60.0F, true);

        // Catapult: real 3x3 multi-tile (nine pieces, master keeps the ID),
        // long range, splash.
        VeteranCatapultObject.register(150.0F);
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
