package org.core.Level;

import it.unimi.dsi.fastutil.Hash;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;
import org.core.Core;
import org.core.coreConfig;
import org.core.coreProgram.Cores.Bambo.coreSystem.bambLeveling;
import org.core.coreProgram.Cores.Benzene.coreSystem.benzLeveling;
import org.core.coreProgram.Cores.Blaze.coreSystem.blazeLeveling;
import org.core.coreProgram.Cores.Carpenter.coreSystem.carpLeveling;
import org.core.coreProgram.Cores.Commander.coreSystem.comLeveling;
import org.core.coreProgram.Cores.Dagger.coreSystem.dagLeveling;
import org.core.coreProgram.Cores.Glacier.coreSystem.glaLeveling;
import org.core.coreProgram.Cores.Knight.coreSystem.knightLeveling;
import org.core.coreProgram.Cores.Luster.coreSystem.lustLeveling;
import org.core.coreProgram.Cores.Nox.coreSystem.noxLeveling;
import org.core.coreProgram.Cores.Pyro.coreSystem.pyroLeveling;
import org.core.playerSettings.persistentPlayerHashMap;
import org.core.playerSettings.persistentPlayerSet;

import javax.naming.Name;
import java.util.*;

public class LevelingManager implements Listener {

    private final JavaPlugin plugin;
    private final coreConfig config;

    public Map<Player, Long> Level;
    public Map<Player, Long> Exp;

    public LevelingManager(JavaPlugin plugin, coreConfig config){
        this.plugin = plugin;
        this.config = config;

        this.Level = new persistentPlayerHashMap(plugin, "level");
        this.Exp = new persistentPlayerHashMap(plugin, "exp");
    }

    @EventHandler
    public void Join(PlayerJoinEvent event){
        Player player = event.getPlayer();

        levelActionBar(player);

        Long level = player.getPersistentDataContainer().getOrDefault(
                new NamespacedKey(plugin, "level"), PersistentDataType.LONG, 0L
        );

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(20.0 + 2.0 * level);
        }
    }


    @EventHandler
    public void Respawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Long level = player.getPersistentDataContainer().getOrDefault(
                new NamespacedKey(plugin, "level"), PersistentDataType.LONG, 0L
        );

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(20.0 + 2.0 * level);
                player.setHealth(maxHealth.getBaseValue());
            }
        }, 1L);
    }

    @EventHandler
    public void Exp(PlayerExpChangeEvent event){

        if(event.getAmount() > 0){
            Player player = event.getPlayer();
            long exp = event.getAmount();

            switch (config.getPlayerCore(player)) {
                case "benzene" :
                    benzLeveling benzene = new benzLeveling(plugin, player, exp);
                    benzene.addExp(player);
                    break;
                case "nox" :
                    noxLeveling nox = new noxLeveling(plugin, player, exp);
                    nox.addExp(player);
                    break;
                case "bambo" :
                    bambLeveling bambo = new bambLeveling(plugin, player, exp);
                    bambo.addExp(player);
                    break;
                case "carpenter" :
                    carpLeveling carpenter = new carpLeveling(plugin, player, exp);
                    carpenter.addExp(player);
                    break;
                case "dagger" :
                    dagLeveling dagger = new dagLeveling(plugin, player, exp);
                    dagger.addExp(player);
                    break;
                case "pyro" :
                    pyroLeveling pyro = new pyroLeveling(plugin, player, exp);
                    pyro.addExp(player);
                    break;
                case "glacier" :
                    glaLeveling glacier = new glaLeveling(plugin, player, exp);
                    glacier.addExp(player);
                    break;
                case "knight" :
                    knightLeveling knight = new knightLeveling(plugin, player, exp);
                    knight.addExp(player);
                    break;
                case "luster" :
                    lustLeveling luster = new lustLeveling(plugin, player, exp);
                    luster.addExp(player);
                    break;
                case "blaze" :
                    blazeLeveling blaze = new blazeLeveling(plugin, player, exp);
                    blaze.addExp(player);
                    break;
                case "commander" :
                    comLeveling commander = new comLeveling(plugin, player, exp);
                    commander.addExp(player);
                    break;
                default :
                    break;
            }
        }
    }

    public HashMap<UUID, BukkitRunnable> runnableHashMap = new HashMap<>();

    public void levelActionBar(Player player) {

        if(runnableHashMap.containsKey(player.getUniqueId())){
            runnableHashMap.get(player.getUniqueId()).cancel();
            runnableHashMap.remove(player.getUniqueId());
        }

        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {

                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                Long level = player.getPersistentDataContainer().getOrDefault(
                        new NamespacedKey(plugin, "level"), PersistentDataType.LONG, 0L
                );
                Long exp = player.getPersistentDataContainer().getOrDefault(
                        new NamespacedKey(plugin, "exp"), PersistentDataType.LONG, 0L
                );

                Scoreboard scoreboard = player.getScoreboard();
                if (scoreboard == null || scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
                    scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                    player.setScoreboard(scoreboard);
                }

                Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
                if (objective == null) {
                    objective = scoreboard.registerNewObjective(
                            config.getPlayerCore(player),
                            Criteria.DUMMY,
                            Component.text(config.getPlayerCore(player))
                    );
                    objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                }

                scoreboard.getEntries().forEach(scoreboard::resetScores);

                objective.getScore("------------").setScore(10);
                objective.getScore("§aLv." + level).setScore(9);
                objective.getScore("§eCORE EXP : " + exp).setScore(8);
            }
        };

        runnableHashMap.put(player.getUniqueId(), runnable);
        runnable.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void lvAtk(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) return;

        long level = player.getPersistentDataContainer().getOrDefault(
                new NamespacedKey(plugin, "level"),
                PersistentDataType.LONG,
                0L
        );

        if (level < 0) return;
        if (level > 10) level = 10;

        double[] cumulativeP = {0.0, 0.15, 0.32, 0.49, 0.66, 0.86, 1.09, 1.36, 1.68, 2.06, 2.50};

        double p = cumulativeP[(int)level];

        double originalDamage = event.getDamage();
        double amplifiedDamage = originalDamage * (1 + p);

        event.setDamage(amplifiedDamage);
    }
}
