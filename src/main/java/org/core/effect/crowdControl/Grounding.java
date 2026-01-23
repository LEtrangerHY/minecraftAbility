package org.core.effect.crowdControl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Grounding implements Effects, Listener {
    public static Map<UUID, Long> groundedEntities = new HashMap<>();

    private final Entity target;
    private final long duration;

    public Grounding(Entity target, long duration) {
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

        if (groundedEntities.containsKey(targetId)) {
            long currentEndTime = groundedEntities.get(targetId);
            if (currentEndTime > currentTime) {
                if (newEndTime > currentEndTime) {
                    groundedEntities.put(targetId, newEndTime);
                }
                return;
            }
        }

        groundedEntities.put(targetId, newEndTime);

        new BukkitRunnable() {
            Location groundLoc = target.getLocation();

            @Override
            public void run() {
                if (!groundedEntities.containsKey(targetId) || target.isDead()) {
                    removeEffect(target);
                    cancel();
                    return;
                }

                long endTime = groundedEntities.get(targetId);

                if (System.currentTimeMillis() >= endTime) {
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
                    target.sendActionBar(Component.text("Grounded").color(NamedTextColor.YELLOW));
                }

                Location fixed = new Location(target.getWorld(), target.getX(), groundLoc.getY(), target.getZ(), target.getYaw(), target.getPitch());
                if (fixed.getY() < target.getY()) {
                    target.teleport(fixed);
                } else if (fixed.getY() > target.getY()) {
                    groundLoc = target.getLocation();
                }
            }
        }.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Core")), 0L, 1L);
    }

    @Override
    public void removeEffect(Entity entity) {
        groundedEntities.remove(entity.getUniqueId());
        if (entity instanceof Player player && player.isOnline()) {
            player.sendActionBar(Component.text(" "));
        }
    }

    public static boolean isGrounded(Entity entity) {
        Long endTime = groundedEntities.get(entity.getUniqueId());
        return endTime != null && System.currentTimeMillis() < endTime;
    }
}