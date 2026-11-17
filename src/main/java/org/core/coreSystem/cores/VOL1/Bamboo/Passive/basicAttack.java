package org.core.coreSystem.cores.VOL1.Bamboo.Passive;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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
import org.core.coreSystem.cores.VOL1.Bamboo.coreSystem.Bamboo;
import org.core.effect.crowdControl.ForceDamage;
import org.core.models.absModelSystem.modelManager;

import java.util.UUID;

public class basicAttack {

    private static final Particle.DustOptions EDGE = new Particle.DustOptions(Color.fromRGB(228, 244, 200), 0.8f);
    private static final Particle.DustOptions LEAF = new Particle.DustOptions(Color.fromRGB(150, 214, 96), 1.0f);
    private static final Particle.DustOptions STALK = new Particle.DustOptions(Color.fromRGB(96, 168, 70), 0.85f);
    private static final Particle.DustOptions SHADE = new Particle.DustOptions(Color.fromRGB(58, 112, 48), 0.65f);
    private static final Particle.DustOptions MIST = new Particle.DustOptions(Color.fromRGB(122, 190, 92), 0.5f);
    private static final double[] ARC_LAYERS = {0.0, 0.1, 0.22, 0.36, 0.54, 0.76};
    private static final Particle.DustOptions[] ARC_COLORS = {EDGE, EDGE, LEAF, STALK, SHADE, MIST};

    private static final int THRUST = 0;
    private static final int SLASH = 1;

    private final Bamboo config;
    private final JavaPlugin plugin;

