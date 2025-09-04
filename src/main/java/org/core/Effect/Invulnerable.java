package org.core.Effect;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.bukkit.Bukkit.getLogger;

public class Invulnerable implements Effects, Listener {
    public static Set<Entity> invulnerablePlayers = new HashSet<>();

    private final Player player;
    private final long duration;

    public Invulnerable(Player player, long duration) {
        this.player = player;
        this.duration = duration;
    }

    @Override
    public void applyEffect(Entity entity) {

        invulnerablePlayers.add(player);
        long endTime = System.currentTimeMillis() + duration;

        entity.setInvulnerable(true);

        new BukkitRunnable() {
            @Override
            public void run() {

                if (System.currentTimeMillis() >= endTime || !player.isOnline()) {
                    removeEffect(player);
                    cancel();
                }

            }
        }.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Core")), 0L, 1L);
    }

    @Override
    public void removeEffect(Entity entity) {
        invulnerablePlayers.remove(entity);
        entity.setInvulnerable(false);
    }

    public static boolean isInvulnerable(Player player) {
        return invulnerablePlayers.contains(player);
    }
}
