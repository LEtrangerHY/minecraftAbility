package org.core.coreSystem.cores.VOL5.Apocalypse.Skill;

import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL5.Apocalypse.coreSystem.Apocalypse;
import org.core.effect.crowdControl.Invulnerable;
import org.core.effect.effectPlugin.swingEchoGuard;

import java.util.ArrayList;
import java.util.List;

public class Q implements SkillBase {

    private final Apocalypse config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final R r;
    private final BatCombo combo;
    private final NamespacedKey keyQ;

    private static final BatCombo.Palette Q_PALETTE = new BatCombo.Palette(
            new Particle.DustOptions(Color.fromRGB(205, 200, 192), 0.7f),
            new Particle.DustOptions(Color.fromRGB(172, 170, 166), 1.0f),
            new Particle.DustOptions(Color.fromRGB(140, 126, 112), 1.35f),
            new Particle.DustOptions(Color.fromRGB(168, 124, 82), 1.8f),
            new Particle.DustOptions(Color.fromRGB(212, 210, 206), 0.56f));

    private static final double[] Q_KEYS = {0.0, 0.05, 0.52, 0.98, 1.08, 1.0};

    public Q(Apocalypse config, JavaPlugin plugin, Cool cool, R r, BatCombo combo) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.r = r;
        this.combo = combo;
        this.keyQ = new NamespacedKey(plugin, "Q");
    }

    @Override
    public void Trigger(Player player) {
        r.endWindow(player, true);
        new Invulnerable(player, config.q_Skill_invulnerable).applyEffect(player);
        swingEchoGuard.swingMainHand(player);

        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        double yawRad = Math.toRadians(eye.getYaw());
        Vector right = new Vector(-Math.cos(yawRad), 0, -Math.sin(yawRad)).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        LivingEntity designated = findDesignated(player);

        Location base = eye.clone().subtract(0, 0.4, 0);
        Location start = base.clone().add(forward.clone().multiply(config.q_Skill_offset));
        Vector path = predictDash(player, forward);

        combo.swingWhoosh(player.getWorld(), player.getLocation(), true);

        combo.grantQ(player);

        combo.playSwingTrail(player, progress -> start.clone().add(path.clone().multiply(dashEase(progress))),
                forward, right, up, config.q_Skill_tilt, config.q_Skill_sweep, false, config.q_Skill_length,
                Q_PALETTE, Q_KEYS, true,
                () -> strike(player, designated, base, forward, right, up, path));
    }

    private Vector predictDash(Player player, Vector forward) {
        Vector flat = forward.clone().setY(0);
        if (flat.lengthSquared() < 1e-6) return new Vector();
        Vector dir = flat.normalize();

        double speed = config.q_Skill_dash;
        Location feet = player.getLocation();
        boolean grounded = !feet.clone().subtract(0, 0.05, 0).getBlock().isPassable();
        double drag = grounded ? 0.546 : 0.91;
        int ticks = Q_KEYS.length - 1;
        double distance = speed * (1 - Math.pow(drag, ticks)) / (1 - drag);
        if (player.isSprinting()) distance += 0.5;

        Location chest = feet.clone().add(0, 0.5, 0);
        RayTraceResult wall = player.getWorld().rayTraceBlocks(chest, dir, distance + 0.4, FluidCollisionMode.NEVER, true);
        if (wall != null) {
            distance = Math.max(0, Math.min(distance, wall.getHitPosition().distance(chest.toVector()) - 0.4));
        }

        Vector push = dir.clone().multiply(speed);
        player.setVelocity(new Vector(push.getX(), player.getVelocity().getY(), push.getZ()));
        return dir.multiply(distance);
    }

    private static double dashEase(double progress) {
        double t = Math.max(0.0, Math.min(1.0, progress));
        return 1 - (1 - t) * (1 - t);
    }

    private LivingEntity findDesignated(Player player) {
        Location eye = player.getEyeLocation();
        RayTraceResult result = player.getWorld().rayTraceEntities(eye, eye.getDirection(),
                config.q_Skill_range, 0.4, entity -> combo.isValidTarget(player, entity));
        if (result == null || !(result.getHitEntity() instanceof LivingEntity target)) return null;
        return target;
    }

    private void strike(Player player, LivingEntity designated, Location base, Vector forward,
                        Vector right, Vector up, Vector path) {
        if (!player.isOnline() || player.isDead()) return;

        World world = player.getWorld();
        double reached = dashEase(Q_KEYS[BatCombo.impactTick(Q_KEYS) + 1]);
        Location center = base.clone().add(forward.clone().multiply(config.q_Skill_offset * 0.5))
                .add(path.clone().multiply(reached));

        double tilt = Math.toRadians(config.q_Skill_tilt);
        Vector slant = right.clone().multiply(Math.cos(tilt)).add(up.clone().multiply(Math.sin(tilt))).normalize();
        Vector normal = forward.clone().crossProduct(slant).normalize();
        Vector force = slant.clone().multiply(0.75).add(forward.clone().multiply(0.65)).normalize();

        long level = player.getPersistentDataContainer().getOrDefault(keyQ, PersistentDataType.LONG, 0L);
        double amp = 1 + config.q_Skill_amp * level;

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        List<LivingEntity> victims = new ArrayList<>();
        LivingEntity loudest = null;
        double reach = config.q_Skill_reach;
        for (Entity entity : world.getNearbyEntities(center, reach + 1.5, reach + 1.5, reach + 1.5)) {
            if (!combo.isValidTarget(player, entity)) continue;
            LivingEntity target = (LivingEntity) entity;
            if (target.equals(designated)) continue;

            BoundingBox box = target.getBoundingBox();
            Vector v = box.getCenter().subtract(center.toVector());
            double dist = v.length();
            double halfWidth = box.getWidthX() / 2.0;
            if (dist - halfWidth > reach) continue;
            if (dist > 0.6 && v.clone().normalize().dot(forward) < Math.cos(Math.toRadians(85))) continue;
            if (Math.abs(v.dot(normal)) > config.q_Skill_band + halfWidth + box.getHeight() * 0.3) continue;

            combo.skillHit(player, target, config.q_Skill_damage * amp, source);
            combo.hitVisual(target, BatCombo.NORMAL, force, false);
            victims.add(target);
            if (loudest == null) loudest = target;
        }

        if (designated != null && designated.isValid() && !designated.isDead()
                && designated.getWorld().equals(world)
                && designated.getLocation().distance(player.getLocation()) <= config.q_Skill_range + 1.0) {
            combo.skillHit(player, designated, config.q_Skill_designatedDamage * amp, source);
            combo.hitVisual(designated, BatCombo.MAX_HIT, force, false);
            impactSpray(world, designated.getBoundingBox().getCenter().toLocation(world), force);
            victims.add(0, designated);
            combo.impactSound(designated, BatCombo.MAX_HIT);
        } else if (loudest != null) {
            combo.impactSound(loudest, BatCombo.NORMAL);
        }

        if (victims.isEmpty()) {
            combo.airBurst(world, center, false);
        } else {
            combo.lowHealthPress(victims, true);
        }
    }

    private void impactSpray(World world, Location body, Vector force) {
        for (int i = 0; i < 10; i++) {
            Vector dir = force.clone().add(new Vector(
                    (Math.random() - 0.5) * 0.7, (Math.random() - 0.5) * 0.7, (Math.random() - 0.5) * 0.7)).normalize();
            world.spawnParticle(Particle.CRIT, body, 0, dir.getX(), dir.getY(), dir.getZ(), 0.5 + Math.random() * 0.4);
        }
        for (double d = 0.2; d <= 1.4; d += 0.15) {
            Location at = body.clone().add(force.clone().multiply(d));
            Color color = d < 0.8 ? Q_PALETTE.core().getColor() : Q_PALETTE.edge().getColor();
            world.spawnParticle(Particle.DUST, at, 1, 0.04, 0.04, 0.04, 0, new Particle.DustOptions(color, 1.0f));
        }
    }
}