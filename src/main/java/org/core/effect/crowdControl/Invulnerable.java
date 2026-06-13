package org.core.effect.crowdControl;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

public class Invulnerable implements Effects, Listener {

    public static Map<Entity, Long> invulnerablePlayers = new HashMap<>();

    private final Player player;
    private final long duration;

    public Invulnerable(Player player, long duration) {
        this.player = player;
        this.duration = duration;
    }

    @Override
    public void applyEffect(Entity entity) {
        long newEndTime = System.currentTimeMillis() + duration;

        Long currentEndTime = invulnerablePlayers.get(entity);

        if (currentEndTime != null) {
            invulnerablePlayers.put(entity, Math.max(currentEndTime, newEndTime));
        } else {
            invulnerablePlayers.put(entity, newEndTime);
            entity.setInvulnerable(true);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    removeEffect(player);
                    cancel();
                    return;
                }

                long maxEndTime = invulnerablePlayers.getOrDefault(entity, 0L);

                if (System.currentTimeMillis() >= maxEndTime) {
                    removeEffect(player);
                    cancel();
                }
            }
        }.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Core")), 0L, 1L);
    }

    @Override
    public void removeEffect(Entity entity) {
        invulnerablePlayers.remove(entity);
        entity.setInvulnerable(false);
    }

    public static boolean isInvulnerable(Player player) {
        return invulnerablePlayers.containsKey(player);
    }
}