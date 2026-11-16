package org.core.coreSystem.cores.VOL1.Bamboo.coreSystem;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.core.cool.Cool;
import org.core.main.Core;
import org.core.main.coreConfig;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL1.Bamboo.Passive.basicAttack;
import org.core.coreSystem.cores.VOL1.Bamboo.Skill.F;
import org.core.coreSystem.cores.VOL1.Bamboo.Skill.Q;
import org.core.coreSystem.cores.VOL1.Bamboo.Skill.R;
import org.core.effect.crowdControl.Stun;
import org.core.effect.effectPlugin.swingEchoGuard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class bambCore extends absCore {

    private final Core plugin;
    private final Bamboo config;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;
    private final basicAttack attack;
    private final Map<UUID, Integer> lastUseTick = new HashMap<>();

    public bambCore(Core plugin, coreConfig tag, Bamboo config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool, Rskill);
        this.attack = new basicAttack(config, plugin);

        plugin.getLogger().info("Bamboo downloaded...");
    }

    @EventHandler
    public void onLeashBreak(EntityUnleashEvent event) {
        if (event.getEntity().getScoreboardTags().contains("bamboo_projectile")) {
            event.setDropLeash(false);
            if (event.getReason() == EntityUnleashEvent.UnleashReason.DISTANCE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDetonatorUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!contains(player)) return;

        ItemStack item = event.getItem();
        if (isSpecialItem(item)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                player.updateInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDetonatorDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!contains(player)) return;

        ItemStack item = event.getItemDrop().getItemStack();
        if (isSpecialItem(item)) {
            event.setCancelled(true);
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
                if (isSpecialItem(event.getCurrentItem())) {
                    if (event.getView().getTopInventory().getType() != InventoryType.CRAFTING) {
                        event.setCancelled(true);
                    }
                }
            }
        } else {
            if (clickedInv.getType() != InventoryType.PLAYER) {
                if (isSpecialItem(event.getCursor())) {
                    event.setCancelled(true);
                }
                if (event.getClick().isKeyboardClick()) {
                    int hotbarSlot = event.getHotbarButton();
                    if (hotbarSlot >= 0) {
                        ItemStack item = player.getInventory().getItem(hotbarSlot);
                        if (isSpecialItem(item)) {
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

        if (isSpecialItem(event.getOldCursor())) {
            int topSize = event.getView().getTopInventory().getSize();
            for (int slot : event.getRawSlots()) {
                if (slot < topSize) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private boolean isSpecialItem(ItemStack item) {
        if (item == null || item.getType() != Material.REDSTONE) return false;
        if (!item.hasItemMeta()) return false;

        NamespacedKey key = new NamespacedKey(plugin, R.REDSTONE_KEY);
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onVanillaAttack(PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        if (!contains(player) || !hasProperItems(player)) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUseInput(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!contains(player)) return;

        lastUseTick.put(player.getUniqueId(), Bukkit.getCurrentTick());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAttackSwing(PlayerArmSwingEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!contains(player) || !hasProperItems(player) || Stun.isStunned(player)) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.BAMBOO) return;
        if (swingEchoGuard.isEchoPending(player)) return;

        Integer used = lastUseTick.get(player.getUniqueId());
        if (used != null && Bukkit.getCurrentTick() - used <= 1) return;

        if (cool.isReloading(player, "L")) return;

        cool.setCooldown(player, config.basic_Attack_Cool, "L");
        attack.attack(player);
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if(tag.Bamboo.contains(player)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL &&
                    player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "noFallDamage"), PersistentDataType.BOOLEAN, false)) {
                event.setCancelled(true);
                player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "noFallDamage"));
            }
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if(tag.Bamboo.contains(player)) {
            if (player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "noFallDamage"), PersistentDataType.BOOLEAN, false)) {
                player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "noFallDamage"));
            }
        }
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Bamboo.contains(player);
    }

    @Override
    protected boolean isCustomAttackUser(Player player) {
        return false;
    }

    @Override
    protected void LSkill(PlayerInteractEvent event, Player player) {
    }

    @Override
    protected SkillBase getRSkill() { return Rskill; }
    @Override
    protected SkillBase getQSkill() { return Qskill; }
    @Override
    protected SkillBase getFSkill() { return Fskill; }

    private boolean hasProperItems(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        boolean hasIron = (off.getType() == Material.IRON_NUGGET);

        if (main.getType() == Material.BAMBOO) return hasIron;

        if (Rskill.isSessionActive(player)) {
            if (main.getType() == Material.REDSTONE && main.hasItemMeta()) {
                NamespacedKey key = new NamespacedKey(plugin, R.REDSTONE_KEY);
                return main.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE) && hasIron;
            }
        }
        return false;
    }

    private boolean canUseRSkill(Player player) { return true; }
    private boolean canUseQSkill(Player player) { return true; }
    private boolean canUseFSkill(Player player) { return true; }

    @Override
    protected boolean isItemRequired(Player player){
        return hasProperItems(player);
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem){
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() != Material.IRON_NUGGET) return false;

        if (droppedItem.getType() == Material.REDSTONE) {
            return isSpecialItem(droppedItem);
        }

        return droppedItem.getType() == Material.BAMBOO;
    }

    @Override
    protected boolean isRCondition(Player player) { return canUseRSkill(player); }
    @Override
    protected boolean isQCondition(Player player) { return canUseQSkill(player); }
    @Override
    protected boolean isFCondition(Player player) { return canUseFSkill(player); }

    @Override
    protected ConfigWrapper getConfigWrapper() {
        return new ConfigWrapper() {
            @Override
            public void variableReset(Player player) {
                config.variableReset(player);
            }

            @Override
            public void cooldownReset(Player player) {
                cool.setCooldown(player, config.frozenCool, "R");
                cool.setCooldown(player, config.frozenCool, "Q");
                cool.setCooldown(player, config.frozenCool, "F");

                cool.updateCooldown(player, "R", config.frozenCool);
                cool.updateCooldown(player, "Q", config.frozenCool);
                cool.updateCooldown(player, "F", config.frozenCool);
            }

            @Override
            public long getLcooldown(Player player) {
                return 0;
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