package org.core.coreSystem.cores.VOL6.Carpenter.Passive;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.coreSystem.cores.VOL6.Carpenter.coreSystem.Carpenter;
import org.core.effect.crowdControl.ForceDamage;
import org.core.models.absModelSystem.modelManager;

import java.util.concurrent.ThreadLocalRandom;

public class basicAttack {

    private static final Particle.DustOptions HEAD = new Particle.DustOptions(Color.fromRGB(196, 160, 72), 0.8f);
    private static final Particle.DustOptions FACE = new Particle.DustOptions(Color.fromRGB(240, 222, 150), 0.7f);
    private static final Particle.DustOptions HANDLE = new Particle.DustOptions(Color.fromRGB(122, 84, 46), 0.55f);
    private static final Particle.DustOptions TRAIL = new Particle.DustOptions(Color.fromRGB(214, 186, 104), 0.45f);
    private static final Particle.DustOptions DUSK = new Particle.DustOptions(Color.fromRGB(150, 118, 64), 0.4f);
    private static final BlockData PLANK = Material.OAK_PLANKS.createBlockData();

    private final Carpenter config;
    private final JavaPlugin plugin;

    public basicAttack(Carpenter config, JavaPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    public void attack(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();

        Vector right = new Vector(-forward.getZ(), 0, forward.getX());
        if (right.lengthSquared() < 1e-6) right = new Vector(1, 0, 0);
        right.normalize();

        Location pivot = eye.clone().add(0, -0.2, 0);
        Vector axis = forward;
        double radius = config.basic_Reach;

        LivingEntity aimed = aim(player);
        if (aimed != null && player.hasLineOfSight(aimed)) {
            Vector center = aimed.getLocation().add(0, aimed.getHeight() * config.basic_Anchor_Height, 0).toVector();
            Vector toTarget = center.clone().subtract(pivot.toVector());
            if (toTarget.length() >= 0.5) {
                axis = toTarget.normalize();
                radius = Math.min(config.basic_Reach - config.basic_Anchor_Inset, toTarget.length());
                pivot = center.clone().subtract(axis.clone().multiply(radius)).toLocation(world);
            }
        }

        Vector flatSide = right.clone().subtract(axis.clone().multiply(right.dot(axis)));
        if (flatSide.lengthSquared() < 1e-6) flatSide = right.clone();
        flatSide.normalize();
        Vector up = flatSide.getCrossProduct(axis).normalize();
        if (up.getY() < 0) up.multiply(-1);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double tilt = Math.toRadians(config.basic_Tilt + random.nextDouble(-8, 8));
        Vector side = flatSide.clone().multiply(Math.cos(tilt)).add(up.clone().multiply(Math.sin(tilt))).normalize();

        boolean rising = config.basic_Rising.remove(player.getUniqueId());
        if (!rising) config.basic_Rising.add(player.getUniqueId());
        if (rising) side.multiply(-1);
        Vector normal = axis.getCrossProduct(side).normalize();

        double half = Math.toRadians(config.basic_Angle / 2.0);

        world.playSound(eye, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.45f, rising ? 0.75f : 0.6f);
        world.playSound(eye, Sound.ITEM_ARMOR_EQUIP_IRON, 0.5f, 0.8f);

        swing(player, pivot, axis, side, normal, half, radius);
    }

    private void swing(Player player, Location pivot, Vector axis, Vector side, Vector normal, double half, double radius) {
        World world = player.getWorld();

        new BukkitRunnable() {
            int tick = 0;
            double angle = half;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                tick++;
                double next = half - 2 * half * tick / config.basic_Swing_Ticks;

                LivingEntity target = null;
                double first = -Double.MAX_VALUE;
                for (Entity entity : world.getNearbyEntities(pivot, radius + 1.0, radius + 1.0, radius + 1.0)) {
                    if (!isTarget(player, entity)) continue;
                    double at = reach((LivingEntity) entity, pivot, axis, side, normal, radius, angle, next);
                    if (at == Double.NEGATIVE_INFINITY || !player.hasLineOfSight(entity)) continue;
                    if (at > first) {
                        first = at;
                        target = (LivingEntity) entity;
                    }
                }

                double stop = target != null ? Math.max(next, Math.min(angle, first)) : next;
                band(world, pivot, axis, side, angle, stop, radius);
                angle = stop;

                if (target != null) {
                    strike(player, target, axis);
                    cancel();
                    return;
                }

                if (tick >= config.basic_Swing_Ticks) {
                    world.playSound(pivot, Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.5f, 0.7f);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private double reach(LivingEntity target, Location pivot, Vector axis, Vector side, Vector normal, double radius, double from, double to) {
        BoundingBox box = target.getBoundingBox();
        Vector rel = box.getCenter().subtract(pivot.toVector());
        double halfWidth = box.getWidthX() / 2.0;

        double offPlane = rel.dot(normal);
        if (Math.abs(offPlane) > config.basic_Thickness + box.getHeight() / 2.0) return Double.NEGATIVE_INFINITY;

        Vector inPlane = rel.clone().subtract(normal.clone().multiply(offPlane));
        double distance = inPlane.length();
        if (distance > radius + halfWidth) return Double.NEGATIVE_INFINITY;
        if (distance <= halfWidth + 0.3) return from;

        double at = Math.atan2(inPlane.dot(side), inPlane.dot(axis));
        double allowance = Math.asin(Math.min(1.0, halfWidth / distance));
        if (at - allowance > from || at + allowance < to) return Double.NEGATIVE_INFINITY;
        return Math.max(to, Math.min(from, at));
    }

    private void strike(Player player, LivingEntity target, Vector axis) {
        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();
        new ForceDamage(target, config.basic_Damage, source, true).applyEffect(player);

        Vector push = axis.clone().setY(0);
        if (push.lengthSquared() > 1e-6) {
            push.normalize().multiply(config.basic_Knockback).setY(0.12);
            target.setVelocity(target.getVelocity().add(push));
        }

        World world = target.getWorld();
        Location center = target.getLocation().add(0, target.getHeight() * config.basic_Anchor_Height, 0);
        world.spawnParticle(Particle.BLOCK, center, 8, 0.15, 0.2, 0.15, 0.1, PLANK);
        world.spawnParticle(Particle.CRIT, center, 6, 0.15, 0.2, 0.15, 0.3);
        world.playSound(center, Sound.BLOCK_ANVIL_PLACE, 0.45f, 1.7f);
        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.9f, 0.8f);
        world.playSound(center, Sound.BLOCK_WOOD_HIT, 1.0f, 0.7f);
    }

    private LivingEntity aim(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        double limit = config.basic_Reach;
        RayTraceResult block = world.rayTraceBlocks(eye, dir, limit, FluidCollisionMode.NEVER, true);
        if (block != null) limit = block.getHitPosition().distance(eye.toVector());

        RayTraceResult hit = world.rayTraceEntities(eye, dir, limit, config.basic_Aim_Width, e -> isTarget(player, e));
        return hit != null && hit.getHitEntity() instanceof LivingEntity target ? target : null;
    }

    public boolean isTarget(Player player, Entity entity) {
        if (!(entity instanceof LivingEntity) || entity == player || entity.isDead() || entity.isInvulnerable()) return false;
        if (entity instanceof ArmorStand || modelManager.isTracked(entity)) return false;
        return !(entity instanceof Player p && p.getGameMode() == GameMode.SPECTATOR);
    }

    private Location point(Location pivot, Vector axis, Vector side, double angle, double length) {
        return pivot.clone().add(axis.clone().multiply(Math.cos(angle) * length)).add(side.clone().multiply(Math.sin(angle) * length));
    }

    private void band(World world, Location pivot, Vector axis, Vector side, double from, double to, double radius) {
        int steps = Math.max(2, (int) Math.ceil(Math.abs(from - to) * radius / 0.09));
        for (int i = 0; i <= steps; i++) {
            double a = from + (to - from) * i / steps;
            world.spawnParticle(Particle.DUST, point(pivot, axis, side, a, radius), 1, 0, 0, 0, 0, FACE);
            world.spawnParticle(Particle.DUST, point(pivot, axis, side, a, radius - 0.12), 1, 0, 0, 0, 0, HEAD);
            if (i % 2 == 0) {
                world.spawnParticle(Particle.DUST, point(pivot, axis, side, a, radius - 0.26), 1, 0, 0, 0, 0, TRAIL);
            }
            if (i % 3 == 0) {
                world.spawnParticle(Particle.DUST, point(pivot, axis, side, a, radius - 0.55), 1, 0.02, 0.02, 0.02, 0, DUSK);
            }
            if (i % 5 == 0) {
                world.spawnParticle(Particle.DUST, point(pivot, axis, side, a, radius - 0.95), 1, 0.03, 0.03, 0.03, 0, HANDLE);
            }
        }
    }
}