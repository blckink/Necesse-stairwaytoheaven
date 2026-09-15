package stairwaytoheaven.mobs;

import java.io.FileNotFoundException;

import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.gfx.gameTexture.GameTexture;

/**
 * What a player carries away from petting a spire cat that lives in town.
 *
 * <p>One buff per cat, because they are two different animals: Siggi is the
 * black one who sulked in a storm and makes you sharper, Peanut is the tabby
 * who chases moths and makes you quicker. It lasts one in-game day
 * ({@link WorldEntity#DEFAULT_DAY_TOTAL} seconds, 960 in 1.3.3), and petting
 * again simply refreshes it.
 *
 * <p>The icon is vanilla's {@code buffs/loved}, borrowed by path the same way
 * {@code SoulExposureBuff} borrows its own; super's {@code buffs/unknown} is
 * the fallback.
 */
public class CatCuddleBuff extends Buff {

    public static final String SIGGI_ID = "siggicuddle";
    public static final String PEANUT_ID = "peanutcuddle";
    public static final String BORROWED_ICON = "buffs/loved";

    private final boolean siggi;

    public CatCuddleBuff(boolean siggi) {
        this.siggi = siggi;
        this.isVisible = true;
        this.canCancel = false;
        this.shouldSave = true;
        this.isImportant = true;
    }

    /** One in-game day, in milliseconds. */
    public static int durationMs() {
        return WorldEntity.DEFAULT_DAY_TOTAL * 1000;
    }

    @Override
    public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
        if (this.siggi) {
            buff.setModifier(BuffModifiers.CRIT_CHANCE, 0.08F);
            buff.setModifier(BuffModifiers.ALL_DAMAGE, 0.05F);
            buff.setModifier(BuffModifiers.ARMOR_FLAT, 4);
        } else {
            buff.setModifier(BuffModifiers.SPEED, 0.12F);
            buff.setModifier(BuffModifiers.HEALTH_REGEN_FLAT, 0.5F);
            buff.setModifier(BuffModifiers.MINING_SPEED, 0.10F);
        }
    }

    @Override
    public void loadTextures() {
        try {
            this.iconTexture = GameTexture.fromFileRaw(BORROWED_ICON);
        } catch (FileNotFoundException e) {
            super.loadTextures();
        }
    }
}
