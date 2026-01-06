package org.core.coreSystem.cores.VOL1.Blue.coreSystem;

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
import org.bukkit.event.entity.EntityDamageEvent;
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
import org.core.coreSystem.cores.VOL1.Blue.Skill.F;
import org.core.coreSystem.cores.VOL1.Blue.Skill.Q;
import org.core.coreSystem.cores.VOL1.Blue.Skill.R;

public class blueCore extends absCore {

    private final Core plugin;
    private final Blue config;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public blueCore(Core plugin, coreConfig tag, Blue config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

        plugin.getLogger().info("Blue downloaded...");
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

    @EventHandler
    public void passiveDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player player)) return;

        if (tag.Blue.contains(player)) {

            if (event.getCause() == EntityDamageEvent.DamageCause.WITHER) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void qSkillAbsorb(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        if(tag.Blue.contains(player) && hasProperItems(player)){
            if(config.qSoulAbsorb.getOrDefault(player.getUniqueId(), false)) {

                World world = player.getWorld();

                world.spawnParticle(Particle.SOUL, target.getLocation().clone().add(0, 1.3, 0), 1, 0.4, 0.4, 0.4, 0);
                world.spawnParticle(Particle.SOUL, player.getLocation().clone().add(0, 1.3, 0), 1, 0.4, 0.4, 0.4, 0);

                player.heal(0.6);

            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void passiveAttackEffect(PlayerInteractEvent event) {

        if(tag.Blue.contains(event.getPlayer()) && hasProperItems(event.getPlayer())) {
            if (!pAttackUsing.contains(event.getPlayer().getUniqueId())) {

                Player player = event.getPlayer();

                if (hasProperItems(player)) {
                    if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {

                        if (cool.isReloading(player, "soul")) {
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.3f, 1.0f);
                            return;
                        }

                        cool.setCooldown(player, 1300L, "soul");

                        World world = player.getWorld();
                        Location playerLocation = player.getLocation();
                        Vector direction = playerLocation.getDirection().normalize().multiply(1.3);

                        player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(1/1.3);
                        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.3f, 1.0f);

                        Particle.DustOptions dustOption_flowerDust = new Particle.DustOptions(Color.AQUA, 0.6f);
                        Particle.DustOptions dustOption_flowerDust_gra = new Particle.DustOptions(Color.NAVY, 0.6f);

                        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                                .withCausingEntity(player)
                                .withDirectEntity(player)
                                .build();

                        if(config.qSoulAbsorb.getOrDefault(player.getUniqueId(), false)){

                            world.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1.0, 0), 66, 0.4, 0.4, 0.4, 0.64);

                            if (Math.random() < 0.6) {
                                world.spawnParticle(Particle.DUST, player.getLocation().add(0, 1.0, 0), 66, 0.4, 0.6, 0.4, 0.64, dustOption_flowerDust_gra);
                            }else{
                                world.spawnParticle(Particle.DUST, player.getLocation().add(0, 1.0, 0), 66, 0.4, 0.6, 0.4, 0.64, dustOption_flowerDust);
                            }

                            for (Entity entity : world.getNearbyEntities(player.getLocation(), 6.0, 4, 6.0)) {
                                if (entity instanceof LivingEntity target && entity != player) {
                                    ForceDamage forceDamage = new ForceDamage(target, 2.6, source);
                                    forceDamage.applyEffect(player);
                                }
                            }
                        }

                        new BukkitRunnable() {
                            int ticks = 0;

                            @Override
                            public void run() {

                                if (ticks >= 13) {
                                    this.cancel();
                                    return;
                                }

                                Location particleLocation = playerLocation.clone()
                                        .add(direction.clone().multiply(ticks * 1.3))
                                        .add(0, 1.4, 0);

                                world.spawnParticle(Particle.SOUL, particleLocation, 2, 0.4, 0.4, 0.4, 0);
                                world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLocation, 4, 0.4, 0.4, 0.4, 0);

                                for (Entity entity : world.getNearbyEntities(particleLocation, 1.3, 1.3, 1.3)) {
                                    if (entity instanceof LivingEntity target && entity != player) {

                                        ForceDamage forceDamage = new ForceDamage(target, 3.0, source);
                                        forceDamage.applyEffect(player);
                                    }
                                }

                                ticks++;
                            }
                        }.runTaskTimer(plugin, 0L, 1L);

                        event.setCancelled(true);
                    }
                } else {
                    player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(4.0);
                }
            } else {
                pAttackUsing.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Blue.contains(player);
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
        return main.getType() == Material.SOUL_LANTERN && off.getType() == Material.WITHER_ROSE;
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
        return droppedItem.getType() == Material.SOUL_LANTERN &&
                off.getType() == Material.WITHER_ROSE;
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