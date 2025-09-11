package org.core.coreProgram.AbsInentory;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.core.coreConfig;

abstract public class absInventory implements Listener {

    public final coreConfig config;

    public absInventory(coreConfig config){
        this.config = config;
    }

    @EventHandler
    public void InventoryClick(InventoryClickEvent event){
        Inventory inventory = event.getInventory();
        ItemStack item = event.getCursor();
    }
}
