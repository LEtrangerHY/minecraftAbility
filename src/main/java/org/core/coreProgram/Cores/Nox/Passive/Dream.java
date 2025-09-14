package org.core.coreProgram.Cores.Nox.Passive;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.coreConfig;
import org.core.coreProgram.Cores.Nox.coreSystem.Nox;

import java.util.*;

public class Dream implements Listener {

    private final Nox config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final coreConfig tag;

    public Dream(Nox config, coreConfig tag, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.tag = tag;
    }

    public void dreamPoint(Player player, long coolTime, String skill) {
        int point = config.dreamPoint.getOrDefault(player.getUniqueId(), 0);

        if(point < 6) {

            World world = player.getWorld();
            Location playerLoc = player.getLocation();
            BlockData chain = Material.CHAIN.createBlockData();

            long cools = (long) Math.min(coolTime * Math.pow(3, point), 6000);
            cool.updateCooldown(player, skill, cools);

            int addPoint = (config.dreamSkill.containsKey(player.getUniqueId()) && !config.dreamSkill.getOrDefault(player.getUniqueId(), "").equals(skill)) ? 2 : 1;

            for(int i = 0; i < addPoint; i++) {
                if(config.dreamPoint.getOrDefault(player.getUniqueId(), 0) < 6) {
                    config.dreamPoint.put(player.getUniqueId(), config.dreamPoint.getOrDefault(player.getUniqueId(), 0) + 1);
                }else{
                    break;
                }
            }
            config.dreamSkill.put(player.getUniqueId(), skill);

            world.playSound(playerLoc, Sound.BLOCK_CHAIN_PLACE, 1.6f, 1.0f);
            world.spawnParticle(Particle.BLOCK, playerLoc.clone().add(0, 1.2, 0), 33, 0.6, 0.6, 0.6,
                    chain);
            if(addPoint == 2){
                world.spawnParticle(Particle.ENCHANTED_HIT, playerLoc.clone().add(0, 1.2, 0), 33, 0.6, 0.6, 0.6, 1);

            }

            if (config.dreamPoint.getOrDefault(player.getUniqueId(), 0) < 6) {
                player.sendActionBar(Component.text("Dreams : " + config.dreamPoint.getOrDefault(player.getUniqueId(), 0)).color(NamedTextColor.GRAY));
            } else {
                player.sendActionBar(Component.text("Enlighten").color(NamedTextColor.DARK_GRAY));
            }
        }

    }

    public void removePoint(Player player){

        long cools = 0;
        cool.updateCooldown(player, "R", cools);
        cool.updateCooldown(player, "Q", cools);

        long coolOfF = Math.max(config.dreamPoint.getOrDefault(player.getUniqueId(), 0) * 1000, 600);
        cool.updateCooldown(player, "F", coolOfF);

        config.dreamPoint.remove(player.getUniqueId());
        config.dreamSkill.remove(player.getUniqueId());

        player.sendActionBar(Component.text("Oblivion").color(NamedTextColor.DARK_GRAY));

    }
}

