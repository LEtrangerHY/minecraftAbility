package org.core.coreSystem.cores.VOL1.Swordsman.Skill;

import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.cores.VOL1.Swordsman.Passive.Iaido;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;
import org.core.effect.crowdControl.Stun;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Swordsman.coreSystem.Swordsman;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class R implements SkillBase {
    private final Swordsman config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Iaido laido;
    private final NamespacedKey keyR;

    private static final Particle.DustOptions DUST_RAPID = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.5f);

    public R(Swordsman config, JavaPlugin plugin, Cool cool, Iaido laido) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.laido = laido;
        this.keyR = new NamespacedKey(plugin, "R");
    }

    @Override
    public void Trigger(Player player) {
        World world = player.getWorld();
        player.swingMainHand();

        Location startLocation = player.getLocation();
        Vector direction = startLocation.getDirection().normalize().multiply(config.r_Skill_dash);

        player.setVelocity(direction);
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        Invulnerable invulnerable = new Invulnerable(player, 550);
        invulnerable.applyEffect(player);

        boolean isLaidoActive = config.laidoSlash.getOrDefault(player.getUniqueId(), false);

        if (!isLaidoActive) {
            Rapid(player);

            int count = config.r_Skill_count.getOrDefault(player.getUniqueId(), 0);
            config.r_Skill_count.put(player.getUniqueId(), count + 1);

            if (config.r_Skill_count.getOrDefault(player.getUniqueId(), 0) >= 2) {
                cool.updateCooldown(player, "R", 8900L);
                config.r_Skill_count.remove(player.getUniqueId());
            } else {
                cool.updateCooldown(player, "R", 550L);
            }
        } else {
            int count = config.r_Skill_count.getOrDefault(player.getUniqueId(), 0);
            Quick(player, count);

            config.r_Skill_count.put(player.getUniqueId(), count + 2);

            cool.updateCooldown(player, "R", 10000L);
            config.r_Skill_count.remove(player.getUniqueId());

            laido.Draw(player);
        }
    }

    public void Rapid(Player player) {
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        config.r_skillUsing.put(uuid, true);

        HashSet<Entity> damagedSet = new HashSet<>();
        config.r_damaged.put(uuid, damagedSet);

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyR, PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 5 || player.isDead()) {
                    config.r_skillUsing.remove(uuid);
                    config.r_damaged.remove(uuid);
                    cancel();
                    return;
                }

                world.spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 100, 0.3, 0, 0.3, 0.08, DUST_RAPID);

                for (Entity entity : player.getNearbyEntities(0.5, 0.5, 0.5)) {
                    if (entity instanceof LivingEntity target && entity != player) {
                        if (!damagedSet.contains(target)) {
                            ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                            forceDamage.applyEffect(player);

                            damagedSet.add(target);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1L);
    }

    public void Quick(Player player, int count) {
        World world = player.getWorld();
        Location firstLoc = player.getLocation();
        UUID uuid = player.getUniqueId();

        config.q_skillUsing.put(uuid, true);

        HashSet<Entity> damagedSet = new HashSet<>();
        config.r_damaged.put(uuid, damagedSet);

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyR, PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks > 5 || player.isDead()) {
                    config.q_skillUsing.remove(uuid);
                    config.r_damaged.remove(uuid);

                    if (!player.isDead()) {
                        Draw(player, firstLoc, player.getLocation(), count);
                    }
                    cancel();
                    return;
                }

                world.spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 100, 0.3, 0, 0.3, 0.08, DUST_RAPID);

                for (Entity entity : player.getNearbyEntities(0.5, 0.5, 0.5)) {
                    if (entity instanceof LivingEntity target && entity != player) {
                        if (!damagedSet.contains(target)) {
                            Stun stun = new Stun(target, config.r_Skill_stun);
                            stun.applyEffect(player);

                            ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                            forceDamage.applyEffect(player);

                            damagedSet.add(target);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void Draw(Player player, Location first, Location second, int count) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World world = player.getWorld();
            UUID uuid = player.getUniqueId();

            HashSet<Entity> damagedSet = new HashSet<>();
            config.r_damaged_2.put(uuid, damagedSet);

            Location playSoundLoc = second.clone().add(0, 1.0, 0);
            world.playSound(playSoundLoc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);
            world.playSound(playSoundLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

            boolean isFirst = (count < 1);
            double additional_damage = (isFirst) ? config.r_Skill_add_damage : config.r_Skill_add_damage / 3;
            double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyR, PersistentDataType.LONG, 0L);
            double finalDamage = additional_damage * (1 + amp);

            DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                    .withCausingEntity(player)
                    .withDirectEntity(player)
                    .build();

            BoundingBox box = BoundingBox.of(first, second).expand(1.0);

            Vector startVec = first.toVector();
            Vector endVec = second.toVector();
            Vector pathVec = endVec.clone().subtract(startVec);
            double totalDistSq = pathVec.lengthSquared();

            List<LivingEntity> potentialTargets = new ArrayList<>();
            for (Entity e : world.getNearbyEntities(box)) {
                if (e instanceof LivingEntity target && e != player) {
                    if (!damagedSet.contains(target)) {
                        potentialTargets.add(target);
                    }
                }
            }

            if (!potentialTargets.isEmpty()) {
                for (LivingEntity target : potentialTargets) {
                    Vector tPos = target.getLocation().toVector();

                    double t = 0;
                    if (totalDistSq > 0) {
                        t = tPos.clone().subtract(startVec).dot(pathVec) / totalDistSq;
                        t = Math.max(0, Math.min(1, t));
                    }

                    Vector closestPoint = startVec.clone().add(pathVec.clone().multiply(t));

                    if (tPos.distanceSquared(closestPoint) <= 0.64) {
                        world.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.0, 0), 20, 0.4, 0.4, 0.4, 1);
                        if (isFirst) world.spawnParticle(Particle.SPIT, target.getLocation().add(0, 1.0, 0), 20, 0.2, 0.3, 0.2, 0.5);

                        ForceDamage forceDamage = new ForceDamage(target, finalDamage, source, true);
                        forceDamage.applyEffect(player);

                        damagedSet.add(target);
                    }
                }
            }

            double distance = Math.sqrt(totalDistSq);
            Vector dirNorm = pathVec.normalize();
            double step = 0.7;

            double startX = first.getX();
            double startY = first.getY();
            double startZ = first.getZ();
            double dirX = dirNorm.getX();
            double dirY = dirNorm.getY();
            double dirZ = dirNorm.getZ();

            for (double d = 0; d <= distance; d += step) {
                double px = startX + dirX * d;
                double py = startY + dirY * d;
                double pz = startZ + dirZ * d;

                world.spawnParticle(Particle.SWEEP_ATTACK, px, py + 1.0, pz, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.SMOKE, px, py + 1.0, pz, (isFirst) ? 20 : 10, 0.5, 0.0, 0.5, 0);
            }

            config.r_damaged_2.remove(uuid);
        }, 5L);
    }
}