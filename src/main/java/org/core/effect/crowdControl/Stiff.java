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
        LivingEntity livingTarget = (LivingEntity) target;

        if (target.isInvulnerable()) return;

        long endTime = System.currentTimeMillis() + duration;

        livingTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) (duration / 50 * 20), 255, false, false));
        livingTarget.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, (int) (duration / 50 * 20), 128, false, false));

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

                if (entity instanceof Player player) {
                    if (System.currentTimeMillis() >= endTime || player.isDead() || !player.isOnline()) {
                        player.sendActionBar(Component.text(" "));
                        removeEffect(player);
                        cancel();
                        return;
                    }
                    target.sendActionBar(Component.text("Stiff").color(NamedTextColor.LIGHT_PURPLE));
                } else {
                    if (System.currentTimeMillis() >= endTime || target.isDead()) {
                        removeEffect(target);
                        cancel();
                        return;
                    }
                }

                target.setVelocity(new Vector(0, 0, 0));
            }
        }.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Core")), 0L, 1L);
    }

    @Override
    public void removeEffect(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            if (livingEntity.hasPotionEffect(PotionEffectType.SLOWNESS))
                livingEntity.removePotionEffect(PotionEffectType.SLOWNESS);
            if (livingEntity.hasPotionEffect(PotionEffectType.JUMP_BOOST))
                livingEntity.removePotionEffect(PotionEffectType.JUMP_BOOST);
        }
        stiffEntities.remove(entity);
    }

    public static void breakStiff(Entity entity) {
        stiffEntities.remove(entity);

        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.removePotionEffect(PotionEffectType.SLOWNESS);
            livingEntity.removePotionEffect(PotionEffectType.JUMP_BOOST);
        }
    }

    public static boolean isStiff(Entity entity) {
        Long endTime = stiffEntities.get(entity);
        return endTime != null && System.currentTimeMillis() < endTime;
    }
}