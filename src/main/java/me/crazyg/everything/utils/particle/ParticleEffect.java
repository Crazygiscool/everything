package me.crazyg.everything.utils.particle;

import org.bukkit.Particle;

public enum ParticleEffect {
    TELEPORT_WARMUP(Particle.PORTAL, 3, 0.3, 1.0),
    TELEPORT_ARRIVE(Particle.END_ROD, 20, 0.3, 1.0),
    TELEPORT_CANCEL(Particle.SMOKE_NORMAL, 10, 0.2, 1.0),

    TP(Particle.END_ROD, 15, 0.2, 1.0),
    TPA_SEND(Particle.SPELL_WITCH, 8, 0.2, 1.0),
    TPA_ACCEPT(Particle.END_ROD, 15, 0.2, 1.0),
    TP_DENY(Particle.SMOKE_NORMAL, 8, 0.2, 1.0),

    HOME(Particle.DRIP_WATER, 12, 0.3, 1.0),
    HOME_SET(Particle.DRIP_WATER, 20, 0.4, 1.0),
    SPAWN(Particle.FIREWORKS_SPARK, 25, 0.4, 1.0),
    SPAWN_SET(Particle.FIREWORKS_SPARK, 30, 0.5, 1.0),
    WARP(Particle.REVERSE_PORTAL, 18, 0.3, 1.0),
    WARP_SET(Particle.REVERSE_PORTAL, 25, 0.4, 1.0),
    RTP(Particle.CAMPFIRE_COSY_SMOKE, 20, 0.5, 1.0),

    PAY_SEND(Particle.VILLAGER_HAPPY, 12, 0.3, 1.0),
    PAY_RECEIVE(Particle.VILLAGER_HAPPY, 15, 0.3, 1.0),
    BALANCE(Particle.CRIT, 8, 0.2, 1.0),
    ECO_GIVE(Particle.VILLAGER_HAPPY, 15, 0.3, 1.0),
    ECO_TAKE(Particle.LAVA, 12, 0.3, 1.0),
    ECO_SET(Particle.ENCHANTMENT_TABLE, 15, 0.3, 1.0),

    GAMEMODE_CREATIVE(Particle.CLOUD, 15, 0.3, 1.0),
    GAMEMODE_SURVIVAL(Particle.HEART, 12, 0.2, 1.0),
    GAMEMODE_SPECTATOR(Particle.SOUL_FIRE_FLAME, 15, 0.3, 1.0),
    GAMEMODE_ADVENTURE(Particle.CRIT_MAGIC, 10, 0.2, 1.0),

    GOD_ENABLE(Particle.ENCHANTMENT_TABLE, 25, 0.5, 1.0),
    GOD_DISABLE(Particle.CAMPFIRE_COSY_SMOKE, 15, 0.3, 1.0),

    KILL(Particle.EXPLOSION_NORMAL, 30, 0.5, 1.0),

    MSG_SENT(Particle.HEART, 5, 0.1, 1.0),
    MSG_RECEIVE(Particle.HEART, 5, 0.1, 1.0),

    REPORT(Particle.REDSTONE, 20, 0.4, 1.0),
    NAME_COLOR(Particle.REDSTONE, 25, 0.4, 1.0),
    STATS(Particle.END_ROD, 8, 0.2, 1.0),

    MAINTENANCE_ON(Particle.MOB_APPEARANCE, 30, 0.6, 1.0),
    MAINTENANCE_OFF(Particle.CLOUD, 20, 0.4, 1.0),

    EVERYTHING_RELOAD(Particle.ENCHANTMENT_TABLE, 15, 0.3, 1.0),
    EVERYTHING_INFO(Particle.END_ROD, 8, 0.2, 1.0),
    EVERYTHING_CHECKUPDATE(Particle.CRIT, 8, 0.2, 1.0),
    EVERYTHING_TEST(Particle.TOTEM, 40, 0.6, 1.0);

    private final Particle particle;
    private final int count;
    private final double spread;
    private final double speed;

    ParticleEffect(Particle particle, int count, double spread, double speed) {
        this.particle = particle;
        this.count = count;
        this.spread = spread;
        this.speed = speed;
    }

    public Particle getParticle() {
        return particle;
    }

    public int getCount() {
        return count;
    }

    public double getSpread() {
        return spread;
    }

    public double getSpeed() {
        return speed;
    }
}
