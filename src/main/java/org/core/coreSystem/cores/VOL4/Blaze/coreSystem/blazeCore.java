package org.core.coreSystem.cores.VOL4.Blaze.coreSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
import org.core.coreSystem.cores.VOL4.Blaze.Passive.BlueFlame;
import org.core.coreSystem.cores.VOL4.Blaze.Skill.F;
import org.core.coreSystem.cores.VOL4.Blaze.Skill.Q;
import org.core.coreSystem.cores.VOL4.Blaze.Skill.R;

import java.util.HashSet;

public class blazeCore extends absCore {
    private final Core plugin;
    private final Blaze config;

    public final BlueFlame blueFlame;

    private static final int CHARGE_TICKS = 4;
    private static final BlockData SOUL_SOIL = Material.SOUL_SOIL.createBlockData();
    private static final Particle.DustTransition CHARGE_DUST = new Particle.DustTransition(Color.fromRGB(120, 230, 255), Color.fromRGB(20, 60, 140), 1.0f);
    private static final Particle.DustTransition CORE_DUST = new Particle.DustTransition(Color.fromRGB(200, 250, 255), Color.fromRGB(60, 170, 255), 1.3f);
    private static final Particle.DustTransition HIT_DUST = new Particle.DustTransition(Color.fromRGB(90, 200, 255), Color.fromRGB(15, 25, 60), 0.9f);

