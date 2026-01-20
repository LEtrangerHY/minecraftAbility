package org.core.coreSystem.absInventorySystem;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class coreMenuHolder implements InventoryHolder {

    private final UUID owner;
    private Inventory inventory;

    public coreMenuHolder(Player player) {
        this.owner = player.getUniqueId();
    }

    public UUID getOwner() {
        return owner;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}