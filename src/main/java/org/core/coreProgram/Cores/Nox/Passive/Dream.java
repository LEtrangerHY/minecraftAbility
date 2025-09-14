package org.core.coreProgram.Cores.Nox.Passive;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.core.Cool.Cool;
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

        long cools = (point > 1) ? coolTime : (long) (coolTime * Math.pow(3, point));
        cool.updateCooldown(player, skill, cools);

        config.dreamPoint.put(player.getUniqueId(), point + 1);
        if(config.dreamPoint.getOrDefault(player.getUniqueId(), 0) < 6){
            player.sendActionBar(Component.text("Dreams : " + config.dreamPoint.getOrDefault(player.getUniqueId(), 0)).color(NamedTextColor.GRAY));
        }else{
            player.sendActionBar(Component.text("Oblivion").color(NamedTextColor.DARK_GRAY));
        }
    }

    public void removePoint(Player player){
        config.dreamPoint.remove(player.getUniqueId());
        player.sendActionBar(Component.text("Dreams : " + config.dreamPoint.getOrDefault(player.getUniqueId(), 0)).color(NamedTextColor.GRAY));
    }
}

