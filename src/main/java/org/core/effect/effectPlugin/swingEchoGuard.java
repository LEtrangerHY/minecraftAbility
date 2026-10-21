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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class swingEchoGuard implements Listener {

    private static final Map<UUID, Integer> pendingEchoes = new HashMap<>();
    private static final Set<UUID> inventoryDrops = new HashSet<>();

    public static void swingMainHand(Player player) {
        expectEcho(player);
        player.swingMainHand();
    }

    public static void swingOffHand(Player player) {
        expectEcho(player);
        player.swingOffHand();
    }

    public static void expectEcho(Player player) {
        pendingEchoes.merge(player.getUniqueId(), 1, Integer::sum);
    }

    public static boolean isEchoPending(Player player) {
        return pendingEchoes.getOrDefault(player.getUniqueId(), 0) > 0;
    }

    private static void reset(Player player) {
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