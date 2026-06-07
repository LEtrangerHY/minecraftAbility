package org.core.coreSystem.absCoreSystem;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.core.cool.Cool;
import org.core.effect.crowdControl.Stun;
import org.core.main.coreConfig;

import java.util.*;

public abstract class absCore implements Listener {

    protected final coreConfig tag;
    protected final Cool cool;
    private final Map<UUID, Boolean> isSwingAnimate = new HashMap<>();

    public absCore(coreConfig tag, Cool cool) {
        this.tag = tag;
        this.cool = cool;
    }

    protected abstract boolean contains(Player player);
    protected abstract boolean isCustomAttackUser(Player player);
    protected abstract void LSkill(PlayerInteractEvent event, Player player);
    protected void onLSkillCooldown(PlayerInteractEvent event, Player player) {}
    protected abstract SkillBase getRSkill();
    protected abstract SkillBase getQSkill();
    protected abstract SkillBase getFSkill();
    protected abstract boolean isItemRequired(Player player);
    protected abstract boolean isDropRequired(Player player, ItemStack droppedItem);
    protected abstract boolean isRCondition(Player player);
    protected abstract boolean isQCondition(Player player);
    protected abstract boolean isFCondition(Player player);
    protected abstract boolean isRAnimated(Player player);
    protected abstract boolean isFAnimated(Player player);
    protected abstract ConfigWrapper getConfigWrapper();

    public void manualReset(Player player) {
        ConfigWrapper wrapper = getConfigWrapper();
        if (wrapper != null) {
            wrapper.variableReset(player);
            wrapper.cooldownReset(player);
        }
    }

    @EventHandler
    public void variableQuitDelete(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (contains(player)) {
            getConfigWrapper().variableReset(player);
        }
    }

    @EventHandler
    public void variableDeathDelete(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (contains(player)) {
            getConfigWrapper().variableReset(player);
        }
    }

    @EventHandler
    public void cooldownReset(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (contains(player)) getConfigWrapper().cooldownReset(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void passiveEffect(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();
        if (Stun.isStunned(entity)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void blockClear(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!contains(player) || !isItemRequired(player)) return;

        event.setCancelled(true);

    }

    @EventHandler(priority = EventPriority.HIGH)
    public void passiveAttackTrigger(PlayerInteractEvent event) {
        if (!(event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();

        if (isSwingAnimate.getOrDefault(player.getUniqueId(), false)) {
            isSwingAnimate.remove(player.getUniqueId());
            return;
        }

        if (!isCustomAttackUser(player) || !contains(player) || !isItemRequired(player) || Stun.isStunned(player)) return;

        event.setCancelled(true);

        if (cool.isReloading(player, "L")) {
            onLSkillCooldown(event, player);
            return;
        }

        cool.setCooldown(player, this.getConfigWrapper().getLcooldown(player), "L");
        LSkill(event, player);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void rSkillTrigger(PlayerInteractEvent event) {
        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();

        if (main.getType() == Material.IRON_SPEAR || main.getType() == Material.MACE) return;

        if (!contains(player) || !isItemRequired(player) || Stun.isStunned(player)) return;

        event.setCancelled(true);

        if (cool.isReloading(player, "R") || !isRCondition(player)) return;

        if (isRAnimated(player)) isSwingAnimate.put(player.getUniqueId(), true);

        cool.setCooldown(player, this.getConfigWrapper().getRcooldown(player), "R");
        getRSkill().Trigger(player);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void qSkillTrigger(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        ItemStack dropped = event.getItemDrop().getItemStack();

        if (!contains(player) || Stun.isStunned(player) || !isDropRequired(player, dropped)) return;

        isSwingAnimate.put(player.getUniqueId(), true);

        event.setCancelled(true);

        if (cool.isReloading(player, "Q") || !isQCondition(player)) return;

        cool.setCooldown(player, getConfigWrapper().getQcooldown(player), "Q");
        getQSkill().Trigger(player);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void fSkillTrigger(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!contains(player) || !isItemRequired(player) || Stun.isStunned(player)) return;

        event.setCancelled(true);

        if (cool.isReloading(player, "F") || !isFCondition(player)) return;

        if (isRAnimated(player)) isSwingAnimate.put(player.getUniqueId(), true);

        cool.setCooldown(player, getConfigWrapper().getFcooldown(player), "F");
        getFSkill().Trigger(player);
    }
}