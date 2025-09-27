package org.core.coreProgram.Cores.Pyro.coreSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Core;
import org.core.Debuff.Burn;
import org.core.Effect.ForceDamage;
import org.core.coreConfig;
import org.core.coreProgram.AbsCoreSystem.ConfigWrapper;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.AbsCoreSystem.absCore;
import org.core.coreProgram.Cores.Pyro.Passive.Causalgia;
import org.core.coreProgram.Cores.Pyro.Skill.F;
import org.core.coreProgram.Cores.Pyro.Skill.Q;
import org.core.coreProgram.Cores.Pyro.Skill.R;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getPlayer;


public class pyroCore extends absCore {
    private final Core plugin;
    private final Pyro config;

    public final Causalgia causalgia;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;


    public pyroCore(Core plugin, coreConfig tag, Pyro config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.causalgia = new Causalgia(tag, config, plugin, cool);

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

        getLogger().info("Pyro downloaded...");
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
                new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L)
                + player.getPersistentDataContainer().getOrDefault(
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

        if(tag.Pyro.contains(event.getPlayer())) {
            if (!skillUsing.contains(event.getPlayer().getUniqueId())) {

                Player player = event.getPlayer();

                if (hasProperItems(player)) {
                    if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {

                        if (cool.isReloading(player, "flame")) {
                            player.playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
                            return;
                        }

                        cool.setCooldown(player, 2000L, "flame");

                        World world = player.getWorld();
                        Location playerLocation = player.getLocation();
                        Vector direction = playerLocation.getDirection().normalize().multiply(1.3);

                        player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(0.5);
                        player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1, 1);

                        config.collision.put(player.getUniqueId(), false);

                        new BukkitRunnable() {
                            int ticks = 0;

                            @Override
                            public void run() {

                                if (ticks >= 25 || config.collision.getOrDefault(player.getUniqueId(), true)) {
                                    config.collision.remove(player.getUniqueId());
                                    this.cancel();
                                    return;
                                }

                                Location particleLocation = playerLocation.clone()
                                        .add(direction.clone().multiply(ticks * 1.5))
                                        .add(0, 1.4, 0);

                                player.spawnParticle(Particle.FLAME, particleLocation, 3, 0.1, 0.1, 0.1, 0);
                                player.spawnParticle(Particle.SMOKE, particleLocation, 2, 0.1, 0.1, 0.1, 0);

                                Block block = particleLocation.getBlock();

                                if (block.isBurnable() || block.getType() == Material.ICE || block.getType() == Material.SNOW || block.getType() == Material.BLUE_ICE || block.getType() == Material.FROSTED_ICE || block.getType() == Material.PACKED_ICE || block.getType() == Material.POWDER_SNOW || block.getType() == Material.SNOW_BLOCK) {
                                    if(block.getType() == Material.BLUE_ICE) {
                                        if(Math.random() < 0.06) {
                                            block.setType(Material.FIRE);
                                        }
                                    }else{
                                        block.setType(Material.FIRE);
                                    }
                                    if(Math.random() < 0.2) {
                                        block.getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_BURN, 1, 1);
                                    }
                                }


                                if(!block.isPassable()){
                                    Burst(player, particleLocation);
                                    config.collision.put(player.getUniqueId(), true);
                                }

                                for (Entity entity : world.getNearbyEntities(particleLocation, 0.5, 0.5, 0.5)) {
                                    if (entity instanceof LivingEntity target && entity != player) {
                                        ForceDamage forceDamage = new ForceDamage(target, 9);
                                        forceDamage.applyEffect(player);
                                        Burst(player, particleLocation);
                                        config.collision.put(player.getUniqueId(), true);
                                        break;
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
                skillUsing.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    public void Burst(Player player, Location burstLoction){

        World world = player.getWorld();

        player.playSound(player.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1, 1);
        player.spawnParticle(Particle.FLAME, burstLoction, 21, 0.1, 0.1, 0.1, 0.9);
        player.spawnParticle(Particle.SOUL_FIRE_FLAME, burstLoction, 14, 0.1, 0.1, 0.1, 0.9);

        for (Entity entity : world.getNearbyEntities(burstLoction, 3, 3, 3)) {
            if (entity instanceof LivingEntity target && entity != player) {

                if (Math.random() < 0.3) {
                    Burn burn = new Burn(target, 7000L);
                    burn.applyEffect(player);
                }

                ForceDamage forceDamage = new ForceDamage(target, 4);
                forceDamage.applyEffect(player);

                Vector direction = entity.getLocation().toVector().subtract(burstLoction.toVector()).normalize().multiply(0.5);
                direction.setY(0.5);

            }
        }
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Pyro.contains(player);
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
        return main.getType() == Material.BLAZE_ROD && off.getType() == Material.BLAZE_POWDER;
    }

    private boolean canUseRSkill(Player player) { return true; }

    private boolean canUseQSkill(Player player) { return true; }

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
        return droppedItem.getType() == Material.BLAZE_ROD &&
                off.getType() == Material.BLAZE_POWDER &&
                canUseQSkill(player);
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