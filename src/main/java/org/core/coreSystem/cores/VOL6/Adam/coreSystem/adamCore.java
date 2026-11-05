package org.core.coreSystem.cores.VOL6.Adam.coreSystem;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL6.Adam.Passive.stackCharge;
import org.core.coreSystem.cores.VOL6.Adam.Skill.F;
import org.core.coreSystem.cores.VOL6.Adam.Skill.Q;
import org.core.coreSystem.cores.VOL6.Adam.Skill.R;
import org.core.main.Core;
import org.core.main.coreConfig;

public class adamCore extends absCore {
    private final Core plugin;
    private final Adam config;

    private final stackCharge charge;
    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public adamCore(Core plugin, coreConfig tag, Adam config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.charge = new stackCharge(config, plugin, cool);
        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool, charge);
        this.Fskill = new F(config, plugin, cool, charge);

        plugin.getLogger().info("Adam downloaded...");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        if(!contains(event.getPlayer())) return;
        applyAdditionalHealth(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRespawn(PlayerRespawnEvent event) {
        if(!contains(event.getPlayer())) return;
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            applyAdditionalHealth(player, true);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void sneakCharge(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking() || !contains(player) || !hasProperItems(player)) return;

        charge.start(player, this::hasProperItems);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void meleeStack(PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        if (!event.willAttack() || !contains(player) || !hasProperItems(player)) return;
        if (!(event.getAttacked() instanceof LivingEntity target) || target.isDead()) return;
        if (player.getAttackCooldown() < config.stack_Melee_Strength) return;

        charge.add(player, 1);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void brace(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !contains(player) || !Fskill.isBracing(player)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID || cause == EntityDamageEvent.DamageCause.KILL) return;

        event.setDamage(event.getDamage() * (1 - config.f_Strong_Guard));
    }

    private void applyAdditionalHealth(Player player, boolean healFull) {
        long addHP = player.getPersistentDataContainer().getOrDefault(
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
    protected boolean contains(Player player) { return tag.Adam.contains(player); }

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
    protected SkillBase getRSkill() { return Rskill; }

    @Override
    protected SkillBase getQSkill() { return Qskill; }

    @Override
    protected SkillBase getFSkill() { return Fskill; }

    private boolean isSword(ItemStack item) {
        if (item == null) return false;
        return item.getType().name().endsWith("_SWORD");
    }

    private boolean hasProperItems(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return isSword(main) && off.getType() == Material.IRON_CHAIN;
    }

    private boolean canUseRSkill(Player player) { return true; }
    private boolean canUseQSkill(Player player) { return true; }
    private boolean canUseFSkill(Player player) { return true; }

    @Override
    protected boolean isItemRequired(Player player){ return hasProperItems(player); }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem){
        ItemStack off = player.getInventory().getItemInOffHand();
        return isSword(droppedItem) && off.getType() == Material.IRON_CHAIN;
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
                Qskill.cleanup(player);
                Fskill.cleanup(player);
                charge.clear(player);
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
            public long getLcooldown(Player player) {
                return 0L;
            }

            @Override
            public long getRcooldown(Player player) { return config.R_COOLDOWN.getOrDefault(player.getUniqueId(), config.r_Skill_Cool); }
            @Override
            public long getQcooldown(Player player) { return config.Q_COOLDOWN.getOrDefault(player.getUniqueId(), config.q_Skill_Cool); }
            @Override
            public long getFcooldown(Player player) { return config.F_COOLDOWN.getOrDefault(player.getUniqueId(), config.f_Skill_Cool); }
        };
    }
}