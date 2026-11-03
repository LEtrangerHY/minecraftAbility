package org.core.coreSystem.cores.VOL3.Knight.coreSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.main.Core;
import org.core.effect.crowdControl.ForceDamage;
import org.core.main.coreConfig;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL3.Knight.Passive.bladeBond;
import org.core.coreSystem.cores.VOL3.Knight.Skill.F;
import org.core.coreSystem.cores.VOL3.Knight.Skill.Q;
import org.core.coreSystem.cores.VOL3.Knight.Skill.R;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class knightCore extends absCore {
    private final Core plugin;
    private final Knight config;

    private final bladeBond blades;
    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public knightCore(Core plugin, coreConfig tag, Knight config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.blades = new bladeBond(config, plugin);
        this.Rskill = new R(config, plugin, cool, blades);
        this.Qskill = new Q(config, plugin, cool, blades);
        this.Fskill = new F(config, plugin, cool, blades);

        plugin.getLogger().info("Knight downloaded...");
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

    private void applyAdditionalHealth(Player player, boolean healFull) {
        long addHP = player.getPersistentDataContainer().getOrDefault(
                new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L)
                + player.getPersistentDataContainer().getOrDefault(
                new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L) * 2;

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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void parry(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player) || !contains(player)) return;
        if (!Qskill.isParrying(player)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && cause != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                && cause != EntityDamageEvent.DamageCause.PROJECTILE) return;

        LivingEntity attacker = null;
        Entity damager = event.getDamager();

        if (damager instanceof LivingEntity living) {
            attacker = living;
        } else if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) {
            attacker = shooter;
        }

        if (attacker == null || attacker.equals(player)) return;

        event.setCancelled(true);
        Qskill.onParry(player, attacker);
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Knight.contains(player);
    }

    @Override
    protected boolean isCustomAttackUser(Player player) {
        return !Fskill.isCasting(player);
    }

    @Override
    protected void onLSkillCooldown(PlayerInteractEvent event, Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 1, 1);
    }

    @Override
    protected void LSkill(PlayerInteractEvent event, Player player) {
        event.setCancelled(true);

        World world = player.getWorld();
        Location playerLocation = player.getLocation();
        Vector direction = playerLocation.getDirection().normalize().multiply(1.3);

        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

        config.collision.put(player.getUniqueId(), false);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        double share = config.l_Skill_Damage * config.bond_Share;
        Set<UUID> struck = new HashSet<>();
        Set<bladeBond.Blade> touched = new HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 7 || config.collision.getOrDefault(player.getUniqueId(), true)) {
                    config.collision.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                Location particleLocation = playerLocation.clone()
                        .add(direction.clone().multiply(ticks * 1.4))
                        .add(0, 1.4, 0);

                world.spawnParticle(Particle.SWEEP_ATTACK, particleLocation, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.ENCHANTED_HIT, particleLocation, 7, 0.3, 0.1, 0.3, 0);

                for (Entity entity : world.getNearbyEntities(particleLocation, 0.7, 0.3, 0.7)) {
                    if (entity instanceof LivingEntity target && entity != player) {

                        if (struck.add(target.getUniqueId())) {
                            ForceDamage forceDamage = new ForceDamage(target, config.l_Skill_Damage, source, false);
                            forceDamage.applyEffect(player);
                        }

                        for (bladeBond.Blade blade : blades.hostedBy(player, target)) {
                            if (touched.add(blade)) blades.bond(player, blade, share, source, struck);
                        }

                        config.collision.put(player.getUniqueId(), true);
                        break;
                    }
                }

                for (bladeBond.Blade blade : blades.near(player, particleLocation, config.l_Touch_Radius)) {
                    if (touched.add(blade)) blades.bond(player, blade, share, source, struck);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && offHand.hasItemMeta()) {
            ItemMeta meta = offHand.getItemMeta();
            if (meta instanceof Damageable && offHand.getType().getMaxDurability() > 0) {
                Damageable damageable = (Damageable) meta;
                int newDamage = damageable.getDamage() + 1;
                damageable.setDamage(newDamage);
                offHand.setItemMeta(meta);

                if (newDamage >= offHand.getType().getMaxDurability()) {
                    player.getInventory().setItemInOffHand(null);
                }
            }
        }
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

    private boolean isSword(ItemStack item) {
        if (item == null) return false;
        return item.getType().name().endsWith("_SWORD");
    }

    private boolean hasProperItems(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        return isSword(main) && isSword(off);
    }

    private boolean canUseRSkill(Player player) { return true; }

    private boolean canUseQSkill(Player player) { return true; }

    private boolean canUseFSkill(Player player) {
        if (blades.count(player) > 0) return true;
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 0.8f);
        return false;
    }

    @Override
    protected boolean isItemRequired(Player player){
        return hasProperItems(player);
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem){
        ItemStack off = player.getInventory().getItemInOffHand();
        return isSword(droppedItem) && isSword(off);
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
                Qskill.cleanup(player);
                Fskill.cleanup(player);
                blades.clear(player);
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
                return config.l_Skill_Cool;
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
                long scaled = Math.min(config.f_Cool_Max, config.f_Cool_Per_Blade * blades.count(player));
                return config.F_COOLDOWN.getOrDefault(player.getUniqueId(), scaled);
            }
        };
    }
}