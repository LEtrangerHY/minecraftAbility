package org.core.coreSystem.cores.VOL2.Undead.Passive;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.core.main.coreConfig;
import org.core.coreSystem.cores.VOL2.Undead.coreSystem.Undead;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Boost {

    private final Undead config;
    private final JavaPlugin plugin;
    private final coreConfig tag;

    private final Map<UUID, BukkitRunnable> activeScoreboardTasks = new HashMap<>();

    public Boost(Undead config, coreConfig tag, JavaPlugin plugin) {
        this.config = config;
        this.tag = tag;
        this.plugin = plugin;
    }

    public void updateUndeadBoard(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (!tag.Undead.contains(player)) {
            return;
        }

        if (activeScoreboardTasks.containsKey(playerUUID)) {
            activeScoreboardTasks.get(playerUUID).cancel();
            activeScoreboardTasks.remove(playerUUID);
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !tag.Undead.contains(player)) {
                    activeScoreboardTasks.remove(playerUUID);
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
                    objective = scoreboard.registerNewObjective("Undead", Criteria.DUMMY, net.kyori.adventure.text.Component.text("Undead"));
                    objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                }

                double buffRate = config.f_buff_rate.getOrDefault(playerUUID, 0.0);
                int buffTicks = config.f_buff_ticks.getOrDefault(playerUUID, 0);
                if (buffRate > 0) {
                    String buffText = String.format("§aBoost: %.0f%% (%.1fs)", buffRate * 100, buffTicks / 20.0);
                    objective.getScore(buffText).setScore(7);
                }

                double debuffRate = config.f_debuff_rate.getOrDefault(playerUUID, 0.0);
                if (debuffRate > 0) {
                    String debuffText = String.format("§cDamage: %.0f%%", debuffRate * 100);
                    objective.getScore(debuffText).setScore(6);
                }

                player.setScoreboard(scoreboard);
            }
        };

        activeScoreboardTasks.put(playerUUID, task);
        task.runTaskTimer(plugin, 0, 1L);
    }
}