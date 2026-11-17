package org.core.coreSystem.cores.VOL6.Carpenter.coreSystem;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.core.cool.Cool;
import org.core.effect.crowdControl.Stun;
import org.core.effect.effectPlugin.swingEchoGuard;
import org.core.main.Core;
import org.core.main.coreConfig;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL6.Carpenter.Passive.Christmas;
import org.core.coreSystem.cores.VOL6.Carpenter.Passive.basicAttack;
import org.core.coreSystem.cores.VOL6.Carpenter.Skill.F;
import org.core.coreSystem.cores.VOL6.Carpenter.Skill.Q;
import org.core.coreSystem.cores.VOL6.Carpenter.Skill.R;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class carpCore extends absCore {
    private final Core plugin;
    private final Carpenter config;

    private final Christmas torque;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;
    private final basicAttack attack;
    private final Map<UUID, Integer> lastUseTick = new HashMap<>();

    public carpCore(Core plugin, coreConfig tag, Carpenter config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.torque = new Christmas(tag, config, plugin, cool);

        this.attack = new basicAttack(config, plugin);
        this.Rskill = new R(config, plugin, cool, attack);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

        plugin.getLogger().info("Carpenter downloaded...");
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
        if (swingEchoGuard.isEchoPending(player)) return;

        Integer used = lastUseTick.get(player.getUniqueId());
        if (used != null && Bukkit.getCurrentTick() - used <= 1) return;

        if (cool.isReloading(player, "L")) return;

        cool.setCooldown(player, config.basic_Attack_Cool, "L");
        attack.attack(player);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void totemDeath(PlayerDeathEvent event) {
        Fskill.death(event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void totemQuit(PlayerQuitEvent event) {
        Fskill.release(event.getPlayer());
    }

    @EventHandler
    public void totemJoin(PlayerJoinEvent event) {
        Fskill.release(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void totemUsed(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player) || !Fskill.isActive(player)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> Fskill.release(player), 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void totemSwap(PlayerSwapHandItemsEvent event) {
        if (Fskill.isActive(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void totemDrop(PlayerDropItemEvent event) {
        if (Fskill.isSkillTotem(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void totemClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !Fskill.isActive(player)) return;

        boolean offHand = event.getClickedInventory() instanceof PlayerInventory && event.getSlot() == 40;
        boolean collect = event.getAction() == InventoryAction.COLLECT_TO_CURSOR && event.getCursor().getType() == Material.TOTEM_OF_UNDYING;
        if (offHand || collect || event.getClick() == ClickType.SWAP_OFFHAND
                || Fskill.isSkillTotem(event.getCurrentItem()) || Fskill.isSkillTotem(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void totemDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !Fskill.isActive(player)) return;

        for (int raw : event.getRawSlots()) {
            if (event.getView().getInventory(raw) instanceof PlayerInventory && event.getView().convertSlot(raw) == 40) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void passiveTotem(EntityResurrectEvent event){
        if (event.isCancelled()) return;

        Entity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (tag.Carpenter.contains(player)) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    torque.recurrent(player);
                }, 1L);
            }
        }
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Carpenter.contains(player);
    }

    @Override
    protected boolean isCustomAttackUser(Player player) {
        return false;
    }

    @Override
    protected void onLSkillCooldown(PlayerInteractEvent event, Player player) {
    }

    @Override
    protected void LSkill(PlayerInteractEvent event, Player player) {
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
        return ((main.getType() == Material.TRIPWIRE_HOOK && off.getType() == Material.AIR) || (main.getType() == Material.TRIPWIRE_HOOK && off.getType() == Material.TOTEM_OF_UNDYING));
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem){
        ItemStack off = player.getInventory().getItemInOffHand();
        return (droppedItem.getType() == Material.TRIPWIRE_HOOK &&
                off.getType() == Material.AIR &&
                canUseQSkill(player)) || (droppedItem.getType() == Material.TRIPWIRE_HOOK &&
                off.getType() == Material.TOTEM_OF_UNDYING);
    }

    private boolean canUseRSkill(Player player) { return true; }

    private boolean canUseQSkill(Player player) {
        return true;
    }

    private boolean canUseFSkill(Player player) { return true; }

    @Override
    protected boolean isItemRequired(Player player){
        return hasProperItems(player);
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
                Fskill.release(player);
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