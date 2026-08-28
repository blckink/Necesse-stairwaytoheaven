package stairwaytoheaven.items;

import necesse.engine.localization.Localization;

/**
 * The one line under an item's name that says what the thing IS.
 *
 * <p>Necesse writes no such line by itself. {@code Item.getBaseTooltips}
 * (jar 1.3.2, Item.java:189) is exactly three blocks — the display name, the
 * debug block, and {@code getCraftingMatTooltips} — and the last one only ever
 * yields the generic "used as a crafting material" sentence carried by the
 * {@code Tech}s and global ingredients that consume the item. A material that
 * nothing consumes therefore says nothing at all, and a material that IS
 * consumed says only that it is a material. Neither tells the player whether
 * he is holding an ore, a petal, a bolt of cloth or a bar.
 *
 * <p>Vanilla's own convention for a per-item sentence is the
 * {@code [itemtooltip]} section keyed {@code <stringID>tip} — {@code
 * surgicalmasktip}, {@code voidshardtip}, {@code glassbottletip} and about
 * eighty others. This class resolves that key, and it is the ONLY place in the
 * mod that decides what "the entry is missing" means.
 *
 * <h2>Why the missing-key test is a string comparison</h2>
 *
 * <p>{@code Localization.translate} never returns null and never signals a
 * miss. {@code Localization.getTranslation} (Localization.java) falls through
 * to {@code new DebugTranslationElement(category, key)}, whose translation is
 * literally {@code category + "." + key} — so an item with no entry would show
 * the raw string {@code itemtooltip.skystonetip} in the player's inventory,
 * which is precisely the failure this mod has already shipped three times for
 * other keys (see tools/locale_audit.py).
 *
 * <p>The obvious guard, {@code Localization.getCurrentLang().translationExists},
 * is NOT usable: {@code Translation.exists} (Translation.java:216) is
 * {@code cat != null ? cat.exists(key) : true} — it answers <em>true</em> when
 * the whole category is absent, because it exists for translation-coverage
 * tooling rather than for lookups. Comparing against the debug element's own
 * text is exact, needs no state, and cannot be fooled by a category that has
 * not been created yet.
 *
 * <p>The lookup also passes {@code debug = false}, the three-argument overload,
 * so a deliberately description-less item does not spam
 * "Translation of itemtooltip.x is not found." into the log once every ten
 * seconds ({@code Localization.warn}).
 */
public final class ItemDescription {

    /** The section vanilla keeps per-item sentences in. */
    public static final String SECTION = "itemtooltip";

    private ItemDescription() {
    }

    /** The locale key an item's description lives under: {@code <stringID>tip}. */
    public static String key(String stringID) {
        return stringID + "tip";
    }

    /**
     * The description line for an item, or {@code null} when the locale has no
     * entry for it. Never returns the raw key.
     */
    public static String of(String stringID) {
        return byKey(key(stringID));
    }

    /** As {@link #of(String)}, for a key that is not derived from a string ID. */
    public static String byKey(String key) {
        String line = Localization.translate(SECTION, key, false);
        if (line == null || line.isEmpty() || line.equals(SECTION + "." + key)) {
            return null;
        }
        return line;
    }
}
