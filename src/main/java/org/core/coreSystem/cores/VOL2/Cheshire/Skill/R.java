package org.core.coreSystem.cores.VOL2.Cheshire.Skill;

import org.core.effect.effectPlugin.swingEchoGuard;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Cheshire.coreSystem.Cheshire;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

public class R implements SkillBase {

    private final Cheshire config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private static final Particle.DustOptions DUST_SCYTHE = new Particle.DustOptions(Color.fromRGB(138, 43, 226), 1.5f);
    private static final Particle.DustOptions DUST_CLAW = new Particle.DustOptions(Color.fromRGB(128, 0, 128), 1.2f);

    public R(Cheshire config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();

        NamespacedKey rKey = new NamespacedKey(plugin, "R");
        double rAmp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(rKey, PersistentDataType.LONG, 0L);
        double scaledDamage = config.r_Skill_damage * (1.0 + rAmp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        swingEchoGuard.swingMainHand(player);

        if (config.invisState.contains(uuid)) {
            config.invisState.remove(uuid);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);

            Vector initialForward = player.getLocation().getDirection().setY(0.0).normalize();
            if (initialForward.lengthSquared() == 0) initialForward.setX(1.0);

            player.setVelocity(initialForward.clone().multiply(1.6));

            Invulnerable invuln = new Invulnerable(player, 600L);
            invuln.applyEffect(player);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.5f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.5f);

            Location center = player.getLocation().add(0.0, 1.0, 0.0);
            HashSet<Entity> damagedSet = new HashSet<>();
            final int maxTicks = 8;
            final double slashLength = 4.0;
            final double innerRadius = 1.0;

            new BukkitRunnable() {
                int ticks = 0;
                boolean hitAny = false;

                @Override
                public void run() {
                    if (ticks >= maxTicks || player.isDead()) {
                        if (hitAny) {
                            long remaining = cool.getRemainCooldown(player, "PASSIVE");
                            if (remaining > 0) {
                                long reducedCooldown = remaining * 2 / 3;
                                cool.updateCooldown(player, "PASSIVE", reducedCooldown, "boss");
                            }
                        }
                        this.cancel();
                        return;
                    }

                    double sweepPerTick = 360.0 / maxTicks;
                    double startDeg = -90.0 + (ticks * sweepPerTick);
                    double endDeg = -90.0 + ((ticks + 1) * sweepPerTick);

                    int steps = (int) (sweepPerTick / 4.0);

                    for (int i = 0; i <= steps; i++) {
                        double currentDeg = startDeg + (endDeg - startDeg) * ((double) i / steps);
                        for (double len = innerRadius; len <= slashLength; len += 0.2) {
                            Vector offset = initialForward.clone().rotateAroundY(Math.toRadians(currentDeg)).multiply(len);
                            Location pLoc = center.clone().add(offset);
                            player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0.0, 0.0, 0.0, 0.0, DUST_SCYTHE);
                        }
                        Vector edgeOffset = initialForward.clone().rotateAroundY(Math.toRadians(currentDeg)).multiply(slashLength);
                        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(edgeOffset), 1, 0.0, 0.0, 0.0, 0.05);
                    }

                    for (Entity entity : player.getNearbyEntities(slashLength, 2.0, slashLength)) {
                        if (entity instanceof LivingEntity target && target != player && !damagedSet.contains(target)) {

                            new ForceDamage(target, scaledDamage, source, true).applyEffect(player);

                            damagedSet.add(target);
                            hitAny = true;

                            if (config.fUltBuffActive.getOrDefault(uuid, false)) {
                                int currentCount = config.fUltSmileCounts.getOrDefault(uuid, 0);
                                if (currentCount < 2) {
                                    createActiveSmile(player, target.getEyeLocation());
                                    config.fUltSmileCounts.put(uuid, currentCount + 1);
                                }
                            }
                        }
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);

        } else {
            Vector dir = player.getLocation().getDirection().setY(0.0).normalize();
            Vector right = new Vector(-dir.getZ(), 0.0, dir.getX()).normalize();
            Vector up = new Vector(0.0, 1.0, 0.0);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.3f);

            boolean l2r = config.rSwingLeftToRight.getOrDefault(uuid, true);
            config.rSwingLeftToRight.put(uuid, !l2r);

            Location center = player.getEyeLocation().add(dir.clone().multiply(1.5));

