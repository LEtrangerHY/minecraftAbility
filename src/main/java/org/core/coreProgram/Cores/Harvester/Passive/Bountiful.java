package org.core.coreProgram.Cores.Harvester.Passive;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.Cool.Cool;
import org.core.Effect.EffectManager;
import org.core.coreConfig;
import org.core.coreProgram.Cores.Harvester.coreSystem.Harvester;

public class Bountiful {

    private final coreConfig tag;
    private final Harvester config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private EffectManager effectManager = new EffectManager();

    public Bountiful(coreConfig tag, Harvester config, JavaPlugin plugin, Cool cool) {
        this.tag = tag;
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }



    public boolean bushCheck(Player player) {
        int baseX = player.getLocation().getBlockX();
        int baseY = player.getLocation().getBlockY();
        int baseZ = player.getLocation().getBlockZ();

        int count = 0;

        for (int x = baseX - 1; x <= baseX + 1; x++) {
            for (int y = baseY - 1; y <= baseY + 1; y++) {
                for (int z = baseZ - 1; z <= baseZ + 1; z++) {
                    Block block = player.getWorld().getBlockAt(x, y, z);
                    Material type = block.getType();

                    if (type == Material.SHORT_GRASS || type == Material.TALL_GRASS) {
                        count++;
                    }
                    else if (type == Material.WHEAT || type == Material.POTATOES || type == Material.CARROTS || type == Material.BEETROOTS) {
                        BlockState state = block.getState();
                        if (state.getBlockData() instanceof Ageable ageable) {
                            if (ageable.getAge() == ageable.getMaximumAge()) {
                                count++;
                            }
                        }
                    }
                }
            }
        }

        Block blockBelow = player.getLocation().getBlock();
        if (blockBelow.getType() == Material.TALL_GRASS) {
            return true;
        }

        return count >= 6;
    }
}
