package org.core.effect.debuff;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class Burn implements Debuffs {
    private static final HashMap<UUID, Long> burnedEntities = new HashMap<>();

    private final Entity target;
    private final long duration;

    public Burn(Entity target, long duration) {
        this.target = target;
        this.duration = duration;
    }

    @Override
    public void applyEffect(Entity entity) {
        if (!(target instanceof LivingEntity)) return;
        if (target.isInvulnerable()) return;

        long currentTime = System.currentTimeMillis();
        UUID targetId = target.getUniqueId();

        if (burnedEntities.containsKey(targetId)) {
            long currentEndTime = burnedEntities.get(targetId);
            if (currentEndTime > currentTime) {
                burnedEntities.put(targetId, currentEndTime + duration);
                return;
            }
        }

        long endTime = currentTime + duration;
        burnedEntities.put(targetId, endTime);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!burnedEntities.containsKey(targetId) || target.isDead()) {
                    removeEffect(target);
                    cancel();
                    return;
                }

                long currentEndTime = burnedEntities.get(targetId);
                if (System.currentTimeMillis() >= currentEndTime) {
                    removeEffect(target);
                    cancel();
                    return;
                }

                target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().clone().add(0, 1.3, 0), 7, 0.5, 0.5, 0.5, 0);
                target.setFireTicks(25);

                if (target instanceof Player player) {
                    if (!player.isOnline()) {
                        removeEffect(player);
                        cancel();
                        return;
                    }
                    player.sendActionBar(Component.text("Burn").color(NamedTextColor.RED));
                }
            }
        }.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Core")), 0L, 20L);
    }

    @Override
    public void removeEffect(Entity entity) {
        burnedEntities.remove(entity.getUniqueId());
        if (entity instanceof Player player) {
            player.sendActionBar(Component.text(" "));
        }
    }

    public static boolean isBurning(Entity entity) {
        Long endTime = burnedEntities.get(entity.getUniqueId());
        return endTime != null && System.currentTimeMillis() < endTime;
    }
}