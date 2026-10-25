package org.core.coreSystem.cores.VOL5.Apocalypse.coreSystem;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL5.Apocalypse.Skill.BatCombo;
import org.core.coreSystem.cores.VOL5.Apocalypse.Skill.F;
import org.core.coreSystem.cores.VOL5.Apocalypse.Skill.Q;
import org.core.coreSystem.cores.VOL5.Apocalypse.Skill.R;
import org.core.effect.crowdControl.Stun;
import org.core.effect.effectPlugin.swingEchoGuard;
import org.core.main.Core;
import org.core.main.coreConfig;

public class apoCore extends absCore {

    private final Core plugin;
    private final Apocalypse config;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;
    private final BatCombo combo;
    private final NamespacedKey speedKey;

    public apoCore(Core plugin, coreConfig tag, Apocalypse config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.combo = new BatCombo(config, plugin, cool);
        this.speedKey = new NamespacedKey(plugin, "apocalypse_bat_speed");
        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool, Rskill, combo);
        this.Fskill = new F(config, plugin, cool, Rskill, combo, this::scheduleSync);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) syncAttackSpeed(player);
        }, 20L, 5L);

        plugin.getLogger().info("Apocalypse downloaded...");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoinHide(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Rskill.hideFrom(player);
        scheduleSync(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuitCleanup(PlayerQuitEvent event) {
        Rskill.forceCleanup(event.getPlayer());
        combo.clearBar(event.getPlayer());
        Fskill.cleanup(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeathCleanup(PlayerDeathEvent event) {
        Rskill.forceCleanup(event.getEntity());
        combo.clearBar(event.getEntity());
        Fskill.cleanup(event.getEntity());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrawnHotbar(PlayerItemHeldEvent event) {
        if (Fskill.isDrawn(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrawnClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && Fskill.isDrawn(player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrawnPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && Fskill.isDrawn(player)) event.setCancelled(true);
    }

    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) return;
        Fskill.onSneakRelease(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onParry(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player) || !contains(player)) return;
        Fskill.tryParry(player, event);
    }

    private boolean hasEquipment(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Apocalypse.BAT
                && player.getInventory().getItemInOffHand().getType() == Apocalypse.SWORD;
    }

    private void syncAttackSpeed(Player player) {
        if (!player.isOnline()) return;
        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed == null) return;

        AttributeModifier current = attackSpeed.getModifier(speedKey);
        boolean drawn = contains(player) && Fskill.isDrawn(player);
        boolean active = drawn || (contains(player) && hasEquipment(player));

        if (!active) {
            if (current != null) attackSpeed.removeModifier(speedKey);
            return;
        }

        double others = attackSpeed.getBaseValue();
        for (AttributeModifier modifier : attackSpeed.getModifiers()) {
            if (modifier.getKey().equals(speedKey)) continue;
            if (modifier.getOperation() == AttributeModifier.Operation.ADD_NUMBER) others += modifier.getAmount();
        }
        double amount = (drawn ? config.f_drawn_attack_speed : config.bat_attack_speed) - others;
        if (current != null && Math.abs(current.getAmount() - amount) < 1e-6) return;
        if (current != null) attackSpeed.removeModifier(speedKey);
        attackSpeed.addTransientModifier(new AttributeModifier(speedKey, amount,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
    }

    private void scheduleSync(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> syncAttackSpeed(player));
    }

    @EventHandler
    public void onHeldChange(PlayerItemHeldEvent event) {
        scheduleSync(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        scheduleSync(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) scheduleSync(player);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBatAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !contains(player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (config.skillDamaging.contains(player.getUniqueId())) return;
        if (!hasEquipment(player) && !Fskill.isDrawn(player)) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBatSwing(PlayerArmSwingEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!contains(player) || !hasEquipment(player) || Stun.isStunned(player)) return;
        if (swingEchoGuard.isEchoPending(player)) return;

        combo.onSwing(player);
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Apocalypse.contains(player);
    }

    @Override
    protected boolean isCustomAttackUser(Player player) {
        return false;
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

    @Override
    protected boolean isItemRequired(Player player) {
        return hasEquipment(player) || Fskill.isDrawn(player);
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem) {
        if (Fskill.isDrawn(player)) {
            return droppedItem.getType() == Apocalypse.SWORD || droppedItem.getType() == Apocalypse.BAT;
        }
        return droppedItem.getType() == Apocalypse.BAT
                && player.getInventory().getItemInOffHand().getType() == Apocalypse.SWORD;
    }

    @Override
    protected boolean isRCondition(Player player) {
        return !Fskill.isDashing(player) && !Fskill.isDrawn(player);
    }

    @Override
    protected boolean isQCondition(Player player) {
        return !Fskill.isDashing(player) && !Fskill.isDrawn(player);
    }

    @Override
    protected boolean isFCondition(Player player) {
        return true;
    }

    @Override
    protected ConfigWrapper getConfigWrapper() {
        return new ConfigWrapper() {
            @Override
            public void variableReset(Player player) {
                Fskill.cleanup(player);
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
                return 0L;
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
                return 0L;
            }
        };
    }
}