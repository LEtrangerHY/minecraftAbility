package org.core.coreSystem.cores.VOL1.Harvester.coreSystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.main.Core;
import org.core.main.coreConfig;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL1.Harvester.Passive.Bountiful;
import org.core.coreSystem.cores.VOL1.Harvester.Skill.F;
import org.core.coreSystem.cores.VOL1.Harvester.Skill.Q;
import org.core.coreSystem.cores.VOL1.Harvester.Skill.R;

public class harvCore extends absCore {

    private final Core plugin;
    private final Harvester config;

    private final Bountiful bountiful;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public harvCore(Core plugin, coreConfig tag, Harvester config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.bountiful = new Bountiful(tag, config, plugin, cool);

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

        plugin.getLogger().info("Harvester downloaded...");
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
                new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);

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
        if(tag.Harvester.contains(event.getPlayer())){
            if (pAttackUsing.contains(event.getPlayer().getUniqueId())) {
                pAttackUsing.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler
    public void passiveDamage(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        if(tag.Harvester.contains(player) && hasProperItems(player)){
            if(!config.rskill_using.getOrDefault(player.getUniqueId(), false) && !config.fskill_using.getOrDefault(player.getUniqueId(), false)) {

                Vector direction = player.getEyeLocation().add(0, -0.5, 0).getDirection().normalize();
                Location particleLocation = player.getEyeLocation().clone()
                        .add(direction.clone().multiply(2.6));

                player.spawnParticle(Particle.SWEEP_ATTACK, particleLocation, 1, 0, 0, 0, 0);

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
                event.setDamage(4.0);

            }
        }
    }

    @EventHandler
    public void invisibleInBush(PlayerMoveEvent event){
        Player player = event.getPlayer();

        if(contains(player)) {
            if(player.isInvisible()) player.sendActionBar(Component.text("Invisible").color(NamedTextColor.DARK_GREEN));
            player.setInvisible(bountiful.bushCheck(player));
        }

    }

    @Override
    protected boolean contains(Player player) {
        return tag.Harvester.contains(player);
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
        return main.getType() == Material.IRON_HOE && off.getType() == Material.AIR;
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
        return droppedItem.getType() == Material.IRON_HOE &&
                off.getType() == Material.AIR;
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
