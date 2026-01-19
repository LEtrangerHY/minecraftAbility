package org.core.coreSystem.cores.VOL1.Nightel.Passive;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.core.cool.Cool;
import org.core.main.coreConfig;
import org.core.coreSystem.cores.VOL1.Nightel.coreSystem.Nightel;

import java.util.*;

public class Chain implements Listener {

    private final Nightel config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final coreConfig tag;

    public Chain(Nightel config, coreConfig tag, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.tag = tag;
    }

    public void chainCount(Player player, long coolTime, String skill) {
        updateChainList(player);
        int point = config.chainCount.getOrDefault(player.getUniqueId(), 0);

        long cools = (long) Math.min(coolTime * Math.pow(3.3, point), 6000);
        cool.updateCooldown(player, skill, cools);

        if(point < 6) {

            World world = player.getWorld();
            Location playerLoc = player.getLocation();
            BlockData chain = Material.IRON_CHAIN.createBlockData();

            int addPoint = config.chainSkill.containsKey(player.getUniqueId()) &&
                    ((skill.equals("R") && !config.chainSkill.getOrDefault(player.getUniqueId(), "").equals(skill))
                    || (skill.equals("Q") && config.chainSkill.getOrDefault(player.getUniqueId(), "").equals(skill))) ? 2 : 1;

            for(int i = 0; i < addPoint; i++) {
                if(config.chainCount.getOrDefault(player.getUniqueId(), 0) < 6) {
                    config.chainCount.put(player.getUniqueId(), config.chainCount.getOrDefault(player.getUniqueId(), 0) + 1);
                }else{
                    break;
                }
            }

            world.playSound(playerLoc, Sound.BLOCK_CHAIN_PLACE, 1.6f, 1.0f);
            world.spawnParticle(Particle.BLOCK, playerLoc.clone().add(0, 1.2, 0), 33, 0.6, 0.6, 0.6, chain);
            if(addPoint == 2) world.spawnParticle(Particle.ENCHANTED_HIT, playerLoc.clone().add(0, 1.2, 0), 33, 0.6, 0.6, 0.6, 1);

            if (config.chainCount.getOrDefault(player.getUniqueId(), 0) < 6) {
                int displayCount = config.chainCount.getOrDefault(player.getUniqueId(), 0);
                String hex = "⬡ ".repeat(displayCount).trim();
                player.sendActionBar(Component.text(hex).color(NamedTextColor.GRAY));
                chainDecrease(player);
            } else {
                hexaChainLoad(player);
            }
        }

        config.chainSkill.put(player.getUniqueId(), skill);
    }

    private final Map<UUID, BukkitRunnable> activeChainTasks = new HashMap<>();

    public void chainDecrease(Player player){
        World world = player.getWorld();
        UUID playerUUID = player.getUniqueId();

        if (activeChainTasks.containsKey(playerUUID)) {
            activeChainTasks.get(playerUUID).cancel();
            activeChainTasks.remove(playerUUID);
        }

        BukkitRunnable task = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run(){

                if(config.chainCount.getOrDefault(player.getUniqueId(), 0).equals(0) || config.chainCount.getOrDefault(player.getUniqueId(), 0).equals(6) || player.isDead() || config.fskill_using.getOrDefault(player.getUniqueId(), false)){
                    if(config.chainCount.getOrDefault(player.getUniqueId(), 0).equals(0)) {
                        config.chainCount.remove(player.getUniqueId());
                        player.sendActionBar(Component.text("⌬").color(NamedTextColor.GRAY));
                    }
                    config.chainSkill.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                tick++;

                if(tick % 60 == 0 && config.chainCount.getOrDefault(player.getUniqueId(), 0) > 0 && config.chainCount.getOrDefault(player.getUniqueId(), 0) < 6) {

                    int displayCount = config.chainCount.getOrDefault(player.getUniqueId(), 0);
                    String hex = "⬡ ".repeat(displayCount - 1).trim();
                    player.sendActionBar(Component.text(hex).color(NamedTextColor.GRAY));

                    config.chainCount.put(player.getUniqueId(), displayCount - 1);

                    Location playerLoc = player.getLocation().clone().add(0, 1.2, 0);

                    world.spawnParticle(Particle.ENCHANTED_HIT, playerLoc, 12, 0.6, 0.6, 0.6, 1);
                    world.playSound(playerLoc, Sound.BLOCK_CHAIN_STEP, 2.2f, 1.0f);
                }
            }
        };

        activeChainTasks.put(playerUUID, task);
        task.runTaskTimer(plugin, 0, 1L);
    }

    public void hexaChainLoad(Player player){
        World world = player.getWorld();

        world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.6f, 1.0f);

        player.setWalkSpeed((float) 0.2 * ((float) 4/3));

        new BukkitRunnable(){
            int tick = 0;

            @Override
            public void run(){

                if(tick > 60 || player.isDead() || config.fskill_using.getOrDefault(player.getUniqueId(), false)){
                    player.setWalkSpeed((float) 0.2);
                    removePoint(player);
                    cancel();
                    return;
                }

                String hex = "⌬ ".repeat(6 - tick / 10).trim();
                player.sendActionBar(Component.text(hex).color(NamedTextColor.DARK_GRAY));

                tick++;

                Location playerLoc = player.getLocation().clone().add(0, 1.2, 0);

                if(tick % 10 == 0) {
                    world.spawnParticle(Particle.ENCHANTED_HIT, playerLoc, 22, 0.6, 0.6, 0.6, 1);
                    world.playSound(playerLoc, Sound.BLOCK_CHAIN_BREAK, 1.2f, 1.0f);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void removePoint(Player player){

        long cools = 0;
        cool.updateCooldown(player, "R", cools);
        cool.updateCooldown(player, "Q", cools);

        long updatedCool = Math.max(config.chainCount.getOrDefault(player.getUniqueId(), 0) * 1000, 600);
        cool.updateCooldown(player, "F", updatedCool);

        config.chainCount.remove(player.getUniqueId());
        config.chainSkill.remove(player.getUniqueId());

        player.sendActionBar(Component.text("⌬").color(NamedTextColor.GRAY));

    }

    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();

    public void updateChainList(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (!tag.Nightel.contains(player)) {
            return;
        }

        if (activeTasks.containsKey(playerUUID)) {
            activeTasks.get(playerUUID).cancel();
            activeTasks.remove(playerUUID);
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !tag.Nightel.contains(player)) {

                    activeTasks.remove(playerUUID);
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                    this.cancel();
                    return;
                }

                Scoreboard scoreboard = player.getScoreboard();
                if (scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
                    scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                    player.setScoreboard(scoreboard);
                }

                Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
                if (objective == null) {
                    objective = scoreboard.registerNewObjective("NIGHTEL", Criteria.DUMMY, Component.text("NIGHTEL"));
                    objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                }

                int chainCount = config.chainCount.getOrDefault(player.getUniqueId(), 0);
                String benzene = (chainCount < 6) ? "⬡ ".repeat(chainCount).trim() : "⌬ ⌬ ⌬ ⌬ ⌬ ⌬";

                Score score1 = objective.getScore(benzene);
                score1.setScore(7);

                player.setScoreboard(scoreboard);
            }
        };

        activeTasks.put(playerUUID, task);
        task.runTaskTimer(plugin, 0, 1L);
    }
}