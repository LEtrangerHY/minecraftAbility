package org.core.coreSystem.cores.VOL1.Pyro.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.debuff.Burn;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Pyro.coreSystem.Pyro;

import java.time.Duration;
import java.util.Random;

public class Q implements SkillBase {

    private final Pyro config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public Q(Pyro config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){

        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

        if(offhandItem.getType() == Material.BLAZE_POWDER && offhandItem.getAmount() > 7) {
            World world = player.getWorld();
            Location center = player.getLocation();

            world.playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
            world.playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);

            world.spawnParticle(Particle.END_ROD, center.clone().add(0, 1.2, 0), 70, 0.7, 0.7, 0.7, 0.7);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 21, 0.1, 0.1, 0.1, 0.9);
            world.spawnParticle(Particle.FLAME, center, 21, 0.1, 0.1, 0.1, 0.9);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 1, 0), 140, 7, 7, 7, 0);

            Random random = new Random();
            int radius = 7;
            int fireCount = 50;

            for (int i = 0; i < fireCount; i++) {
                int x = random.nextInt(radius * 2 + 1) - radius;
                int z = random.nextInt(radius * 2 + 1) - radius;

                Location fireLoc = center.clone().add(x, 0, z);
                fireLoc.setY(world.getHighestBlockYAt(fireLoc) + 1);

                Block block = fireLoc.getBlock();

                if (block.getType() == Material.AIR) {
                    block.setType(Material.FIRE);
                }
            }

            for (Entity rangeTarget : world.getNearbyEntities(center, 7.7, 7.7, 7.7)) {
                if (rangeTarget instanceof LivingEntity target) {

                    if (rangeTarget == player) {
                        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
                        if (maxHealthAttr != null) {
                            double maxHealth = maxHealthAttr.getValue();
                            double currentHealth = player.getHealth();

                            double healthPercentage = currentHealth / maxHealth;

                            double healRatio = 0.40 - (0.25 * healthPercentage);

                            healRatio = Math.max(0.15, Math.min(0.40, healRatio));

                            double healAmount = maxHealth * healRatio;

                            player.setHealth(Math.min(maxHealth, currentHealth + healAmount));
                        }
                    } else {
                        Burn burn = new Burn(target, 3000L);
                        burn.applyEffect(player);
                    }
                }
            }

            world.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.0f);

            Vector upward = new Vector(0, config.q_Skill_Jump, 0);
            player.setVelocity(upward);

            spawnFireTornado(player, center);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.getPersistentDataContainer().set(new NamespacedKey(plugin, "noFallDamage"), PersistentDataType.BOOLEAN, true);
            }, 1L);

            offhandItem.setAmount(offhandItem.getAmount() - 7);
        } else {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
            Title title = Title.title(
                    Component.empty(),
                    Component.text("blaze powder needed").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);
            long cools = 500L;
            cool.updateCooldown(player, "Q", cools);
        }
    }

    private void spawnFireTornado(Player player, Location center) {
        new BukkitRunnable() {
            int ticks = 20;
            double angle = 0;
            final double radius = 2.0;
            final double maxHeight = 7.7;
            final int density = 2;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks <= 0) {
                    cancel();
                    return;
                }

                for (int k = 0; k < density; k++) {
                    angle += Math.PI / 5 / density;

                    double currentTickProgress = ticks - (k / (double) density);
                    double yFactor = 1 - (currentTickProgress / 20.0);
                    double y = maxHeight * yFactor;

                    for (int i = 0; i < 3; i++) {
                        double currentAngle = angle + i * 2 * Math.PI / 3;
                        double x = Math.cos(currentAngle) * radius;
                        double z = Math.sin(currentAngle) * radius;

                        Location particleLoc = center.clone().add(x, y, z);

                        center.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);

                        if (i % 2 == 0) {
                            center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0, 0, 0, 0);
                        }
                    }
                }

                center.getWorld().spawnParticle(Particle.SMOKE, center.clone().add(0, maxHeight * (1 - (double) ticks / 20.0) / 2, 0), 2, 0.1, 0.1, 0.1, 0.01);

                ticks--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}