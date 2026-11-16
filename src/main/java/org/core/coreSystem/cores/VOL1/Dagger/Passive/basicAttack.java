package org.core.coreSystem.cores.VOL1.Dagger.Passive;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.core.coreSystem.cores.VOL1.Dagger.coreSystem.Dagger;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.effectPlugin.swingEchoGuard;
import org.core.models.absModelSystem.modelManager;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class basicAttack {

    private static final Particle.DustOptions EDGE = new Particle.DustOptions(Color.fromRGB(226, 252, 252), 0.5f);
    private static final Particle.DustOptions CORE = new Particle.DustOptions(Color.fromRGB(96, 206, 214), 0.45f);
    private static final Particle.DustOptions DEEP = new Particle.DustOptions(Color.fromRGB(28, 104, 120), 0.4f);
    private static final Particle.DustOptions MIST = new Particle.DustOptions(Color.fromRGB(64, 150, 164), 0.35f);
    private static final double[] LAYERS = {0.0, 0.09, 0.2};
    private static final Particle.DustOptions[] COLORS = {EDGE, CORE, DEEP};
    private static final double[] INNER = {0.4, 0.7, 1.05, 1.45};
    private static final int[] INNER_GAP = {2, 3, 4, 5};
    private static final Particle.DustOptions[] INNER_COLORS = {CORE, DEEP, MIST, MIST};

    private final Dagger config;
    private final JavaPlugin plugin;

    public basicAttack(Dagger config, JavaPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    public void attack(Player player) {
        UUID uuid = player.getUniqueId();
        LivingEntity aimed = aim(player);
        boolean dual = isDual(player);
        int stage = stage(uuid, aimed, dual);
        double damage = config.basic_Total_Damage.get(stage - 1) / stage;
        UUID reference = config.basic_Target.get(uuid);

        slash(player, aimed, reference, damage, stage);
        for (int i = 1; i < stage; i++) {
            boolean offHand = dual && i % 2 == 1;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || player.isDead()) return;
                    if (offHand) swingEchoGuard.swingOffHand(player);
                    slash(player, aimed, reference, damage, stage);
                }
            }.runTaskLater(plugin, i);
        }
    }

    public boolean isDual(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.ECHO_SHARD
                && player.getInventory().getItemInOffHand().getType() == Material.ECHO_SHARD;
    }

    private int next(int last, boolean dual) {
        if (!dual) return Math.min(config.basic_Max_Stage, last + 1);
        for (int stage : config.basic_Dual_Stage) {
            if (stage > last) return stage;
        }
        return config.basic_Dual_Stage.getLast();
    }

    private int stage(UUID uuid, LivingEntity aimed, boolean dual) {
        long now = System.currentTimeMillis();
        int last = config.basic_Stage.getOrDefault(uuid, 0);
        Long time = config.basic_Time.get(uuid);
        UUID reference = config.basic_Target.get(uuid);

        boolean alive = time != null && now - time <= config.basic_Reset && config.basic_Hit.contains(uuid);
        boolean switched = aimed != null && reference != null && !aimed.getUniqueId().equals(reference);

        int stage = (alive && !switched) ? next(last, dual) : (dual ? config.basic_Dual_Stage.getFirst() : 1);
        if (aimed != null) config.basic_Target.put(uuid, aimed.getUniqueId());

        config.basic_Stage.put(uuid, stage);
        config.basic_Time.put(uuid, now);
        config.basic_Hit.remove(uuid);
        return stage;
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

    private void slash(Player player, LivingEntity aimed, UUID reference, double damage, int stage) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();

        Vector right = new Vector(-forward.getZ(), 0, forward.getX());
        if (right.lengthSquared() < 1e-6) right = new Vector(1, 0, 0);
        right.normalize();

        Location pivot = eye.clone().add(0, -0.15, 0);
        Vector axis = forward;
        double radius = config.basic_Reach;

        if (aimed != null && !aimed.isDead() && player.hasLineOfSight(aimed)) {
            Vector center = aimed.getLocation().add(0, aimed.getHeight() * config.basic_Anchor_Height, 0).toVector();
            Vector toTarget = center.clone().subtract(pivot.toVector());
            if (toTarget.length() >= 0.5) {
                axis = toTarget.normalize();
                radius = config.basic_Reach - config.basic_Anchor_Inset;
                pivot = center.clone().subtract(axis.clone().multiply(radius)).toLocation(world);
            }
        }

        Vector flatSide = right.clone().subtract(axis.clone().multiply(right.dot(axis)));
        if (flatSide.lengthSquared() < 1e-6) flatSide = right.clone();
        flatSide.normalize();
        Vector up = flatSide.getCrossProduct(axis).normalize();
        if (up.getY() < 0) up.multiply(-1);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double tilt = Math.toRadians(random.nextDouble(-config.basic_Tilt, config.basic_Tilt));
        Vector side = flatSide.clone().multiply(Math.cos(tilt)).add(up.clone().multiply(Math.sin(tilt))).normalize();
        Vector normal = axis.getCrossProduct(side).normalize();

        double half = Math.toRadians(config.basic_Angle / 2.0);
        boolean reverse = random.nextBoolean();

        playArc(world, pivot, axis, side, reverse ? half : -half, reverse ? -half : half, radius);

        world.playSound(eye, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, (float) random.nextDouble(1.75, 2.0));
        world.playSound(eye, Sound.ENTITY_ARROW_SHOOT, 0.3f, (float) random.nextDouble(1.8, 2.0));
        if (stage > 1) world.playSound(eye, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.25f, 1.4f + 0.15f * stage);

        for (Entity entity : world.getNearbyEntities(pivot, radius + 1.0, radius + 1.0, radius + 1.0)) {
            if (!isTarget(player, entity)) continue;
            LivingEntity target = (LivingEntity) entity;

            double rate;
            if (target == aimed) {
                if (target.getBoundingBox().getCenter().distance(eye.toVector()) > config.basic_Reach + target.getBoundingBox().getWidthX()) continue;
                rate = 1.0;
            } else {
                double angle = arcAngle(target, pivot, axis, side, normal, half, radius);
                if (angle < 0 || !player.hasLineOfSight(target)) continue;
                rate = config.basic_Edge_Ratio + (1.0 - config.basic_Edge_Ratio) * (1.0 - Math.min(1.0, angle / half));
            }

            if (strike(player, target, damage * rate) && target.getUniqueId().equals(reference != null ? reference : aimed != null ? aimed.getUniqueId() : null)) {
                config.basic_Hit.add(player.getUniqueId());
            }
        }
    }

    private double arcAngle(LivingEntity target, Location pivot, Vector forward, Vector side, Vector normal, double half, double radius) {
        BoundingBox box = target.getBoundingBox();
        Vector to = box.getCenter().subtract(pivot.toVector());
        double halfWidth = box.getWidthX() / 2.0;

        double offPlane = to.dot(normal);
        if (Math.abs(offPlane) > config.basic_Thickness + box.getHeight() / 2.0) return -1;

        Vector inPlane = to.clone().subtract(normal.clone().multiply(offPlane));
        double distance = inPlane.length();
        if (distance > radius + halfWidth) return -1;
        if (distance <= halfWidth + 0.3) return 0;

        double angle = Math.abs(Math.atan2(inPlane.dot(side), inPlane.dot(forward)));
        double allowance = Math.asin(Math.min(1.0, halfWidth / distance));
        double offset = Math.max(0.0, angle - allowance);
        return offset <= half ? offset : -1;
    }

    private boolean strike(Player player, LivingEntity target, double damage) {
        double before = target.getHealth() + target.getAbsorptionAmount();

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();
        new ForceDamage(target, damage, source, true).applyEffect(player);

        boolean landed = target.isDead() || target.getHealth() + target.getAbsorptionAmount() < before - 1e-6;
        if (!landed) return false;

        World world = target.getWorld();
        Location center = target.getBoundingBox().getCenter().toLocation(world);
        world.spawnParticle(Particle.DUST, center, 6, 0.18, 0.25, 0.18, 0, EDGE);
        world.spawnParticle(Particle.DUST, center, 6, 0.22, 0.3, 0.22, 0, CORE);
        world.spawnParticle(Particle.CRIT, center, 4, 0.15, 0.2, 0.15, 0.25);
        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.45f, 1.7f);
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 1.9f);
        return true;
    }

    public boolean isTarget(Player player, Entity entity) {
        if (!(entity instanceof LivingEntity) || entity == player || entity.isDead() || entity.isInvulnerable()) return false;
        if (entity instanceof ArmorStand || modelManager.isTracked(entity)) return false;
        return !(entity instanceof Player p && p.getGameMode() == GameMode.SPECTATOR);
    }

    private void playArc(World world, Location pivot, Vector forward, Vector side, double from, double to, double radius) {
        new BukkitRunnable() {
            int tick = 0;
            double drawn = from;

            @Override
            public void run() {
                tick++;
                double target = from + (to - from) * tick / config.basic_Animation_Ticks;
                drawArc(world, pivot, forward, side, drawn, target, radius);
                drawn = target;
                if (tick >= config.basic_Animation_Ticks) cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawArc(World world, Location pivot, Vector forward, Vector side, double a0, double a1, double radius) {
        int steps = Math.max(2, (int) Math.ceil(Math.abs(a1 - a0) * radius / 0.09));
        for (int i = 0; i <= steps; i++) {
            double a = a0 + (a1 - a0) * i / steps;
            Vector dir = forward.clone().multiply(Math.cos(a)).add(side.clone().multiply(Math.sin(a)));
            for (int layer = 0; layer < LAYERS.length; layer++) {
                if (layer == LAYERS.length - 1 && i % 2 == 1) continue;
                world.spawnParticle(Particle.DUST, pivot.clone().add(dir.clone().multiply(radius - LAYERS[layer])), 1, 0, 0, 0, 0, COLORS[layer]);
            }
            for (int layer = 0; layer < INNER.length; layer++) {
                if (i % INNER_GAP[layer] != 0) continue;
                double spread = 0.02 + 0.015 * layer;
                world.spawnParticle(Particle.DUST, pivot.clone().add(dir.clone().multiply(radius - INNER[layer])),
                        1, spread, spread, spread, 0, INNER_COLORS[layer]);
            }
        }
    }
}