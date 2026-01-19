package org.core.coreSystem.absCoreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.core.cool.Cool;
import org.core.effect.crowdControl.Stun;
import org.core.main.coreConfig;

import java.util.*;

public abstract class absCore implements Listener {

    protected final coreConfig tag;
    protected final Cool cool;

    public absCore(coreConfig tag, Cool cool) {
        this.tag = tag;
        this.cool = cool;
    }

    protected abstract boolean contains(Player player);

    protected abstract SkillBase getRSkill();
    protected abstract SkillBase getQSkill();
    protected abstract SkillBase getFSkill();

    protected abstract boolean isItemRequired(Player player);
    protected abstract boolean isDropRequired(Player player, ItemStack droppedItem);
    protected abstract boolean isRCondition(Player player);
    protected abstract boolean isQCondition(Player player);
    protected abstract boolean isFCondition(Player player);

    protected abstract ConfigWrapper getConfigWrapper();

    @EventHandler
    public void variableQuitDelete(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if(contains(player)) getConfigWrapper().variableReset(player);
    }

    @EventHandler
    public void variableDeathDelete(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if(contains(player)) getConfigWrapper().variableReset(player);
    }

    @EventHandler
    public void cooldownReset(PlayerJoinEvent event){
        Player player = event.getPlayer();
        if(contains(player)) getConfigWrapper().cooldownReset(player);
    }

    public static HashSet<UUID> pAttackUsing = new HashSet<>();

    @EventHandler(priority = EventPriority.HIGH)
    public void passiveEffect(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();

        if(Stun.isStunned(entity)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void rSkillTrigger(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!contains(player) || !isItemRequired(player) || Stun.isStunned(player)) return;

        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {

                event.setCancelled(true);

                if(!pAttackUsing.contains(player.getUniqueId())) {
                    pAttackUsing.add(player.getUniqueId());
                }

                if (this.cool.isReloading(player, "R") || !isRCondition(player)) {
                    return;
                }

                this.cool.setCooldown(player, this.getConfigWrapper().getRcooldown(player), "R");
                this.getRSkill().Trigger(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void qSkillTrigger(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        ItemStack dropped = event.getItemDrop().getItemStack();

        if (contains(player) && !Stun.isStunned(player) && isDropRequired(player, dropped)) {

            event.setCancelled(true);

            if(!pAttackUsing.contains(player.getUniqueId())) pAttackUsing.add(player.getUniqueId());

            if (cool.isReloading(player, "Q") || !isQCondition(player)) return;

            cool.setCooldown(player, getConfigWrapper().getQcooldown(player), "Q");
            getQSkill().Trigger(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void fSkillTrigger(PlayerSwapHandItemsEvent event) {

        Player player = event.getPlayer();

        if (!contains(player) || !isItemRequired(player) || Stun.isStunned(player)) return;

        event.setCancelled(true);

        if(!pAttackUsing.contains(player.getUniqueId())) {
            pAttackUsing.add(player.getUniqueId());
        }

        if (cool.isReloading(player, "F") || !isFCondition(player)) return;

        cool.setCooldown(player, getConfigWrapper().getFcooldown(player), "F");
        getFSkill().Trigger(player);
    }
}