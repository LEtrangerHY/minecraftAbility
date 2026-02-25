package org.core.coreSystem.absInventorySystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.core.main.coreConfig;

import java.util.List;

abstract public class absInventory implements Listener {

    protected final coreConfig tag;

    public absInventory(coreConfig tag) {
        this.tag = tag;
    }

    protected abstract Plugin getPlugin();

    protected abstract boolean contains(Player player);

    protected abstract Material getMainTotem(Player player);

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

        if (event.getView().getTopInventory().getHolder() instanceof coreMenuHolder holder) {
            if (!holder.getOwner().equals(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);

            if (clicked == null || clicked.getType().isAir()) return;

            if (event.isRightClick()) {
                if (clicked.getType() == getMainTotem(player)) {
                    reinforceSkill(player, "main", getSkillLevel(player, "main"), event.getView().getTopInventory());
                    return;
                }

                if (clicked.getType() == getTotem(player, "R")) {
                    reinforceSkill(player, "R", getSkillLevel(player, "R"), event.getView().getTopInventory());
                    return;
                }
                if (clicked.getType() == getTotem(player, "Q")) {
                    reinforceSkill(player, "Q", getSkillLevel(player, "Q"), event.getView().getTopInventory());
                    return;
                }
                if (clicked.getType() == getTotem(player, "F")) {
                    reinforceSkill(player, "F", getSkillLevel(player, "F"), event.getView().getTopInventory());
                    return;
                }
            }
            return;
        }

        if (clicked != null && event.isRightClick() && isCoreItemClicked(player, clicked)) {
            event.setCancelled(true);
            openCoreMenu(player);
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();

        if (!contains(player) || !isLoreBookClicked(main)) return;

        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            openCoreMenu(player);
        }
    }

    public void openCoreMenu(Player player) {
        coreMenuHolder holder = new coreMenuHolder(player);
        Inventory customInv = Bukkit.createInventory(holder, 36, Component.text("DATA MENU").color(NamedTextColor.LIGHT_PURPLE));
        holder.setInventory(customInv);

        customInvReroll(player, customInv);

        Bukkit.getScheduler().runTask(getPlugin(), () -> {
            player.openInventory(customInv);
        });
    }

    public boolean isLoreBookClicked(ItemStack clicked){
        if (clicked == null || clicked.getType() != Material.ENCHANTED_BOOK) {
            return false;
        }

        if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
            return false;
        }

        String displayName = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
        return displayName.equalsIgnoreCase("DataMenu") || displayName.equalsIgnoreCase("DataLore");
    }

    public boolean isCoreItemClicked(Player player, ItemStack clicked){
        return clicked != null && clicked.getType() == getMainTotem(player);
    }

    private void draw3x3Background(Inventory inv, int centerSlot, Material glassMaterial) {
        int[] offsets = {-10, -9, -8, -1, 1, 8, 9, 10};
        ItemStack glass = new ItemStack(glassMaterial);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            glass.setItemMeta(meta);
        }
        for (int offset : offsets) {
            int slot = centerSlot + offset;
            if (slot >= 0 && slot < inv.getSize()) {
                inv.setItem(slot, glass);
            }
        }
    }

    private void setTotemItem(Player player, Inventory inv, String skill, int slot, NamedTextColor color) {
        ItemStack totem = new ItemStack(getTotem(player, skill));
        ItemMeta meta = totem.getItemMeta();

        if (meta != null) {
            meta.displayName(getName(player, skill).color(color).decoration(TextDecoration.ITALIC, false));
            meta.lore(getTotemLore(player, skill));
            totem.setItemMeta(meta);
        }

        inv.setItem(slot, totem);
    }

    public void customInvReroll(Player player, Inventory customInv){
        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayGlass.getItemMeta();
        if (grayMeta != null) {
            grayMeta.displayName(Component.empty());
            grayGlass.setItemMeta(grayMeta);
        }
        for (int i = 0; i < 9; i++) {
            if (i != 4) {
                customInv.setItem(i, grayGlass);
            }
        }

        ItemStack mainTotem = new ItemStack(getMainTotem(player));
        ItemMeta mainMeta = mainTotem.getItemMeta();
        if (mainMeta != null) {
            mainMeta.displayName(getName(player, "main").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            mainMeta.lore(getTotemLore(player, "main"));
            mainTotem.setItemMeta(mainMeta);
        }
        customInv.setItem(4, mainTotem);

        draw3x3Background(customInv, 19, Material.LIME_STAINED_GLASS_PANE);
        draw3x3Background(customInv, 22, Material.MAGENTA_STAINED_GLASS_PANE);
        draw3x3Background(customInv, 25, Material.YELLOW_STAINED_GLASS_PANE);

        setTotemItem(player, customInv, "R", 19, NamedTextColor.GREEN);
        setTotemItem(player, customInv, "Q", 22, NamedTextColor.LIGHT_PURPLE);
        setTotemItem(player, customInv, "F", 25, NamedTextColor.YELLOW);
    }
}