            new BukkitRunnable() {
                int tick = 0;
                @Override
                public void run() {
                    if (tick >= 4 || player.isDead()) {
                        this.cancel();
                        return;
                    }

                    double startY = 1.2;
                    double endY = -1.2;
                    double stepY = (endY - startY) / 4.0;

                    double y0 = startY + stepY * tick;
                    double y1 = startY + stepY * (tick + 1);

                    double minY = Math.min(y0, y1);
                    double maxY = Math.max(y0, y1);

                    double angle = Math.toRadians(l2r ? 30 : -30);

                    for (double y = minY; y <= maxY; y += 0.05) {
                        double ratio = y / 1.2;

                        double x = l2r ? (-ratio * 1.5) : (ratio * 1.5);
                        double arcZ = (1.0 - ratio * ratio) * 0.5;

                        double rotX = x * Math.cos(angle) - y * Math.sin(angle);
                        double rotY = x * Math.sin(angle) + y * Math.cos(angle);

                        for (int claw = -1; claw <= 1; claw++) {
                            double offsetX = -Math.sin(angle) * 0.4 * claw;
                            double offsetY = Math.cos(angle) * 0.4 * claw;

                            double cx = rotX + offsetX;
                            double cy = rotY + offsetY;
                            double cz = arcZ;

                            Vector point = right.clone().multiply(cx).add(up.clone().multiply(cy)).add(dir.clone().multiply(cz));
                            Location drawLoc = center.clone().add(point);
                            player.getWorld().spawnParticle(Particle.DUST, drawLoc, 1, 0.0, 0.0, 0.0, 0.0, DUST_CLAW);
                        }
                    }
                    tick++;
                }
            }.runTaskTimer(plugin, 0L, 1L);

            for (Entity entity : player.getNearbyEntities(3.0, 2.0, 3.0)) {
                if (entity instanceof LivingEntity target && target != player) {
                    Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0.0).normalize();
                    if (dir.dot(toTarget) > 0.5) {

                        new ForceDamage(target, scaledDamage, source, true).applyEffect(player);

                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.5f);
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.8f, 2.0f);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);

                        if (config.fUltBuffActive.getOrDefault(uuid, false)) {
                            int currentCount = config.fUltSmileCounts.getOrDefault(uuid, 0);
                            if (currentCount < 2) {
                                createActiveSmile(player, target.getEyeLocation());
                                config.fUltSmileCounts.put(uuid, currentCount + 1);
                            }
                        }
                    }
                }
            }
        }
    }

    private void createActiveSmile(Player player, Location targetLoc) {
        UUID uuid = player.getUniqueId();
        Location smileLoc = targetLoc.clone();
        Vector dir = player.getLocation().getDirection().setY(0.0).normalize();

        config.activeSmiles.putIfAbsent(uuid, new ArrayList<>());
        config.activeSmiles.get(uuid).add(smileLoc);

        BukkitRunnable runnable = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= 60 || player.isDead() || !player.isOnline() ||
                        !config.activeSmiles.containsKey(uuid) || !config.activeSmiles.get(uuid).contains(smileLoc)) {

                    if (config.activeSmiles.containsKey(uuid)) config.activeSmiles.get(uuid).remove(smileLoc);
                    if (config.activeSmileTasks.containsKey(uuid)) config.activeSmileTasks.get(uuid).remove((Integer) this.getTaskId());

                    this.cancel();
                    return;
                }

                smileLoc.getWorld().spawnParticle(Particle.WITCH, smileLoc, 5, 0.3, 0.3, 0.3, 0.02);
                smileLoc.getWorld().spawnParticle(Particle.PORTAL, smileLoc, 10, 0.5, 0.5, 0.5, 0.1);

                drawActiveSmileShape(smileLoc, dir, tick);
                tick++;
            }
        };

        int task = runnable.runTaskTimer(plugin, 0L, 2L).getTaskId();

        config.activeSmileTasks.putIfAbsent(uuid, new ArrayList<>());
        config.activeSmileTasks.get(uuid).add(task);
    }

    private void drawActiveSmileShape(Location loc, Vector dir, int tick) {
        Vector right = new Vector(-dir.getZ(), 0.0, dir.getX());
        if (right.lengthSquared() < 0.001) right = new Vector(1.0, 0.0, 0.0);
        else right.normalize();

        Vector up = new Vector(0.0, 1.0, 0.0);

        float scale = 1.1f + (float) Math.sin(tick * 0.3) * 0.2f;

        Particle.DustOptions smileDust = new Particle.DustOptions(Color.PURPLE, scale);
        Particle.DustOptions eyeDust = new Particle.DustOptions(Color.FUCHSIA, scale * 1.2f);

        for(double t = -1.0; t <= 1.0; t += 0.05) {
            double x = t * (0.6 * scale);
            double y = (t * t) * (0.4 * scale) - (0.25 * scale);

            Vector point = right.clone().multiply(x).add(up.clone().multiply(y));
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(point), 1, 0.0, 0.0, 0.0, 0.0, smileDust);
        }

        Location leftEye = loc.clone().add(right.clone().multiply(-0.35 * scale)).add(up.clone().multiply(0.3 * scale));
        Location rightEye = loc.clone().add(right.clone().multiply(0.35 * scale)).add(up.clone().multiply(0.3 * scale));

        loc.getWorld().spawnParticle(Particle.DUST, leftEye, 2, 0.0, 0.0, 0.0, 0.0, eyeDust);
        loc.getWorld().spawnParticle(Particle.DUST, rightEye, 2, 0.0, 0.0, 0.0, 0.0, eyeDust);
    }
}