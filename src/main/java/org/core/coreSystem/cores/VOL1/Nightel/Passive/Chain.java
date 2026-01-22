package org.core.coreSystem.cores.VOL1.Nightel.Passive;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
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

    private static final BlockData CHAIN_DATA = Material.IRON_CHAIN.createBlockData();
    private static final Component ICON_FULL = Component.text("⌬").color(NamedTextColor.GRAY);
    private static final Component ICON_FULL_BAR = Component.text("⌬ ⌬ ⌬ ⌬ ⌬ ⌬").color(NamedTextColor.WHITE);

    private static final String[] HEX_ICONS = new String[7];
    private static final String[] BENZENE_ICONS = new String[7];

    static {
        for (int i = 0; i <= 6; i++) {
            HEX_ICONS[i] = "⬡ ".repeat(i).trim();
            BENZENE_ICONS[i] = "⌬ ".repeat(Math.max(0, 6 - i)).trim();
        }
    }

    public Chain(Nightel config, coreConfig tag, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.tag = tag;
    }

    public void chainCount(Player player, long coolTime, String skill) {
        UUID uuid = player.getUniqueId();
        updateChainList(player);

        int point = config.chainCount.getOrDefault(uuid, 0);

        long cools = (long) Math.min(coolTime * Math.pow(3.3, point), 6000);
        cool.updateCooldown(player, skill, cools);

        if (point < 6) {
            World world = player.getWorld();
            Location playerLoc = player.getLocation();

            String lastSkill = config.chainSkill.getOrDefault(uuid, "");

            boolean isBonusCondition = (skill.equals("R") && !lastSkill.equals("R"))
                    || (skill.equals("Q") && lastSkill.equals("Q"));
            int addPoint = (config.chainSkill.containsKey(uuid) && isBonusCondition) ? 2 : 1;

            for (int i = 0; i < addPoint; i++) {
                int current = config.chainCount.getOrDefault(uuid, 0);
                if (current < 6) {
                    config.chainCount.put(uuid, current + 1);
                } else {
                    break;
                }
            }

            world.playSound(playerLoc, Sound.BLOCK_CHAIN_PLACE, 1.6f, 1.0f);
            world.spawnParticle(Particle.BLOCK, playerLoc.add(0, 1.2, 0), 33, 0.6, 0.6, 0.6, CHAIN_DATA);

            if (addPoint == 2) {
                world.spawnParticle(Particle.ENCHANTED_HIT, playerLoc, 33, 0.6, 0.6, 0.6, 1);
            }

            int finalPoint = config.chainCount.getOrDefault(uuid, 0);

            if (finalPoint < 6) {
                player.sendActionBar(Component.text(HEX_ICONS[finalPoint]).color(NamedTextColor.GRAY));
                chainDecrease(player);
            } else {
                hexaChainLoad(player);
            }
        }

        config.chainSkill.put(uuid, skill);
    }

    private final Map<UUID, BukkitRunnable> activeChainTasks = new HashMap<>();

    public void chainDecrease(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (activeChainTasks.containsKey(playerUUID)) {
            activeChainTasks.get(playerUUID).cancel();
        }

        BukkitRunnable task = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                int currentCount = config.chainCount.getOrDefault(playerUUID, 0);
                boolean isUsingF = config.fskill_using.getOrDefault(playerUUID, false);

                if (currentCount == 0 || currentCount == 6 || player.isDead() || isUsingF) {
                    if (currentCount == 0) {
                        config.chainCount.remove(playerUUID);
                        player.sendActionBar(ICON_FULL);
                    }
                    config.chainSkill.remove(playerUUID);
                    activeChainTasks.remove(playerUUID);
                    cancel();
                    return;
                }

                tick++;

                if (tick % 60 == 0) {
                    if (currentCount > 0 && currentCount < 6) {
                        int nextCount = currentCount - 1;

                        player.sendActionBar(Component.text(HEX_ICONS[nextCount]).color(NamedTextColor.GRAY));
                        config.chainCount.put(playerUUID, nextCount);

                        Location particleLoc = player.getLocation().add(0, 1.2, 0);
                        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, particleLoc, 12, 0.6, 0.6, 0.6, 1);
                        player.getWorld().playSound(particleLoc, Sound.BLOCK_CHAIN_STEP, 2.2f, 1.0f);
                    }
                }
            }
        };

        activeChainTasks.put(playerUUID, task);
        task.runTaskTimer(plugin, 0, 1L);
    }

    public void hexaChainLoad(Player player) {
        World world = player.getWorld();

        world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.6f, 1.0f);
        player.setWalkSpeed(0.2f * (4.0f / 3.0f));

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick > 60 || player.isDead() || config.fskill_using.getOrDefault(player.getUniqueId(), false)) {
                    player.setWalkSpeed(0.2f);
                    removePoint(player);
                    cancel();
                    return;
                }

                int index = tick / 10;
                if (index < BENZENE_ICONS.length) {
                    player.sendActionBar(Component.text(BENZENE_ICONS[index]).color(NamedTextColor.DARK_GRAY));
                }

                tick++;

                if (tick % 10 == 0) {
                    Location pLoc = player.getLocation().add(0, 1.2, 0);
                    world.spawnParticle(Particle.ENCHANTED_HIT, pLoc, 22, 0.6, 0.6, 0.6, 1);
                    world.playSound(pLoc, Sound.BLOCK_CHAIN_BREAK, 1.2f, 1.0f);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void removePoint(Player player) {
        UUID uuid = player.getUniqueId();
        cool.updateCooldown(player, "R", 0);
        cool.updateCooldown(player, "Q", 0);

        long updatedCool = Math.max(config.chainCount.getOrDefault(uuid, 0) * 1000L, 600L);
        cool.updateCooldown(player, "F", updatedCool);

        config.chainCount.remove(uuid);
        config.chainSkill.remove(uuid);

        player.sendActionBar(ICON_FULL);
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

                int chainCount = config.chainCount.getOrDefault(playerUUID, 0);

                String benzene;
                if (chainCount < 6) {
                    benzene = HEX_ICONS[chainCount];
                } else {
                    benzene = "⌬ ⌬ ⌬ ⌬ ⌬ ⌬";
                }

                Score score1 = objective.getScore(benzene);
                score1.setScore(7);

                player.setScoreboard(scoreboard);
            }
        };

        activeTasks.put(playerUUID, task);
        task.runTaskTimer(plugin, 0, 1L);
    }
}