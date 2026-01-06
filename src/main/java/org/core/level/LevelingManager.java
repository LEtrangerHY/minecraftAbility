package org.core.level;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.core.coreSystem.cores.VOL2.Lavender.coreSystem.lavLeveling;
import org.core.main.coreConfig;
import org.core.coreSystem.cores.VOL1.Bambo.coreSystem.bambLeveling;
import org.core.coreSystem.cores.VOL1.Benzene.coreSystem.benzLeveling;
import org.core.coreSystem.cores.VOL1.Blaze.coreSystem.blazeLeveling;
import org.core.coreSystem.cores.VOL1.Bloom.coreSystem.bloomLeveling;
import org.core.coreSystem.cores.VOL1.Blue.coreSystem.blueLeveling;
import org.core.coreSystem.cores.VOL1.Carpenter.coreSystem.carpLeveling;
import org.core.coreSystem.cores.VOL1.Commander.coreSystem.comLeveling;
import org.core.coreSystem.cores.VOL1.Dagger.coreSystem.dagLeveling;
import org.core.coreSystem.cores.VOL1.Glacier.coreSystem.glaLeveling;
import org.core.coreSystem.cores.VOL1.Harvester.coreSystem.harvLeveling;
import org.core.coreSystem.cores.VOL1.Knight.coreSystem.knightLeveling;
import org.core.coreSystem.cores.VOL1.Luster.coreSystem.lustLeveling;
import org.core.coreSystem.cores.VOL1.Nightel.coreSystem.nightLeveling;
import org.core.coreSystem.cores.VOL1.Pyro.coreSystem.pyroLeveling;
import org.core.coreSystem.cores.VOL1.Saboteur.coreSystem.sabLeveling;
import org.core.coreSystem.cores.VOL1.Swordsman.coreSystem.swordLeveling;
import org.core.coreSystem.cores.VOL2.Burst.coreSystem.burstLeveling;
import org.core.playerSettings.persistentPlayerHashMap;

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

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        levelScoreBoard(player);
        applyLevelHealth(player, false);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        levelScoreBoard(player);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            applyLevelHealth(player, true);
        }, 1L);
    }

    public void applyLevelHealth(Player player, boolean healFull) {
        long level = player.getPersistentDataContainer().getOrDefault(
                new NamespacedKey(plugin, "level"), PersistentDataType.LONG, 0L
        );

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double base = maxHealth.getDefaultValue();
            double newMax = base + (4.0 * level);

            maxHealth.setBaseValue(newMax);

            if (healFull) {
                player.setHealth(newMax);
            }
        }
    }

    @EventHandler
    public void Exp(PlayerExpChangeEvent event){

        if(event.getAmount() > 0){
            Player player = event.getPlayer();
            long exp = event.getAmount();

            switch (config.getPlayerCore(player)) {
                case "BENZENE" :
                    benzLeveling benzene = new benzLeveling(plugin, player, exp);
                    benzene.addExp(player);
                    break;
                case "NIGHTEL" :
                    nightLeveling nox = new nightLeveling(plugin, player, exp);
                    nox.addExp(player);
                    break;
                case "BAMBO" :
                    bambLeveling bambo = new bambLeveling(plugin, player, exp);
                    bambo.addExp(player);
                    break;
                case "CARPENTER" :
                    carpLeveling carpenter = new carpLeveling(plugin, player, exp);
                    carpenter.addExp(player);
                    break;
                case "DAGGER" :
                    dagLeveling dagger = new dagLeveling(plugin, player, exp);
                    dagger.addExp(player);
                    break;
                case "PYRO" :
                    pyroLeveling pyro = new pyroLeveling(plugin, player, exp);
                    pyro.addExp(player);
                    break;
                case "GLACIER" :
                    glaLeveling glacier = new glaLeveling(plugin, player, exp);
                    glacier.addExp(player);
                    break;
                case "KNIGHT" :
                    knightLeveling knight = new knightLeveling(plugin, player, exp);
                    knight.addExp(player);
                    break;
                case "LUSTER" :
                    lustLeveling luster = new lustLeveling(plugin, player, exp);
                    luster.addExp(player);
                    break;
                case "BLAZE" :
                    blazeLeveling blaze = new blazeLeveling(plugin, player, exp);
                    blaze.addExp(player);
                    break;
                case "COMMANDER" :
                    comLeveling commander = new comLeveling(plugin, player, exp);
                    commander.addExp(player);
                    break;
                case "HARVESTER" :
                    harvLeveling harvester = new harvLeveling(plugin, player, exp);
                    harvester.addExp(player);
                    break;
                case "BLOOM" :
                    bloomLeveling bloom = new bloomLeveling(plugin, player, exp);
                    bloom.addExp(player);
                    break;
                case "BLUE" :
                    blueLeveling blue = new blueLeveling(plugin, player, exp);
                    blue.addExp(player);
                    break;
                case "SWORDSMAN" :
                    swordLeveling sword = new swordLeveling(plugin, player, exp);
                    sword.addExp(player);
                    break;
                case "SABOTEUR" :
                    sabLeveling saboteur = new sabLeveling(plugin, player, exp);
                    saboteur.addExp(player);
                    break;
                case "BURST" :
                    burstLeveling burst = new burstLeveling(plugin, player, exp);
                    burst.addExp(player);
                    break;
                case "LAVENDER" :
                    lavLeveling lavender = new lavLeveling(plugin, player, exp);
                    lavender.addExp(player);
                    break;
                default :
                    break;
            }
        }
    }

    public HashMap<UUID, BukkitRunnable> runnableHashMap = new HashMap<>();

    public void levelScoreBoard(Player player) {

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

        double p = 0.005 * level * level + 0.055 * level;

        double originalDamage = event.getDamage();
        double amplifiedDamage = originalDamage * (1 + p);

        event.setDamage(amplifiedDamage);
    }
}
