package org.core.coreSystem.cores.VOL2.Burst.coreSystem;

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
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.main.Core;
import org.core.main.coreConfig;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL2.Burst.Skill.F;
import org.core.coreSystem.cores.VOL2.Burst.Skill.Q;
import org.core.coreSystem.cores.VOL2.Burst.Skill.R;

public class burstCore extends absCore {
    private final Core plugin;
    private final Burst config;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public burstCore(Core plugin, coreConfig tag, Burst config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

        plugin.getLogger().info("Burst downloaded...");
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void passiveAttackEffect(PlayerInteractEvent event) {

        if(tag.Burst.contains(event.getPlayer())) {
            if (!pAttackUsing.contains(event.getPlayer().getUniqueId())) {

                Player player = event.getPlayer();

                if (hasProperItems(player)) {
                    if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {

                        if (cool.isReloading(player, "burst")) {
                            player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1, 1);
                            return;
                        }

                        cool.setCooldown(player, 2000L, "burst");

                        World world = player.getWorld();

                        DamageSource selfSource = DamageSource.builder(DamageType.EXPLOSION)
                                .build();

                        player.damage(1.4, selfSource);
                        player.setVelocity(new Vector(0, 0, 0));

                        world.playSound(player.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2, 1);
                        world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 1);
                        world.spawnParticle(Particle.FLAME, player.getLocation().clone().add(0, 0.6, 0), 67, 0.1, 0.1, 0.1, 0.4);
                        world.spawnParticle(Particle.SMOKE, player.getLocation().clone().add(0, 0.6, 0), 24, 0.1, 0.1, 0.1, 0.8);

                        Location center = player.getLocation();

                        DamageSource source = DamageSource.builder(DamageType.PLAYER_EXPLOSION)
                                .withCausingEntity(player)
                                .withDirectEntity(player)
                                .build();

                        for (Entity entity : world.getNearbyEntities(center, 6, 6, 6)) {
                            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;


                            ForceDamage forceDamage = new ForceDamage((LivingEntity) entity, 6.0, source);
                            forceDamage.applyEffect(player);

                            Vector direction = entity.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.8);
                            direction.setY(0.4);

                            entity.setVelocity(direction);
                        }

                        for (Entity entity : world.getNearbyEntities(center, 3, 3, 3)) {
                            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

                            world.spawnParticle(Particle.EXPLOSION, entity.getLocation().clone().add(0, 1, 0), 1, 0, 0, 0, 0);

                            ForceDamage forceDamage = new ForceDamage((LivingEntity) entity, 7.0, source);
                            forceDamage.applyEffect(player);

                            Vector direction = entity.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.3);
                            direction.setY(0.6);

                            entity.setVelocity(direction);
                        }

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
        return tag.Burst.contains(player);
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
        return main.getType() == Material.REDSTONE && off.getType() == Material.AIR;
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
        return droppedItem.getType() == Material.REDSTONE && off.getType() == Material.AIR;
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
