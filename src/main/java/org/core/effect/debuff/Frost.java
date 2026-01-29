package org.core.effect.debuff;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class Frost implements Debuffs {
    // 쿨타임/지속시간 관리용
    private static final HashMap<UUID, Long> frostbiteEntities = new HashMap<>();
    // 보스바 관리용 (플레이어 UUID -> BossBar)
    private static final HashMap<UUID, BossBar> activeBars = new HashMap<>();

    private final Entity target;
    private final long duration;

    public Frost(Entity target, long duration) {
        this.target = target;
        this.duration = duration;
    }

    @Override
    public void applyEffect(Entity entity) {
        if (!(target instanceof LivingEntity)) return;
        if (target.isInvulnerable()) return;

        long currentTime = System.currentTimeMillis();
        UUID targetId = target.getUniqueId();

        if (frostbiteEntities.containsKey(targetId)) {
            long currentEndTime = frostbiteEntities.get(targetId);
            if (currentEndTime > currentTime) {
                frostbiteEntities.put(targetId, currentEndTime + duration);
                return;
            }
        }

        long endTime = currentTime + duration;
        frostbiteEntities.put(targetId, endTime);

        if (target instanceof Player player) {
            createOrUpdateBossBar(player);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!frostbiteEntities.containsKey(targetId) || target.isDead()) {
                    removeEffect(target);
                    cancel();
                    return;
                }

                long currentEndTime = frostbiteEntities.get(targetId);
                long now = System.currentTimeMillis();

                if (now >= currentEndTime) {
                    removeEffect(target);
                    cancel();
                    return;
                }

                target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().clone().add(0, 1.3, 0), 1, 0.2, 0.5, 0.2, 0);

                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 0.6f);
                target.getWorld().spawnParticle(Particle.DUST, target.getLocation().clone().add(0, 1.3, 0), 1, 0.3, 0.4, 0.3, 0, dustOptions);

                target.setFreezeTicks(140);

                if (target instanceof Player player) {
                    if (!player.isOnline()) {
                        removeEffect(player);
                        cancel();
                        return;
                    }

                    updateBossBarProgress(player, now, currentEndTime);
                }
            }
        }.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Core")), 0L, 1L);
    }

    @Override
    public void removeEffect(Entity entity) {
        frostbiteEntities.remove(entity.getUniqueId());
        entity.setFreezeTicks(0);

        if (entity instanceof Player player) {
            BossBar bar = activeBars.remove(player.getUniqueId());
            if (bar != null) {
                bar.removeAll();
            }
        }
    }

    public static boolean isFrostbite(Entity entity) {
        Long endTime = frostbiteEntities.get(entity.getUniqueId());
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    private void createOrUpdateBossBar(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = activeBars.get(uuid);

        if (bar == null) {
            bar = Bukkit.createBossBar("init", BarColor.BLUE, BarStyle.SOLID);
            bar.setTitle("§b❄ Frost");
            bar.addPlayer(player);
            activeBars.put(uuid, bar);
        }
    }

    private void updateBossBarProgress(Player player, long now, long endTime) {
        BossBar bar = activeBars.get(player.getUniqueId());
        if (bar == null) return;

        long remaining = endTime - now;
        double progress = (double) remaining / (double) duration;

        progress = Math.max(0.0, Math.min(1.0, progress));

        bar.setProgress(progress);
    }
}