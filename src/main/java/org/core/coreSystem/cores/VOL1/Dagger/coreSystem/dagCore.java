package org.core.coreSystem.cores.VOL1.Dagger.coreSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.main.Core;
import org.core.effect.crowdControl.ForceDamage;
import org.core.main.coreConfig;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL1.Dagger.Passive.bloodStroke;
import org.core.coreSystem.cores.VOL1.Dagger.Skill.F;
import org.core.coreSystem.cores.VOL1.Dagger.Skill.Q;
import org.core.coreSystem.cores.VOL1.Dagger.Skill.R;

import java.util.HashSet;
import java.util.LinkedHashSet;


public class dagCore extends absCore {
    private final Core plugin;
    private final Dagger config;

    private final bloodStroke bloodStroker;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public dagCore(Core plugin, coreConfig tag, Dagger config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.bloodStroker = new bloodStroke(tag, config, plugin, cool);

        this.Rskill = new R(config, plugin, cool, bloodStroker);
        this.Qskill = new Q(config, plugin, cool, bloodStroker);
        this.Fskill = new F(config, plugin, cool, bloodStroker);

        plugin.getLogger().info("Dagger downloaded...");
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
        long addHP = 0;

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
        if(tag.Dagger.contains(event.getPlayer())){
            if (pAttackUsing.contains(event.getPlayer().getUniqueId())) {
                pAttackUsing.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler
    public void passiveDamage(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        if(tag.Dagger.contains(player) && hasProperItems(player)){
            if(!config.r_damaged.getOrDefault(player.getUniqueId(), false) && !config.f_using.getOrDefault(player.getUniqueId(), false) && !config.f_damaging.getOrDefault(player.getUniqueId(), false)) {

                Vector direction = player.getEyeLocation().add(0, -0.5, 0).getDirection().normalize();
                Location particleLocation = player.getEyeLocation().clone()
                        .add(direction.clone().multiply(2.6));

                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, particleLocation, 1, 0, 0, 0, 0);

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
                event.setDamage(4.0);

            }
        }
    }

    @EventHandler
    public void fskillEffect(PlayerInteractEvent event){

        Player player = event.getPlayer();
        Action action = event.getAction();

        if(!tag.Dagger.contains(player)) return;

        if(!cool.isReloading(player, "X_slash") && config.f_using.getOrDefault(player.getUniqueId(), false) && config.f_slash.getOrDefault(player.getUniqueId(), 0) < 2) {

            World world = player.getWorld();

            Location playerLocation = player.getLocation();
            Vector direction = playerLocation.getDirection().normalize().multiply(1.3);

            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {

                if(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK){
                    return;
                }

                if(!hasProperItems(player)){
                    return;
                }

                config.f_damaged.put(player.getUniqueId(), new HashSet<>());

                DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                        .withCausingEntity(player)
                        .withDirectEntity(player)
                        .build();

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
                event.setCancelled(true);

                new BukkitRunnable() {
                    int ticks = 0;

                    @Override
                    public void run() {
                        if (ticks >= 10 || !config.f_using.getOrDefault(player.getUniqueId(), false)) {
                            config.f_slash.put(player.getUniqueId(), config.f_slash.getOrDefault(player.getUniqueId(), 0) + 1);
                            this.cancel();
                            return;
                        }

                        Location particleLocation = playerLocation.clone()
                                .add(direction.clone().multiply(ticks * 1.8))
                                .add(0, 1.5, 0);

                        world.spawnParticle(Particle.SWEEP_ATTACK, particleLocation, 1, 0, 0, 0, 0);
                        world.spawnParticle(Particle.SMOKE, particleLocation, 1, 0, 0, 0, 0);

                        for (Entity entity : world.getNearbyEntities(particleLocation, 0.6, 0.6, 0.6)) {
                            if (entity instanceof LivingEntity target
                                    && entity != player
                                    && !config.f_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                                config.f_damaging.put(player.getUniqueId(), true);

                                ForceDamage forceDamage = new ForceDamage(target, config.f_Skill_Damage, source);
                                forceDamage.applyEffect(player);
                                target.setVelocity(new Vector(0, 0, 0));
                                config.f_damaging.remove(player.getUniqueId());

                                config.f_damaged.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(target);

                                config.dash_object.computeIfAbsent(player.getUniqueId(), k -> new LinkedHashSet<>()).add(target);
                            }
                        }

                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }

            cool.setCooldown(player, 400L, "X_slash");
        }
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Dagger.contains(player);
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
        return main.getType() == Material.ECHO_SHARD && off.getType() == Material.AIR;
    }

    private boolean canUseRSkill(Player player) { return true; }

    private boolean canUseQSkill(Player player) { return true; }

    private boolean canUseFSkill(Player player) { return true; }

    @Override
    protected boolean isItemRequired(Player player){
        return hasProperItems(player);
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem){
        ItemStack off = player.getInventory().getItemInOffHand();
        return droppedItem.getType() == Material.ECHO_SHARD &&
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