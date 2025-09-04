package org.core.Effect;

import com.destroystokyo.paper.event.entity.EntityJumpEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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

        long endTime = System.currentTimeMillis() + duration;

        new BukkitRunnable() {
            Location groundLoc = target.getLocation();

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
                    target.sendActionBar(Component.text("Grounded").color(NamedTextColor.YELLOW));
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
