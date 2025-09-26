package org.core.coreProgram.Cores.Carpenter.coreSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Core;
import org.core.coreConfig;
import org.core.coreProgram.AbsCoreSystem.ConfigWrapper;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.AbsCoreSystem.absCore;
import org.core.coreProgram.Cores.Carpenter.Passive.Christmas;
import org.core.coreProgram.Cores.Carpenter.Skill.F;
import org.core.coreProgram.Cores.Carpenter.Skill.Q;
import org.core.coreProgram.Cores.Carpenter.Skill.R;

import static org.bukkit.Bukkit.getLogger;

public class carpCore extends absCore {
    private final Core plugin;
    private final Carpenter config;

    private final Christmas torque;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public carpCore(Core plugin, coreConfig tag, Carpenter config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.torque = new Christmas(tag, config, plugin, cool);

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

        getLogger().info("Carpenter downloaded...");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyAdditionalHealth(player, false);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            applyAdditionalHealth(player, true);
        }, 1L);
    }

    private void applyAdditionalHealth(Player player, boolean healFull) {
        long addHP =
                player.getPersistentDataContainer().getOrDefault(
                        new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L) * 3;

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
        if(tag.Carpenter.contains(event.getPlayer())){
            if (skillUsing.contains(event.getPlayer().getUniqueId())) {
                skillUsing.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler
    public void passiveDamage(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        ItemStack itemInOffHand = player.getInventory().getItemInOffHand();

        if(tag.Carpenter.contains(player) && hasProperItems(player)){
            if(!config.r_damaging.getOrDefault(player.getUniqueId(), false)) {

                Vector direction = player.getEyeLocation().add(0, -0.5, 0).getDirection().normalize();
                Location particleLocation = player.getEyeLocation().clone()
                        .add(direction.clone().multiply(2.6));

                player.spawnParticle(Particle.EXPLOSION, particleLocation, 1, 0, 0, 0, 0);

                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1, 1);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1, 1);
                event.setDamage(3.0);

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
    protected boolean isQCondition(Player player, ItemStack droppedItem) {
        ItemStack off = player.getInventory().getItemInOffHand();
        return (droppedItem.getType() == Material.TRIPWIRE_HOOK &&
                off.getType() == Material.AIR &&
                canUseQSkill(player)) || (droppedItem.getType() == Material.TRIPWIRE_HOOK &&
                off.getType() == Material.TOTEM_OF_UNDYING &&
                canUseQSkill(player));
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
