package org.core.coreSystem.cores.VOL1.Bamboo.coreSystem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.core.cool.Cool;
import org.core.main.Core;
import org.core.main.coreConfig;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL1.Bamboo.Passive.IngReload;
import org.core.coreSystem.cores.VOL1.Bamboo.Skill.F;
import org.core.coreSystem.cores.VOL1.Bamboo.Skill.Q;
import org.core.coreSystem.cores.VOL1.Bamboo.Skill.R;

public class bambCore extends absCore {

    private final Core plugin;
    private final Bamboo config;

    private final IngReload ingreload;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public bambCore(Core plugin, coreConfig tag, Bamboo config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.ingreload = new IngReload();

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

        plugin.getLogger().info("Bamboo downloaded...");
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
        long addHP =
                player.getPersistentDataContainer().getOrDefault(
                        new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L)
                        +
                        player.getPersistentDataContainer().getOrDefault(
                                new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);

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

    @EventHandler(priority = EventPriority.NORMAL)
    public void passiveAttackEffect(PlayerInteractEvent event) {
        if(tag.Bamboo.contains(event.getPlayer())){
            if (pAttackUsing.contains(event.getPlayer().getUniqueId())) {
                pAttackUsing.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler
    public void passiveEffect(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        if(tag.Bamboo.contains(player)) {
            if (hasProperItems(player)) {
                if (!config.r_damaged.getOrDefault(player.getUniqueId(), false)) {
                    if (!config.reloaded.getOrDefault(player.getUniqueId(), false)) {
                        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT, 1, 1);
                        event.setDamage(4.0);
                    } else {
                        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1, 1);
                        event.setDamage(6.0);
                    }
                }
            }
        }
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
        return main.getType() == Material.BAMBOO && off.getType() == Material.IRON_NUGGET;
    }

    private boolean canUseRSkill(Player player) {
        return true;
    }

    private boolean canUseQSkill(Player player) {
        return true;
    }

    private boolean canUseFSkill(Player player) {
        return true;
    }

    @Override
    protected boolean isItemRequired(Player player){
        return hasProperItems(player);
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem){
        ItemStack off = player.getInventory().getItemInOffHand();
        return droppedItem.getType() == Material.BAMBOO &&
                off.getType() == Material.IRON_NUGGET;
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
                cool.setCooldown(player, config.frozenCool, "R");
                cool.setCooldown(player, config.frozenCool, "Q");
                cool.setCooldown(player, config.frozenCool, "F");

                cool.updateCooldown(player, "R", config.frozenCool);
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
