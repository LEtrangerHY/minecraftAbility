package org.core.coreProgram.AbsInentory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class CoreMenuHolder implements InventoryHolder {

    private final UUID owner;
    private final Inventory inventory;

    public CoreMenuHolder(Player player, Inventory inventory) {
        this.owner = player.getUniqueId();
        this.inventory = inventory;
    }

    public UUID getOwner() {
        return owner;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
