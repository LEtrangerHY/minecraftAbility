package org.core.coreSystem.cores.VOL2.Undead.coreSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL2.Undead.Passive.Boost;
import org.core.coreSystem.cores.VOL2.Undead.Skill.F;
import org.core.coreSystem.cores.VOL2.Undead.Skill.Q;
import org.core.coreSystem.cores.VOL2.Undead.Skill.DeadlyWeapons.R;
import org.core.main.Core;
import org.core.main.coreConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class undeadCore extends absCore {
    private final Core plugin;
    private final Undead config;

    private final Boost boost;
    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    private final Map<UUID, BossBar> activeChargeBars = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeChargeTasks = new HashMap<>();

    public undeadCore(Core plugin, coreConfig tag, Undead config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.boost = new Boost(config, tag, plugin);

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

        plugin.getLogger().info("Undead downloaded...");
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

    @EventHandler
    public void sneakCharge(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (!contains(player)) return;

        if (!event.isSneaking()) {
            cleanupCharge(player);
            return;
        }

        if (!hasProperItems(player)) return;

        config.f_charge_consumed.remove(player.getUniqueId());
        config.f_charge_progress.put(player.getUniqueId(), 0.0);

        config.f_charge_hit_count.put(player.getUniqueId(), 0);

        BossBar bossBar = Bukkit.createBossBar("F skill Charge", BarColor.RED, BarStyle.SEGMENTED_10);
        bossBar.setProgress(0.0);
        bossBar.addPlayer(player);
        activeChargeBars.put(player.getUniqueId(), bossBar);

        BukkitRunnable task = new BukkitRunnable() {
            long ticks = 0;
            final long maxTicks = 80L;

            @Override
            public void run() {
                if (!player.isSneaking() || !hasProperItems(player) || !player.isValid() || config.f_charge_consumed.getOrDefault(player.getUniqueId(), false)) {
                    cleanupCharge(player);
                    return;
                }

                if (ticks < maxTicks) {
                    ticks++;
                }

                double progress = (double) ticks / maxTicks;
                bossBar.setProgress(progress);
                config.f_charge_progress.put(player.getUniqueId(), progress);

                double baseReduction;
                if (progress <= 0.5) {
                    baseReduction = 0.16 + (0.48 * (progress / 0.5));
                } else {
                    baseReduction = 0.64;
                }

                int hitCount = config.f_charge_hit_count.getOrDefault(player.getUniqueId(), 0);
                double penalty = hitCount * 0.04;

                double currentReduction = Math.max(0.0, baseReduction - penalty);

                double multiplier = 1.0 - currentReduction;
                config.f_charge_defend.put(player.getUniqueId(), multiplier);

                if (progress >= 1.0) {
                    bossBar.setTitle("F skill Charge - MAX");
                    bossBar.setColor(BarColor.RED);
                }
            }
        };

        task.runTaskTimer(plugin, 0L, 1L);
        activeChargeTasks.put(player.getUniqueId(), task);
    }

    private void cleanupCharge(Player player) {
        UUID uuid = player.getUniqueId();
        if (activeChargeBars.containsKey(uuid)) {
            activeChargeBars.get(uuid).removeAll();
            activeChargeBars.remove(uuid);
        }
        if (activeChargeTasks.containsKey(uuid)) {
            activeChargeTasks.get(uuid).cancel();
            activeChargeTasks.remove(uuid);
        }
        config.f_charge_progress.put(uuid, 0.0);

        config.f_charge_defend.remove(uuid);
        config.f_charge_hit_count.remove(uuid);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!contains(player)) return;

        event.setFoodLevel(0);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageTaken(EntityDamageEvent event) {
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

        double debuffRate = config.f_debuff_rate.getOrDefault(player.getUniqueId(), 0.0);
        if (debuffRate > 0) {
            finalDamage *= (1.0 + debuffRate);
        }

        if (activeChargeTasks.containsKey(player.getUniqueId())) {
            double multiplier = config.f_charge_defend.getOrDefault(player.getUniqueId(), 0.84);
            finalDamage *= multiplier;

            int currentHits = config.f_charge_hit_count.getOrDefault(player.getUniqueId(), 0);
            config.f_charge_hit_count.put(player.getUniqueId(), currentHits + 1);
        }

        event.setDamage(finalDamage);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageGiven(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!contains(player)) return;

        double buffRate = config.f_buff_rate.getOrDefault(player.getUniqueId(), 0.0);
        if (buffRate > 0 && !event.isCancelled()) {
            event.setDamage(event.getDamage() * (1.0 + buffRate));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickupDuringSwap(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (config.is_swapping.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
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

    public void syncCooldowns(Player player) {
        if (!contains(player)) return;

        Material handMat = player.getInventory().getItemInMainHand().getType();

        String rKey = config.getWeaponCoolKey(handMat);
        if (rKey != null) {
            cool.updateCooldown(player, "R", cool.getRemainCooldown(player, rKey));
        } else {
            cool.updateCooldown(player, "R", 0L);
        }

        String qKey = config.getWeaponQCoolKey(handMat);
        if (qKey != null) {
            cool.updateCooldown(player, "Q", cool.getRemainCooldown(player, qKey));
        } else {
            cool.updateCooldown(player, "Q", 0L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSwap(org.bukkit.event.player.PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (contains(player)) {
            Bukkit.getScheduler().runTask(plugin, () -> syncCooldowns(player));
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

    private boolean isUndeadWeapon(Material material) {
        return material == Material.IRON_SHOVEL ||
                material == Material.IRON_PICKAXE ||
                material == Material.IRON_AXE ||
                material == Material.IRON_HOE ||
                material == Material.IRON_HORSE_ARMOR;
    }

    private boolean isUndeadOrAir(Material material) {
        return material == Material.AIR || isUndeadWeapon(material);
    }

    private boolean hasProperItems(Player player) {
        Material mainHand = player.getInventory().getItemInMainHand().getType();
        Material offHand = player.getInventory().getItemInOffHand().getType();

        if (!isUndeadOrAir(mainHand) || !isUndeadOrAir(offHand)) {
            return false;
        }

        return isUndeadWeapon(mainHand) || isUndeadWeapon(offHand);
    }

    private boolean canUseRSkill(Player player) { return true; }

    private boolean canUseQSkill(Player player) { return true; }

    private boolean canUseFSkill(Player player) { return true; }

    @Override
    protected boolean isItemRequired(Player player) {
        if (hasProperItems(player)) {
            return true;
        } else {
            AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
            if (attackSpeed != null) attackSpeed.setBaseValue(4.0);
            return false;
        }
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem) {
        return isUndeadWeapon(droppedItem.getType());
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
    protected boolean isRAnimated(Player player) {
        return true;
    }

    @Override
    protected boolean isFAnimated(Player player) {
        return false;
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
                return 0L;
            }

            @Override
            public long getRcooldown(Player player) {
                return 0L;
            }

            @Override
            public long getQcooldown(Player player) {
                return 0L;
            }

            @Override
            public long getFcooldown(Player player) {
                return config.F_COOLDOWN.getOrDefault(player.getUniqueId(), config.f_Skill_Cool);
            }
        };
    }
}