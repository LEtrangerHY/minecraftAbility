package org.core.coreSystem.cores.VOL2.Undead.Skill.DeadlyWeapons;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Undead.coreSystem.Undead;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;

import java.util.HashSet;

public class R_scythe implements SkillBase {
    private final Undead config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final NamespacedKey keyR;

    private static final Particle.DustOptions DUST_IRON = new Particle.DustOptions(Color.fromRGB(160, 160, 160), 0.6f);
    private static final Particle.DustOptions DUST_MID = new Particle.DustOptions(Color.fromRGB(140, 140, 140), 0.6f);
    private static final Particle.DustOptions DUST_DECAY = new Particle.DustOptions(Color.fromRGB(100, 100, 100), 0.6f);

    private static final BlockData BLOOD_DATA = Material.REDSTONE_BLOCK.createBlockData();
    private static final Particle.DustOptions CELL_RED = new Particle.DustOptions(Color.fromRGB(180, 20, 20), 1.2f);

    public R_scythe(Undead config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.keyR = new NamespacedKey(plugin, "R");
    }

    @Override
    public void Trigger(Player player) {
        Material handMat = player.getInventory().getItemInMainHand().getType();
        String coolKey_re = config.getWeaponRCoolKey(handMat);
        String coolKey = config.getWeaponCoolKey(handMat);

        if (coolKey == null || coolKey_re == null) return;

        if (cool.isReloading(player, coolKey_re)) {
            cool.updateCooldown(player, coolKey_re, 0L, "boss");
            cool.setCooldown(player, config.r_Skill_Cool, coolKey);
            cool.setCooldown(player, config.r_Skill_Cool, "R");
            Reuse(player);
        } else {
            Effect(player);
            cool.setCooldown(player, config.r_Skill_Cool_re, coolKey_re, "boss");
            cool.setCooldown(player, 0L, coolKey);
            cool.setCooldown(player, 0L, "R");

            final long expireTime = System.currentTimeMillis() + config.r_Skill_Cool_re;

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || !player.isValid()) {
                        this.cancel();
                        return;
                    }

                    if (System.currentTimeMillis() >= expireTime) {
                        Material handMat_2 = player.getInventory().getItemInMainHand().getType();
                        if (handMat_2 == handMat) cool.setCooldown(player, config.r_Skill_Cool, "R");

                        cool.updateCooldown(player, coolKey_re, 0L, "boss");
                        cool.setCooldown(player, config.r_Skill_Cool, coolKey);

                        this.cancel();
                        return;
                    }

                    if (!cool.isReloading(player, coolKey_re)) {
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    public void Effect(Player player) {
        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyR, PersistentDataType.LONG, 0L);
        double damage = 2.0 * (1 + amp);

        final double maxHeal = 4.0 * (1 + amp);
        final double[] currentHealed = {0.0};

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Vector backDash = player.getLocation().getDirection().setY(0).normalize().multiply(-1.3);
        player.setVelocity(backDash);

        final Vector initialForward = player.getLocation().getDirection().setY(0).normalize();
        if (initialForward.lengthSquared() == 0) initialForward.setX(1);

        World world = player.getWorld();
        player.swingMainHand();
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.0f);
        world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.2f);

        HashSet<Entity> damagedSet = new HashSet<>();
        final int maxTicks = 8;
        final double slashLength = 4.0;
        final double innerRadius = 1.3;

