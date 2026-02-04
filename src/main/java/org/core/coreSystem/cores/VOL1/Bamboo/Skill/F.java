package org.core.coreSystem.cores.VOL1.Bamboo.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.cores.VOL1.Bamboo.coreSystem.Bamboo;
import org.core.coreSystem.absCoreSystem.SkillBase;

import java.time.Duration;
import java.util.*;

public class F implements SkillBase {

    private final Bamboo config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Bamboo config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {

        Block block = player.getTargetBlockExact(40);

        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);
        int amount = offhandItem.getAmount();

        if (offhandItem.getType() == Material.IRON_NUGGET && amount >= 8) {
            if (config.stringCount.getOrDefault(player.getUniqueId(), 0) < 3) {
                if (config.stringOn.contains(player.getUniqueId())) {

                    offhandItem.setAmount(amount - 8);

                    if(config.moveToSneaking.contains(player.getUniqueId())){
                        config.moveToThrow.add(player.getUniqueId());
                    }

                    string(player);

                    if (config.stringCount.getOrDefault(player.getUniqueId(), 0) == 2) {
                        config.stringCount.remove(player.getUniqueId());
                        long cools = 25000L;
                        cool.updateCooldown(player, "F", cools);

                    } else {
                        long cools = 300L;
                        cool.updateCooldown(player, "F", cools);
                        config.stringCount.put(player.getUniqueId(), config.stringCount.getOrDefault(player.getUniqueId(), 0) + 1);
                    }
                } else {

                    if(block != null) {
                        bambooThrow(player, block);

                        long cools = 100L;
                        cool.updateCooldown(player, "F", cools);
                    }else{

                        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
                        long cools = 100L;
                        cool.updateCooldown(player, "F", cools);
                    }
                }
            }
        }else {
            Title title = Title.title(
                    Component.empty(),
                    Component.text("iron nugget needed").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
            long cools = 500L;
            cool.updateCooldown(player, "F", cools);
        }

    }

    public void string(Player player) {
        UUID uuid = player.getUniqueId();
        config.stringOn.remove(uuid);

        Location targetLoc = config.stringPoint.getOrDefault(uuid, player.getLocation());

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation(), 25, 0.3, 0.3, 0.3, 0.08, dustOptions);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 2, 0.3, 0.3, 0.3, 1);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);

        new BukkitRunnable() {

            @Override
            public void run() {

                Location currentLoc = player.getLocation();
                Vector toTarget = targetLoc.toVector().subtract(currentLoc.toVector());

                Vector velocityCheck = toTarget.clone().normalize().multiply(1.8);
                Location checkLoc = currentLoc.clone().add(velocityCheck);
                if (!checkLoc.getBlock().isPassable() || toTarget.lengthSquared() < 1.8) {
                    if(!config.moveToSneaking.contains(player.getUniqueId())) {
                        config.stringPoint.remove(uuid);
                        config.stringOn.remove(uuid);
                        config.moveToSneaking.add(player.getUniqueId());
                    }
                    player.setVelocity(new Vector(0, 0, 0));
                    if(player.isSneaking() || config.moveToThrow.contains(player.getUniqueId())) {
                        config.moveToSneaking.remove(player.getUniqueId());
                        config.moveToThrow.remove(player.getUniqueId());
                        this.cancel();
                        return;
                    }
                }else if(!config.moveToSneaking.contains(player.getUniqueId())){

                    Vector velocity = toTarget.normalize().multiply(2.5);
                    player.setVelocity(velocity);

                    Location loc = currentLoc.clone().add(0, 1, 0);
                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);
                    player.getWorld().spawnParticle(Particle.DUST, loc, 25, 0.3, 0.3, 0.3, 0.08, dustOptions);
                    player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 2, 0.3, 0.3, 0.3, 1);
                    player.getWorld().playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);
                }else{
                    config.moveToSneaking.remove(player.getUniqueId());
                    config.moveToThrow.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }


    public void bambooThrow(Player player, Block block){

        World world = player.getWorld();

        Location playerLocation = player.getLocation();
        Vector direction = playerLocation.getDirection().normalize().multiply(1.2);

        for (int ticks = 0; ticks < 50; ticks++) {
            Location particleLocation = playerLocation.clone()
                    .add(direction.clone().multiply(ticks * 0.4))
                    .add(0, 1.5, 0);

            Particle.DustOptions dustOptions_green = new Particle.DustOptions(Color.fromRGB(0, 255, 0),  1.0f);
            world.spawnParticle(Particle.DUST, particleLocation, 6, 0, 0, 0, 0, dustOptions_green);
        }

        config.stringOn.add(player.getUniqueId());

        config.stringPoint.put(player.getUniqueId(), block.getLocation());

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.0f);

    }
}