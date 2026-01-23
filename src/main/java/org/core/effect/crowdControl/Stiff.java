package org.core.effect.crowdControl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Stiff implements Effects, Listener {
    public static Map<UUID, Long> stiffEntities = new HashMap<>();

    private final Entity target;
    private final long duration;

    public Stiff(Entity target, long duration) {
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

        if (stiffEntities.containsKey(targetId)) {
            long currentEndTime = stiffEntities.get(targetId);
            if (currentEndTime > currentTime) {
                if (newEndTime > currentEndTime) {
                    stiffEntities.put(targetId, newEndTime);
                }
                return;
            }
        }

        stiffEntities.put(targetId, newEndTime);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!stiffEntities.containsKey(targetId)) {
                    removeEffect(target);
                    cancel();
                    return;
                }

                long endTime = stiffEntities.get(targetId);

                if (System.currentTimeMillis() >= endTime || target.isDead()) {
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
                    player.sendActionBar(Component.text("Stiff").color(NamedTextColor.YELLOW));
                }
            }
        }.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Core")), 0L, 1L);
    }

    @Override
    public void removeEffect(Entity entity) {
        stiffEntities.remove(entity.getUniqueId());
        if (entity instanceof Player player && player.isOnline()) {
            player.sendActionBar(Component.text(" "));
        }
    }

    public static void breakStiff(Entity entity) {
        stiffEntities.remove(entity.getUniqueId());
    }

    public static boolean isStiff(Entity entity) {
        Long endTime = stiffEntities.get(entity.getUniqueId());
        return endTime != null && System.currentTimeMillis() < endTime;
    }
}