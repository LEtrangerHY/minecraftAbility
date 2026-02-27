package org.core.coreSystem.cores.VOL3.Residue.coreSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL3.Residue.Skill.F;
import org.core.coreSystem.cores.VOL3.Residue.Skill.Q;
import org.core.coreSystem.cores.VOL3.Residue.Skill.R;
import org.core.main.Core;
import org.core.main.coreConfig;

public class residueCore extends absCore {
    private final Core plugin;
    private final Residue config;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public residueCore(Core plugin, coreConfig tag, Residue config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

        plugin.getLogger().info("Residue downloaded...");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        if(!contains(event.getPlayer())) return;

        Player player = event.getPlayer();
        applyAdditionalHealth(player, false);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRespawn(PlayerRespawnEvent event) {
        if(!contains(event.getPlayer())) return;

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            applyAdditionalHealth(player, true);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPearlThrow(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!contains(player)) return;

        ItemStack item = event.getItem();
        if (isSpecialPearl(item)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setUseItemInHand(Event.Result.DENY);
                player.updateInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPearlDropPrevention(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!contains(player)) return;

        ItemStack item = event.getItemDrop().getItemStack();
        if (isSpecialPearl(item)) {
            event.setCancelled(true);
            if (Q.isSessionActive(player) && Q.isLanded(player)) {
                if (cool.isReloading(player, "Q Reuse")) {
                    Qskill.Trigger(player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!contains(player)) return;

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        if (event.isShiftClick()) {
            if (clickedInv.getType() == InventoryType.PLAYER) {
                if (isSpecialPearl(event.getCurrentItem())) {
                    if (event.getView().getTopInventory().getType() != InventoryType.CRAFTING) {
                        event.setCancelled(true);
                    }
                }
            }
        }
        else {
            if (clickedInv.getType() != InventoryType.PLAYER) {
                if (isSpecialPearl(event.getCursor())) {
                    event.setCancelled(true);
                }
                if (event.getClick().isKeyboardClick()) {
                    int hotbarSlot = event.getHotbarButton();
                    if (hotbarSlot >= 0) {
                        ItemStack item = player.getInventory().getItem(hotbarSlot);
                        if (isSpecialPearl(item)) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!contains(player)) return;

        if (isSpecialPearl(event.getOldCursor())) {
            int topSize = event.getView().getTopInventory().getSize();
            for (int slot : event.getRawSlots()) {
                if (slot < topSize) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    public boolean isSpecialPearl(ItemStack item) {
        if (item == null || item.getType() != Material.ENDER_PEARL) return false;
        if (!item.hasItemMeta()) return false;

        NamespacedKey key = new NamespacedKey(plugin, Q.PEARL_KEY);
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private void applyAdditionalHealth(Player player, boolean healFull) {
        long addHP =
                player.getPersistentDataContainer().getOrDefault(
                        new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L) * 3;

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double current = maxHealth.getBaseValue();
            double newMax = current + addHP;

            maxHealth.setBaseValue(newMax);

            if (healFull) {
                player.setHealth(newMax);
            } else if (player.getHealth() > newMax) {
                player.setHealth(newMax);
            }
        }
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Residue.contains(player);
    }

    @Override
    protected SkillBase getRSkill() {
        return Rskill;
    }

    @Override
    protected SkillBase getQSkill() {
        return Qskill;
    }

    @Override
    protected SkillBase getFSkill() {
        return Fskill;
    }

    private boolean hasProperItems(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        boolean hasChain = (off.getType() == Material.IRON_CHAIN);

        if (Q.isSessionActive(player)) {
            boolean isTeleportPearl = isSpecialPearl(main);
            return isTeleportPearl && hasChain;
        } else {
            boolean hasSpear = (main.getType() == Material.IRON_SPEAR);
            return hasSpear && hasChain;
        }
    }

    private boolean canUseRSkill(Player player) { return true; }

    private boolean canUseQSkill(Player player) {
        if (Q.isSessionActive(player) && Q.isLanded(player)) {
            return true;
        }
        return !config.isSpearFlying.getOrDefault(player.getUniqueId(), false);
    }

    private boolean canUseFSkill(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        return (main.getType() == Material.IRON_SPEAR);
    }

    @Override
    protected boolean isItemRequired(Player player){
        return hasProperItems(player);
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem){
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() != Material.IRON_CHAIN) return false;

        if (Q.isSessionActive(player)) {
            return isSpecialPearl(droppedItem);
        } else {
            return droppedItem.getType() == Material.IRON_SPEAR;
        }
    }

    @Override
    protected boolean isRCondition(Player player) {
        return canUseRSkill(player);
    }

    @Override
    protected boolean isQCondition(Player player) {
        return canUseQSkill(player);
    }

    @Override
    protected boolean isFCondition(Player player) {
        return canUseFSkill(player);
    }

    @Override
    protected ConfigWrapper getConfigWrapper() {
        return new ConfigWrapper() {
            @Override
            public void variableReset(Player player) {
                config.variableReset(player);
            }
            @Override
            public void cooldownReset(Player player) {
                cool.setCooldown(player, 6000L, "R");
                cool.pauseCooldown(player, "R");
                cool.setCooldown(player, config.frozenCool, "Q");
                cool.setCooldown(player, config.frozenCool, "F");

                cool.updateCooldown(player, "R", 6000L);
                cool.pauseCooldown(player, "R");
                cool.updateCooldown(player, "Q", config.frozenCool);
                cool.updateCooldown(player, "F", config.frozenCool);
            }

            @Override
            public long getRcooldown(Player player) {
                return config.R_COOLDOWN.getOrDefault(player.getUniqueId(), config.r_Skill_Cool);
            }

            @Override
            public long getQcooldown(Player player) {
                return config.Q_COOLDOWN.getOrDefault(player.getUniqueId(), config.q_Skill_Cool);
            }

            @Override
            public long getFcooldown(Player player) {
                return config.F_COOLDOWN.getOrDefault(player.getUniqueId(), config.f_Skill_Cool);
            }
        };
    }
}