package org.core.coreSystem.cores.VOL1.Bloom.Skill;

import org.bukkit.*;
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
import org.core.effect.crowdControl.ForceDamage;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Bloom.coreSystem.Bloom;

import java.util.HashSet;

public class Q implements SkillBase {
    private final Bloom config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public Q(Bloom config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){

        World world = player.getWorld();

        world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.4f, 1);
        world.playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.4f, 1);
        world.playSound(player.getLocation(), Sound.BLOCK_GRASS_PLACE, 1, 1);

        FlowerSurge(player);

    }

    public void FlowerSurge(Player player) {

        World world = player.getWorld();

        double Length = 5.0;
        double maxAngle = Math.toRadians(105);
        double maxTicks = 3;
        double innerRadius = 2.0;

        config.q_damaged.putIfAbsent(player.getUniqueId(), new HashSet<>());

        double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Vector direction = player.getLocation().getDirection().clone().setY(0).normalize();

        Particle.DustOptions pinkDust = new Particle.DustOptions(Color.fromRGB(255, 175, 185), 1.1f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (ticks >= maxTicks || player.isDead() || !player.isOnline()) {

                    int n = config.repeatCount.getOrDefault(player.getUniqueId(), 0);
                    config.repeatCount.put(player.getUniqueId(), n+1);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if(config.repeatCount.getOrDefault(player.getUniqueId(), 0) < 3 && !player.isDead() && player.isOnline()) {
                            FlowerSurge(player);
                        }else{
                            config.repeatCount.remove(player.getUniqueId());
                            config.q_damaged.remove(player.getUniqueId());
                        }
                    }, 1L);

                    this.cancel();
                    return;
                }

                double progress = (ticks + 1) * (maxAngle * 2 / maxTicks) - maxAngle;
                Vector rotatedDir = direction.clone().rotateAroundY(progress);

                for (double length = 0; length <= Length; length += 1.0) {
                    for (double angle = -maxAngle; angle <= maxAngle; angle += Math.toRadians(3)) {
                        Vector angleDir = rotatedDir.clone().rotateAroundY(angle);
                        Vector particleOffset = angleDir.clone().multiply(length);

                        Location particleLocation = player.getLocation().clone().add(particleOffset).add(0, 1.4, 0);

                        double distanceFromOrigin = particleLocation.distance(player.getLocation().clone());

                        if (distanceFromOrigin >= innerRadius) {
                            if(Math.random() < 0.3) {
                                if (Math.random() < 0.07) {
                                    world.spawnParticle(Particle.CHERRY_LEAVES, particleLocation, 1, 0.3, 0.3, 0.3, 0.06);
                                } else {
                                    world.spawnParticle(Particle.DUST, particleLocation, 3, 0.3, 0.3, 0.3, pinkDust);
                                }
                            }
                        }

                        for (Entity entity : world.getNearbyEntities(particleLocation, 1.4, 1.4, 1.4)) {
                            if (entity instanceof LivingEntity target && entity != player && !config.q_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                                world.spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 1.4, 0), 1, 0, 0, 0, 1);
                                world.spawnParticle(Particle.FALLING_DUST, target.getLocation().add(0, 1.4, 0), 10, 0.5, 0.5, 0.5, Material.PINK_CONCRETE.createBlockData());

                                ForceDamage forceDamage = new ForceDamage(target, damage, source);
                                forceDamage.applyEffect(player);

                                Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.7);
                                direction.setY(0.34);

                                target.setVelocity(direction);

                                config.q_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).add(entity);
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}