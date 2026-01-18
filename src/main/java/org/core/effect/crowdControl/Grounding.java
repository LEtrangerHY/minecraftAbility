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

public class Grounding implements Effects, Listener {
    public static Map<Entity, Long> groundedEntities = new HashMap();

    private final Entity target;
    private final long duration;

    public Grounding(Entity target, long duration) {
        this.target = target;
        this.duration = duration;
    }

    @Override
    public void applyEffect(Entity entity) {
        if (!(entity instanceof LivingEntity)) return;

        if(target.isInvulnerable()) return;

        long endTime = System.currentTimeMillis() + duration;

        new BukkitRunnable() {
            Location groundLoc = target.getLocation();

            @Override
            public void run() {

                if (target instanceof Player player) {
                    if(System.currentTimeMillis() >= endTime || player.isDead() || !player.isOnline()){
                        player.sendActionBar(Component.text(" "));
                        removeEffect(player);
                        cancel();
                    }
                    target.sendActionBar(Component.text("Grounded").color(NamedTextColor.YELLOW));
                }

                if (System.currentTimeMillis() >= endTime || target.isDead()) {
                    removeEffect(target);
                    cancel();
                }

                groundedEntities.put(target, endTime);

                Location fixed = new Location(target.getWorld(), target.getX(), groundLoc.getY(), target.getZ(), target.getYaw(), target.getPitch());
                if(fixed.getY() < target.getY()) {
                    target.teleport(fixed);
                }else if(fixed.getY() > target.getY()){
                    groundLoc = target.getLocation();
                }
            }
        }.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Core")), 0L, 1L);
    }

    @Override
    public void removeEffect(Entity entity) {
        groundedEntities.remove(entity);
    }

    public static boolean isGrounded(Entity entity) {
        Long endTime = groundedEntities.get(entity);
        return endTime != null && System.currentTimeMillis() < endTime;
    }

}
