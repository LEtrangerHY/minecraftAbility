package org.core.coreSystem.cores.VOL4.Undead.Passive;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.coreSystem.cores.VOL4.Undead.coreSystem.Undead;
import org.core.effect.crowdControl.ForceDamage;
import org.core.models.coresObj.bulletModel;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class basicAttack {

    private static final Particle.DustOptions CORE = new Particle.DustOptions(Color.fromRGB(238, 240, 244), 1.0f);
    private static final Particle.DustOptions EDGE = new Particle.DustOptions(Color.fromRGB(198, 202, 210), 0.85f);
    private static final Particle.DustOptions INNER = new Particle.DustOptions(Color.fromRGB(152, 156, 164), 0.7f);
    private static final Particle.DustOptions HAZE = new Particle.DustOptions(Color.fromRGB(122, 126, 132), 0.5f);
    private static final Particle.DustOptions TRAIL = new Particle.DustOptions(Color.fromRGB(214, 218, 226), 0.45f);
    private static final Particle.DustOptions[] CELLS = {CORE, EDGE, INNER};
    private static final Particle.DustOptions BLADE = new Particle.DustOptions(Color.fromRGB(246, 248, 252), 0.75f);
    private static final Particle.DustOptions KEEN = new Particle.DustOptions(Color.fromRGB(212, 216, 224), 0.62f);
    private static final Particle.DustOptions BODY = new Particle.DustOptions(Color.fromRGB(158, 163, 172), 0.52f);
    private static final Particle.DustOptions MIST = new Particle.DustOptions(Color.fromRGB(118, 122, 130), 0.42f);

    private static final double[][] SECTION = {
            {0.0, 0.0, 0},
            {0.15, 0.1, 1}, {0.15, -0.1, 1},
            {0.3, 0.0, 1},
            {0.45, 0.0, 2}
    };

    private static final double[] SWING_KEYS = {0.0, 0.06, 0.24, 0.58, 0.9, 1.05, 1.0};
    private static final double[] QUICK_KEYS = {0.0, 0.14, 0.52, 0.9, 1.04, 1.0};
    private static final double[] HEAVY_KEYS = {0.0, 0.04, 0.14, 0.4, 0.78, 1.0, 1.04, 1.0};
    private static final double[] STRIKE_KEYS = {0.0, 0.05, 0.2, 0.56, 1.0, 1.05, 1.0};
    private static final double[] FLURRY_KEYS = {0.0, 0.42, 0.9, 1.0};
    private static final double[] FLURRY_TILT = {-18, -50, 50, 18};

    private static final Vector WORLD_UP = new Vector(0, 1, 0);

    private final Undead config;
    private final JavaPlugin plugin;
    private final handSwap swap;
    private final stickyBomb bomb;
    private final deepWound wound;

    public basicAttack(Undead config, JavaPlugin plugin, handSwap swap, stickyBomb bomb, deepWound wound) {
        this.config = config;
        this.plugin = plugin;
        this.swap = swap;
        this.bomb = bomb;
        this.wound = wound;
    }

    private record Basis(Location eye, Vector forward, Vector flat, Vector right, Vector up) {
    }

    private record Flurry(double reach, double sweep, double thick, double tolerance, Sound sound, float pitch,
                          boolean push, double knockback, double lift) {
    }

    public long cooldown(Player player) {
        if (swap.isFirearm(player)) return config.gun_Attack_Cool;

        return switch (player.getInventory().getItemInMainHand().getType()) {
            case IRON_AXE -> config.axe_Attack_Cool;
            case IRON_PICKAXE -> config.pickaxe_Attack_Cool;
            case IRON_SHOVEL -> config.shovel_Attack_Cool;
            case IRON_HOE -> config.hoe_Attack_Cool;
            default -> config.sword_Attack_Cool;
        };
    }

    public long attack(Player player) {
        if (swap.isFirearm(player)) {
            return shoot(player) ? config.gun_Attack_Cool : config.gun_Attack_Empty_Cool;
        }

        long cooldown = cooldown(player);

        switch (player.getInventory().getItemInMainHand().getType()) {
            case IRON_AXE -> chop(player);
            case IRON_PICKAXE -> strike(player);
            case IRON_SHOVEL -> scoop(player);
            case IRON_HOE -> reap(player);
            default -> slash(player);
        }

        return cooldown;
    }

    public void combo(Player player) {
        UUID uuid = player.getUniqueId();
        Material tool = player.getInventory().getItemInMainHand().getType();
        Flurry shape = flurry(tool);
        double amount = damage(tool) * config.melee_r_Skill_Rate;

        int[] starts = new int[FLURRY_TILT.length];
        do {
            int at = config.melee_r_Skill_Delay;
            for (int i = 0; i < starts.length; i++) {
                at += (int) (Math.random() * 3);
                starts[i] = at;
            }
        } while (starts[starts.length - 1] - config.melee_r_Skill_Delay != 6);

        LivingEntity[] focus = new LivingEntity[1];

        config.is_acting.add(uuid);
        player.getWorld().playSound(player, Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 0.7f);
        player.getWorld().playSound(player, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 0.6f);
        player.getWorld().playSound(player, Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.8f, 0.5f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, config.melee_r_Skill_Delay, 4, false, false, false));

        new BukkitRunnable() {
            int ticks = 0;
            int next = 0;

            @Override
            public void run() {
                ticks++;

                if (!player.isOnline() || player.isDead() || !config.is_acting.contains(uuid)) {
                    config.is_acting.remove(uuid);
                    cancel();
                    return;
                }

                while (next < starts.length && ticks >= starts[next]) {
                    if (next == 0) focus[0] = aim(player, shape.reach() + 1.0);
                    comboSlash(player, shape, amount, next++, focus[0]);
                }

                if (ticks >= starts[starts.length - 1] + FLURRY_KEYS.length + 1) {
                    config.is_acting.remove(uuid);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Flurry flurry(Material tool) {
        return switch (tool) {
            case IRON_AXE -> new Flurry(config.axe_Attack_Reach + 2.2, 58, 0.6, 1.0,
                    Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.6f, true, config.axe_Attack_Knockback, config.axe_Attack_Lift);
            case IRON_PICKAXE -> new Flurry(config.pickaxe_Attack_Reach + 1.2, 52, 0.35, 0.9,
                    Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, true, config.pickaxe_Attack_Knockback, config.pickaxe_Attack_Lift);
            case IRON_SHOVEL -> new Flurry(config.shovel_Attack_Reach + 1.2, 62, 0.5, 0.9,
                    Sound.ITEM_SHOVEL_FLATTEN, 0.8f, true, config.shovel_Attack_Knockback, config.shovel_Attack_Lift);
            case IRON_HOE -> new Flurry(config.hoe_Attack_Reach + 1.2, 80, 0.32, 0.9,
                    Sound.ITEM_HOE_TILL, 1.1f, true, config.hoe_Attack_Knockback, config.hoe_Attack_Lift);
            default -> new Flurry(config.sword_Attack_Reach + 1.2, 65, 0.45, 1.0,
                    Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.75f, false, 0, 0);
        };
    }

    private double damage(Material tool) {
        return switch (tool) {
            case IRON_AXE -> config.axe_Attack_Damage;
            case IRON_PICKAXE -> config.pickaxe_Attack_Damage;
            case IRON_SHOVEL -> config.shovel_Attack_Damage;
            case IRON_HOE -> config.hoe_Attack_Damage;
            default -> config.sword_Attack_Damage;
        };
    }

    private LivingEntity aim(Player player, double range) {
        Location eye = player.getEyeLocation();
        RayTraceResult hit = player.getWorld().rayTraceEntities(eye, eye.getDirection(), range, 0.4,
                entity -> isTarget(player, entity));

        if (hit == null || !(hit.getHitEntity() instanceof LivingEntity target)) return null;
        return player.hasLineOfSight(target) ? target : null;
    }

    private void comboSlash(Player player, Flurry shape, double amount, int index, LivingEntity focus) {
        Basis b = basis(player);
        World world = player.getWorld();
        boolean last = index == FLURRY_TILT.length - 1;

        double tilt = Math.toRadians(FLURRY_TILT[index] + (Math.random() * 2 - 1) * 8);
        Vector slant = b.right().clone().multiply(Math.cos(tilt)).add(b.up().clone().multiply(Math.sin(tilt))).normalize();

        double sweep = Math.toRadians(shape.sweep() + (Math.random() * 2 - 1) * 6);
        double reach = shape.reach();
        double rim = reach - 0.6;

        Location origin = b.eye().clone().subtract(0, 0.5, 0);
        Location pivot = origin.clone().add(b.forward().clone().multiply(0.6));

        Vector toFocus = focus != null && focus.isValid() && !focus.isDead() && focus.getWorld().equals(world)
                ? focus.getBoundingBox().getCenter().subtract(origin.toVector()) : null;
        boolean locked = toFocus != null && toFocus.length() > 0.5 && toFocus.length() <= reach + 1.0;

        Vector axis;
        Vector lean;
        Location center;

        if (locked) {
            axis = toFocus.clone().normalize();
            Vector side = b.right().clone().subtract(axis.clone().multiply(b.right().dot(axis))).normalize();
            Vector top = side.clone().crossProduct(axis).normalize();
            lean = side.clone().multiply(Math.cos(tilt)).add(top.multiply(Math.sin(tilt))).normalize();
            center = origin.clone().add(toFocus).subtract(axis.clone().multiply(rim));
        } else {
            axis = b.forward();
            lean = slant;
            center = pivot;
        }

        Vector normal = axis.clone().crossProduct(lean).normalize();
        if (normal.getY() < 0) normal.multiply(-1);

        world.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.4f, 0.55f + 0.05f * index);
        world.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.25f + 0.1f * index);
        world.playSound(player, Sound.ITEM_TRIDENT_THROW, 1.3f, 0.65f + 0.05f * index);
        world.playSound(player, Sound.ITEM_TRIDENT_HIT, 1.3f, 0.6f + 0.05f * index);
        world.playSound(player, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.2f, 0.6f + 0.05f * index);
        world.playSound(player, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.9f, 0.75f + 0.05f * index);
        world.playSound(player, Sound.ENTITY_SHEEP_SHEAR, 1.0f, 0.6f + 0.08f * index);
        world.playSound(player, shape.sound(), 1.0f, shape.pitch() + 0.08f * index);
        if (last) world.playSound(player, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                if (ticks < FLURRY_KEYS.length - 1) {
                    double a0 = -sweep + 2 * sweep * FLURRY_KEYS[ticks];
                    double a1 = -sweep + 2 * sweep * FLURRY_KEYS[ticks + 1];

                    crescent(world, center, axis, lean, normal, a0, a1, sweep, rim, shape.thick());

                    Vector tangent = axis.clone().multiply(-Math.sin(a1)).add(lean.clone().multiply(Math.cos(a1)));
                    tipSparks(world, point(center, axis, lean, a1, rim), tangent, 4);

                    if (ticks == 1) {
                        arcHit(player, origin, b.flat(), b.forward(), slant, reach, -sweep - Math.toRadians(8), sweep + Math.toRadians(8),
                                shape.tolerance(), amount,
                                !last || shape.push(), last ? shape.knockback() : 0, last ? shape.lift() : 0);
                    }
                } else if (ticks < FLURRY_KEYS.length + 1) {
                    double step = (ticks == FLURRY_KEYS.length - 1 ? 0.14 : 0.24) / rim;
                    for (double a = -sweep + Math.random() * step; a <= sweep; a += step) {
                        world.spawnParticle(Particle.DUST, point(center, axis, lean, a, rim), 1, 0, 0, 0, 0, TRAIL);
                    }
                } else {
                    cancel();
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void crescent(World world, Location pivot, Vector axis, Vector slant, Vector normal, double a0, double a1,
                          double sweep, double rim, double thick) {
        double step = 0.08 / rim;

        for (double a = a0; a <= a1; a += step) {
            double u = Math.max(0.0, Math.min(1.0, (a + sweep) / (2 * sweep)));
            double width = Math.pow(Math.sin(Math.PI * u), 0.75) * thick;

            world.spawnParticle(Particle.DUST, point(pivot, axis, slant, a, rim), 1, 0.005, 0.005, 0.005, 0, BLADE);

            for (double depth = 0.07; depth < width; depth += 0.075) {
                double ratio = depth / width;
                Particle.DustOptions tone = ratio < 0.3 ? KEEN : ratio < 0.65 ? BODY : MIST;
                if (tone == MIST && Math.random() < 0.4) continue;

                Location at = point(pivot, axis, slant, a, rim - depth * 0.6).subtract(normal.clone().multiply(depth));
                world.spawnParticle(Particle.DUST, at, 1, 0.012, 0.012, 0.012, 0, tone);
            }
        }
    }

    public void burst(Player player) {
        UUID uuid = player.getUniqueId();
        boolean[] landed = new boolean[2];
        boolean[] ended = new boolean[2];

        if (!pay(player, config.gun_r_Skill_Cost_Ratio)) return;

        config.is_acting.add(uuid);
        fire(player, config.gun_r_Skill_Rate, 0, config.gun_Attack_Recoil, false, hit -> {
            landed[0] = hit;
            ended[0] = true;
        });

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                if (!player.isOnline() || player.isDead() || !config.is_acting.contains(uuid)) {
                    config.is_acting.remove(uuid);
                    cancel();
                    return;
                }

                if (ticks == config.gun_r_Skill_Interval) {
                    if (!pay(player, config.gun_r_Skill_Cost_Ratio)) {
                        config.is_acting.remove(uuid);
                        cancel();
                        return;
                    }

                    fire(player, config.gun_r_Skill_Rate, 0, config.gun_Attack_Recoil, false, hit -> {
                        landed[1] = hit;
                        ended[1] = true;
                    });
                    return;
                }

                if (ticks < config.gun_r_Skill_Interval + config.gun_r_Skill_Finish_Delay || !ended[0] || !ended[1]) return;

                if (landed[0] && landed[1] && pay(player, config.gun_r_Skill_Finish_Cost_Ratio)) {
                    fire(player, config.gun_r_Skill_Finish_Rate, config.gun_r_Skill_Knockback, config.gun_r_Skill_Recoil, true, null);

                    Vector back = player.getEyeLocation().getDirection().normalize().multiply(-config.gun_r_Skill_Push);
                    back.setY(Math.max(back.getY(), 0.15));
                    player.setVelocity(back);

                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.8f);
                }

                config.is_acting.remove(uuid);
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Basis basis(Player player) {
        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        double yaw = Math.toRadians(eye.getYaw());
        Vector flat = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector right = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vector up = right.clone().crossProduct(forward).normalize();
        return new Basis(eye, forward, flat, right, up);
    }

    private boolean nextFlip(Player player) {
        UUID uuid = player.getUniqueId();
        boolean flip = config.slash_Flip.getOrDefault(uuid, false);
        config.slash_Flip.put(uuid, !flip);
        return flip;
    }

    private void deal(Player player, LivingEntity target, double amount, DamageSource source, boolean noKnock) {
        UUID uuid = player.getUniqueId();
        double buff = config.restore_buff_rate.getOrDefault(uuid, 0.0);

        config.is_damaging.add(uuid);
        try {
            new ForceDamage(target, amount * (1.0 + buff), source, noKnock).applyEffect(player);
        } finally {
            config.is_damaging.remove(uuid);
        }
    }

    private DamageSource meleeSource(Player player) {
        return DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();
    }

    private double centerWeight(Player player, Vector flat, LivingEntity target) {
        Vector toTarget = target.getBoundingBox().getCenter().subtract(player.getLocation().toVector());
        toTarget.setY(0);

        double distance = toTarget.length();
        double halfWidth = target.getBoundingBox().getWidthX() / 2.0;
        if (distance <= halfWidth + 0.3) return 1.0;

        double angle = Math.toDegrees(toTarget.angle(flat));
        double allowance = Math.toDegrees(Math.asin(Math.min(1.0, halfWidth / distance)));
        double offset = Math.max(0.0, angle - allowance);
        double sigma = config.attack_Center_Sigma;

        return Math.exp(-(offset * offset) / (2 * sigma * sigma));
    }

    private boolean meleeHit(Player player, LivingEntity target, double amount, boolean push, double knockback, double lift, double weight, DamageSource source) {
        if (target.isInvulnerable()) return false;

        double scaled = amount * (config.attack_Edge_Ratio + (1.0 - config.attack_Edge_Ratio) * weight);
        double before = target.getHealth() + target.getAbsorptionAmount();

        deal(player, target, scaled, source, push);

        boolean landed = target.isDead() || target.getHealth() + target.getAbsorptionAmount() < before - 1e-6;
        if (!landed) return false;

        if (push && (knockback > 0 || lift > 0)) {
            Vector velocity = target.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0);
            if (velocity.lengthSquared() > 1e-6) velocity.normalize().multiply(knockback);
            velocity.setY(lift);
            target.setVelocity(velocity);
        }

        if (!push || knockback > 0 || lift > 0) bomb.trigger(player, target);

        Material held = player.getInventory().getItemInMainHand().getType();
        if (held == Material.IRON_AXE || held == Material.IRON_PICKAXE) wound.pop(player, target);

        hitVisual(player.getWorld(), target, weight);
        return true;
    }

    private void heal(Player player, int hits) {
        if (hits <= 0 || player.isDead() || !player.isOnline()) return;

        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        double maxHealth = maxHealthAttr.getValue();
        double ratio = Math.min(config.attack_Heal_Ratio * hits, config.attack_Heal_Cap);
        player.setHealth(Math.min(maxHealth, player.getHealth() + maxHealth * ratio));
    }

    private boolean isTarget(Player player, Entity entity) {
        return entity instanceof LivingEntity && entity != player && !(entity instanceof ArmorStand) && !entity.isDead();
    }

    private void slash(Player player) {
        Basis b = basis(player);
        World world = player.getWorld();

        boolean flip = nextFlip(player);
        double tilt = Math.toRadians(flip ? -22 : 22);
        Vector slant = b.right().clone().multiply(Math.cos(tilt)).add(b.up().clone().multiply(Math.sin(tilt))).normalize();

        double sweep = Math.toRadians(70);
        double from = flip ? -sweep : sweep;
        double reach = config.sword_Attack_Reach;

        Location origin = b.eye().clone().subtract(0, 0.4, 0);
        Location pivot = origin.clone().add(b.forward().clone().multiply(0.6));

        world.playSound(b.eye(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.3f, 0.75f);
        world.playSound(b.eye(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.2f, 0.7f);
        world.playSound(b.eye(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 1.1f);

        playStroke(player, pivot, b.forward(), slant, from, -from, reach - 0.6, 1.0, SWING_KEYS, 0.5,
                () -> arcHit(player, origin, b.flat(), b.forward(), slant, reach, -sweep - Math.toRadians(8), sweep + Math.toRadians(8),
                        0.9, config.sword_Attack_Damage, false, 0, 0));
    }

    private void reap(Player player) {
        Basis b = basis(player);
        World world = player.getWorld();

        boolean flip = nextFlip(player);
        double tilt = Math.toRadians(flip ? -30 : 30);
        Vector slant = b.right().clone().multiply(Math.cos(tilt)).add(b.up().clone().multiply(Math.sin(tilt))).normalize();

        double sweep = Math.toRadians(85);
        double from = flip ? -sweep : sweep;
        double reach = config.hoe_Attack_Reach;

        Location origin = b.eye().clone().subtract(0, 0.3, 0);
        Location pivot = origin.clone().add(b.forward().clone().multiply(0.5));

        world.playSound(b.eye(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.2f);
        world.playSound(b.eye(), Sound.ITEM_HOE_TILL, 1.0f, 1.1f);
        world.playSound(b.eye(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.9f, 1.0f);

        playStroke(player, pivot, b.forward(), slant, from, -from, reach - 0.5, 0.75, QUICK_KEYS, 0.5,
                () -> arcHit(player, origin, b.flat(), b.forward(), slant, reach, -sweep - Math.toRadians(6), sweep + Math.toRadians(6),
                        0.8, config.hoe_Attack_Damage, true, config.hoe_Attack_Knockback, config.hoe_Attack_Lift));
    }

    private void scoop(Player player) {
        Basis b = basis(player);
        World world = player.getWorld();

        double from = Math.toRadians(-35);
        double to = Math.toRadians(55);
        double reach = config.shovel_Attack_Reach;

        Location origin = b.eye().clone().subtract(0, 0.9, 0);
        Location pivot = origin.clone().add(b.flat().clone().multiply(0.3));

        world.playSound(b.eye(), Sound.ITEM_SHOVEL_FLATTEN, 1.2f, 0.8f);
        world.playSound(b.eye(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.1f, 0.6f);
        world.playSound(b.eye(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.9f, 0.6f);

        Location ground = player.getLocation().add(b.flat().clone().multiply(1.4));
        Material soil = ground.clone().subtract(0, 0.1, 0).getBlock().getType();
        if (soil.isSolid()) {
            world.spawnParticle(Particle.BLOCK, ground.clone().add(0, 0.3, 0), 14, 0.3, 0.2, 0.3, soil.createBlockData());
        }

        playStroke(player, pivot, b.flat(), WORLD_UP, from, to, reach - 0.3, 1.1, SWING_KEYS, 0.5,
                () -> arcHit(player, origin, b.flat(), b.flat(), WORLD_UP, reach, Math.toRadians(-55), Math.toRadians(70),
                        0.7, config.shovel_Attack_Damage, true, config.shovel_Attack_Knockback, config.shovel_Attack_Lift));
    }

    private void chop(Player player) {
        Basis b = basis(player);
        World world = player.getWorld();

        double from = Math.toRadians(85);
        double to = Math.toRadians(-30);
        double reach = config.axe_Attack_Reach;

        Location pivot = b.eye().clone().subtract(0, 0.5, 0).add(b.flat().clone().multiply(0.3));

        world.playSound(b.eye(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.3f, 0.6f);
        world.playSound(b.eye(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.45f);
        world.playSound(b.eye(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0f, 0.5f);

        playStroke(player, pivot, b.flat(), WORLD_UP, from, to, reach + 0.4, 1.3, HEAVY_KEYS, 1.0,
                () -> chopHit(player, b.flat(), reach));
    }

    private void strike(Player player) {
        Basis b = basis(player);
        World world = player.getWorld();

        double lean = Math.toRadians(18);
        Vector slant = b.up().clone().multiply(Math.cos(lean)).add(b.right().clone().multiply(Math.sin(lean))).normalize();

        double from = Math.toRadians(80);
        double reach = config.pickaxe_Attack_Reach;

        Location pivot = b.eye().clone().subtract(0, 0.3, 0).add(b.forward().clone().multiply(0.6));

        world.playSound(b.eye(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.2f, 1.0f);
        world.playSound(b.eye(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.1f, 0.55f);
        world.playSound(b.eye(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.6f);

        playStroke(player, pivot, b.forward(), slant, from, 0, reach - 0.6, 0.7, STRIKE_KEYS, 1.0,
                () -> strikeHit(player, b.eye(), b.forward(), reach));
    }

    private static int impactTick(double[] keys, double threshold) {
        for (int i = 0; i < keys.length - 1; i++) {
            if (keys[i + 1] >= threshold) return i;
        }
        return keys.length - 2;
    }

    private static Location point(Location pivot, Vector axis, Vector slant, double angle, double radius) {
        return pivot.clone().add(axis.clone().multiply(Math.cos(angle) * radius)
                .add(slant.clone().multiply(Math.sin(angle) * radius)));
    }

    private void playStroke(Player player, Location pivot, Vector axis, Vector slant, double from, double to,
                            double length, double thick, double[] keys, double impactAt, Runnable onImpact) {
        World world = player.getWorld();
        int impactTick = impactTick(keys, impactAt);
        Vector normal = axis.clone().crossProduct(slant).normalize();
        double sign = to >= from ? 1.0 : -1.0;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= keys.length - 1 || !player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                double k0 = keys[ticks];
                double k1 = keys[ticks + 1];
                double a0 = from + (to - from) * k0;
                double a1 = from + (to - from) * k1;

                if (k1 < k0) {
                    drawStroke(world, pivot, axis, slant, normal, a0, a1, length * 0.97, thick * 0.5, 0, 1.6);
                } else {
                    double progress = Math.max(0.0, Math.min(1.0, (k0 + k1) / 2.0));
                    drawStroke(world, pivot, axis, slant, normal, a0, a1, length, thick * (0.55 + 0.45 * progress),
                            length * 0.35, 1.0);

                    Vector tangent = axis.clone().multiply(-Math.sin(a1)).add(slant.clone().multiply(Math.cos(a1))).multiply(sign);
                    tipSparks(world, point(pivot, axis, slant, a1, length), tangent, 3);
                }

                if (ticks == impactTick && onImpact != null) onImpact.run();

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawStroke(World world, Location pivot, Vector axis, Vector slant, Vector normal, double a0, double a1,
                            double rim, double thick, double hazeFrom, double density) {
        double lo = Math.min(a0, a1);
        double hi = Math.max(a0, a1);

        if (hazeFrom > 0) {
            double strokeIn = rim - 0.6 * thick;
            for (double r = hazeFrom + Math.random() * 0.15; r < strokeIn - 0.1; r += 0.26) {
                double step = 0.34 * density / r;
                for (double a = lo + Math.random() * step; a <= hi; a += step) {
                    if (Math.random() < 0.4) continue;
                    world.spawnParticle(Particle.DUST, point(pivot, axis, slant, a, r), 1, 0.06, 0.06, 0.06, 0, HAZE);
                }
            }
        }

        double step = 0.12 * density / rim;
        for (double a = lo + Math.random() * step; a <= hi; a += step) {
            Vector dir = axis.clone().multiply(Math.cos(a)).add(slant.clone().multiply(Math.sin(a)));
            for (double[] cell : SECTION) {
                Location p = pivot.clone().add(dir.clone().multiply(rim - cell[0] * thick))
                        .add(normal.clone().multiply(cell[1] * thick));
                world.spawnParticle(Particle.DUST, p, 1, 0.02, 0.02, 0.02, 0, CELLS[(int) cell[2]]);
            }
        }
    }

    private void tipSparks(World world, Location tip, Vector tangent, int count) {
        if (tangent.lengthSquared() < 1e-6) return;
        Vector dir = tangent.clone().normalize();
        for (int i = 0; i < count; i++) {
            Location at = tip.clone().subtract(dir.clone().multiply(0.12 * i));
            world.spawnParticle(Particle.CRIT, at, 0, dir.getX(), dir.getY(), dir.getZ(), 0.45);
        }
    }

    private void hitVisual(World world, LivingEntity target, double weight) {
        Location body = target.getBoundingBox().getCenter().toLocation(world);
        world.spawnParticle(Particle.CRIT, body, (int) Math.round(4 + 8 * weight), 0.2, 0.3, 0.2, 0.15);
        world.playSound(body, Sound.ENTITY_PLAYER_ATTACK_CRIT, (float) (0.6 + 0.4 * weight), 1.0f);
        if (weight >= 0.6) world.playSound(body, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.7f, 0.9f);
    }

    private void arcHit(Player player, Location origin, Vector flat, Vector axis, Vector slant, double reach,
                        double lo, double hi, double planeTol, double amount, boolean push, double knockback, double lift) {
        World world = player.getWorld();
        DamageSource source = meleeSource(player);
        Vector normal = axis.clone().crossProduct(slant).normalize();
        int hits = 0;

        for (Entity entity : world.getNearbyEntities(origin, reach + 1.5, reach + 1.5, reach + 1.5)) {
            if (!isTarget(player, entity)) continue;
            LivingEntity target = (LivingEntity) entity;

            Vector toTarget = target.getBoundingBox().getCenter().subtract(origin.toVector());
            double halfWidth = target.getBoundingBox().getWidthX() / 2.0;
            double distance = toTarget.length();

            if (distance > reach + halfWidth) continue;

            double tolerance = planeTol + halfWidth + target.getHeight() * 0.3 * Math.abs(normal.getY());
            if (Math.abs(toTarget.dot(normal)) > tolerance) continue;

            double angle = Math.atan2(toTarget.dot(slant), toTarget.dot(axis));
            if (distance > 1.0 + halfWidth && (angle < lo || angle > hi)) continue;

            if (!player.hasLineOfSight(target)) continue;

            if (meleeHit(player, target, amount, push, knockback, lift, centerWeight(player, flat, target), source)) hits++;
        }

        heal(player, hits);
    }

    private void chopHit(Player player, Vector flat, double reach) {
        World world = player.getWorld();
        DamageSource source = meleeSource(player);
        Location impact = player.getLocation().add(flat.clone().multiply(reach));

        world.playSound(impact, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.9f, 0.8f);
        world.playSound(impact, Sound.BLOCK_ANVIL_PLACE, 0.4f, 1.2f);
        world.spawnParticle(Particle.CRIT, impact.clone().add(0, 0.4, 0), 12, 0.5, 0.3, 0.5, 0.1);

        Material ground = impact.clone().subtract(0, 0.1, 0).getBlock().getType();
        if (ground.isSolid()) {
            world.spawnParticle(Particle.BLOCK, impact.clone().add(0, 0.1, 0), 24, 0.6, 0.2, 0.6, ground.createBlockData());
        }

        int hits = 0;

        for (Entity entity : world.getNearbyEntities(impact, 1.3, 1.8, 1.3)) {
            if (!isTarget(player, entity)) continue;
            LivingEntity target = (LivingEntity) entity;
            if (!player.hasLineOfSight(target)) continue;

            if (meleeHit(player, target, config.axe_Attack_Damage, true, config.axe_Attack_Knockback, config.axe_Attack_Lift, centerWeight(player, flat, target), source)) hits++;
        }

        heal(player, hits);
    }

    private void strikeHit(Player player, Location eye, Vector forward, double reach) {
        World world = player.getWorld();

        Predicate<Entity> filter = entity -> isTarget(player, entity);
        RayTraceResult hit = world.rayTrace(eye, forward, reach, FluidCollisionMode.NEVER, true, 0.45, filter);

        Location tip = hit != null
                ? hit.getHitPosition().toLocation(world)
                : eye.clone().add(forward.clone().multiply(reach));

        for (double back = 0; back <= 1.0; back += 0.12) {
            world.spawnParticle(Particle.DUST, tip.clone().subtract(forward.clone().multiply(back)), 1, 0.01, 0.01, 0.01, 0,
                    back < 0.3 ? CORE : EDGE);
        }

        if (hit == null) return;

        if (hit.getHitEntity() instanceof LivingEntity target) {
            if (meleeHit(player, target, config.pickaxe_Attack_Damage, true, config.pickaxe_Attack_Knockback, config.pickaxe_Attack_Lift, 1.0, meleeSource(player))) heal(player, 1);
            world.playSound(tip, Sound.ITEM_TRIDENT_HIT, 1.2f, 0.7f);
        } else if (hit.getHitBlock() != null) {
            world.spawnParticle(Particle.BLOCK, tip, 12, 0.15, 0.15, 0.15, hit.getHitBlock().getBlockData());
            world.playSound(tip, Sound.BLOCK_STONE_HIT, 1.2f, 0.7f);
        }
    }

    private double levelMultiplier(Player player) {
        long level = player.getPersistentDataContainer().getOrDefault(
                new NamespacedKey(plugin, "level"), PersistentDataType.LONG, 0L);

        if (level < 0) return 1.0;
        if (level > 10) level = 10;

        return 1.0 + 0.005 * level * level + 0.055 * level;
    }

    private boolean shoot(Player player) {
        if (!pay(player, config.gun_Attack_Cost_Ratio)) return false;

        fire(player, 1.0, config.gun_Attack_Knockback, config.gun_Attack_Recoil, false, null);

        return true;
    }

    private boolean pay(Player player, double ratio) {
        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;
        double cost = maxHealth * ratio;

        if (player.getHealth() <= cost) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.6f);
            return false;
        }

        player.setHealth(player.getHealth() - cost);
        return true;
    }

    private void fire(Player player, double rate, double knockback, float kick, boolean instant, Consumer<Boolean> done) {
        World world = player.getWorld();

        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;
        double lost = Math.max(0.0, maxHealth - player.getHealth());

        Vector dir = player.getEyeLocation().getDirection().normalize();

        Location spawn = player.getEyeLocation().add(dir.clone().multiply(0.2)).add(0, -0.3, 0);
        BlockDisplay bullet = new bulletModel(world, spawn, Material.IRON_BLOCK.createBlockData(), 0.15f, 0.15f, 0.35f).getDisplay();

        double speed = config.gun_Attack_Speed;
        Vector velocity = dir.clone().multiply(speed);

        world.playSound(spawn, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 1.15f);
        world.playSound(spawn, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.4f, 0.7f);
        world.playSound(spawn, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.2f, 0.6f);
        world.playSound(spawn, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.8f, 0.6f);
        world.playSound(spawn, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1.0f, 1.2f);
        world.spawnParticle(Particle.SMOKE, spawn, 8, 0.06, 0.06, 0.06, 0.04);
        world.spawnParticle(Particle.CRIT, spawn, 6, 0.05, 0.05, 0.05, 0.25);

        recoil(player, kick);

        DamageSource source = DamageSource.builder(DamageType.MOB_PROJECTILE)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Set<UUID> pierced = new HashSet<>();

        Predicate<Entity> filter = entity -> isTarget(player, entity)
                && !((LivingEntity) entity).isInvulnerable()
                && !entity.hasMetadata("NPC")
                && !pierced.contains(entity.getUniqueId());

        Vector push = new Vector(dir.getX(), 0, dir.getZ());
        if (push.lengthSquared() > 1e-6) push.normalize().multiply(knockback);
        push.setY(0.1);

        double scale = levelMultiplier(player);
        double reached = instant
                ? pierce(player, world, spawn, dir, speed * config.gun_Attack_Life, rate, knockback, lost, scale, source, push, pierced, filter)
                : -1;
        double limit = reached >= 0 ? reached : speed * config.gun_Attack_Life;

        new BukkitRunnable() {
            int life = config.gun_Attack_Life;
            double flown = 0;
            boolean reported = false;
            final Location current = spawn.clone();

            private void report() {
                if (reported || done == null) return;
                reported = true;
                done.accept(!pierced.isEmpty());
            }

            private void end() {
                bullet.remove();
                cancel();
                report();
            }

            @Override
            public void run() {
                if (!bullet.isValid() || life-- <= 0) {
                    end();
                    return;
                }

                double travelled = speed;
                boolean stopped = false;

                if (instant) {
                    if (flown + speed >= limit) {
                        travelled = limit - flown;
                        stopped = true;
                    }
                } else {
                    double hit = pierce(player, world, current, dir, speed, rate, knockback, lost, scale, source, push, pierced, filter);
                    if (!pierced.isEmpty()) report();
                    if (hit >= 0) {
                        travelled = hit;
                        stopped = true;
                    }
                }

                for (double d = 0; d < travelled; d += 0.8) {
                    world.spawnParticle(Particle.DUST, current.clone().add(dir.clone().multiply(d)), 1, 0, 0, 0, 0, TRAIL);
                }

                if (stopped) {
                    end();
                    return;
                }

                flown += speed;
                current.add(velocity);
                bullet.teleport(current);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private double pierce(Player player, World world, Location from, Vector dir, double range, double rate, double knockback,
                          double lost, double scale, DamageSource source, Vector push, Set<UUID> pierced, Predicate<Entity> filter) {
        while (true) {
            RayTraceResult hit = world.rayTrace(from, dir, range, FluidCollisionMode.NEVER, true, 0.2, filter);
            if (hit == null) return -1;

            Location at = hit.getHitPosition().toLocation(world);

            if (hit.getHitEntity() instanceof LivingEntity target) {
                pierced.add(target.getUniqueId());

                double amount = (config.gun_Attack_Damage
                        + (target.getHealth() * config.gun_Attack_Health_Ratio
                        + lost * config.gun_Attack_Lost_Ratio) / scale) * rate;

                deal(player, target, amount, source, true);
                if (knockback > 0) {
                    target.setVelocity(push);
                    bomb.trigger(player, target);
                } else {
                    wound.pop(player, target);
                }

                world.spawnParticle(Particle.CRIT, at, 8, 0.1, 0.1, 0.1, 0.15);
                world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.9f);
                world.playSound(at, Sound.ITEM_TRIDENT_HIT, 1.2f, 0.7f);
                world.playSound(at, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.7f);

                if (pierced.size() < config.gun_Attack_Pierce) continue;
            } else if (hit.getHitBlock() != null) {
                world.spawnParticle(Particle.BLOCK, at, 10, 0.1, 0.1, 0.1, hit.getHitBlock().getBlockData());
                world.playSound(at, Sound.BLOCK_STONE_HIT, 1.1f, 1.2f);
            }

            return at.toVector().distance(from.toVector());
        }
    }

    private void recoil(Player player, float amount) {
        float kick = -amount;
        if (kick == 0f) return;

        new BukkitRunnable() {
            int ticks = 0;
            float current = 0f;
            float target = kick;

            @Override
            public void run() {
                if (ticks > 4 || !player.isOnline() || player.isDead()) {
                    if (Math.abs(current) > 0.01f) turn(player, -current);
                    cancel();
                    return;
                }

                float next;
                if (ticks < 1) {
                    next = current + (target - current) * 0.9f;
                } else {
                    target = 0f;
                    next = current + (target - current) * 0.55f;
                }

                float delta = next - current;
                current = next;

                if (Math.abs(delta) > 0.05f) turn(player, delta);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void turn(Player player, float pitchDelta) {
        Location loc = player.getLocation();
        loc.setPitch(Math.max(-90f, Math.min(90f, loc.getPitch() + pitchDelta)));
        Vector velocity = player.getVelocity();
        player.teleport(loc);
        player.setVelocity(velocity);
    }
}