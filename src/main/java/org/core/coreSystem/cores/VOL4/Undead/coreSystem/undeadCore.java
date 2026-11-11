package org.core.coreSystem.cores.VOL4.Undead.coreSystem;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL4.Undead.Passive.Boost;
import org.core.coreSystem.cores.VOL4.Undead.Passive.Restoration;
import org.core.coreSystem.cores.VOL4.Undead.Passive.basicAttack;
import org.core.coreSystem.cores.VOL4.Undead.Passive.handSwap;
import org.core.coreSystem.cores.VOL4.Undead.Passive.stickyBomb;
import org.core.coreSystem.cores.VOL4.Undead.Skill.F;
import org.core.coreSystem.cores.VOL4.Undead.Skill.Q;
import org.core.coreSystem.cores.VOL4.Undead.Skill.R;
import org.core.effect.crowdControl.Stun;
import org.core.effect.effectPlugin.swingEchoGuard;
import org.core.main.Core;
import org.core.main.coreConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class undeadCore extends absCore {
    private final Core plugin;
    private final Undead config;

    private final handSwap swap;
    private final basicAttack attack;
    private final Restoration restore;
    private final stickyBomb bomb;
    private final Boost boost;

    private final SkillBase Rskill;
    private final SkillBase Qskill;
    private final SkillBase Fskill;

    private final Map<UUID, Integer> lastUseTick = new HashMap<>();
    private final Map<UUID, Boolean> sprintInput = new HashMap<>();

    public undeadCore(Core plugin, coreConfig tag, Undead config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.swap = new handSwap(config, plugin, cool);
        this.bomb = new stickyBomb(config, plugin);
        this.attack = new basicAttack(config, plugin, swap, bomb);
        this.restore = new Restoration(config, plugin, swap);
        this.boost = new Boost(config, tag, plugin);

        this.Rskill = slot("R", new R(config, plugin, cool, swap, attack));
        this.Qskill = slot("Q", new Q(config, plugin, cool, swap, bomb));
        this.Fskill = slot("F", new F(config, plugin, cool, swap));

        cool.addActionBarEditor((player, skill, remaining, label, status) ->
                contains(player) && swap.isEquipped(player) ? swap.actionBar(player, skill, remaining, label, status) : null);

        plugin.getLogger().info("Undead downloaded...");
    }

    private SkillBase slot(String key, SkillBase skill) {
        return player -> swap.use(player, key, skill);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        if (!contains(event.getPlayer())) return;

        Player player = event.getPlayer();
        applyAdditionalHealth(player, false);

        player.setFoodLevel(0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, PotionEffect.INFINITE_DURATION, 0, false, false));

        boost.updateUndeadBoard(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!contains(event.getPlayer())) return;

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            applyAdditionalHealth(player, true);

            player.setFoodLevel(0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, PotionEffect.INFINITE_DURATION, 0, false, false));
        }, 1L);

        boost.updateUndeadBoard(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!contains(player)) return;

        event.setFoodLevel(0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, PotionEffect.INFINITE_DURATION, 0, false, false));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionEffectRemove(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!contains(player)) return;

        if (event.getOldEffect() != null && event.getOldEffect().getType().equals(PotionEffectType.HUNGER)) {
            EntityPotionEffectEvent.Action action = event.getAction();
            if (action == EntityPotionEffectEvent.Action.CLEARED || action == EntityPotionEffectEvent.Action.REMOVED) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!contains(player)) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isValid() && contains(player)) {
                player.setFoodLevel(0);
                if (!player.hasPotionEffect(PotionEffectType.HUNGER)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, PotionEffect.INFINITE_DURATION, 0, false, false));
                }
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageTaken(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!contains(player)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();

        if (cause == EntityDamageEvent.DamageCause.STARVATION) {
            event.setCancelled(true);
            return;
        }

        if (cause == EntityDamageEvent.DamageCause.KILL ||
                cause == EntityDamageEvent.DamageCause.VOID ||
                cause == EntityDamageEvent.DamageCause.SUICIDE ||
                cause == EntityDamageEvent.DamageCause.CUSTOM) {
            return;
        }

        if (event.isCancelled()) return;

        double finalDamage = event.getDamage();
        if (finalDamage >= 1000000.0) return;

        double debuff = config.restore_debuff_rate.getOrDefault(player.getUniqueId(), 0.0);
        if (debuff > 0) finalDamage *= (1.0 + debuff);

        if (restore.isCharging(player)) {
            finalDamage *= config.restore_charge_defend.getOrDefault(player.getUniqueId(), 0.84);
            config.restore_charge_hit_count.merge(player.getUniqueId(), 1, Integer::sum);
        }

        event.setDamage(finalDamage);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageGiven(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!contains(player) || event.isCancelled() || config.is_damaging.contains(player.getUniqueId())) return;

        double buff = config.restore_buff_rate.getOrDefault(player.getUniqueId(), 0.0);
        if (buff > 0) event.setDamage(event.getDamage() * (1.0 + buff));
    }

    @EventHandler
    public void onSprintInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        boolean sprint = event.getInput().isSprint();
        Boolean previous = sprintInput.put(player.getUniqueId(), sprint);

        if (!sprint || Boolean.TRUE.equals(previous)) return;
        if (!contains(player) || Stun.isStunned(player)) return;

        restore.startCharge(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickupDuringSwap(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (config.is_swapping.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onVanillaAttack(PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        if (!contains(player) || !swap.isEquipped(player)) return;

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
        if (!contains(player) || !swap.isEquipped(player) || swap.isBusy(player) || Stun.isStunned(player)) return;
        if (swingEchoGuard.isEchoPending(player)) return;

        Integer used = lastUseTick.get(player.getUniqueId());
        if (used != null && Bukkit.getCurrentTick() - used <= 1) return;

        if (cool.isReloading(player, "L")) return;

        cool.setCooldown(player, attack.attack(player), "L");
    }

    private void applyAdditionalHealth(Player player, boolean healFull) {
        long addHP = player.getPersistentDataContainer().getOrDefault(
                new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L) * 2;

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
        return tag.Undead.contains(player);
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

    private boolean canUseRSkill(Player player) { return !swap.isBusy(player); }

    private boolean canUseQSkill(Player player) { return !swap.isBusy(player); }

    private boolean canUseFSkill(Player player) { return !swap.isBusy(player); }

    @Override
    protected boolean isItemRequired(Player player) {
        return swap.isEquipped(player);
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem) {
        ItemStack off = player.getInventory().getItemInOffHand();
        boolean melee = config.isTool(droppedItem.getType()) && config.isGun(off.getType());
        boolean firearm = config.isGun(droppedItem.getType()) && config.isTool(off.getType());
        if (!melee && !firearm) return false;

        swap.markDrop(player, firearm);
        return true;
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
                lastUseTick.remove(player.getUniqueId());
                restore.clear(player);
                bomb.clear(player);
            }

            @Override
            public void cooldownReset(Player player) {
                boost.updateUndeadBoard(player);

                cool.setCooldown(player, config.frozenCool, "R");
                cool.setCooldown(player, config.frozenCool, "Q");
                cool.setCooldown(player, config.frozenCool, "F");

                cool.updateCooldown(player, "R", config.frozenCool);
                cool.updateCooldown(player, "Q", config.frozenCool);
                cool.updateCooldown(player, "F", config.frozenCool);
            }

            @Override
            public long getLcooldown(Player player) {
                return attack.cooldown(player);
            }

            @Override
            public long getRcooldown(Player player) {
                return config.R_COOLDOWN.getOrDefault(player.getUniqueId(),
                        swap.isFirearm(player) ? config.gun_r_Skill_Cool : config.melee_r_Skill_Cool);
            }

            @Override
            public long getQcooldown(Player player) {
                return config.Q_COOLDOWN.getOrDefault(player.getUniqueId(),
                        swap.isFirearm(player) ? config.gun_q_Skill_Cool : config.melee_q_Skill_Cool);
            }

            @Override
            public long getFcooldown(Player player) {
                return config.F_COOLDOWN.getOrDefault(player.getUniqueId(),
                        swap.isFirearm(player) ? config.gun_f_Skill_Cool : config.melee_f_Skill_Cool);
            }
        };
    }
}