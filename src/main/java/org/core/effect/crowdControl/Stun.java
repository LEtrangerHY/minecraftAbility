package org.core.effect.crowdControl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Stun implements Effects, Listener {
    public static Map<UUID, Long> stunnedEntities = new HashMap<>();
    private static final Map<UUID, BossBar> activeBars = new HashMap<>();

    private final Entity target;
    private final long duration;

    public Stun(Entity target, long duration) {
        this.target = target;
        this.duration = duration;
    }

    @Override
    public void applyEffect(Entity entity) {
        if (!(entity instanceof LivingEntity)) return;
        if (target.isInvulnerable()) return;

        UUID targetId = target.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long newEndTime = currentTime + duration;

        if (stunnedEntities.containsKey(targetId)) {
            long currentEndTime = stunnedEntities.get(targetId);

            if (currentEndTime > currentTime) {
                if (newEndTime > currentEndTime) {
                    stunnedEntities.put(targetId, newEndTime);
                }
                return;
            }
        }

        stunnedEntities.put(targetId, newEndTime);
        Location stunPos = target.getLocation();

        if (target instanceof Player player) {
            createOrUpdateBossBar(player);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!stunnedEntities.containsKey(targetId) || target.isDead()) {
                    removeEffect(target);
                    cancel();
                    return;
                }

                long endTime = stunnedEntities.get(targetId);
                long now = System.currentTimeMillis();

                if (now >= endTime) {
                    removeEffect(target);
                    cancel();
                    return;
                }

                if (target instanceof Player player) {
                    if (!player.isOnline()) {
                        removeEffect(player);
                        cancel();
                        return;
                    }
                    updateBossBarProgress(player, now, endTime);
                }

                target.teleport(stunPos);
                target.setVelocity(new Vector(0, 0, 0));
            }
        }.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Core")), 0L, 1L);
    }

    @Override
    public void removeEffect(Entity entity) {
        if (!(entity instanceof LivingEntity)) return;
        stunnedEntities.remove(entity.getUniqueId());

        if (entity instanceof Player player) {
            BossBar bar = activeBars.remove(player.getUniqueId());
            if (bar != null) {
                bar.removeAll();
            }
        }
    }

    public static boolean isStunned(Entity entity) {
        Long endTime = stunnedEntities.get(entity.getUniqueId());
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    private void createOrUpdateBossBar(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = activeBars.get(uuid);

        if (bar == null) {
            bar = Bukkit.createBossBar("init", BarColor.YELLOW, BarStyle.SOLID);
            bar.setTitle(Component.text("Stun").color(NamedTextColor.WHITE).content());
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