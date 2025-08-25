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
    private static final Map<Entity, Long> stunnedEntities = new HashMap();

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

                stunnedEntities.put(target, endTime);

                target.teleport(stunPos);
                target.setVelocity(new Vector(0, 0, 0));

                if (target instanceof Player) {
                    target.sendActionBar(Component.text("Stunned").color(NamedTextColor.YELLOW));
                }
            }
        }.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Core")), 0L, 1L);
    }

    @Override
    public void removeEffect(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) return;

        stunnedEntities.remove(entity);
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event){
        Player player= event.getPlayer();
        removeEffect(player);
    }

    public static boolean isStunned(Entity entity) {
        Long endTime = stunnedEntities.get(entity);
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    public static void handlePlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (isStunned(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();

            to.setX(from.getX());
            to.setY(from.getY());
            to.setZ(from.getZ());
            event.setTo(to);

            player.setVelocity(new Vector(0, 0, 0));
        }
    }

}