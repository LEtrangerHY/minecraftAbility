package org.core.coreProgram.AbsInentory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.core.Cool.Cool;
import org.core.coreConfig;
import org.core.coreProgram.AbsCoreSystem.ConfigWrapper;

import java.util.ArrayList;
import java.util.List;

abstract public class absInventory implements Listener {

    protected final coreConfig tag;

    public absInventory(coreConfig tag) {
        this.tag = tag;
    }

    protected abstract boolean contains(Player player);
    protected abstract boolean isCoreItemClicked(Player player, ItemStack clicked);

    protected abstract Long getSkillLevel(Player player, String skill);
    protected abstract Component getName(Player player, String skill);
    protected abstract Material getTotem(Player player, String skill);
    protected abstract List<Component> getTotemLore(Player player, String skill);

    protected abstract void reinforceSkill(Player player, String skill, Long skillLevel, Inventory customInv);

    protected abstract InventoryWrapper getInventoryWrapper();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !contains(player)) return;

        ItemStack clicked = event.getCurrentItem();

        if (event.getView().getTopInventory().getHolder() instanceof CoreMenuHolder holder) {
            if (!holder.getOwner().equals(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);

            if (clicked == null || clicked.getType().isAir()) return;

            if (event.isRightClick()) {
                if (clicked.getType() == getTotem(player, "R")) {
                    reinforceSkill(player, "R", getSkillLevel(player, "R"), event.getView().getTopInventory());
                    customInvReroll(player, event.getView().getTopInventory());
                }

                if (clicked.getType() == getTotem(player, "Q")) {
                    reinforceSkill(player, "Q", getSkillLevel(player, "Q"), event.getView().getTopInventory());
                    customInvReroll(player, event.getView().getTopInventory());
                }

                if (clicked.getType() == getTotem(player, "F")) {
                    reinforceSkill(player, "F", getSkillLevel(player, "F"), event.getView().getTopInventory());
                    customInvReroll(player, event.getView().getTopInventory());
                }
            }
            return;
        }

        if (clicked != null && event.isRightClick() && isCoreItemClicked(player, clicked)) {

            ItemStack cursorItem = player.getItemOnCursor();
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                player.getInventory().addItem(cursorItem);
                player.setItemOnCursor(null);
            }

            event.setCancelled(true);

            Inventory customInv = Bukkit.createInventory(player, 27, Component.text("CORE MENU").color(NamedTextColor.LIGHT_PURPLE));

            CoreMenuHolder holder = new CoreMenuHolder(player, customInv);

            customInv = Bukkit.createInventory(holder, 27, Component.text("CORE MENU").color(NamedTextColor.LIGHT_PURPLE));

            customInvReroll(player, customInv);

            player.openInventory(customInv);
        }

    }

    public void customInvReroll(Player player, Inventory customInv){
        ItemStack RSkillTotem = new ItemStack(getTotem(player, "R"));
        ItemStack QSkillTotem = new ItemStack(getTotem(player, "Q"));
        ItemStack FSkillTotem = new ItemStack(getTotem(player, "F"));

        ItemMeta R = RSkillTotem.getItemMeta();
        ItemMeta Q = QSkillTotem.getItemMeta();
        ItemMeta F = FSkillTotem.getItemMeta();

        R.displayName(getName(player, "R").color(NamedTextColor.GREEN));
        Q.displayName(getName(player, "Q").color(NamedTextColor.LIGHT_PURPLE));
        F.displayName(getName(player, "F").color(NamedTextColor.YELLOW));

        R.lore(getTotemLore(player, "R"));
        Q.lore(getTotemLore(player, "Q"));
        F.lore(getTotemLore(player, "F"));

        RSkillTotem.setItemMeta(R);
        QSkillTotem.setItemMeta(Q);
        FSkillTotem.setItemMeta(F);

        customInv.setItem(10, RSkillTotem);
        customInv.setItem(13, QSkillTotem);
        customInv.setItem(16, FSkillTotem);
    }

}
