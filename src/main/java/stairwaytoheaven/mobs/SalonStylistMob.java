package stairwaytoheaven.mobs;

import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.StylistHumanMob;
import stairwaytoheaven.SalonWares;

/**
 * The vanilla Stylist with a salon on the side.
 *
 * <p>Vanilla's Stylist already is a {@code HumanShop} — her talk menu is a
 * {@code StylistContainer extends ShopContainer}, so anything in
 * {@code this.shop} shows up in the same list that today holds the wig, the
 * shirt and the shoes. She just never had furniture on it.
 *
 * <h2>Why a subclass and not a hook</h2>
 *
 * The shop has to be filled in the constructor. {@code HumanShop.init()} calls
 * {@code ShopManager.init()}, and that closes both shop registries
 * ({@code ShopManager.java:167/168}); every later {@code addSellingItem} throws
 * {@code RegistryClosedException}. A listener on {@code MobInteractEvent} —
 * the obvious "when you talk to her" hook — runs long after that. So the only
 * place the wares can be added is where vanilla adds its own.
 *
 * <p>{@link SalonWares#replaceVanillaStylist()} puts this class behind the
 * unchanged mob string ID {@code stylisthuman}, so the five places vanilla
 * looks the Stylist up by that string (pirate boss preset, village house 5,
 * {@code StylistSettler}, the free-stylist journal challenge and the incursion
 * rescue perk) all keep working, and stylists in existing worlds become salon
 * owners on load.
 *
 * <h2>Prices</h2>
 *
 * {@code setStaticPriceBasedOnHappiness(best, worst, range)} — the same shape
 * vanilla uses for her clothes, so a happy settlement gets the discount. The
 * furniture sits far above the 75–150 of a shirt: this is a shop fit-out, not
 * an outfit.
 */
public class SalonStylistMob extends StylistHumanMob {

    public SalonStylistMob() {
        super();
        for (SalonWares.Ware ware : SalonWares.available()) {
            this.shop.addSellingItem(ware.id, new SellingShopItem())
                    .setStaticPriceBasedOnHappiness(ware.bestPrice, ware.worstPrice, ware.priceRange);
        }
    }
}
