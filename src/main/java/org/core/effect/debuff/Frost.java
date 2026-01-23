package org.core.effect.debuff;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class Frost implements Debuffs {
    private static final HashMap<UUID, Long> frostbiteEntities = new HashMap<>();

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

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!frostbiteEntities.containsKey(targetId) || target.isDead()) {
                    removeEffect(target);
                    cancel();
                    return;
                }

                long currentEndTime = frostbiteEntities.get(targetId);
                if (System.currentTimeMillis() >= currentEndTime) {
                    removeEffect(target);
                    cancel();
                    return;
                }

                target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().clone().add(0, 1.3, 0), 6, 0.5, 0.5, 0.5, 0);

                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 0.6f);
                target.getWorld().spawnParticle(Particle.DUST, target.getLocation().clone().add(0, 1.3, 0), 3, 0.4, 0.4, 0.4, 0, dustOptions);

                target.setFreezeTicks(140);

                if (target instanceof Player player) {
                    if (!player.isOnline()) {
                        removeEffect(player);
                        cancel();
                        return;
                    }
                    player.sendActionBar(Component.text("Frost").color(NamedTextColor.AQUA));
                }
            }
        }.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Core")), 0L, 20L);
    }

    @Override
    public void removeEffect(Entity entity) {
        frostbiteEntities.remove(entity.getUniqueId());
        entity.setFreezeTicks(0);
        if (entity instanceof Player player) {
            player.sendActionBar(Component.text(" "));
        }
    }

    public static boolean isFrostbite(Entity entity) {
        Long endTime = frostbiteEntities.get(entity.getUniqueId());
        return endTime != null && System.currentTimeMillis() < endTime;
    }
}