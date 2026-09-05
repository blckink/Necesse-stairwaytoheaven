package stairwaytoheaven.mobs;

import java.util.HashMap;

import necesse.engine.registries.MobRegistry;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameTexture;

/**
 * The bestiary face for a mob that wears somebody else's body.
 *
 * <h2>The problem, and why it was not a missing PNG</h2>
 * Nineteen of this mod's creatures deliberately carry no art of their own: each
 * either subclasses a vanilla mob and inherits its whole draw, or blits a
 * vanilla sheet directly ({@code EdenRealm.loadTextures}). That is the working
 * method {@code AGENTS.md} lays down — build with vanilla stand-ins now, replace
 * them all in one pass later — and it is why {@code docs/VANILLA_ASSET_MAP.md}
 * exists.
 *
 * <p>The <b>bestiary</b> did not get the memo. {@code MobRegistry.loadMobIcons}
 * calls {@code GameTexture.fromFile("mobs/icons/" + stringID)} for <b>every</b>
 * registered mob (MobRegistry.java:950-953, :985-987, VERIFIED [jar]), and
 * {@code fromFile} falls back to {@code GameResources.error} when the file is
 * absent (GameTexture.java:170-172). So a mob that wears a crocodile in the
 * world was already showing the engine's ERR tile in the journal — and six of
 * them ship that way today, because Steinfeld's four and the Crooked Beyond's
 * door mimic and tongue plant are registered {@code countKillStat = true} and
 * therefore already have a journal row.
 *
 * <h2>Why this is the fix and not twelve new PNGs</h2>
 * <b>VERIFIED [jar]:</b> {@code Mob.getMobIcon()} (Mob.java:1760-1762) is a
 * plain overridable method whose default is
 * {@code MobRegistry.getMobIcon(this.getStringID())}, and the journal asks the
 * MOB, not the registry — {@code FormJournalEntryComponent.java:240} reads
 * {@code mob.getMobIcon()}. So a mob can simply answer with a different icon,
 * and the right answer is already loaded: the face of the creature whose body it
 * is wearing.
 *
 * <p>Drawing twelve new 32x32 icons would have been the <i>wrong</i> fix even
 * if it were cheap. A Drifter that walks around as a Deep Cave Spirit and shows
 * a hand-drawn something-else in the journal is two different creatures to the
 * player. When these mobs get their own bodies, they get their own faces in the
 * same pass, and every override that calls this class goes away with them.
 *
 * <h2>Failing safe</h2>
 * Everything here degrades to today's behaviour rather than to a crash: an
 * unknown parent ID, an unloaded registry (a dedicated server never loads a
 * texture at all) or a parent whose own icon is missing all return the caller's
 * {@code super.getMobIcon()}, which is exactly the ERR tile the player already
 * sees. Nothing gets worse; most things get better.
 */
public final class BorrowedMobIcon {

    private BorrowedMobIcon() {
    }

    /**
     * Resolved icons, by the parent's mob string ID.
     *
     * <p>{@code MobRegistry.getMobIcon} is a registry lookup plus an array
     * index, so this cache is not about speed — it is about asking the registry
     * once per parent rather than once per journal frame, and about having one
     * place to look when an icon is wrong.
     *
     * <p>Not synchronised: every caller is the client's UI thread drawing a
     * journal page. A dedicated server never reaches this class, because
     * nothing on it draws.
     */
    private static final HashMap<String, GameTexture> CACHE = new HashMap<>();

    /**
     * The icon of the vanilla creature this mob is wearing.
     *
     * @param vanillaMobStringID the mob whose body this one borrows — the class
     *     it subclasses, or the owner of the sheet it blits. Every one of them
     *     is written down in {@code docs/VANILLA_ASSET_MAP.md}.
     * @param fallback the caller's own {@code super.getMobIcon()}, used
     *     unchanged whenever the borrow cannot be resolved
     */
    public static GameTexture from(String vanillaMobStringID, GameTexture fallback) {
        GameTexture cached = CACHE.get(vanillaMobStringID);
        if (cached != null) {
            return cached;
        }
        GameTexture icon = resolve(vanillaMobStringID);
        if (icon == null) {
            return fallback;
        }
        CACHE.put(vanillaMobStringID, icon);
        return icon;
    }

    /**
     * Asks the registry, and answers null for every way that can go wrong.
     *
     * <p>{@code MobRegistry.getMobIcon(String)} returns {@code GameResources
     * .error} for an unknown ID (MobRegistry.java:919) and null for a mob whose
     * icons were never loaded, so both have to be treated as "no answer" rather
     * than as an icon — returning the ERR tile from here would cache it and
     * make the situation permanent for the session.
     */
    private static GameTexture resolve(String vanillaMobStringID) {
        try {
            if (!MobRegistry.mobExists(vanillaMobStringID)) {
                return null;
            }
            GameTexture icon = MobRegistry.getMobIcon(vanillaMobStringID);
            if (icon == null || icon == GameResources.error) {
                return null;
            }
            return icon;
        } catch (RuntimeException failed) {
            // The registry is not open, or this is a server. Either way the
            // caller's own icon is the honest answer.
            return null;
        }
    }
}