    public basicAttack(Bamboo config, JavaPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    public void attack(Player player) {
        UUID uuid = player.getUniqueId();
        boolean reinforced = config.reloaded.getOrDefault(uuid, false);
        int mode = mode(player);
        int count = combo(uuid, mode);
        double rate = Math.min(config.basic_Combo_Max, 1.0 + config.basic_Combo_Step * (count - 1));

        if (mode == THRUST) {
            thrust(player, config.thrust_Damage * rate, count, reinforced ? 2 : 1);
        } else {
            boolean rising = !config.slashRising.getOrDefault(uuid, false);
            config.slashRising.put(uuid, rising);
            double damage = reinforced ? config.spin_Damage : config.slash_Damage;
            slash(player, damage * rate, count, reinforced, rising);
        }
    }

    private int mode(Player player) {
        Input input = player.getCurrentInput();
        if (input.isForward() && !input.isBackward()) return THRUST;
        if (input.isBackward() && !input.isForward()) return SLASH;
        Integer last = config.comboMode.get(player.getUniqueId());
        return last == null || last == SLASH ? THRUST : SLASH;
    }

    private int combo(UUID uuid, int mode) {
        long now = System.currentTimeMillis();
        Integer lastMode = config.comboMode.get(uuid);
        Long lastTime = config.comboTime.get(uuid);

        boolean keep = lastMode != null && lastMode == mode
                && lastTime != null && now - lastTime <= config.basic_Combo_Reset
                && config.comboHit.contains(uuid);

        int count = keep ? config.comboCount.getOrDefault(uuid, 1) + 1 : 1;

        config.comboMode.put(uuid, mode);
        config.comboCount.put(uuid, count);
        config.comboTime.put(uuid, now);
        config.comboHit.remove(uuid);
        return count;
    }

    private void thrust(Player player, double damage, int count, int remaining) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        double limit = config.thrust_Reach;
        RayTraceResult block = world.rayTraceBlocks(eye, dir, limit, FluidCollisionMode.NEVER, true);
        if (block != null) limit = block.getHitPosition().distance(eye.toVector());

        RayTraceResult hit = world.rayTraceEntities(eye, dir, limit, config.thrust_Width, e -> isTarget(player, e));
        double length = hit != null ? hit.getHitPosition().distance(eye.toVector()) : limit;

        Vector right = right(dir);
        Location start = eye.clone().add(right.multiply(0.22)).add(0, -0.28, 0);
        playThrust(world, start, eye.clone().add(dir.clone().multiply(length)), count);

        float pitch = 0.06f * count;
        world.playSound(eye, Sound.ITEM_TRIDENT_THROW, 0.8f, 1.45f + pitch);
        world.playSound(eye, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.6f, 1.3f + pitch);
        world.playSound(eye, Sound.BLOCK_BAMBOO_HIT, 0.9f, 1.6f + pitch);
        world.playSound(eye, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.35f, 1.8f);
        comboChime(world, eye, count);

        if (hit != null && hit.getHitEntity() instanceof LivingEntity target) {
            Vector push = dir.clone().setY(0);
            if (push.lengthSquared() > 1e-6) push.normalize().multiply(config.thrust_Knockback);
            push.setY(0.08);
            strike(player, target, damage * reachRate(length, config.thrust_Reach), push);
        }

        if (remaining > 1) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || player.isDead()) return;
                    thrust(player, damage, count, remaining - 1);
                }
            }.runTaskLater(plugin, config.thrust_Interval);
        }
    }

    private void slash(Player player, double damage, int count, boolean spin, boolean rising) {
        World world = player.getWorld();
        Location base = player.getLocation();

        Vector flat = base.getDirection().setY(0);
        if (flat.lengthSquared() < 1e-6) flat = new Vector(0, 0, 1);
        flat.normalize();

        Vector right = right(flat);
        double tilt = Math.toRadians(config.slash_Tilt);
        Vector side = right.clone().multiply(Math.cos(tilt))
                .add(new Vector(0, rising ? Math.sin(tilt) : -Math.sin(tilt), 0));

        double half = Math.toRadians((spin ? config.spin_Angle : config.slash_Angle) / 2.0);
        double from = rising ? -half : half;
        double to = rising ? half : -half;
        Location pivot = base.clone().add(0, 1.1, 0);

        playArc(world, pivot, flat, side, from, to, config.slash_Radius, spin ? 5 : 3);

        float pitch = 0.06f * count;
        world.playSound(pivot, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, (spin ? 0.85f : 1.15f) + pitch);
        world.playSound(pivot, Sound.BLOCK_BAMBOO_BREAK, 0.7f, 1.4f + pitch);
        world.playSound(pivot, Sound.ITEM_TRIDENT_THROW, 0.45f, (spin ? 0.9f : 1.25f) + pitch);
        if (spin) {
            world.playSound(pivot, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.6f, 1.5f);
            world.playSound(pivot, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.6f, 0.9f);
        }
        comboChime(world, pivot, count);

        double knockback = spin ? config.spin_Knockback : config.slash_Knockback;
        for (Entity entity : world.getNearbyEntities(pivot, config.slash_Radius + 1.0, 2.2, config.slash_Radius + 1.0)) {
            if (!isTarget(player, entity)) continue;
            LivingEntity target = (LivingEntity) entity;
            if (!inArc(target, pivot, flat, half, config.slash_Radius)) continue;
            if (!player.hasLineOfSight(target)) continue;

            Vector push = target.getLocation().toVector().subtract(base.toVector()).setY(0);
            double distance = Math.max(0.0, push.length() - target.getBoundingBox().getWidthX() / 2.0);
            if (push.lengthSquared() > 1e-6) push.normalize().multiply(knockback);
            push.setY(0.12);
            strike(player, target, damage * reachRate(distance, config.slash_Radius), push);
        }
    }

    private double reachRate(double distance, double reach) {
        double t = Math.max(0.0, Math.min(1.0, distance / reach));
        return config.basic_Close_Ratio + (1.0 - config.basic_Close_Ratio) * t;
    }

    private boolean inArc(LivingEntity target, Location pivot, Vector flat, double half, double radius) {
        BoundingBox box = target.getBoundingBox();
        Vector to = box.getCenter().subtract(pivot.toVector());
        double dy = to.getY();
        to.setY(0);

        double halfWidth = box.getWidthX() / 2.0;
        double distance = to.length();
        if (distance > radius + halfWidth) return false;
        if (Math.abs(dy) > 1.4 + box.getHeight() / 2.0) return false;
        if (distance <= halfWidth + 0.3) return true;

        double allowance = Math.asin(Math.min(1.0, halfWidth / distance));
        return to.angle(flat) - allowance <= half;
    }

    private void strike(Player player, LivingEntity target, double damage, Vector push) {
        double before = target.getHealth() + target.getAbsorptionAmount();

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();
        new ForceDamage(target, damage, source, true).applyEffect(player);

        boolean landed = target.isDead() || target.getHealth() + target.getAbsorptionAmount() < before - 1e-6;
        if (!landed) return;

        config.comboHit.add(player.getUniqueId());
        if (!target.isDead()) target.setVelocity(push);

        World world = target.getWorld();
        Location center = target.getBoundingBox().getCenter().toLocation(world);
        world.spawnParticle(Particle.DUST, center, 18, 0.25, 0.35, 0.25, 0, LEAF);
        world.spawnParticle(Particle.DUST, center, 10, 0.3, 0.4, 0.3, 0, EDGE);
        world.spawnParticle(Particle.CRIT, center, 10, 0.2, 0.25, 0.2, 0.3);
        world.spawnParticle(Particle.COMPOSTER, center, 6, 0.3, 0.35, 0.3, 0);
        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.7f, 1.3f);
        world.playSound(center, Sound.BLOCK_BAMBOO_HIT, 0.8f, 0.9f);
    }

    private void comboChime(World world, Location at, int count) {
        if (count < 2) return;
        float pitch = (float) Math.min(2.0, 0.8 + 0.2 * count);
        world.playSound(at, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.45f, pitch);
        if (count >= 6) world.playSound(at, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
    }

    public boolean isTarget(Player player, Entity entity) {
        if (!(entity instanceof LivingEntity) || entity == player || entity.isDead() || entity.isInvulnerable()) return false;
        if (entity instanceof ArmorStand || modelManager.isTracked(entity)) return false;
        if (entity.getScoreboardTags().contains("bamboo_projectile")) return false;
        return !(entity instanceof Player p && p.getGameMode() == GameMode.SPECTATOR);
    }

    private Vector right(Vector forward) {
        Vector flat = forward.clone().setY(0);
        if (flat.lengthSquared() < 1e-6) return new Vector(1, 0, 0);
        flat.normalize();
        return new Vector(-flat.getZ(), 0, flat.getX());
    }

    private void playThrust(World world, Location start, Location tip, int count) {
        Vector line = tip.toVector().subtract(start.toVector());
        double length = line.length();
        if (length < 1e-3) return;
        Vector dir = line.multiply(1.0 / length);
        Vector side = right(dir);
        Vector up = side.getCrossProduct(dir).normalize();

        new BukkitRunnable() {
            int tick = 0;
            double drawn = 0;

            @Override
            public void run() {
                tick++;
                double target = length * Math.min(1.0, tick / 2.0);
                for (double d = drawn; d <= target; d += 0.05) {
                    Location p = start.clone().add(dir.clone().multiply(d));
                    world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, d > target - 0.6 ? EDGE : LEAF);

                    double spin = d * 9.0;
                    Vector ring = side.clone().multiply(Math.cos(spin) * 0.09).add(up.clone().multiply(Math.sin(spin) * 0.09));
                    world.spawnParticle(Particle.DUST, p.clone().add(ring), 1, 0, 0, 0, 0, STALK);
                    world.spawnParticle(Particle.DUST, p.clone().subtract(ring), 1, 0, 0, 0, 0, SHADE);
                    if (((int) (d / 0.05)) % 6 == 0) {
                        world.spawnParticle(Particle.DUST, p, 1, 0.06, 0.06, 0.06, 0, MIST);
                    }
                }
                drawn = target;

                if (tick >= 2) {
                    world.spawnParticle(Particle.DUST, tip, 10 + 2 * count, 0.12, 0.12, 0.12, 0, LEAF);
                    world.spawnParticle(Particle.DUST, tip, 6 + count, 0.08, 0.08, 0.08, 0, EDGE);
                    world.spawnParticle(Particle.CRIT, tip, 6 + count, 0.08, 0.08, 0.08, 0.18);
                    world.spawnParticle(Particle.COMPOSTER, tip, 4, 0.1, 0.1, 0.1, 0);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void playArc(World world, Location pivot, Vector flat, Vector side, double from, double to, double radius, int ticks) {
        new BukkitRunnable() {
            int tick = 0;
            double drawn = from;

            @Override
            public void run() {
                tick++;
                double target = from + (to - from) * tick / ticks;
                drawArc(world, pivot, flat, side, drawn, target, radius);
                drawn = target;
                if (tick >= ticks) cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawArc(World world, Location pivot, Vector flat, Vector side, double a0, double a1, double radius) {
        int steps = Math.max(2, (int) Math.ceil(Math.abs(a1 - a0) * radius / 0.1));
        for (int i = 0; i <= steps; i++) {
            double a = a0 + (a1 - a0) * i / steps;
            Vector dir = flat.clone().multiply(Math.cos(a)).add(side.clone().multiply(Math.sin(a)));

            for (int layer = 0; layer < ARC_LAYERS.length; layer++) {
                if (layer >= 3 && i % 3 != 0) continue;
                double spread = layer < 3 ? 0 : 0.012 * layer;
                world.spawnParticle(Particle.DUST, pivot.clone().add(dir.clone().multiply(radius - ARC_LAYERS[layer])),
                        1, spread, spread, spread, 0, ARC_COLORS[layer]);
            }
            if (i % 10 == 0) {
                world.spawnParticle(Particle.COMPOSTER, pivot.clone().add(dir.clone().multiply(radius - 0.2)), 1, 0.05, 0.05, 0.05, 0);
            }
        }
    }
}