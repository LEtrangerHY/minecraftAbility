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

public class Frost implements Debuffs{
    private static final HashMap<Entity, Long> frostbiteEntities = new HashMap();

    private final Entity target;
    private final long duration;

    public Frost(Entity target, long duration) {
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
                frostbiteEntities.put(target, endTime);
                entity.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().clone().add(0, 1.3, 0), 6, 0.5, 0.5, 0.5, 0);

                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 0.6f);
                entity.getWorld().spawnParticle(Particle.DUST, target.getLocation().clone().add(0, 1.3, 0), 3, 0.4, 0.4, 0.4, 0, dustOptions);

                target.setFreezeTicks((int) duration / 50);

                if (entity instanceof Player player) {
                    if(System.currentTimeMillis() >= endTime || player.isDead() || !player.isOnline()){
                        player.sendActionBar(Component.text(" "));
                        removeEffect(player);
                        cancel();
                    }else {
                        target.sendActionBar(Component.text("Frost").color(NamedTextColor.AQUA));
                    }
                }

                if (System.currentTimeMillis() >= endTime || target.isDead()) {
                    target.setFreezeTicks(0);
                    removeEffect(target);
                    cancel();
                }
            }
        }.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Core")), 0L, 20L);
    }

    @Override
    public void removeEffect(Entity entity) {
        frostbiteEntities.remove(entity);
    }

    public static boolean isFrostbite(Entity entity) {
        Long endTime = frostbiteEntities.get(entity);
        return endTime != null && System.currentTimeMillis() < endTime;
    }
}
