package org.core.coreSystem.cores.VOL1.Harvester.Skill;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Harvester.coreSystem.Harvester;

import java.util.Random;

public class Q implements SkillBase {

    public final Harvester config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Random random = new Random();

    public Q(Harvester config, JavaPlugin plugin, Cool cool){
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        healFromBlocks(player, 2);
        createBush(player, 3);
    }

    public void createBush(Player player, int radius){

        int count = 0;

        World world = player.getWorld();
        Location center = player.getLocation();

        double radiusSq = radius * radius;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    Location targetLoc = center.clone().add(x, y, z);
                    if (targetLoc.distanceSquared(center) > radiusSq) continue;

                    Block block = world.getBlockAt(targetLoc);
                    Block above = block.getRelative(0, 1, 0);

                    if (block.getType() == Material.GRASS_BLOCK && above.getType() == Material.AIR) {
                        above.setType(Material.SHORT_GRASS);
                        count++;
                    }

                    else if (block.getType() == Material.FARMLAND && above.getType() == Material.AIR) {
                        above.setType(Material.WHEAT);

                        if (above.getBlockData() instanceof Ageable ageable) {
                            ageable.setAge(ageable.getMaximumAge());
                            above.setBlockData(ageable);
                        }

                        count++;
                    }
                }
            }
        }

        if(count > 0){
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_PLACE, 1, 1);
        }
    }

    public void healFromBlocks(Player player, int radius){
        World world = player.getWorld();
        Location center = player.getLocation();

        double radiusSq = radius * radius;
        int count = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    Location targetLoc = center.clone().add(x, y, z);
                    if (targetLoc.distanceSquared(center) > radiusSq) continue;

                    Block block = world.getBlockAt(targetLoc);
                    Material type = block.getType();

                    if (type != Material.AIR && type != Material.GRASS_BLOCK && type != Material.FARMLAND) {
                        count++;
                    }
                }
            }
        }

        if(count > 0) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f, 1);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().clone().add(0, 1.2, 0), 5, 0.2, 0.2, 0.2, 0);
        }

        double healAmount = count;
        double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
        player.setHealth(newHealth);
    }
}
