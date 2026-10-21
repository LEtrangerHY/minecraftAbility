package org.core.effect.effectPlugin;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class swingEchoGuard implements Listener {

    private static final Map<UUID, Integer> pendingEchoes = new HashMap<>();
    private static final Set<UUID> inventoryDrops = new HashSet<>();

    private static Method getHandleMethod;
    private static Field swingTimeField;
    private static boolean reflectionUnavailable = false;

    public static void swingMainHand(Player player) {
        player.swingMainHand();
        if (wasBroadcast(player)) expectEcho(player);
    }

    public static void swingOffHand(Player player) {
        player.swingOffHand();
        if (wasBroadcast(player)) expectEcho(player);
    }

    private static boolean wasBroadcast(Player player) {
        if (reflectionUnavailable) return true;
        try {
            if (getHandleMethod == null) {
                getHandleMethod = player.getClass().getMethod("getHandle");
            }
            Object handle = getHandleMethod.invoke(player);
            if (swingTimeField == null) {
                swingTimeField = findField(handle.getClass(), "swingTime");
            }
            return swingTimeField.getInt(handle) < 0;
        } catch (ReflectiveOperationException | RuntimeException e) {
            reflectionUnavailable = true;
            return true;
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    public static void expectEcho(Player player) {
        pendingEchoes.merge(player.getUniqueId(), 1, Integer::sum);
    }

    public static boolean isEchoPending(Player player) {
        return pendingEchoes.getOrDefault(player.getUniqueId(), 0) > 0;
    }

    public static void reset(Player player) {
        pendingEchoes.remove(player.getUniqueId());
        inventoryDrops.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmSwing(PlayerArmSwingEvent event) {
        pendingEchoes.computeIfPresent(event.getPlayer().getUniqueId(), (uuid, count) -> count > 1 ? count - 1 : null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrop(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryAction action = event.getAction();
        if (action == InventoryAction.DROP_ALL_CURSOR || action == InventoryAction.DROP_ONE_CURSOR
                || action == InventoryAction.DROP_ALL_SLOT || action == InventoryAction.DROP_ONE_SLOT) {
            inventoryDrops.add(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (inventoryDrops.remove(player.getUniqueId())) return;
        expectEcho(player);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        inventoryDrops.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        reset(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        reset(event.getEntity());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        reset(event.getPlayer());
    }
}