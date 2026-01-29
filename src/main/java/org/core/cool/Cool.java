package org.core.cool;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class Cool {

    private final JavaPlugin plugin;
    // UUID -> SkillName -> Data
    private final HashMap<UUID, HashMap<String, CooldownData>> cooldowns = new HashMap<>();

    public Cool(JavaPlugin plugin) {
        this.plugin = plugin;
        startGlobalTickerTask();
    }

    private static class CooldownData {
        long endTime;
        long totalDuration;
        long remainingOnPause;
        boolean isPaused;
        BossBar bossBar; // 보스바 객체 추가

        CooldownData(long endTime, long totalDuration, BossBar bossBar) {
            this.endTime = endTime;
            this.totalDuration = totalDuration;
            this.isPaused = false;
            this.remainingOnPause = 0;
            this.bossBar = bossBar;
        }
    }

    /**
     * 모든 온라인 플레이어의 쿨타임(액션바 + 보스바)을 갱신하는 글로벌 태스크
     */
    private void startGlobalTickerTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    if (cooldowns.containsKey(uuid)) {

                        // 1. 보스바 업데이트 및 완료된 쿨타임 정리
                        updateBossBarsAndCleanup(player);

                        // 2. 액션바 전송
                        sendActionBarDisplay(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void updateBossBarsAndCleanup(Player player) {
        HashMap<String, CooldownData> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return;

        Iterator<Map.Entry<String, CooldownData>> iterator = playerCooldowns.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, CooldownData> entry = iterator.next();
            CooldownData data = entry.getValue();

            long remaining = 0;
            if (data.isPaused) {
                remaining = data.remainingOnPause;
            } else {
                remaining = data.endTime - System.currentTimeMillis();
            }

            if (data.bossBar != null) {
                if (remaining > 0) {
                    double progress = (double) remaining / data.totalDuration;
                    data.bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                    if (!data.bossBar.getPlayers().contains(player)) {
                        data.bossBar.addPlayer(player);
                    }
                } else {
                    data.bossBar.removeAll();
                    data.bossBar = null;
                }
            }
        }
    }

    private void sendActionBarDisplay(Player player) {
        Component spacer = Component.text("       ");

        Component rComponent = getSkillComponent(player, "R");
        Component qComponent = getSkillComponent(player, "Q");
        Component fComponent = getSkillComponent(player, "F");

        Component fullBar = Component.empty()
                .append(rComponent)
                .append(spacer)
                .append(qComponent)
                .append(spacer)
                .append(fComponent);

        player.sendActionBar(fullBar);
    }

    private Component getSkillComponent(Player player, String skillName) {
        long remaining = getRemainCooldown(player, skillName);

        Component label = Component.text(skillName + " : ").color(NamedTextColor.WHITE);
        Component status;

        if (remaining > 0) {
            double seconds = remaining / 1000.0;
            String timeText = String.format("%.1fs", seconds);
            status = Component.text(timeText).color(NamedTextColor.YELLOW);
        } else {
            status = Component.text("Ready").color(NamedTextColor.GREEN);
        }

        return label.append(status);
    }

    public void pauseCooldown(Player player, String skill) {
        UUID playerId = player.getUniqueId();
        if (!cooldowns.containsKey(playerId) || !cooldowns.get(playerId).containsKey(skill)) return;

        CooldownData data = cooldowns.get(playerId).get(skill);
        if (data.isPaused) return;

        long remaining = data.endTime - System.currentTimeMillis();
        if (remaining <= 0) return;

        data.remainingOnPause = remaining;
        data.isPaused = true;
    }

    public void resumeCooldown(Player player, String skill) {
        UUID playerId = player.getUniqueId();
        if (!cooldowns.containsKey(playerId) || !cooldowns.get(playerId).containsKey(skill)) return;

        CooldownData data = cooldowns.get(playerId).get(skill);
        if (!data.isPaused) return;

        long now = System.currentTimeMillis();
        data.endTime = now + data.remainingOnPause;
        data.isPaused = false;
    }

    public boolean isReloading(Player player, String skill) {
        return getRemainCooldown(player, skill) > 0;
    }

    public void setCooldown(Player player, long duration, String skill) {
        setCooldown(player, duration, skill, null);
    }

    public void setCooldown(Player player, long duration, String skill, String type) {
        UUID playerId = player.getUniqueId();
        cooldowns.putIfAbsent(playerId, new HashMap<>());

        long cooldownEndTime = System.currentTimeMillis() + duration;

        CooldownData oldData = cooldowns.get(playerId).get(skill);
        BossBar bossBar = null;

        if (type != null && type.equalsIgnoreCase("boss")) {
            if (oldData != null && oldData.bossBar != null) {
                bossBar = oldData.bossBar;
            } else {
                bossBar = Bukkit.createBossBar(skill, BarColor.WHITE, BarStyle.SOLID);
            }
            bossBar.addPlayer(player);
            bossBar.setProgress(1.0);
            bossBar.setTitle(skill);
        } else {
            if (oldData != null && oldData.bossBar != null) {
                oldData.bossBar.removeAll();
            }
        }

        CooldownData newData = new CooldownData(cooldownEndTime, duration, bossBar);
        cooldowns.get(playerId).put(skill, newData);
    }

    public void updateCooldown(Player player, String skill, long newRemainingTime) {
        updateCooldown(player, skill, newRemainingTime, null);
    }

    public void updateCooldown(Player player, String skill, long newRemainingTime, String type) {
        UUID playerId = player.getUniqueId();
        cooldowns.putIfAbsent(playerId, new HashMap<>());

        HashMap<String, CooldownData> playerCooldowns = cooldowns.get(playerId);
        CooldownData data = playerCooldowns.get(skill);

        long now = System.currentTimeMillis();
        long newEndTime = now + newRemainingTime;

        BossBar bossBar = null;

        if (data == null) {
            if (type != null && type.equalsIgnoreCase("boss")) {
                bossBar = Bukkit.createBossBar(skill, BarColor.WHITE, BarStyle.SOLID);
                bossBar.addPlayer(player);
            }
            data = new CooldownData(newEndTime, newRemainingTime, bossBar);
            playerCooldowns.put(skill, data);
        } else {
            data.endTime = newEndTime;
            data.remainingOnPause = newRemainingTime;
            data.isPaused = false;

            if (newRemainingTime > data.totalDuration) {
                data.totalDuration = newRemainingTime;
            }

            if (type != null && type.equalsIgnoreCase("boss")) {
                if (data.bossBar == null) {
                    data.bossBar = Bukkit.createBossBar(skill, BarColor.WHITE, BarStyle.SOLID);
                    data.bossBar.addPlayer(player);
                }
            }
        }
    }

    public long getRemainCooldown(Player player, String skill) {
        UUID playerId = player.getUniqueId();

        if (cooldowns.containsKey(playerId) && cooldowns.get(playerId).containsKey(skill)) {
            CooldownData data = cooldowns.get(playerId).get(skill);

            if (data.isPaused) {
                return data.remainingOnPause;
            }

            long remainingTime = data.endTime - System.currentTimeMillis();
            return Math.max(0, remainingTime);
        }
        return 0L;
    }
}