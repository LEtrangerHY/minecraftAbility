package org.core.coreSystem.cores.VOL1.Glacier.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
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

        if (offhandItem.getType() == Material.BLUE_ICE && offhandItem.getAmount() > 7) {

            world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1, 1);
            world.playSound(player.getLocation(), Sound.BLOCK_SNOW_BREAK, 1, 1);

            spawnIceBreath(player);

            placePowderSnowCone(player, 8.0, 60.0);

            offhandItem.setAmount(offhandItem.getAmount() - 6);
        } else {
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

    private void spawnIceBreath(Player player) {
        final Location startLoc = player.getEyeLocation().subtract(0, 0.3, 0);
        final Vector direction = startLoc.getDirection();

        Vector upAxis = new Vector(0, 1, 0);
        if (Math.abs(direction.getY()) > 0.95) upAxis = new Vector(1, 0, 0);

        final Vector rightVector = direction.getCrossProduct(upAxis).normalize();
        final Vector upVector = rightVector.getCrossProduct(direction).normalize();

        final Particle.DustOptions skyDust = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 0.6f);
        final BlockData blueIceData = Material.BLUE_ICE.createBlockData();

        new BukkitRunnable() {
            double currentDistance = 0;
            final double maxDist = 8.0;
            final double speed = 0.6;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                int density = 1;
                double stepSize = speed / density;

                for (int d = 0; d < density; d++) {
                    currentDistance += stepSize;

                    if (currentDistance > maxDist) {
                        cancel();
                        return;
                    }

                    double radius = currentDistance * 0.55;

                    int arms = 5;
                    for (int i = 0; i < arms; i++) {
                        double angle = (i * (2 * Math.PI / arms)) + (currentDistance * 0.8);

                        double x = Math.cos(angle) * radius;
                        double y = Math.sin(angle) * radius;

                        Vector offset = rightVector.clone().multiply(x).add(upVector.clone().multiply(y));
                        Location particleLoc = startLoc.clone().add(direction.clone().multiply(currentDistance)).add(offset);

                        player.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0);

                        player.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, skyDust);

                        if (d == 0 && i % 3 == 0) {
                            player.getWorld().spawnParticle(Particle.BLOCK, particleLoc, 1, 0, 0, 0, 0, blueIceData);
                        }
                    }

                    if (currentDistance > 1.0) {
                        Location centerLoc = startLoc.clone().add(direction.clone().multiply(currentDistance));
                        player.getWorld().spawnParticle(Particle.CLOUD, centerLoc, 1, radius * 0.2, radius * 0.2, radius * 0.2, 0.02);

                        player.getWorld().spawnParticle(Particle.DUST, centerLoc, 1, radius * 0.3, radius * 0.3, radius * 0.3, 0, skyDust);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
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