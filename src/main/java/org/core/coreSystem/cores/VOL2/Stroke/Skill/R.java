package org.core.coreSystem.cores.VOL2.Stroke.Skill;

import org.core.effect.effectPlugin.swingEchoGuard;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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
import org.core.coreSystem.cores.VOL2.Stroke.coreSystem.Stroke;
import org.core.effect.crowdControl.ForceDamage;

import java.util.HashSet;
import java.util.UUID;

public class R implements SkillBase {
    private final Stroke config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private static final Particle.DustOptions BAT_TRAIL_CORE = new Particle.DustOptions(Color.fromRGB(20, 20, 20), 2.5f);
    private static final Particle.DustOptions BAT_TRAIL_EDGE = new Particle.DustOptions(Color.fromRGB(150, 150, 150), 1.5f);
    private static final Particle.DustOptions BAT_TRAIL_WHITE = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);

    public R(Stroke config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();

        if (config.isFActive.getOrDefault(uuid, false)) {
            Vector dashVec = player.getLocation().getDirection().setY(0.0).normalize().multiply(0.7).setY(0.1);
            player.setVelocity(dashVec);

            new BukkitRunnable() {
                @Override
                public void run() {
                    executeSwing(player, 0.7);
                }
            }.runTaskLater(plugin, 3L);
            return;
        }

        config.rIsCharging.put(uuid, true);
        config.rLastClickTime.put(uuid, System.currentTimeMillis());

        BossBar chargeBar = Bukkit.createBossBar("STRIKE CHARGING...", BarColor.WHITE, BarStyle.SOLID);
        chargeBar.setProgress(0.0);
        chargeBar.addPlayer(player);
        config.rChargeBars.put(uuid, chargeBar);

        new BukkitRunnable() {
            int activeTicks = 0;
            int chargeTicks = 0;
            final int maxTicks = 30;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || player.getInventory().getItemInMainHand().getType() != Material.STICK) {
                    cancelCharge(uuid);
                    this.cancel();
                    return;
                }

                long lastClick = config.rLastClickTime.getOrDefault(uuid, 0L);

                if (System.currentTimeMillis() - lastClick > 250) {
                    executeSwing(player, (double) chargeTicks / maxTicks);
                    cancelCharge(uuid);
                    this.cancel();
                    return;
                }

                if (chargeTicks < maxTicks) {
                    chargeTicks++;
                    chargeBar.setProgress((double) chargeTicks / maxTicks);

                    if (chargeTicks % 5 == 0) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 0.5f + (chargeTicks / 30f));
                    }
                } else {
                    double rad = activeTicks * 0.2;
                    Location center = player.getLocation().add(0, 1.0, 0);

                    Vector offset1 = new Vector(Math.cos(rad) * 1.2, Math.sin(rad * 0.5) * 0.4, Math.sin(rad) * 1.2);
                    player.getWorld().spawnParticle(Particle.SMOKE, center.clone().add(offset1), 1, 0, 0.05, 0, 0);

                    Vector offset2 = new Vector(Math.cos(rad + Math.PI) * 1.2, Math.sin((rad + Math.PI) * 0.5) * 0.4, Math.sin(rad + Math.PI) * 1.2);
                    player.getWorld().spawnParticle(Particle.SMOKE, center.clone().add(offset2), 1, 0, 0.05, 0, 0);
                }

                activeTicks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void cancelCharge(UUID uuid) {
        config.rIsCharging.put(uuid, false);
        if (config.rChargeBars.containsKey(uuid)) {
            config.rChargeBars.get(uuid).removeAll();
            config.rChargeBars.remove(uuid);
        }
    }

    private void executeSwing(Player player, double chargeRatio) {
        swingEchoGuard.swingMainHand(player);
        cool.setCooldown(player, config.r_Skill_Cool, "R");

        NamespacedKey rKey = new NamespacedKey(plugin, "R");
        double rAmp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(rKey, PersistentDataType.LONG, 0L);

        double multiplier = 0.5 + (2.5 * chargeRatio);
        double finalDamage = config.r_Skill_damage * (1.0 + rAmp) * multiplier;

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Vector direction = player.getLocation().getDirection().normalize();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.6f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 0.5f);

        drawBatSwingParticle(player, direction, chargeRatio);

        for (Entity entity : player.getNearbyEntities(4.5, 2.5, 4.5)) {
            if (entity instanceof LivingEntity target && target != player) {
                Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();

                if (direction.dot(toTarget) > 0.4) {

                    target.setVelocity(new Vector(0, 0, 0));
                    new ForceDamage(target, finalDamage, source, true).applyEffect(player);

                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.9f);

                    if (chargeRatio >= 0.7) {
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.7f, 1.0f);
                    }

                    target.getWorld().spawnParticle(Particle.ASH, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

                    Vector pushVector = direction.clone().multiply(0.7 + chargeRatio * 1.4).setY(0.2 + (0.4 * chargeRatio));

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!target.isDead()) {
                                target.setVelocity(pushVector);

                                target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.2, Bukkit.createBlockData(Material.BONE_BLOCK));
                                target.getWorld().spawnParticle(Particle.WHITE_ASH, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

                                runProjectileBowling(target, finalDamage, player);
                            }
                        }
                    }.runTaskLater(plugin, 3L);
                }
            }
        }
    }

    private void drawBatSwingParticle(Player player, Vector direction, double chargeRatio) {
        Location origin = player.getEyeLocation().subtract(0, 0.2, 0);
        Vector flatDir = direction.clone().setY(0).normalize();

        double maxAngle = Math.toRadians(45 + (15 * chargeRatio));
        int maxTicks = 4;
        double swingLength = 3.5 + chargeRatio;
        double thickness = 0.6;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= maxTicks || player.isDead()) {
                    this.cancel();
                    return;
                }

                double startAngle = -maxAngle;
                double endAngle = maxAngle;

                double currentStartAngle = startAngle + (endAngle - startAngle) * ((double) ticks / maxTicks);
                double currentEndAngle = startAngle + (endAngle - startAngle) * ((double) (ticks + 1) / maxTicks);

                for (double angle = currentStartAngle; angle <= currentEndAngle; angle += Math.toRadians(3)) {
                    Vector rotatedDir = flatDir.clone().rotateAroundY(angle);
                    double yOffset = (angle / maxAngle) * 1.5;

                    for (double r = swingLength - thickness; r <= swingLength + (thickness / 2); r += 0.15) {
                        Vector point = rotatedDir.clone().multiply(r).setY(yOffset);
                        Location drawLoc = origin.clone().add(point);

                        Particle.DustOptions opt;
                        if (r < swingLength - (thickness * 0.5)) opt = BAT_TRAIL_WHITE;
                        else if (r < swingLength) opt = BAT_TRAIL_EDGE;
                        else opt = BAT_TRAIL_CORE;

                        player.getWorld().spawnParticle(Particle.DUST, drawLoc, 1, 0.05, 0.05, 0.05, 0, opt);
                    }

                    if (Math.random() > 0.3) {
                        Vector edgePoint = rotatedDir.clone().multiply(swingLength + 0.5).setY(yOffset);
                        player.getWorld().spawnParticle(Particle.ASH, origin.clone().add(edgePoint), 2, 0.1, 0.1, 0.1, 0.05);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void runProjectileBowling(LivingEntity projectileTarget, double damage, Player shooter) {
        HashSet<UUID> alreadyHit = new HashSet<>();

        new BukkitRunnable() {
            int flyingTicks = 0;
            @Override
            public void run() {
                if (flyingTicks > 10 || projectileTarget.isDead() || projectileTarget.isOnGround()) {
                    this.cancel();
                    return;
                }

                if (projectileTarget.getVelocity().lengthSquared() > 0.4) {
                    projectileTarget.getWorld().spawnParticle(Particle.WHITE_ASH, projectileTarget.getLocation(), 3, 0.3, 0.3, 0.3, 0.0);

                    for (Entity e : projectileTarget.getNearbyEntities(1.8, 1.8, 1.8)) {
                        if (e instanceof LivingEntity collateral && collateral != shooter && collateral != projectileTarget) {
                            if (alreadyHit.contains(collateral.getUniqueId())) continue;
                            alreadyHit.add(collateral.getUniqueId());

                            collateral.setVelocity(projectileTarget.getVelocity().multiply(0.6));
                            collateral.damage(damage * 0.5, shooter);
                            collateral.getWorld().playSound(collateral.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.3f, 0.7f);
                            collateral.getWorld().spawnParticle(Particle.BLOCK, collateral.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1, Bukkit.createBlockData(Material.BONE_BLOCK));
                        }
                    }
                }
                flyingTicks++;
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }
}