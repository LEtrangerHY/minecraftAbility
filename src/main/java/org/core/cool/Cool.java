package org.core.cool;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class Cool {

    private final JavaPlugin plugin;

    public Cool(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private static class CooldownData {
        long endTime;
        long totalDuration;
        long remainingOnPause;
        BossBar bossBar;
        BukkitRunnable cooldownTask;
        boolean isOnCooldown;
        boolean isPaused;

        CooldownData(long endTime, long totalDuration, BossBar bossBar, BukkitRunnable cooldownTask, boolean isOnCooldown) {
            this.endTime = endTime;
            this.totalDuration = totalDuration;
            this.bossBar = bossBar;
            this.cooldownTask = cooldownTask;
            this.isOnCooldown = isOnCooldown;
            this.isPaused = false;
            this.remainingOnPause = 0;
        }
    }

    private final HashMap<UUID, HashMap<String, CooldownData>> cooldowns = new HashMap<>();

    private void refreshBossBar(CooldownData data) {
        if (data.bossBar == null) return;

        long remaining;
        if (data.isPaused) {
            remaining = data.remainingOnPause;
        } else {
            remaining = data.endTime - System.currentTimeMillis();
        }

        if (remaining < 0) remaining = 0;

        if (data.totalDuration <= 0) {
            data.bossBar.setProgress(0.0);
            return;
        }

        double progress = (double) remaining / data.totalDuration;

        if (Double.isNaN(progress) || Double.isInfinite(progress)) {
            progress = 0.0;
        }

        data.bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
    }

    public void pauseCooldown(Player player, String skill) {
        UUID playerId = player.getUniqueId();

        if (!cooldowns.containsKey(playerId) || !cooldowns.get(playerId).containsKey(skill)) {
            return;
        }

        CooldownData data = cooldowns.get(playerId).get(skill);

        if (data.isPaused || !data.isOnCooldown) {
            return;
        }

        long remaining = data.endTime - System.currentTimeMillis();
        data.remainingOnPause = Math.max(0, remaining);
        data.isPaused = true;

        if (data.cooldownTask != null && !data.cooldownTask.isCancelled()) {
            data.cooldownTask.cancel();
        }

        refreshBossBar(data);
    }

    public void resumeCooldown(Player player, String skill) {
        UUID playerId = player.getUniqueId();

        if (!cooldowns.containsKey(playerId) || !cooldowns.get(playerId).containsKey(skill)) {
            return;
        }

        CooldownData data = cooldowns.get(playerId).get(skill);

        if (!data.isPaused) {
            return;
        }

        long now = System.currentTimeMillis();
        data.endTime = now + data.remainingOnPause;
        data.isPaused = false;

        startCooldownTask(player, skill, data);
    }

    private void startCooldownTask(Player player, String skill, CooldownData data) {
        if (data.cooldownTask != null && !data.cooldownTask.isCancelled()) {
            data.cooldownTask.cancel();
        }

        refreshBossBar(data);

        data.cooldownTask = new BukkitRunnable() {
            @Override
            public void run() {
                long remainingTime = data.endTime - System.currentTimeMillis();

                if (remainingTime <= 0 || !player.isOnline()) {
                    data.bossBar.setProgress(0);
                    data.bossBar.removePlayer(player);

                    if (cooldowns.containsKey(player.getUniqueId())) {
                        cooldowns.get(player.getUniqueId()).remove(skill);
                    }

                    data.isOnCooldown = false;
                    cancel();
                } else {
                    refreshBossBar(data);
                }
            }
        };
        data.cooldownTask.runTaskTimer(plugin, 0L, 1L);
    }

    public boolean isReloading(Player player, String skill) {
        UUID playerId = player.getUniqueId();
        if (cooldowns.containsKey(playerId) && cooldowns.get(playerId).containsKey(skill)) {
            CooldownData cooldownData = cooldowns.get(playerId).get(skill);
            return cooldownData.isOnCooldown || cooldownData.isPaused;
        }
        return false;
    }

    public void setCooldown(Player player, long duration, String skill) {
        UUID playerId = player.getUniqueId();
        cooldowns.putIfAbsent(playerId, new HashMap<>());

        long cooldownEndTime = System.currentTimeMillis() + duration;
        CooldownData existingData = cooldowns.get(playerId).get(skill);

        if (existingData == null || (!existingData.isOnCooldown && !existingData.isPaused)) {
            BossBar bossBar;
            if (existingData == null || existingData.bossBar == null) {
                bossBar = Bukkit.createBossBar(skill + " Cooldown", BarColor.WHITE, BarStyle.SOLID);
                bossBar.addPlayer(player);
            } else {
                bossBar = existingData.bossBar;
                bossBar.setProgress(1.0);
            }

            CooldownData newData = new CooldownData(cooldownEndTime, duration, bossBar, null, true);
            cooldowns.get(playerId).put(skill, newData);

            startCooldownTask(player, skill, newData);
        }
    }

    public void updateCooldown(Player player, String skill, long newRemainingTime) {
        UUID playerId = player.getUniqueId();
        cooldowns.putIfAbsent(playerId, new HashMap<>());

        HashMap<String, CooldownData> playerCooldowns = cooldowns.get(playerId);
        CooldownData data = playerCooldowns.get(skill);

        long now = System.currentTimeMillis();
        long newEndTime = now + newRemainingTime;

        if (data == null) {
            BossBar bossBar = Bukkit.createBossBar(skill + " Cooldown", BarColor.WHITE, BarStyle.SOLID);
            bossBar.addPlayer(player);

            data = new CooldownData(newEndTime, newRemainingTime, bossBar, null, true);
            playerCooldowns.put(skill, data);
            startCooldownTask(player, skill, data);

        } else {
            if (newRemainingTime > data.totalDuration) {
                data.totalDuration = newRemainingTime;
            }

            data.endTime = newEndTime;
            data.remainingOnPause = newRemainingTime;
            data.isPaused = false;
            data.isOnCooldown = true;

            refreshBossBar(data);
            startCooldownTask(player, skill, data);
        }
    }

    public long getRemainCooldown(Player player, String skill) {
        UUID playerId = player.getUniqueId();

        if (cooldowns.containsKey(playerId) && cooldowns.get(playerId).containsKey(skill)) {
            CooldownData data = cooldowns.get(playerId).get(skill);

            if (data.isPaused) {
                return data.remainingOnPause;
            }

            if (data.isOnCooldown) {
                long remainingTime = data.endTime - System.currentTimeMillis();
                return Math.max(0, remainingTime);
            }
        }
        return 0L;
    }
}