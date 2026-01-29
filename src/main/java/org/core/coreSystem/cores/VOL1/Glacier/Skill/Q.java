package org.core.coreSystem.cores.VOL1.Glacier.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Glacier.coreSystem.Glacier;

import java.time.Duration;

public class Q implements SkillBase {
    private final Glacier config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public Q(Glacier config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

        World world = player.getWorld();

        if (offhandItem.getType() == Material.BLUE_ICE && offhandItem.getAmount() >= 7) {

            world.spawnParticle(Particle.SNOWFLAKE, player.getLocation().clone().add(0, 1, 0), 80, 1.5, 1.5, 1.5, 0.1);
            world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1, 1);
            world.playSound(player.getLocation(), Sound.BLOCK_SNOW_BREAK, 1, 1);
            placePowderSnowCone(player, 8.0, 60.0);

            offhandItem.setAmount(offhandItem.getAmount() - 6);
        }else{
            world.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, 1);

            Title title = Title.title(
                    Component.empty(),
                    Component.text("Blue Ice needed").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);

            long cools = 500L;
            cool.updateCooldown(player, "Q", cools);
        }
    }

    public void placePowderSnowCone(Player player, double radius, double angleDegrees) {
        Location playerLoc = player.getLocation();
        World world = player.getWorld();

        Vector forward = playerLoc.getDirection().setY(0).normalize();
        Vector origin = new Vector(playerLoc.getX() + 0.5, playerLoc.getY(), playerLoc.getZ() + 0.5);

        double halfAngleRad = Math.toRadians(angleDegrees / 2);

        int minX = (int)Math.floor(playerLoc.getX() - radius);
        int maxX = (int)Math.ceil(playerLoc.getX() + radius);
        int minZ = (int)Math.floor(playerLoc.getZ() - radius);
        int maxZ = (int)Math.ceil(playerLoc.getZ() + radius);
        int playerY = playerLoc.getBlockY();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Vector blockPos = new Vector(x + 0.5, playerY, z + 0.5);
                Vector directionToBlock = blockPos.clone().subtract(origin);
                directionToBlock.setY(0);
                double distance = directionToBlock.length();

                if (distance == 0 || distance > radius) continue;

                directionToBlock.normalize();
                double dot = forward.dot(directionToBlock);
                dot = Math.min(1.0, Math.max(-1.0, dot));
                double angleBetween = Math.acos(dot);

                if (angleBetween <= halfAngleRad) {
                    int foundY = -1;
                    for (int y = playerY + 2; y >= playerY - 7; y--) {
                        Block baseBlock = world.getBlockAt(x, y, z);
                        if (baseBlock.getType().isSolid() && !baseBlock.isPassable()) {
                            foundY = y + 1;
                            break;
                        }
                    }

                    int foundUpperY = -1;
                    for (int y = playerY + 1; y <= playerY + 8; y++) {
                        Block baseBlock = world.getBlockAt(x, y, z);
                        if (baseBlock.getType().isSolid() && !baseBlock.isPassable()) {
                            foundUpperY = y + 1;
                            break;
                        }
                    }

                    Location playerBlockLoc = playerLoc.getBlock().getLocation();
                    int px = playerBlockLoc.getBlockX();
                    int py = playerBlockLoc.getBlockY();
                    int pz = playerBlockLoc.getBlockZ();

                    if (foundY != -1 && !(x == px && foundY == py && z == pz)) {
                        Block aboveBlock = world.getBlockAt(x, foundY, z);
                        if (aboveBlock.isPassable() || aboveBlock.getType() == Material.AIR) {
                            aboveBlock.setType(Material.POWDER_SNOW);
                        }
                    }

                    if (foundUpperY != -1 && foundUpperY != foundY && !(x == px && foundUpperY == py && z == pz)) {
                        Block aboveBlock = world.getBlockAt(x, foundUpperY, z);
                        if (aboveBlock.isPassable() || aboveBlock.getType() == Material.AIR) {
                            aboveBlock.setType(Material.POWDER_SNOW);
                        }
                    }
                }
            }
        }
    }
}