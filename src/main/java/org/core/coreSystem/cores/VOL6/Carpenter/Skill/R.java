package org.core.coreSystem.cores.VOL6.Carpenter.Skill;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL6.Carpenter.Passive.basicAttack;
import org.core.coreSystem.cores.VOL6.Carpenter.coreSystem.Carpenter;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.effectPlugin.swingEchoGuard;

import java.util.HashSet;
import java.util.UUID;

public class R implements SkillBase {

    private static final Particle.DustOptions STEEL = new Particle.DustOptions(Color.fromRGB(214, 218, 224), 0.7f);
    private static final Particle.DustOptions SHADE = new Particle.DustOptions(Color.fromRGB(112, 118, 126), 0.55f);

    private final Carpenter config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final basicAttack attack;

    public R(Carpenter config, JavaPlugin plugin, Cool cool, basicAttack attack) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.attack = attack;
    }

    @Override
    public void Trigger(Player player) {
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();
        Location feet = player.getLocation();

        double yaw = Math.toRadians(feet.getYaw());
        Vector forward = new Vector(-Math.sin(yaw), 0, Math.cos(yaw)).normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX());

        Location pivot = feet.clone().add(0, 0.25, 0);
        double half = Math.toRadians(config.r_Angle / 2.0);
        double radius = config.r_Radius;

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_damage * (1 + amp);
        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        HashSet<Entity> damaged = new HashSet<>();
        config.r_damaged.put(uuid, damaged);

        swingEchoGuard.swingMainHand(player);
        world.playSound(feet, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 0.55f);
        world.playSound(feet, Sound.BLOCK_CHAIN_HIT, 0.9f, 1.1f);

        new BukkitRunnable() {
            int tick = 0;
            double angle = half;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    finish();
                    return;
                }

                tick++;
                double next = half - 2 * half * tick / config.r_Sweep_Ticks;
                sweep(world, pivot, forward, right, angle, next, radius);

                for (Entity entity : world.getNearbyEntities(pivot, radius + 1.0, config.r_Height + 1.0, radius + 1.0)) {
                    if (damaged.contains(entity) || !attack.isTarget(player, entity)) continue;
                    LivingEntity target = (LivingEntity) entity;
                    if (!crossed(target, pivot, forward, right, radius, angle, next)) continue;

                    damaged.add(target);
                    trip(player, target, damage, source);
                }

                angle = next;
                if (tick >= config.r_Sweep_Ticks) finish();
            }

            private void finish() {
                if (config.r_damaged.get(uuid) == damaged) config.r_damaged.remove(uuid);
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean crossed(LivingEntity target, Location pivot, Vector forward, Vector right, double radius, double from, double to) {
        double rise = target.getLocation().getY() - pivot.getY();
        if (rise < -1.2 || rise > config.r_Height) return false;

        BoundingBox box = target.getBoundingBox();
        Vector flat = box.getCenter().subtract(pivot.toVector()).setY(0);
        double halfWidth = box.getWidthX() / 2.0;
        double distance = flat.length();
        if (distance > radius + halfWidth) return false;
        if (distance <= halfWidth + 0.3) return true;

        double at = Math.atan2(flat.dot(right), flat.dot(forward));
        double allowance = Math.asin(Math.min(1.0, halfWidth / distance));
        return at - allowance <= from && at + allowance >= to;
    }

    private void trip(Player player, LivingEntity target, double damage, DamageSource source) {
        config.r_damaging.put(player.getUniqueId(), true);
        new ForceDamage(target, damage, source, true).applyEffect(player);
        config.r_damaging.remove(player.getUniqueId());

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, config.r_Slow_Ticks, config.r_Slow_Level, false, true, true));
        Vector velocity = target.getVelocity();
        target.setVelocity(new Vector(0, Math.min(velocity.getY(), 0), 0));

        World world = target.getWorld();
        Location foot = target.getLocation().add(0, 0.15, 0);
        Material ground = target.getLocation().clone().subtract(0, 0.1, 0).getBlock().getType();
        if (ground.isSolid()) {
            world.spawnParticle(Particle.BLOCK, foot, 12, 0.25, 0.05, 0.25, 0.1, ground.createBlockData());
        }
        world.spawnParticle(Particle.CRIT, foot, 6, 0.2, 0.1, 0.2, 0.2);
        world.spawnParticle(Particle.DUST, foot, 6, 0.2, 0.05, 0.2, 0, STEEL);
        world.playSound(foot, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 0.7f);
        world.playSound(foot, Sound.BLOCK_CHAIN_FALL, 0.8f, 1.3f);
    }

    private Location point(Location pivot, Vector forward, Vector right, double angle, double length) {
        return pivot.clone().add(forward.clone().multiply(Math.cos(angle) * length)).add(right.clone().multiply(Math.sin(angle) * length));
    }

    private void sweep(World world, Location pivot, Vector forward, Vector right, double from, double to, double radius) {
        int steps = Math.max(2, (int) Math.ceil(Math.abs(from - to) * radius / 0.12));
        for (int i = 0; i <= steps; i++) {
            double a = from + (to - from) * i / steps;
            world.spawnParticle(Particle.DUST, point(pivot, forward, right, a, radius), 1, 0, 0, 0, 0, STEEL);
            if (i % 2 == 0) {
                world.spawnParticle(Particle.DUST, point(pivot, forward, right, a, radius - 0.2), 1, 0, 0, 0, 0, SHADE);
            }
            if (i % 4 == 0) {
                world.spawnParticle(Particle.SWEEP_ATTACK, point(pivot, forward, right, a, radius - 0.6).add(0, 0.1, 0), 1, 0, 0, 0, 0);
            }
        }

        Vector tangent = forward.clone().multiply(-Math.sin(to)).add(right.clone().multiply(Math.cos(to))).normalize();
        Vector inward = forward.clone().multiply(-Math.cos(to)).add(right.clone().multiply(-Math.sin(to))).normalize();
        Location tip = point(pivot, forward, right, to, radius);
        for (int i = 1; i <= 6; i++) {
            double c = Math.PI * i / 6;
            Location curl = tip.clone()
                    .add(tangent.clone().multiply(-Math.sin(c) * 0.28))
                    .add(inward.clone().multiply((1 - Math.cos(c)) * 0.28));
            world.spawnParticle(Particle.DUST, curl, 1, 0, 0, 0, 0, i == 6 ? SHADE : STEEL);
        }
    }
}