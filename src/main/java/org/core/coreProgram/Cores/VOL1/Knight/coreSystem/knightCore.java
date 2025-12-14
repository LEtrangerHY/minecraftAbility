package org.core.coreProgram.Cores.VOL1.Knight.coreSystem;

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
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Main.Core;
import org.core.Effect.ForceDamage;
import org.core.Main.coreConfig;
import org.core.coreProgram.AbsCoreSystem.ConfigWrapper;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.AbsCoreSystem.absCore;
import org.core.coreProgram.Cores.VOL1.Knight.Skill.F;
import org.core.coreProgram.Cores.VOL1.Knight.Skill.Q;
import org.core.coreProgram.Cores.VOL1.Knight.Skill.R;

public class knightCore extends absCore {
    private final Core plugin;
    private final Knight config;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;


    public knightCore(Core plugin, coreConfig tag, Knight config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

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
        long addHP =
                player.getPersistentDataContainer().getOrDefault(
                        new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L)
                        +
                        player.getPersistentDataContainer().getOrDefault(
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

    @EventHandler
    public void focusCancel(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        LivingEntity attacker = null;
        Entity damager = event.getDamager();

        if (damager instanceof LivingEntity) {
            attacker = (LivingEntity) damager;
        } else if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof LivingEntity shooter) {
                attacker = shooter;
            }
        }

        if (attacker == null) return;

        if (tag.Knight.contains(player)) {
            World world = player.getWorld();

            if (config.isFocusing.getOrDefault(player.getUniqueId(), false)) {
                event.setCancelled(true);

                if (!config.isFocusCancel.getOrDefault(player.getUniqueId(), false)) {

                    Location playerLoc = player.getLocation();
                    Vector dirToAttacker = attacker.getLocation().toVector()
                            .subtract(player.getLocation().toVector())
                            .normalize();
                    playerLoc.setDirection(dirToAttacker);
                    player.teleport(playerLoc);

                    Vector knockback = dirToAttacker.multiply(1.4);
                    damager.setVelocity(knockback);

                    PotionEffect glowing = new PotionEffect(PotionEffectType.GLOWING, 20 * 3, 1, false, true);
                    ((LivingEntity) attacker).addPotionEffect(glowing);

                    world.spawnParticle(Particle.ENCHANTED_HIT, damager.getLocation().clone().add(0, 1, 0), 21, 0.4, 0.4, 0.4, 1);
                    world.spawnParticle(Particle.SWEEP_ATTACK, damager.getLocation().clone().add(0, 1, 0), 1, 0., 0, 0, 0);

                    world.playSound(playerLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
                    world.playSound(playerLoc, Sound.ENTITY_IRON_GOLEM_REPAIR, 1, 1);

                    config.isFocusCancel.put(player.getUniqueId(), true);
                }
            }
        }
    }

    @EventHandler
    public void respawnHealthSet(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return;

        double originalMax = 20.0;
        maxHealth.setBaseValue(originalMax);

        player.setHealth(originalMax);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void passiveAttackEffect(PlayerInteractEvent event) {

        if(tag.Knight.contains(event.getPlayer())) {

            if (!pAttackUsing.contains(event.getPlayer().getUniqueId()) && !config.q_Skill_Using.getOrDefault(event.getPlayer().getUniqueId(), false)) {

                Player player = event.getPlayer();

                if (hasProperItems(player)) {
                    if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {

                        if (cool.isReloading(player, "cutting")) {
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 1, 1);
                            return;
                        }

                        cool.setCooldown(player, 625L, "cutting");

                        World world = player.getWorld();
                        Location playerLocation = player.getLocation();
                        Vector direction = playerLocation.getDirection().normalize().multiply(1.3);

                        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

                        config.collision.put(player.getUniqueId(), false);

                        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                                .withCausingEntity(player)
                                .withDirectEntity(player)
                                .build();

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

                                        ForceDamage forceDamage = new ForceDamage(target, 3.0, source);
                                        forceDamage.applyEffect(player);

                                        config.collision.put(player.getUniqueId(), true);
                                        break;
                                    }
                                }

                                ticks++;
                            }
                        }.runTaskTimer(plugin, 0L, 1L);

                        ItemStack offHand = player.getInventory().getItemInOffHand();
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

                        event.setCancelled(true);
                    }
                }
            } else {
                pAttackUsing.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Knight.contains(player);
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
        return main.getType() == Material.NETHERITE_SWORD && off.getType() == Material.DIAMOND_SWORD;
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
        return droppedItem.getType() == Material.NETHERITE_SWORD &&
                off.getType() == Material.DIAMOND_SWORD;
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
