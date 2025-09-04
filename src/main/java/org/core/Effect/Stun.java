package org.core.Effect;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Stun implements Effects, Listener {
    public static Map<Entity, Long> stunnedEntities = new HashMap();

    private final Entity target;
    private final long duration;

    public Stun(Entity target, long duration) {
        this.target = target;
        this.duration = duration;
    }

    @Override
    public void applyEffect(Entity entity) {
        if (!(entity instanceof LivingEntity)) return;

        LivingEntity livingEntity = (LivingEntity) target;

        long endTime = System.currentTimeMillis() + duration;

        Location stunPos = target.getLocation();

        new BukkitRunnable() {
            @Override
            public void run() {

                if (System.currentTimeMillis() >= endTime || target.isDead()) {
                    removeEffect(target);
                    cancel();
                }

                if (entity instanceof Player player) {
                    if(!player.isOnline()){
                        removeEffect(player);
                        cancel();
                    }
                    target.sendActionBar(Component.text("Stunned").color(NamedTextColor.YELLOW));
                }

                stunnedEntities.put(target, endTime);

                target.teleport(stunPos);
                target.setVelocity(new Vector(0, 0, 0));
            }
        }.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Core")), 0L, 1L);
    }

    @Override
    public void removeEffect(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) return;

        stunnedEntities.remove(entity);
    }

    public static boolean isStunned(Entity entity) {
        Long endTime = stunnedEntities.get(entity);
        return endTime != null && System.currentTimeMillis() < endTime;
    }

}