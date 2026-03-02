package org.core.coreSystem.cores.VOL1.Blaze.coreSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.main.Core;
import org.core.effect.debuff.Burn;
import org.core.effect.crowdControl.ForceDamage;
import org.core.main.coreConfig;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL1.Blaze.Passive.BlueFlame;
import org.core.coreSystem.cores.VOL1.Blaze.Skill.F;
import org.core.coreSystem.cores.VOL1.Blaze.Skill.Q;
import org.core.coreSystem.cores.VOL1.Blaze.Skill.R;

import java.util.HashSet;

public class blazeCore extends absCore {
    private final Core plugin;
    private final Blaze config;

    public final BlueFlame blueFlame;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public blazeCore(Core plugin, coreConfig tag, Blaze config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.blueFlame = new BlueFlame();

        this.Rskill = new R(config, plugin, cool, blueFlame);
        this.Qskill = new Q(config, plugin, cool, blueFlame);
        this.Fskill = new F(config, plugin, cool, blueFlame);

        plugin.getLogger().info("Blaze downloaded...");
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
                        new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);

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
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (tag.Blaze.contains(player)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                    event.getCause() == EntityDamageEvent.DamageCause.LAVA ||
                    event.getCause() == EntityDamageEvent.DamageCause.HOT_FLOOR) {

                event.setCancelled(true);
                player.setFireTicks(0);
            }
        }
    }

    @EventHandler
    public void projectileEvent(ProjectileHitEvent event) {
        Entity victim = event.getHitEntity();
        Projectile fireball = event.getEntity();

        if (!(fireball.getShooter() instanceof Player shooter)) {
            return;
        }

        if (fireball.getType() == EntityType.FIREBALL || tag.Blaze.contains(shooter)) {
            if (victim instanceof LivingEntity livingVictim) {
                if (Math.random() < 0.6) {
                    Burn burn = new Burn(livingVictim, 4000L);
                    burn.applyEffect(shooter);

                    PotionEffect wither = new PotionEffect(
                            PotionEffectType.WITHER,
                            20 * 4,
                            3,
                            false,
                            false
                    );
                    livingVictim.addPotionEffect(wither);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void passiveAttackEffect(PlayerInteractEvent event) {
        if (tag.Blaze.contains(event.getPlayer())) {
            if (!pAttackUsing.contains(event.getPlayer().getUniqueId())) {

                Player player = event.getPlayer();

                if (hasProperItems(player)) {
                    if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {

                        if (cool.isReloading(player, "blaze")) {
                            player.getWorld().playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
                            return;
                        }

                        boolean isBurst = config.BurstBlaze.getOrDefault(player.getUniqueId(), false);
                        double damage = 0.13;

                        if(isBurst){
                            double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
                            damage = 0.13 * (1 + amp);
                            player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(1 / 1.3);
                            cool.setCooldown(player, 1300L, "blaze");
                        } else {
                            player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(1 / 2.6);
                            cool.setCooldown(player, 2600L, "blaze");
                        }

                        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                                .withCausingEntity(player)
                                .withDirectEntity(player)
                                .build();

                        config.damaged.putIfAbsent(player.getUniqueId(), new HashSet<>());

                        World world = player.getWorld();
                        Location origin = player.getLocation().add(0, 1.3, 0);

                        Vector forward = origin.getDirection();
                        if (isBurst) {
                            forward.setY(0);
                            if (forward.lengthSquared() > 0.0001) {
                                forward.normalize();
                            } else {
                                float yaw = player.getLocation().getYaw();
                                forward = new Vector(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw))).normalize();
                            }
                        } else {
                            forward.normalize();
                        }

                        world.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1, 1);
                        world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1);

                        double maxDistance = isBurst ? 11.0 : 9.0;
                        double coneAngle = isBurst ? 360.0 : 60.0;
                        double angleStep = isBurst ? 20.0 : 10.0;

                        if(isBurst){
                            player.spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().clone().add(0, 0.6, 0), 44, 0.1, 0.1, 0.1, 0.8);
                            for (Entity entity : world.getNearbyEntities(player.getLocation(), 13, 13, 13)) {
                                if (entity instanceof LivingEntity target && entity != player) {

                                    if (Math.random() < 0.4) {
                                        Burn burn = new Burn(target, 2000L);
                                        burn.applyEffect(player);
                                        PotionEffect wither = new PotionEffect(PotionEffectType.WITHER, 40, 3, false, false);
                                        ((LivingEntity) target).addPotionEffect(wither);
                                    }

                                    world.spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0), 4, 0.2, 0.4, 0.2, 0.05);
                                }
                            }
                        }

                        double finalDamage = damage;

                        Vector finalForward = forward;
                        new BukkitRunnable() {
                            double distance = 1.0;

                            @Override
                            public void run() {
                                if (distance > maxDistance || !player.isOnline()) {
                                    config.damaged.remove(player.getUniqueId());
                                    cancel();
                                    return;
                                }

                                for (double angle = -coneAngle / 2; angle <= coneAngle / 2; angle += angleStep) {
                                    Vector dir = finalForward.clone().rotateAroundY(Math.toRadians(angle));
                                    Location particleLoc = origin.clone().add(dir.multiply(distance));

                                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 2, 0.2, 0.1, 0.2, 0.04);

                                    double dist = isBurst ? 1.0 : 0.6;

                                    for (Entity entity : world.getNearbyEntities(particleLoc, dist, dist, dist)) {
                                        if (entity instanceof LivingEntity target && entity != player) {

                                            config.damaged.get(player.getUniqueId()).add(entity);

                                            ForceDamage forceDamage = new ForceDamage(target, finalDamage, source, false);
                                            forceDamage.applyEffect(player);

                                            double per = isBurst ? 1.0 : 0.4;
                                            long burnTime = isBurst ? 6000L : 4000L;

                                            if (Math.random() < per) {
                                                Burn burn = new Burn(target, burnTime);
                                                burn.applyEffect(player);
                                                PotionEffect wither = new PotionEffect(PotionEffectType.WITHER, (int) (20 * (burnTime / 1000)), 3, false, false);
                                                ((LivingEntity) target).addPotionEffect(wither);
                                            }

                                            world.spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0), 4, 0.2, 0.4, 0.2, 0.05);
                                        }
                                    }

                                    if(isBurst) {
                                        for (int i = -3; i < 4; i++) {
                                            Block block = particleLoc.clone().add(0, i, 0).getBlock();
                                            Material type = block.getType();
                                            if (block.isBurnable() || type == Material.ICE || type == Material.SNOW || type == Material.BLUE_ICE || type == Material.FROSTED_ICE || type == Material.PACKED_ICE || type == Material.POWDER_SNOW || type == Material.SNOW_BLOCK ||
                                                    type == Material.WHEAT || type == Material.POTATOES || type == Material.CARROTS || type == Material.BEETROOTS) {

                                                if (block.getType() == Material.BLUE_ICE) {
                                                    if (Math.random() < 0.06) {
                                                        block.setType(Material.FIRE);
                                                    }
                                                } else {
                                                    block.setType(Material.FIRE);
                                                }
                                                if (Math.random() < 0.2) {
                                                    block.getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_BURN, 1, 1);
                                                }
                                            }
                                        }
                                    }
                                }

                                distance += 0.4;
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
        return tag.Blaze.contains(player);
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
        return main.getType() == Material.SOUL_TORCH && (off.getType() == Material.SOUL_SAND || off.getType() == Material.SOUL_SOIL || off.getType() == Material.SOUL_LANTERN);
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
        return droppedItem.getType() == Material.SOUL_TORCH &&
                (off.getType() == Material.SOUL_SAND || off.getType() == Material.SOUL_SOIL || off.getType() == Material.SOUL_LANTERN);
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