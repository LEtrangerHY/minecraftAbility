package org.core.coreSystem.cores.VOL1.Bloom.Skill;

import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Bloom.coreSystem.Bloom;

public class R implements SkillBase {
    private final Bloom config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R(Bloom config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        Location startLoc = player.getEyeLocation().clone().add(player.getLocation().getDirection().normalize().multiply(1.5));
        Vector direction = player.getLocation().getDirection().normalize().multiply(1.4);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.4f, 1);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation().add(direction).add(0, 1.4, 0), 1, 0, 0, 0, 1);

        new BukkitRunnable() {
            int ticks = 70;
            Location current = startLoc.clone();

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks <= 0) {
                    cancel();
                    return;
                }

                current.add(direction);

                player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, current, 2, 0.1, 0.1, 0.1, 0);

                if (current.getBlock().getType().isSolid()) {
                    spawnTornadoEffect(player, current);
                    cancel();
                    return;
                }

                for (Entity e : current.getWorld().getNearbyEntities(current, 1, 1, 1)) {
                    if (e instanceof LivingEntity le && !le.equals(player)) {
                        spawnTornadoEffect(player, current);
                        cancel();
                        return;
                    }
                }

                ticks--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnTornadoEffect(Player player, Location center) {
        double amp = config.r_Skill_amp * player.getPersistentDataContainer()
                .getOrDefault(new NamespacedKey(plugin, "R"), org.bukkit.persistence.PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        center.getWorld().playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.4f, 1);
        center.getWorld().playSound(center, Sound.BLOCK_GRASS_PLACE, 1.4f, 1);

        new BukkitRunnable() {
            int ticks = 14;
            double angle = 0;
            final double radius = 1.4;
            final double maxHeight = 4.0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks <= 0) {
                    cancel();
                    return;
                }

                angle += Math.PI / 10;

                for (int i = 0; i < 3; i++) {
                    double x = Math.cos(angle + i * 2 * Math.PI / 3) * radius;
                    double z = Math.sin(angle + i * 2 * Math.PI / 3) * radius;
                    double y = maxHeight * (1 - (double) ticks / 20);

                    Location particleLoc = center.clone().add(x, y, z);
                    center.getWorld().spawnParticle(Particle.CHERRY_LEAVES, particleLoc, 1, 0.02, 0.02, 0.02, 0);
                }

                center.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(0, maxHeight * (1 - (double) ticks / 20) / 2, 0), 5, 0.3, 0.3, 0.3, 0.01);

                for (Entity e : center.getWorld().getNearbyEntities(center, radius, maxHeight, radius)) {
                    if (e instanceof LivingEntity target && !target.equals(player)) {

                        ForceDamage forceDamage = new ForceDamage(target, damage, source, false);
                        forceDamage.applyEffect(player);
                        target.setVelocity(new Vector(0, 0.7, 0));

                        center.getWorld().spawnParticle(Particle.FALLING_DUST, target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, Material.PINK_CONCRETE.createBlockData());
                    }
                }

                ticks--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
