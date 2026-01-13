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

public class Burn implements Debuffs{
    private static final HashMap<Entity, Long> burnedEntities = new HashMap();

    private final Entity target;
    private final long duration;

    public Burn(Entity target, long duration) {
        this.target = target;
        this.duration = duration;
    }

    @Override
    public void applyEffect(Entity entity) {
        if (!(entity instanceof LivingEntity)) return;

        if(target.isInvulnerable()) return;

        long endTime = System.currentTimeMillis() + duration;

        new BukkitRunnable() {
            @Override
            public void run() {
                burnedEntities.put(target, endTime);
                entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().clone().add(0, 1.3, 0), 7, 0.5, 0.5, 0.5, 0);
                target.setFireTicks(25);

                if (entity instanceof Player player) {
                    if(System.currentTimeMillis() >= endTime || player.isDead() || !player.isOnline()){
                        player.sendActionBar(Component.text(" "));
                        removeEffect(player);
                        cancel();
                    }else {
                        target.sendActionBar(Component.text("Burn").color(NamedTextColor.RED));
                    }
                }

                if (System.currentTimeMillis() >= endTime || target.isDead()) {
                    removeEffect(target);
                    cancel();
                }
            }
        }.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Core")), 0L, 20L);
    }

    @Override
    public void removeEffect(Entity entity) {
        burnedEntities.remove(entity);
    }

    public static boolean isBurning(Entity entity) {
        Long endTime = burnedEntities.get(entity);
        return endTime != null && System.currentTimeMillis() < endTime;
    }
}
