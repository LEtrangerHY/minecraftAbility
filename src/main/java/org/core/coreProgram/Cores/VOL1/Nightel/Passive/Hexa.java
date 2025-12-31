package org.core.coreProgram.Cores.VOL1.Nightel.Passive;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.Cool.Cool;
import org.core.Main.coreConfig;
import org.core.coreProgram.Cores.VOL1.Nightel.coreSystem.Nightel;

public class Hexa implements Listener {

    private final Nightel config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final coreConfig tag;

    public Hexa(Nightel config, coreConfig tag, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.tag = tag;
    }

    public void hexaPoint(Player player, long coolTime, String skill) {
        int point = config.hexaPoint.getOrDefault(player.getUniqueId(), 0);

        long cools = (long) Math.min(coolTime * Math.pow(3, point), 6000);
        cool.updateCooldown(player, skill, cools);

        if(point < 6) {

            World world = player.getWorld();
            Location playerLoc = player.getLocation();
            BlockData chain = Material.IRON_CHAIN.createBlockData();

            int addPoint = (config.hexaSkill.containsKey(player.getUniqueId()) && !config.hexaSkill.getOrDefault(player.getUniqueId(), "").equals(skill)) ? 2 : 1;

            for(int i = 0; i < addPoint; i++) {
                if(config.hexaPoint.getOrDefault(player.getUniqueId(), 0) < 6) {
                    config.hexaPoint.put(player.getUniqueId(), config.hexaPoint.getOrDefault(player.getUniqueId(), 0) + 1);
                }else{
                    break;
                }
            }

            world.playSound(playerLoc, Sound.BLOCK_CHAIN_PLACE, 1.6f, 1.0f);
            world.spawnParticle(Particle.BLOCK, playerLoc.clone().add(0, 1.2, 0), 33, 0.6, 0.6, 0.6,
                    chain);
            if(addPoint == 2){
                world.spawnParticle(Particle.ENCHANTED_HIT, playerLoc.clone().add(0, 1.2, 0), 33, 0.6, 0.6, 0.6, 1);
            }

            if (config.hexaPoint.getOrDefault(player.getUniqueId(), 0) < 6) {
                int displayCount = config.hexaPoint.getOrDefault(player.getUniqueId(), 0);
                String hex = "⬡ ".repeat(displayCount).trim();
                player.sendActionBar(Component.text(hex).color(NamedTextColor.GRAY));
            } else {
                player.sendActionBar(Component.text("⌬ ⌬ ⌬ ⌬ ⌬ ⌬").color(NamedTextColor.DARK_GRAY));
            }
        }

        config.hexaSkill.put(player.getUniqueId(), skill);

    }

    public void removePoint(Player player){

        long cools = 0;
        cool.updateCooldown(player, "R", cools);
        cool.updateCooldown(player, "Q", cools);

        long coolOfF = Math.max(config.hexaPoint.getOrDefault(player.getUniqueId(), 0) * 1000, 600);
        cool.updateCooldown(player, "F", coolOfF);

        config.hexaPoint.remove(player.getUniqueId());
        config.hexaSkill.remove(player.getUniqueId());

        player.sendActionBar(Component.text("⌬").color(NamedTextColor.GRAY));

    }
}

