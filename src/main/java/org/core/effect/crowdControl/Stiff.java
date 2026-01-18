package org.core.effect.crowdControl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Stiff implements Effects, Listener {
    public static Map<Entity, Long> stiffEntities = new HashMap<>();

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

        long endTime = System.currentTimeMillis() + duration;

        stiffEntities.put(target, endTime);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!stiffEntities.containsKey(target)) {
                    if (target instanceof Player player) {
                        player.sendActionBar(Component.text(" "));
                    }
                    removeEffect(target);
                    cancel();
                    return;
                }

                if (target instanceof Player player) {
                    if (System.currentTimeMillis() >= endTime || player.isDead() || !player.isOnline()) {
                        player.sendActionBar(Component.text(" "));
                        removeEffect(player);
                        cancel();
                        return;
                    }else {
                        player.sendActionBar(Component.text("Stiff").color(NamedTextColor.YELLOW));
                    }
                } else {
                    if (System.currentTimeMillis() >= endTime || target.isDead()) {
                        removeEffect(target);
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Core")), 0L, 1L);
    }

    @Override
    public void removeEffect(Entity entity) {
        stiffEntities.remove(entity);
    }

    public static void breakStiff(Entity entity) {
        stiffEntities.remove(entity);
    }

    public static boolean isStiff(Entity entity) {
        Long endTime = stiffEntities.get(entity);
        return endTime != null && System.currentTimeMillis() < endTime;
    }
}