    private final NamespacedKey meteorKey;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public blazeCore(Core plugin, coreConfig tag, Blaze config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.blueFlame = new BlueFlame();
        this.meteorKey = new NamespacedKey(plugin, "blazeMeteor");

        this.Rskill = new R(config, plugin, cool, blueFlame);
        this.Qskill = new Q(config, plugin, cool, blueFlame);
        this.Fskill = new F(config, plugin, cool, blueFlame);

        plugin.getLogger().info("Blaze downloaded...");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        if (!contains(event.getPlayer())) return;

        Player player = event.getPlayer();
        applyAdditionalHealth(player, false);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!contains(event.getPlayer())) return;

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            applyAdditionalHealth(player, true);
        }, 1L);
    }

    private void applyAdditionalHealth(Player player, boolean healFull) {
        long addHP = player.getPersistentDataContainer().getOrDefault(
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
                    event.getCause() == EntityDamageEvent.DamageCause.LAVA) {

                event.setCancelled(true);
                player.setFireTicks(0);
            }
        }
    }

    @EventHandler
    public void meteorSelfGuard(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof Fireball fireball)) return;
        if (fireball.getShooter() != player) return;
        if (!fireball.getPersistentDataContainer().has(meteorKey, PersistentDataType.BYTE)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void meteorImpact(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Fireball fireball)) return;
        if (!fireball.getPersistentDataContainer().has(meteorKey, PersistentDataType.BYTE)) return;

        World world = fireball.getWorld();
        Location loc = fireball.getLocation();
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 40, 0.3, 0.3, 0.3, 0.25);
        world.spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, 25, 0.8, 0.5, 0.8, 0, CORE_DUST);
        world.spawnParticle(Particle.SOUL, loc, 8, 0.6, 0.3, 0.6, 0.05);
        world.spawnParticle(Particle.BLOCK, loc, 20, 0.6, 0.2, 0.6, 0, SOUL_SOIL);
        world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 1.4f);
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

    @Override
    protected boolean contains(Player player) {
        return tag.Blaze.contains(player);
    }

    @Override
    protected boolean isCustomAttackUser(Player player) {
        return true;
    }

    @Override
    protected void onLSkillCooldown(PlayerInteractEvent event, Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
    }

    @Override
    protected void LSkill(PlayerInteractEvent event, Player player) {
        event.setCancelled(true);

        boolean isBurst = config.BurstBlaze.getOrDefault(player.getUniqueId(), false);
        double damage = 0.13;

        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);

        if (isBurst) {
            double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
            damage = 0.13 * (1 + amp);
            if (attackSpeed != null) attackSpeed.setBaseValue(1 / 1.3);
        } else {
            if (attackSpeed != null) attackSpeed.setBaseValue(1 / 2.6);
        }

        double finalDamage = damage;

        if (isBurst) {
            chargeBurst(player, finalDamage);
        } else {
            releaseBlaze(player, false, finalDamage);
        }
    }

    private void chargeBurst(Player player, double damage) {
        World world = player.getWorld();

        world.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.6f);
        world.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.2f, 0.6f);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !hasProperItems(player) || !tag.Blaze.contains(player)) {
                    cancel();
                    return;
                }

                if (tick >= CHARGE_TICKS) {
                    cancel();
                    releaseBlaze(player, true, damage);
                    return;
                }

                Location center = player.getLocation().add(0, 1.0, 0);
                double progress = (double) tick / CHARGE_TICKS;
                double radius = 2.6 - 1.8 * progress;
                int points = 18;
                double spin = tick * 0.35;

                for (int i = 0; i < points; i++) {
                    double theta = spin + Math.PI * 2 * i / points;
                    double y = Math.sin(theta * 2 + tick) * 0.6;
                    Vector offset = new Vector(Math.cos(theta) * radius, y, Math.sin(theta) * radius);
                    Location p = center.clone().add(offset);
                    Vector inward = offset.clone().multiply(-1).normalize();

                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 0,
                            inward.getX(), inward.getY(), inward.getZ(), 0.12 + 0.1 * progress);
                    if (i % 3 == 0) {
                        world.spawnParticle(Particle.DUST_COLOR_TRANSITION, p, 1, 0.05, 0.05, 0.05, 0, CHARGE_DUST);
                    }
                }

                world.spawnParticle(Particle.SOUL, center, 2, 0.15, 0.2, 0.15, 0.01);
                world.spawnParticle(Particle.DUST_COLOR_TRANSITION, center, 4 + tick * 2, 0.1 + 0.05 * tick, 0.1 + 0.05 * tick, 0.1 + 0.05 * tick, 0, CORE_DUST);

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void releaseBlaze(Player player, boolean isBurst, double damage) {
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

        if (isBurst) {
            world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.4f);
            world.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 1.2f);

            Location base = player.getLocation();
            for (double y = 0; y <= 3.5; y += 0.25) {
                for (int i = 0; i < 4; i++) {
                    double theta = Math.random() * Math.PI * 2;
                    double r = 0.15 + Math.random() * 0.35;
                    Location p = base.clone().add(Math.cos(theta) * r, y, Math.sin(theta) * r);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 0, 0, 1, 0, 0.12 + Math.random() * 0.1);
                }
            }
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, base.clone().add(0, 0.6, 0), 44, 0.1, 0.1, 0.1, 0.8);
            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, base.clone().add(0, 1, 0), 30, 0.6, 0.6, 0.6, 0, CORE_DUST);

            for (Entity entity : world.getNearbyEntities(player.getLocation(), 13, 13, 13)) {
                if (entity instanceof LivingEntity target && entity != player) {

                    if (Math.random() < 0.4) {
                        Burn burn = new Burn(target, 2000L);
                        burn.applyEffect(player);
                        PotionEffect wither = new PotionEffect(PotionEffectType.WITHER, 40, 3, false, false);
                        target.addPotionEffect(wither);
                    }

                    world.spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0), 4, 0.2, 0.4, 0.2, 0.05);
                }
            }
        } else {
            world.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.8f);

            Location muzzle = origin.clone().add(forward.clone().multiply(0.8));
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, muzzle, 10, 0.05, 0.05, 0.05, 0.08);
            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, muzzle, 6, 0.1, 0.1, 0.1, 0, CORE_DUST);
        }

        Vector finalForward = forward;
        double groundY = player.getLocation().getY() + 0.1;

        new BukkitRunnable() {
            double distance = 1.0;

            @Override
            public void run() {
                if (distance > maxDistance || !player.isOnline()) {
                    config.damaged.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                double t = distance / maxDistance;
                double visualStep = Math.toDegrees(0.45 / distance);
                if (isBurst) visualStep = Math.max(visualStep, 4.0);

                for (double angle = -coneAngle / 2; angle <= coneAngle / 2; angle += visualStep) {
                    Vector dir = finalForward.clone().rotateAroundY(Math.toRadians(angle));
                    Location p = origin.clone().add(dir.clone().multiply(distance));

                    double speed = 0.3 * (1 - t) + 0.05;
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 0,
                            dir.getX(), dir.getY() + 0.03, dir.getZ(), speed);

                    if (Math.random() < 0.15) {
                        world.spawnParticle(Particle.SOUL, p, 0, 0, 1, 0, 0.04);
                    }
                    if (t > 0.7 && Math.random() < 0.4) {
                        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p, 0, dir.getX(), 0.2, dir.getZ(), 0.03);
                    }

                    if (isBurst) {
                        Location ground = p.clone();
                        ground.setY(groundY);
                        world.spawnParticle(Particle.BLOCK, ground, 2, 0.1, 0, 0.1, 0, SOUL_SOIL);
                        if (Math.random() < 0.2) {
                            world.spawnParticle(Particle.WHITE_ASH, ground.clone().add(0, 1.5, 0), 2, 0.3, 0.5, 0.3, 0);
                        }
                    }
                }

                for (double angle = -coneAngle / 2; angle <= coneAngle / 2; angle += angleStep) {
                    Vector dir = finalForward.clone().rotateAroundY(Math.toRadians(angle));
                    Location particleLoc = origin.clone().add(dir.multiply(distance));

                    double dist = isBurst ? 1.0 : 0.6;

                    for (Entity entity : world.getNearbyEntities(particleLoc, dist, dist, dist)) {
                        if (entity instanceof LivingEntity target && entity != player) {

                            config.damaged.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(entity);

                            ForceDamage forceDamage = new ForceDamage(target, damage, source, false);
                            forceDamage.applyEffect(player);

                            double per = isBurst ? 1.0 : 0.4;
                            long burnTime = isBurst ? 6000L : 4000L;

                            if (Math.random() < per) {
                                Burn burn = new Burn(target, burnTime);
                                burn.applyEffect(player);
                                PotionEffect wither = new PotionEffect(PotionEffectType.WITHER, (int) (20 * (burnTime / 1000)), 3, false, false);
                                target.addPotionEffect(wither);
                            }

                            Location hit = target.getLocation().add(0, target.getHeight() / 2, 0);
                            world.spawnParticle(Particle.SMOKE, hit, 2, 0.2, 0.4, 0.2, 0.03);
                            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, hit, 3, 0.25, 0.4, 0.25, 0, HIT_DUST);
                            if (Math.random() < 0.3) {
                                world.spawnParticle(Particle.SOUL, hit, 1, 0.2, 0.3, 0.2, 0.02);
                            }
                        }
                    }

                    if (isBurst) {
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

    @Override
    protected boolean isItemRequired(Player player) {
        if (hasProperItems(player)) {
            return true;
        } else {
            AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
            if (attackSpeed != null) attackSpeed.setBaseValue(4.0);
            return false;
        }
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem) {
        ItemStack off = player.getInventory().getItemInOffHand();
        return droppedItem.getType() == Material.SOUL_TORCH &&
                (off.getType() == Material.SOUL_SAND || off.getType() == Material.SOUL_SOIL || off.getType() == Material.SOUL_LANTERN);
    }

    @Override
    protected boolean isRCondition(Player player) { return true; }

    @Override
    protected boolean isQCondition(Player player) { return true; }

    @Override
    protected boolean isFCondition(Player player) { return true; }

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
            public long getLcooldown(Player player) {
                boolean isBurst = config.BurstBlaze.getOrDefault(player.getUniqueId(), false);
                return isBurst ? 1300L : 2600L;
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