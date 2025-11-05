package org.core.coreProgram.Cores.Blue.Skill;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.Blue.coreSystem.Blue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class R implements SkillBase {
    private final Blue config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R(Blue config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){

        World world = player.getWorld();

        world.spawnParticle(Particle.WITCH, player.getLocation().clone().add(0, 1, 0), 80, 1.5, 1.5, 1.5, 0.1);
        world.spawnParticle(Particle.SMOKE, player.getLocation().clone().add(0, 1, 0), 80, 1.5, 1.5, 1.5, 0.1);
        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);
        world.playSound(player.getLocation(), Sound.BLOCK_GRASS_PLACE, 1, 1);
        placeWitherFlower(player, 10.0, 44.0);

    }

    public void placeWitherFlower(Player player, double radius, double angleDegrees) {
        Location playerLoc = player.getLocation();
        World world = player.getWorld();

        Vector forward = playerLoc.getDirection().setY(0).normalize();
        Vector origin = new Vector(playerLoc.getX() + 0.5, playerLoc.getY(), playerLoc.getZ() + 0.5);

        double halfAngleRad = Math.toRadians(angleDegrees / 2);

        int minX = (int) Math.floor(playerLoc.getX() - radius);
        int maxX = (int) Math.ceil(playerLoc.getX() + radius);
        int minZ = (int) Math.floor(playerLoc.getZ() - radius);
        int maxZ = (int) Math.ceil(playerLoc.getZ() + radius);
        int playerY = playerLoc.getBlockY();

        List<Block> blocksToPlace = new ArrayList<>();

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
                            blocksToPlace.add(aboveBlock);
                        }
                    }

                    if (foundUpperY != -1 && foundUpperY != foundY && !(x == px && foundUpperY == py && z == pz)) {
                        Block aboveBlock = world.getBlockAt(x, foundUpperY, z);
                        if (aboveBlock.isPassable() || aboveBlock.getType() == Material.AIR) {
                            blocksToPlace.add(aboveBlock);
                        }
                    }
                }
            }
        }

        blocksToPlace.sort(Comparator.comparingDouble(b -> b.getLocation().distance(playerLoc)));

        new BukkitRunnable() {
            int index = 0;
            @Override
            public void run() {
                int perTick = 3;
                for (int i = 0; i < perTick && index < blocksToPlace.size(); i++, index++) {
                    Block block = blocksToPlace.get(index);
                    block.setType(Material.WITHER_ROSE, false);

                    world.spawnParticle(Particle.SOUL, block.getLocation().add(0.5, 0.5, 0.5), 3, 0.1, 0.1, 0.1, 0.02);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (block.getType() == Material.WITHER_ROSE) {
                                block.setType(Material.AIR);
                                world.spawnParticle(Particle.SMOKE, block.getLocation().add(0.5, 0.2, 0.5), 5, 0.1, 0.1, 0.1, 0.02);
                            }
                        }
                    }.runTaskLater(plugin, 80L);
                }

                if (index >= blocksToPlace.size()) cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }


}