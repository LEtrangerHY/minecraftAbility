package org.core.effect.effectPlugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class skillSpectatorGuard implements Listener {

    private static JavaPlugin plugin;
    private static NamespacedKey modeKey;
    private static NamespacedKey locKey;

    private static final Map<UUID, GameMode> originalModes = new ConcurrentHashMap<>();
    private static final Map<UUID, Location> returnLocations = new ConcurrentHashMap<>();

    public skillSpectatorGuard(JavaPlugin plugin) {
        skillSpectatorGuard.plugin = plugin;
        modeKey = new NamespacedKey(plugin, "skill_spectator_mode");
        locKey = new NamespacedKey(plugin, "skill_spectator_loc");
    }

    public static GameMode begin(Player player, Location returnLocation) {
        UUID uuid = player.getUniqueId();

        GameMode original = originalModes.get(uuid);
        if (original == null) {
            original = player.getGameMode();
            if (original == GameMode.SPECTATOR) original = GameMode.SURVIVAL;
            originalModes.put(uuid, original);
        }
        returnLocations.put(uuid, returnLocation.clone());

        if (modeKey != null) {
            PersistentDataContainer pdc = player.getPersistentDataContainer();
            pdc.set(modeKey, PersistentDataType.STRING, original.name());
            pdc.set(locKey, PersistentDataType.STRING, serialize(returnLocation));
        }
        return original;
    }

    public static boolean isTracked(Player player) {
        return originalModes.containsKey(player.getUniqueId());
    }

    public static GameMode getOriginalMode(Player player) {
        return originalModes.getOrDefault(player.getUniqueId(), player.getGameMode());
    }

    public static void end(Player player) {
        restore(player, false);
    }

    private static void restore(Player player, boolean forceReturnIfSpectator) {
        UUID uuid = player.getUniqueId();
        GameMode original = originalModes.remove(uuid);
        Location returnLoc = returnLocations.remove(uuid);

        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (original == null && modeKey != null && pdc.has(modeKey, PersistentDataType.STRING)) {
            original = parseMode(pdc.get(modeKey, PersistentDataType.STRING));
            if (returnLoc == null) returnLoc = deserialize(pdc.get(locKey, PersistentDataType.STRING));
        }
        if (modeKey != null) {
            pdc.remove(modeKey);
            pdc.remove(locKey);
        }

        if (original == null) return;

        if (forceReturnIfSpectator && player.getGameMode() == GameMode.SPECTATOR
                && returnLoc != null && !isSafe(player.getLocation())) {
            player.teleport(returnLoc);
        }
        if (player.getGameMode() != original) {
            player.setGameMode(original);
        }
    }

    public static void restoreAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isTracked(player)) restore(player, true);
        }
        originalModes.clear();
        returnLocations.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent e) {
        restore(e.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (modeKey != null && player.getPersistentDataContainer().has(modeKey, PersistentDataType.STRING)) {
            restore(player, true);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        if (!isTracked(player)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) restore(player, false);
        });
    }

    private static boolean isSafe(Location loc) {
        return !loc.clone().add(0, 1, 0).getBlock().getType().isSolid();
    }

    private static GameMode parseMode(String name) {
        if (name == null) return null;
        try {
            return GameMode.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return GameMode.SURVIVAL;
        }
    }

    private static String serialize(Location loc) {
        World world = loc.getWorld();
        return (world != null ? world.getUID() : "") + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ()
                + ";" + loc.getYaw() + ";" + loc.getPitch();
    }

    private static Location deserialize(String s) {
        if (s == null) return null;
        try {
            String[] p = s.split(";");
            World world = Bukkit.getWorld(UUID.fromString(p[0]));
            if (world == null) return null;
            return new Location(world, Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]),
                    Float.parseFloat(p[4]), Float.parseFloat(p[5]));
        } catch (Exception ex) {
            return null;
        }
    }
}