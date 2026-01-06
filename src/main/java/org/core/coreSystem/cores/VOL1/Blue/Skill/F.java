package org.core.coreSystem.cores.VOL1.Blue.Skill;

import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Blue.coreSystem.Blue;

import java.util.HashSet;

public class F implements SkillBase {
    private final Blue config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Blue config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){

        World world = player.getWorld();

        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1, 1);
        world.playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1, 1);
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1, 1);
        world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);
        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1, 1);

        FlowerSurge(player);

    }

    public void FlowerSurge(Player player) {

        World world = player.getWorld();

        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.6f, 0.4f);

        double Length = 8.0;
        double maxAngle = Math.toRadians(105);
        double maxTicks = 6;
        double innerRadius = 3.0;

        config.f_damaged.putIfAbsent(player.getUniqueId(), new HashSet<>());
        config.fskill_using.put(player.getUniqueId(), true);

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Vector direction = player.getLocation().getDirection().clone().setY(0).normalize();

        Particle.DustOptions dustOption_flowerDust = new Particle.DustOptions(Color.AQUA, 0.6f);
        Particle.DustOptions dustOption_flowerDust_gra = new Particle.DustOptions(Color.NAVY, 0.6f);

        world.spawnParticle(Particle.SMOKE, player.getLocation().clone().add(0, 1.3, 0), 20, 2.3, 1.3, 2.3, 0);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (ticks >= maxTicks || player.isDead() || !player.isOnline()) {

                    config.f_damaged.remove(player.getUniqueId());

                    int n = config.repeatCount.getOrDefault(player.getUniqueId(), 0);
                    config.repeatCount.put(player.getUniqueId(), n+1);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if(config.repeatCount.getOrDefault(player.getUniqueId(), 0) < 26 && !player.isDead() && player.isOnline()) {
                            FlowerSurge(player);
                        }else{
                            config.repeatCount.remove(player.getUniqueId());
                            config.fskill_using.remove(player.getUniqueId());
                        }
                    }, 2L);

                    this.cancel();
                    return;
                }

                double progress = (ticks + 1) * (maxAngle * 2 / maxTicks) - maxAngle;
                Vector rotatedDir = direction.clone().rotateAroundY(progress);

                for (double length = 0; length <= Length; length += 1.0) {
                    for (double angle = -maxAngle; angle <= maxAngle; angle += Math.toRadians(3)) {
                        Vector angleDir = rotatedDir.clone().rotateAroundY(angle);
                        Vector particleOffset = angleDir.clone().multiply(length);

                        Location particleLocation = player.getLocation().clone().add(particleOffset);

                        double distanceFromOrigin = particleLocation.distance(player.getLocation().clone());

                        if (distanceFromOrigin >= innerRadius) {
                            if(Math.random() < 0.4) {
                                if (Math.random() < 0.06) {
                                    world.spawnParticle(Particle.PALE_OAK_LEAVES, particleLocation, 1, 0.4, 0.6, 0.4, 0.06);
                                } else {
                                    if(Math.random() < 0.8) {
                                        if (Math.random() < 0.6) {
                                            world.spawnParticle(Particle.DUST, particleLocation, 2, 0.4, 0.6, 0.4, 0, dustOption_flowerDust_gra);
                                        } else {
                                            world.spawnParticle(Particle.DUST, particleLocation, 1, 0.4, 0.6, 0.4, 0, dustOption_flowerDust);
                                        }
                                    }
                                    world.spawnParticle(Particle.SMOKE, particleLocation, 2, 0.4, 0.6, 0.4, 0.06);
                                }
                            }
                        }

                        for (Entity entity : world.getNearbyEntities(particleLocation, 4, 4, 4)) {
                            if (entity instanceof LivingEntity target && entity != player && !config.f_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                                world.spawnParticle(Particle.SOUL, target.getLocation().clone().add(0, 1.3, 0), 4, 0.4, 0.4, 0.4, 0);

                                PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS, (int) maxTicks, 3, false, false);
                                target.addPotionEffect(slowness);

                                PotionEffect wither = new PotionEffect(PotionEffectType.WITHER, (int) maxTicks, 5, false, false);
                                target.addPotionEffect(wither);

                                ForceDamage forceDamage = new ForceDamage(target, damage, source);
                                forceDamage.applyEffect(player);
                                target.setVelocity(new Vector(0, 0, 0));

                                config.f_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).add(entity);
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