        Location center = player.getLocation().add(0, 1.0, 0);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= maxTicks || player.isDead()) {
                    this.cancel();
                    return;
                }
                Vector forward = initialForward;

                double sweepPerTick = 360.0 / maxTicks;
                double startDeg = -90.0 + (ticks * sweepPerTick);
                double endDeg = -90.0 + ((ticks + 1) * sweepPerTick);

                double midDeg = -90.0 + (ticks * sweepPerTick) + (sweepPerTick / 2.0);
                Vector currentTickDir = forward.clone().rotateAroundY(Math.toRadians(midDeg));
                double hitThreshold = Math.cos(Math.toRadians((sweepPerTick / 2.0) + 20.0));

                int steps = (int) (sweepPerTick / 3.0);
                double range = slashLength - innerRadius;

                for (int i = 0; i <= steps; i++) {
                    double currentDeg = startDeg + (endDeg - startDeg) * ((double) i / steps);

                    for (double len = innerRadius; len <= slashLength; len += 0.06) {
                        Vector offset = forward.clone().rotateAroundY(Math.toRadians(currentDeg)).multiply(len);
                        Location pLoc = center.clone().add(offset);

                        Particle.DustOptions opt;
                        if (len < innerRadius + (range * 0.66)) opt = DUST_IRON;
                        else opt = DUST_MID;

                        opt = (Math.random() < 0.22) ? DUST_DECAY : opt;
                        world.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, opt);
                    }
                }

                for (Entity entity : world.getNearbyEntities(center, slashLength, 1.3, slashLength)) {
                    if (!(entity instanceof LivingEntity target) || entity == player || damagedSet.contains(target)) continue;

                    Vector toTarget = target.getLocation().toVector().subtract(center.toVector());
                    toTarget.setY(0);

                    if (toTarget.lengthSquared() > (slashLength + 1.0) * (slashLength + 1.0)) continue;

                    boolean isHit = false;
                    if (toTarget.lengthSquared() < 0.25) {
                        isHit = true;
                    } else {
                        Vector toTargetDir = toTarget.normalize();
                        if (currentTickDir.dot(toTargetDir) > hitThreshold) {
                            isHit = true;
                        }
                    }

                    if (isHit) {
                        new ForceDamage(target, damage, source, true).applyEffect(player);
                        damagedSet.add(target);

                        if (currentHealed[0] < maxHeal) {
                            double healAmount = damage * 0.4;
                            double actualHeal = Math.min(healAmount, maxHeal - currentHealed[0]);
                            applyLifesteal(player, actualHeal);
                            currentHealed[0] += actualHeal;
                        }

                        world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.1f);
                        world.spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1.0, 0), 15, 0.4, 0.4, 0.4, 0.1);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void Reuse(Player player) {
        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyR, PersistentDataType.LONG, 0L);
        double damage = 4.0 * (1 + amp);

        final double maxHeal = 4.0 * (1 + amp);
        final double[] currentHealed = {0.0};

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Invulnerable invulnerable = new Invulnerable(player, 400);
        invulnerable.applyEffect(player);

        Vector forwardDash = player.getLocation().getDirection().setY(0).normalize().multiply(1.6);
        player.setVelocity(forwardDash);

        final Vector initialForward = player.getLocation().getDirection().setY(0).normalize();
        if (initialForward.lengthSquared() == 0) initialForward.setX(1);

        World world = player.getWorld();
        player.swingMainHand();
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.4f, 0.8f);
        world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.9f);

        HashSet<Entity> damagedSet = new HashSet<>();
        final int maxTicks = 8;
        final double slashLength = 4.0;
        final double innerRadius = 1.3;

        Location center = player.getLocation().add(0, 1.0, 0);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= maxTicks || player.isDead()) {
                    this.cancel();
                    return;
                }

                Vector forward = initialForward;

                double sweepPerTick = 360.0 / maxTicks;
                double startDeg = 90.0 - (ticks * sweepPerTick);
                double endDeg = 90.0 - ((ticks + 1) * sweepPerTick);

                double midDeg = 90.0 - (ticks * sweepPerTick) - (sweepPerTick / 2.0);
                Vector currentTickDir = forward.clone().rotateAroundY(Math.toRadians(midDeg));
                double hitThreshold = Math.cos(Math.toRadians((sweepPerTick / 2.0) + 20.0));

                int steps = (int) (sweepPerTick / 3.0);
                double range = slashLength - innerRadius;

                for (int i = 0; i <= steps; i++) {
                    double currentDeg = startDeg + (endDeg - startDeg) * ((double) i / steps);

                    for (double len = innerRadius; len <= slashLength; len += 0.06) {
                        Vector offset = forward.clone().rotateAroundY(Math.toRadians(currentDeg)).multiply(len);
                        Location pLoc = center.clone().add(offset);

                        Particle.DustOptions opt;
                        if (len < innerRadius + (range * 0.66)) opt = DUST_IRON;
                        else opt = DUST_MID;

                        opt = (Math.random() < 0.22) ? DUST_DECAY : opt;
                        world.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, opt);
                    }

                    if (Math.random() < 0.1) {
                        Vector sweepOffset = forward.clone().rotateAroundY(Math.toRadians(currentDeg)).multiply(slashLength);
                        world.spawnParticle(Particle.SWEEP_ATTACK, center.clone().add(sweepOffset), 1, 0, 0, 0, 0);
                    }
                }

                for (Entity entity : world.getNearbyEntities(center, slashLength, 1.3, slashLength)) {
                    if (!(entity instanceof LivingEntity target) || entity == player || damagedSet.contains(target)) continue;

                    Vector toTarget = target.getLocation().toVector().subtract(center.toVector());
                    toTarget.setY(0);

                    if (toTarget.lengthSquared() > (slashLength + 1.0) * (slashLength + 1.0)) continue;

                    boolean isHit = false;
                    if (toTarget.lengthSquared() < 0.25) {
                        isHit = true;
                    } else {
                        Vector toTargetDir = toTarget.normalize();
                        if (currentTickDir.dot(toTargetDir) > hitThreshold) {
                            isHit = true;
                        }
                    }

                    if (isHit) {
                        new ForceDamage(target, damage, source, true).applyEffect(player);
                        damagedSet.add(target);

                        if (currentHealed[0] < maxHeal) {
                            double healAmount = damage * 0.4;
                            double actualHeal = Math.min(healAmount, maxHeal - currentHealed[0]);
                            applyLifesteal(player, actualHeal);
                            currentHealed[0] += actualHeal;
                        }

                        world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.1f);
                        world.spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1.0, 0), 15, 0.4, 0.4, 0.4, 0.1);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void applyLifesteal(Player player, double healAmount) {
        if (player.isDead() || !player.isValid()) return;

        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double maxHealth = maxHealthAttr.getValue();
            player.setHealth(Math.min(maxHealth, player.getHealth() + healAmount));

            Location centerLoc = player.getLocation().add(0, 1.0, 0);
            World world = player.getWorld();

            world.spawnParticle(Particle.BLOCK, centerLoc, 6, 0.4, 0.5, 0.4, 0, BLOOD_DATA);
            world.spawnParticle(Particle.DUST, centerLoc, 4, 0.4, 0.5, 0.4, 0, CELL_RED);

            world.playSound(centerLoc, Sound.BLOCK_SLIME_BLOCK_STEP, 0.35f, 1.2f);
        }
    }
}