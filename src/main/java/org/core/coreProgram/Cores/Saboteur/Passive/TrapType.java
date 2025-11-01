package org.core.coreProgram.Cores.Saboteur.Passive;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.core.Cool.Cool;
import org.core.Effect.EffectManager;
import org.core.coreConfig;
import org.core.coreProgram.Cores.Saboteur.coreSystem.Saboteur;

import java.util.*;

public class TrapType {
    private final coreConfig tag;
    private final Saboteur config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public TrapType(coreConfig tag, Saboteur config, JavaPlugin plugin, Cool cool) {
        this.tag = tag;
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();

    public void trapTypeBoard(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (activeTasks.containsKey(playerUUID) || !tag.Saboteur.contains(player)) {
            return;
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !tag.Saboteur.contains(player) || player.isDead()) {
                    activeTasks.remove(playerUUID);
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                    this.cancel();
                    return;
                }

                Scoreboard scoreboard = player.getScoreboard();
                if (scoreboard == null || scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
                    scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                    player.setScoreboard(scoreboard);
                }

                Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
                if (objective == null) {
                    objective = scoreboard.registerNewObjective("SABOTEUR", Criteria.DUMMY, Component.text("SABOTEUR"));
                    objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                }

                String trap = (config.trapType.getOrDefault(player.getUniqueId(), 1) == 1) ? "Spike" : "Throw" ;

                objective.getScore("TrapType : " + trap).setScore(6);

                player.setScoreboard(scoreboard);
            }
        };

        activeTasks.put(playerUUID, task);
        task.runTaskTimer(plugin, 0, 1L);
    }
}
