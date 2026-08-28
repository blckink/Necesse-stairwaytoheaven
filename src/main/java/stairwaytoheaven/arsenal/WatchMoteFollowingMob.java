package stairwaytoheaven.arsenal;

import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.CryoFlakeFollowingMob;

/**
 * Watch Mote — the companion the Skywatch Whistle calls up.
 *
 * <p>A shard of the same rime machinery the Rime Sentries are made of, which is
 * why it wears vanilla's own {@code MobRegistry.Textures.cryoFlakePet}: this
 * class subclasses {@link CryoFlakeFollowingMob} and does not override
 * {@code addDrawables}, so the drawing, the {@code PlayerFlyingFollowerShooterChaserAI}
 * that circles the owner and shoots at range 576, and the
 * {@code CryoMissileProjectile} it fires all come from the vanilla class
 * unchanged. Its damage is not set here at all — {@code SummonToolItem.summonServerMob}
 * calls {@code updateDamage(getAttackDamage(item))}, so the WEAPON owns the
 * number and enchantments and upgrades reach the mote for free.
 *
 * <p>Registered with {@code countKillStat = false}: a summon is not something
 * the player kills.
 */
public class WatchMoteFollowingMob extends CryoFlakeFollowingMob {

    /** Required: MobRegistry instantiates reflectively with no args. */
    public WatchMoteFollowingMob() {
        super();
    }
}
