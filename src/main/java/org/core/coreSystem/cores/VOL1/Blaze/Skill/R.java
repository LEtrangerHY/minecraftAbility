package org.core.coreSystem.cores.VOL1.Blaze.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Blaze.Passive.BlueFlame;
import org.core.coreSystem.cores.VOL1.Blaze.coreSystem.Blaze;

import java.time.Duration;

public class R implements SkillBase{
    private final Blaze config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final BlueFlame blueFlame;

    public R(Blaze config, JavaPlugin plugin, Cool cool, BlueFlame blueFlame) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.blueFlame = blueFlame;
    }

    @Override
    public void Trigger(Player player) {
        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

        if (offhandItem.getType() == Material.SOUL_LANTERN || ((offhandItem.getType() == Material.SOUL_SAND || offhandItem.getType() == Material.SOUL_SOIL) && offhandItem.getAmount() > 7)) {
            World world = player.getWorld();
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PARROT_IMITATE_BLAZE, 1, 1);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_BURN, 1, 1);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1, 1);

            new BukkitRunnable() {
                int tick = 0;

                @Override
                public void run() {
                    if (tick > 26 || player.isDead()) {
                        cancel();
                        return;
                    }

                    if (tick < 20) {
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().clone().add(0, 1, 0), 13, 0.2, 0.1, 0.2, 0.05);
                    } else {
                        world.spawnParticle(Particle.SOUL, player.getLocation().clone().add(0, 1, 0), 4, 0.4, 0.4, 0.4, 0.04);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1);
                        Vector direction = player.getLocation().getDirection().normalize().multiply(1.4);
                        player.launchProjectile(Fireball.class, direction);
                    }

                    tick++;
                }

            }.runTaskTimer(plugin, 0L, 3L);

            if((offhandItem.getType() == Material.SOUL_SAND || offhandItem.getType() == Material.SOUL_SOIL) && offhandItem.getAmount() >= 7) {
                offhandItem.setAmount(offhandItem.getAmount() - 7);
            }

        }else{
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1, 1);

            Title title = Title.title(
                    Component.empty(),
                    Component.text("soul sand needed").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);

            long cools = 500L;
            cool.updateCooldown(player, "R", cools);
        }
    }
}