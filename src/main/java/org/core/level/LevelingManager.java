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
import org.core.coreSystem.cores.KEY.Benzene.coreSystem.Benzene;
import org.core.coreSystem.cores.KEY.Nightel.coreSystem.Nightel;
import org.core.coreSystem.cores.KEY.PLAYER.coreSystem.PLAYER;
import org.core.coreSystem.cores.VOL1.Bamboo.coreSystem.Bamboo;
import org.core.coreSystem.cores.VOL1.Blossom.coreSystem.Blossom;
import org.core.coreSystem.cores.VOL1.Commander.coreSystem.Commander;
import org.core.coreSystem.cores.VOL1.Dagger.coreSystem.Dagger;
import org.core.coreSystem.cores.VOL1.Glacier.coreSystem.Glacier;
import org.core.coreSystem.cores.VOL1.Harvester.coreSystem.Harvester;
import org.core.coreSystem.cores.VOL1.Pyro.coreSystem.Pyro;
import org.core.coreSystem.cores.VOL1.Saboteur.coreSystem.Saboteur;
import org.core.coreSystem.cores.VOL2.Knight.coreSystem.Knight;
import org.core.coreSystem.cores.VOL2.Rose.coreSystem.Rose;
import org.core.coreSystem.cores.VOL2.Undead.coreSystem.Undead;
import org.core.coreSystem.cores.VOL3.Burst.coreSystem.Burst;
import org.core.coreSystem.cores.VOL3.Cheshire.coreSystem.Cheshire;
import org.core.coreSystem.cores.VOL3.Stroke.coreSystem.Stroke;
import org.core.coreSystem.cores.VOL4.Blaze.coreSystem.Blaze;
import org.core.coreSystem.cores.VOL4.Blue.coreSystem.Blue;
import org.core.coreSystem.cores.VOL5.Apocalypse.coreSystem.Apocalypse;
import org.core.coreSystem.cores.VOL5.Scout.coreSystem.Scout;
import org.core.coreSystem.cores.VOL6.Adam.coreSystem.Adam;
import org.core.coreSystem.cores.VOL6.Carpenter.coreSystem.Carpenter;
import org.core.coreSystem.cores.VOL6.Charlotte.coreSystem.Charlotte;
import org.core.coreSystem.cores.VOL6.Darmes.coreSystem.Darmes;
import org.core.coreSystem.cores.VOL6.Jester.coreSystem.Jester;
import org.core.coreSystem.cores.VOL6.Lavender.coreSystem.Lavender;
import org.core.coreSystem.cores.VOL6.Luster.coreSystem.Luster;
import org.core.coreSystem.cores.VOL6.Residue.coreSystem.Residue;
import org.core.coreSystem.cores.VOL6.Swordsman.coreSystem.Swordsman;
import org.core.main.coreConfig;
import org.core.playerSettings.persistentPlayerHashMap;

import java.util.*;

public class LevelingManager implements Listener {

    private static final double BASE_HEALTH = 60.0;
    private static final double HEALTH_PER_LEVEL = 6.0;
    private static final List<Double> DAMAGE_RATE = List.of(1.0, 1.05, 1.09, 1.28, 1.66, 2.03, 2.5, 3.06, 3.53, 3.81, 4.0);

    private static final Map<String, List<Long>> REQUIRE_EXP = Map.ofEntries(
            Map.entry("PLAYER", PLAYER.REQUIRE_EXP),
            Map.entry("NIGHTEL", Nightel.REQUIRE_EXP),
            Map.entry("BENZENE", Benzene.REQUIRE_EXP),
            Map.entry("BAMBOO", Bamboo.REQUIRE_EXP),
            Map.entry("CARPENTER", Carpenter.REQUIRE_EXP),
            Map.entry("DAGGER", Dagger.REQUIRE_EXP),
            Map.entry("PYRO", Pyro.REQUIRE_EXP),
            Map.entry("GLACIER", Glacier.REQUIRE_EXP),
            Map.entry("KNIGHT", Knight.REQUIRE_EXP),
            Map.entry("LUSTER", Luster.REQUIRE_EXP),
            Map.entry("BLAZE", Blaze.REQUIRE_EXP),
            Map.entry("COMMANDER", Commander.REQUIRE_EXP),
            Map.entry("HARVESTER", Harvester.REQUIRE_EXP),
            Map.entry("BLOSSOM", Blossom.REQUIRE_EXP),
            Map.entry("BLUE", Blue.REQUIRE_EXP),
            Map.entry("SWORDSMAN", Swordsman.REQUIRE_EXP),
            Map.entry("SABOTEUR", Saboteur.REQUIRE_EXP),
            Map.entry("BURST", Burst.REQUIRE_EXP),
            Map.entry("LAVENDER", Lavender.REQUIRE_EXP),
            Map.entry("ROSE", Rose.REQUIRE_EXP),
            Map.entry("RESIDUE", Residue.REQUIRE_EXP),
            Map.entry("DARMES", Darmes.REQUIRE_EXP),
            Map.entry("CHARLOTTE", Charlotte.REQUIRE_EXP),
            Map.entry("ADAM", Adam.REQUIRE_EXP),
            Map.entry("UNDEAD", Undead.REQUIRE_EXP),
            Map.entry("JESTER", Jester.REQUIRE_EXP),
            Map.entry("CHESHIRE", Cheshire.REQUIRE_EXP),
            Map.entry("STROKE", Stroke.REQUIRE_EXP),
            Map.entry("SCOUT", Scout.REQUIRE_EXP),
            Map.entry("APOCALYPSE", Apocalypse.REQUIRE_EXP)
    );

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

    public static double damageRate(long level) {
        if (level < 0) level = 0;
        if (level > DAMAGE_RATE.size() - 1) level = DAMAGE_RATE.size() - 1;

        return DAMAGE_RATE.get((int) level);
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
            double newMax = BASE_HEALTH + (HEALTH_PER_LEVEL * level);

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
            List<Long> requireExp = REQUIRE_EXP.get(config.getPlayerCore(player));
            if (requireExp == null) return;

            addExp(player, event.getAmount(), requireExp);
        }
    }

    private void addExp(Player player, long exp, List<Long> requireExp) {
        NamespacedKey expKey = new NamespacedKey(plugin, "exp");
        NamespacedKey levelKey = new NamespacedKey(plugin, "level");

        long currentLevel = player.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.LONG, 0L);
        long currentExp = player.getPersistentDataContainer().getOrDefault(expKey, PersistentDataType.LONG, 0L);

        long maxExp = Collections.max(requireExp);

        if (currentLevel < requireExp.size()) {
            player.sendMessage("§e" + "exp : " + exp + " 획득");
        }

        for (int i = 0; i < exp; i++) {
            if (currentLevel >= requireExp.size()) {
                break;
            }
            if (currentExp >= maxExp) {
                break;
            }

            currentExp++;

            player.getPersistentDataContainer().set(expKey, PersistentDataType.LONG, currentExp);

            if (requireExp.contains(currentExp)) {
                addLV(player);
                currentLevel++;
            }
        }
    }

    private void addLV(Player player) {
        NamespacedKey levelKey = new NamespacedKey(plugin, "level");

        long current = player.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.LONG, 0L);
        player.getPersistentDataContainer().set(levelKey, PersistentDataType.LONG, current + 1);

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getBaseValue() + HEALTH_PER_LEVEL);

            player.setHealth(maxHealth.getBaseValue());
        }

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);
        player.sendMessage("§a" + "level " + (current + 1) + " 로 상승!");
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

        event.setDamage(event.getDamage() * damageRate(level));
    }
}