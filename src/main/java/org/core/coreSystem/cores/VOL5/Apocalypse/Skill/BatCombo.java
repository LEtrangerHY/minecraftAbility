package org.core.coreSystem.cores.VOL5.Apocalypse.Skill;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.coreSystem.cores.VOL5.Apocalypse.coreSystem.Apocalypse;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;
import org.core.effect.crowdControl.Stun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.DoubleFunction;

public class BatCombo {

    public static final int FIRST = 1;
    public static final int SECOND = 2;
    public static final int EMPOWERED = 3;
    public static final int Q_EMPOWERED = 4;

    private final Apocalypse config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Map<UUID, String> barKeys = new HashMap<>();

    private static final String BAR_SECOND = "Combo - 2nd Hit";
    private static final String BAR_CRUSH = "Combo - Crush";
    private static final String BAR_RUIN = "Combo - Ruin";

    static final Palette WOOD = new Palette(
            new Particle.DustOptions(Color.fromRGB(232, 226, 214), 0.5f),
            new Particle.DustOptions(Color.fromRGB(220, 210, 192), 0.75f),
            new Particle.DustOptions(Color.fromRGB(222, 206, 176), 1.0f),
            new Particle.DustOptions(Color.fromRGB(202, 176, 130), 1.25f),
            new Particle.DustOptions(Color.fromRGB(216, 208, 194), 0.48f));

    static final Palette DEEP_WOOD = new Palette(
            new Particle.DustOptions(Color.fromRGB(230, 224, 212), 0.6f),
            new Particle.DustOptions(Color.fromRGB(218, 206, 186), 0.9f),
            new Particle.DustOptions(Color.fromRGB(216, 198, 166), 1.15f),
            new Particle.DustOptions(Color.fromRGB(188, 158, 112), 1.45f),
            new Particle.DustOptions(Color.fromRGB(214, 206, 192), 0.52f));

    static final Palette BLOODY = new Palette(
            new Particle.DustOptions(Color.fromRGB(230, 224, 212), 0.6f),
            new Particle.DustOptions(Color.fromRGB(218, 206, 186), 0.9f),
            new Particle.DustOptions(Color.fromRGB(216, 198, 166), 1.15f),
            new Particle.DustOptions(Color.fromRGB(174, 34, 30), 1.45f),
            new Particle.DustOptions(Color.fromRGB(214, 206, 192), 0.52f));

    private static final Color BLOOD = Color.fromRGB(168, 22, 26);

    private static final Particle.DustOptions GHOST = new Particle.DustOptions(Color.fromRGB(214, 208, 196), 0.5f);
    private static final Particle.DustOptions GHOST_TIP = new Particle.DustOptions(Color.fromRGB(220, 212, 198), 0.6f);
    private static final Particle.DustOptions STREAK = new Particle.DustOptions(Color.fromRGB(196, 166, 116), 0.85f);
    private static final Particle.DustOptions STREAK_RUST = new Particle.DustOptions(BLOOD, 0.75f);
    private static final Particle.DustOptions STREAK_HEAD = new Particle.DustOptions(Color.fromRGB(226, 220, 208), 0.7f);

    private static final double[][] SECTION = {
            {0.0, 0.0, 0},
            {0.15, 0.11, 1}, {0.15, -0.11, 1},
            {0.3, 0.0, 1},
            {0.45, 0.09, 2}, {0.45, -0.09, 2},
            {0.6, 0.0, 2}
    };

    static final double[] SWING_KEYS = {0.0, 0.05, 0.2, 0.52, 0.86, 1.06, 1.0};
    static final double[] HEAVY_KEYS = {0.0, 0.12, 0.46, 0.86, 1.05, 1.0};

    public static final int EDGE = 0;
    public static final int NORMAL = 1;
    public static final int HEAVY_HIT = 2;
    public static final int MAX_HIT = 3;
    private static final double[] SPIN_KEYS = {0.0, 0.2, 0.44, 0.68, 0.9, 1.03, 1.0};

    private static final double SWING_SWEEP = 80;
    private static final double SPIN_FRONT = 100;
    private static final double SPIN_START = -60;
    private static final double SPIN_TILT = 14;
    private static final double FRONT_HI = -270;
    private static final double FRONT_LO = -360 - Math.toDegrees(Math.acos(-0.05));

    public BatCombo(Apocalypse config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    private int stage(UUID id) {
        Long last = config.combo_lastHit.get(id);
        if (last == null || System.currentTimeMillis() - last > config.combo_reset) {
            config.combo_stage.remove(id);
            return FIRST;
        }
        return config.combo_stage.getOrDefault(id, FIRST);
    }

    private int qLevel(UUID id) {
        Long until = config.q_empower_until.get(id);
        if (until == null || System.currentTimeMillis() > until) {
            config.q_empower.remove(id);
            config.q_empower_until.remove(id);
            return 0;
        }
        return config.q_empower.getOrDefault(id, 0);
    }

    public int pending(Player player) {
        UUID id = player.getUniqueId();
        int q = qLevel(id);
        if (q >= 2) return Q_EMPOWERED;
        if (q == 1) return EMPOWERED;
        return Math.min(stage(id), EMPOWERED);
    }

    public void grantQ(Player player) {
        UUID id = player.getUniqueId();
        int level = pending(player) >= EMPOWERED ? 2 : 1;
        config.q_empower.put(id, level);
        config.q_empower_until.put(id, System.currentTimeMillis() + config.q_empower_duration);
        showBar(player, level == 2 ? BAR_RUIN : BAR_CRUSH, config.q_empower_duration);

        World world = player.getWorld();
        Location chest = player.getLocation().add(0, 1.2, 0);
        world.spawnParticle(Particle.CRIT, chest, level == 2 ? 14 : 7, 0.3, 0.3, 0.3, 0.12);
    }

    private void consume(UUID id) {
        config.combo_stage.remove(id);
        config.q_empower.remove(id);
        config.q_empower_until.remove(id);
    }

    private void showBar(Player player, String key, long duration) {
        String old = barKeys.put(player.getUniqueId(), key);
        if (old != null && !old.equals(key)) cool.updateCooldown(player, old, 0L);
        cool.setCooldown(player, duration, key, "boss");
    }

    public void clearBar(Player player) {
        String old = barKeys.remove(player.getUniqueId());
        if (old != null) cool.updateCooldown(player, old, 0L);
    }

    public void onSwing(Player player) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long busy = config.combo_busyUntil.get(id);
        if (busy != null && now < busy) return;

        double interval = 1000.0 / config.bat_attack_speed;
        Long last = config.combo_lastSwing.get(id);
        double c = last == null ? 1.0 : Math.min(1.0, (now - last) / interval);
        double charge = 0.2 + 0.8 * c * c;
        config.combo_lastSwing.put(id, now);
        config.combo_busyUntil.put(id, now + 400);

        int mode = pending(player);
        if (mode >= EMPOWERED) {
            consume(id);
            clearBar(player);
            new Invulnerable(player, config.empower_invulnerable).applyEffect(player);
            spin(player, mode == Q_EMPOWERED, charge);
        } else {
            swing(player, mode, charge);
        }
    }

    private void release(UUID id) {
        config.combo_busyUntil.put(id, System.currentTimeMillis() + 50);
    }

    private void resetCombo(UUID id) {
        config.combo_stage.remove(id);
        config.combo_lastHit.remove(id);
    }

    private double damageAt(double weight) {
        return config.bat_damage_min + (config.bat_damage_max - config.bat_damage_min) * weight;
    }

    private LivingEntity aimed(Player player, Location eye, Vector forward, double reach) {
        RayTraceResult result = player.getWorld().rayTrace(eye, forward, reach, FluidCollisionMode.NEVER, true, 0.25,
                entity -> isValidTarget(player, entity));
        if (result == null || !(result.getHitEntity() instanceof LivingEntity target)) return null;
        return target;
    }

    private static DamageSource source(Player player) {
        return DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();
    }

    private void swing(Player player, int mode, double charge) {
        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        double yawRad = Math.toRadians(eye.getYaw());
        Vector right = new Vector(-Math.cos(yawRad), 0, -Math.sin(yawRad)).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        double tiltDeg = mode == FIRST ? 62 : 32;
        double drop = mode == FIRST ? 0.4 : 0.7;
        Location origin = eye.clone().subtract(0, drop, 0);
        Location pivot = origin.clone().add(forward.clone().multiply(0.6));
        LivingEntity target = aimed(player, eye, forward, config.bat_reach);

        swingWhoosh(player.getWorld(), eye, false);

        playSwingTrail(player, progress -> pivot, forward, right, up, tiltDeg, SWING_SWEEP, mode != FIRST,
                config.bat_reach - 0.6, WOOD, SWING_KEYS, false,
                () -> resolveSwing(player, mode, charge, origin, forward, right, up, tiltDeg, target));
    }

    private void resolveSwing(Player player, int mode, double charge, Location origin, Vector forward,
                              Vector right, Vector up, double tiltDeg, LivingEntity target) {
        UUID id = player.getUniqueId();
        double tilt = Math.toRadians(tiltDeg);
        Vector slant = right.clone().multiply(Math.cos(tilt)).add(up.clone().multiply(Math.sin(tilt))).normalize();
        Vector normal = forward.clone().crossProduct(slant).normalize();

        List<Hit> hits = collect(player, origin, forward, slant, normal, Math.toRadians(SWING_SWEEP),
                config.bat_reach, config.bat_band, target);

        Vector facing = forward.clone().setY(0);
        if (facing.lengthSquared() < 1e-6) facing = new Vector(-Math.sin(Math.toRadians(player.getLocation().getYaw())), 0,
                Math.cos(Math.toRadians(player.getLocation().getYaw())));
        facing.normalize();

        DamageSource source = source(player);
        boolean centerHit = false;
        Hit primary = null;
        for (Hit hit : hits) {
            LivingEntity victim = hit.target();
            double ratio = damageAt(hit.weight()) / config.bat_damage_max;
            double vy = victim.getVelocity().getY();
            boolean ground = victim.isOnGround();

            skillHit(player, victim, damageAt(hit.weight()) * charge, source);

            double strength = config.bat_vanilla_knockback * ratio;
            victim.setVelocity(new Vector(facing.getX() * strength,
                    ground ? 0.36 * (0.5 + 0.5 * ratio) : vy, facing.getZ() * strength));

            hitVisual(victim, hit.center() ? NORMAL : EDGE, facing, false);
            if (hit.center()) centerHit = true;
            if (primary == null || hit.weight() > primary.weight()) primary = hit;
        }
        if (primary != null) {
            impactSound(primary.target(), centerHit ? NORMAL : EDGE);
            lowHealthPress(victims(hits), false);
        }

        if (centerHit) {
            config.combo_stage.put(id, mode + 1);
            config.combo_lastHit.put(id, System.currentTimeMillis());
            showBar(player, mode + 1 >= EMPOWERED ? BAR_CRUSH : BAR_SECOND, config.combo_reset);
        } else {
            resetCombo(id);
            clearBar(player);
            if (hits.isEmpty()) whiffTail(player.getWorld(), origin);
        }
        release(id);
    }

    private List<Hit> collect(Player player, Location origin, Vector forward, Vector slant, Vector normal,
                              double maxAngle, double reach, double band, LivingEntity aimed) {
        List<Hit> hits = new ArrayList<>();
        World world = origin.getWorld();
        double centerAngle = Math.toRadians(config.bat_center_angle);

        for (Entity entity : world.getNearbyEntities(origin, reach + 2, reach + 2, reach + 2)) {
            if (!isValidTarget(player, entity)) continue;
            LivingEntity target = (LivingEntity) entity;
            boolean isAimed = target.equals(aimed);

            BoundingBox box = target.getBoundingBox();
            Vector v = box.getCenter().subtract(origin.toVector());
            double half = box.getWidthX() / 2.0;
            double along = v.dot(forward);
            double side = v.dot(slant);
            double off = v.dot(normal);
            double planar = Math.sqrt(along * along + side * side);

            double angle = Math.abs(Math.atan2(side, along));
            double slack = planar > 1e-3 ? Math.atan(half / planar) : Math.PI;
            double eff = Math.max(0, angle - slack);

            if (!isAimed) {
                if (planar - half > reach) continue;
                if (Math.abs(off) > band + half + box.getHeight() * 0.3) continue;
                if (eff > maxAngle) continue;
                if (!player.hasLineOfSight(target)) continue;
            }

            boolean center = isAimed || eff <= centerAngle;
            double sigma = Math.toRadians(config.bat_damage_sigma);
            double weight = isAimed ? 1.0 : Math.exp(-(eff * eff) / (2 * sigma * sigma));
            hits.add(new Hit(target, weight, center));
        }
        return hits;
    }

    void swingWhoosh(World world, Location at, boolean heavy) {
        world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.4f, heavy ? 0.48f : 0.55f);
        world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.4f, heavy ? 0.45f : 0.5f);
        world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_SWEEP, heavy ? 1.0f : 0.8f, heavy ? 0.7f : 0.8f);
    }

    private void whiffTail(World world, Location at) {
        world.playSound(at, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.45f, 0.55f);
        world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.45f);
    }

    void airBurst(World world, Location at, boolean strong) {
        world.playSound(at, Sound.ENTITY_WIND_CHARGE_WIND_BURST, strong ? 1.0f : 0.85f, strong ? 0.68f : 0.85f);
        world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.5f);
    }

    void impactSound(LivingEntity primary, int power) {
        World world = primary.getWorld();
        Location at = primary.getBoundingBox().getCenter().toLocation(world);

        switch (power) {
            case MAX_HIT -> {
                world.playSound(at, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 0.6f);
                world.playSound(at, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 0.95f, 1.0f);
                world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.58f);
                world.playSound(at, Sound.ENTITY_GOAT_RAM_IMPACT, 1.0f, 0.68f);
                later(1, () -> {
                    world.playSound(at, Sound.BLOCK_BONE_BLOCK_BREAK, 1.0f, 0.68f);
                    world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.72f);
                });
                later(2, () -> world.playSound(at, Sound.ENTITY_GENERIC_BIG_FALL, 0.8f, 0.6f));
            }
            case HEAVY_HIT -> {
                world.playSound(at, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 0.72f);
                world.playSound(at, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 0.7f, 1.22f);
                world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.68f);
                world.playSound(at, Sound.ENTITY_GOAT_RAM_IMPACT, 1.0f, 0.82f);
                later(1, () -> {
                    world.playSound(at, Sound.BLOCK_BONE_BLOCK_BREAK, 1.0f, 0.78f);
                    world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.82f);
                });
                later(2, () -> world.playSound(at, Sound.ENTITY_GENERIC_BIG_FALL, 0.6f, 0.7f));
            }
            case NORMAL -> {
                world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.82f);
                world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.7f);
                world.playSound(at, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.6f, 1.1f);
                world.playSound(at, Sound.ENTITY_GOAT_RAM_IMPACT, 0.55f, 1.25f);
                later(1, () -> {
                    world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.85f, 0.95f);
                    world.playSound(at, Sound.BLOCK_BONE_BLOCK_HIT, 1.0f, 0.85f);
                });
            }
            default -> {
                world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.9f, 0.95f);
                world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.55f, 1.05f);
                world.playSound(at, Sound.BLOCK_BONE_BLOCK_HIT, 0.4f, 1.1f);
            }
        }
    }

    private void later(long ticks, Runnable task) {
        Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
    }

    void hitVisual(LivingEntity target, int power, Vector dir, boolean rust) {
        World world = target.getWorld();
        Location body = target.getBoundingBox().getCenter().toLocation(world);

        switch (power) {
            case MAX_HIT -> {
                world.spawnParticle(Particle.CRIT, body, 16, 0.3, 0.4, 0.3, 0.4);
                impactStreak(world, body, dir, 1.4, rust);
            }
            case HEAVY_HIT -> {
                world.spawnParticle(Particle.CRIT, body, 10, 0.25, 0.35, 0.25, 0.3);
                impactStreak(world, body, dir, 1.0, rust);
            }
            case NORMAL -> {
                world.spawnParticle(Particle.CRIT, body, 6, 0.25, 0.35, 0.25, 0.25);
                impactStreak(world, body, dir, 0.7, rust);
            }
            default -> world.spawnParticle(Particle.CRIT, body, 3, 0.25, 0.35, 0.25, 0.2);
        }
    }

    void lowHealthPress(List<LivingEntity> victims, boolean heavy) {
        for (LivingEntity victim : victims) {
            if (!isLowHealth(victim)) continue;
            Location body = victim.getBoundingBox().getCenter().toLocation(victim.getWorld());
            pressThud(victim.getWorld(), body, heavy);
            return;
        }
    }

    private void impactStreak(World world, Location body, Vector dir, double length, boolean rust) {
        if (dir == null || dir.lengthSquared() < 1e-6) return;
        Vector d = dir.clone().normalize();
        for (double t = 0.15; t <= length + 1e-6; t += 0.12) {
            Location at = body.clone().add(d.clone().multiply(t));
            world.spawnParticle(Particle.DUST, at, 1, 0.01, 0.01, 0.01, 0, t < length * 0.35 ? STREAK_HEAD : (rust ? STREAK_RUST : STREAK));
        }
    }

    private boolean isLowHealth(LivingEntity target) {
        AttributeInstance max = target.getAttribute(Attribute.MAX_HEALTH);
        if (max == null || max.getValue() <= 0) return false;
        return target.getHealth() < max.getValue() * 0.25;
    }

    private void pressThud(World world, Location at, boolean heavy) {
        float pitch = heavy ? 0.9f : 1.0f;
        later(2, () -> pressPulse(world, at, 1.0f, pitch));
        later(5, () -> pressPulse(world, at, 0.75f, pitch * 0.88f));
    }

    private void pressPulse(World world, Location at, float vol, float pitch) {
        world.playSound(at, Sound.ENTITY_GENERIC_BIG_FALL, vol, 0.6f * pitch);
        world.playSound(at, Sound.BLOCK_MUD_HIT, vol, 0.5f * pitch);
        world.playSound(at, Sound.ENTITY_WARDEN_STEP, vol * 0.8f, 0.65f * pitch);
        world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_STRONG, vol * 0.6f, 0.45f * pitch);
    }

    private void spin(Player player, boolean qEmpowered, double charge) {
        World world = player.getWorld();
        Location base = player.getLocation();
        double yawRad = Math.toRadians(base.getYaw());
        Vector forward = new Vector(-Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();
        Vector right = new Vector(-Math.cos(yawRad), 0, -Math.sin(yawRad)).normalize();
        Vector up = new Vector(0, 1, 0);

        double tilt = Math.toRadians(SPIN_TILT);
        Vector slant = right.clone().multiply(Math.cos(tilt)).add(up.clone().multiply(Math.sin(tilt))).normalize();
        Vector normal = forward.clone().crossProduct(slant).normalize();

        Location center = base.clone().add(0, 1.05, 0);
        Location eye = player.getEyeLocation();
        LivingEntity aimedTarget = aimed(player, eye, eye.getDirection().normalize(),
                qEmpowered ? config.q_empower_length : config.empower_length);
        double length = qEmpowered ? config.q_empower_length : config.empower_length;
        double total = qEmpowered ? 430 : 410;
        Palette palette = qEmpowered ? BLOODY : DEEP_WOOD;
        double speckle = qEmpowered ? 0.28 : 0;
        double thick = qEmpowered ? 1.5 : 1.3;
        double size = qEmpowered ? 1.15 : 1.1;

        int impact = 0;
        double impactProgress = (360 + SPIN_START) / total;
        for (int i = 0; i < SPIN_KEYS.length - 1; i++) {
            if (SPIN_KEYS[i + 1] >= impactProgress) {
                impact = i;
                break;
            }
        }
        final int impactTick = impact;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= SPIN_KEYS.length - 1 || !player.isOnline() || player.isDead()) {
                    release(player.getUniqueId());
                    cancel();
                    return;
                }

                double from = SPIN_KEYS[ticks];
                double to = SPIN_KEYS[ticks + 1];
                boolean recoil = to < from;

                double d0 = SPIN_START - total * from;
                double d1 = SPIN_START - total * to;
                double hi = Math.max(d0, d1);
                double lo = Math.min(d0, d1);
                double fHi = Math.min(hi, FRONT_HI);
                double fLo = Math.max(lo, FRONT_LO);

                if (fHi > fLo) {
                    if (recoil) {
                        drawStroke(world, a -> center, forward, slant, Math.toRadians(fLo), Math.toRadians(fHi),
                                length * 0.97, thick * 0.5, 0, palette, size * 0.8, 1.5, false, speckle * 0.5);
                    } else {
                        drawStroke(world, a -> center, forward, slant, Math.toRadians(fLo), Math.toRadians(fHi),
                                length, thick, length * 0.3, palette, size, 1.1, true, speckle);
                    }
                    if (hi > fHi) drawGhost(world, center, forward, slant, Math.toRadians(fHi), Math.toRadians(hi), length);
                    if (fLo > lo) drawGhost(world, center, forward, slant, Math.toRadians(lo), Math.toRadians(fLo), length);
                } else {
                    drawGhost(world, center, forward, slant, Math.toRadians(lo), Math.toRadians(hi), length);
                }

                if (!recoil) {
                    double rad = Math.toRadians(d1);
                    Vector dir = forward.clone().multiply(Math.cos(rad)).add(slant.clone().multiply(Math.sin(rad)));
                    Vector tangent = forward.clone().multiply(Math.sin(rad)).subtract(slant.clone().multiply(Math.cos(rad)));
                    headSparks(world, center.clone().add(dir.multiply(length)), tangent, 4);
                }

                if (ticks == 0) swingWhoosh(world, center, true);

                if (ticks == impactTick) {
                    spinImpact(player, center, forward, slant, normal, length, qEmpowered, charge, aimedTarget);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spinImpact(Player player, Location center, Vector forward, Vector slant, Vector normal,
                            double length, boolean qEmpowered, double charge, LivingEntity aimedTarget) {
        double multiplier = qEmpowered ? config.q_empower_multiplier : config.empower_multiplier;
        double knock = qEmpowered ? config.q_empower_knockback : config.empower_knockback;
        DamageSource source = source(player);

        List<Hit> hits = collect(player, center, forward, slant, normal, Math.toRadians(SPIN_FRONT),
                length, 1.3, aimedTarget);

        Hit primary = null;
        double centerDamage = 0;
        for (Hit hit : hits) {
            LivingEntity victim = hit.target();
            double ratio = damageAt(hit.weight()) / config.bat_damage_max;
            double vy = victim.getVelocity().getY();
            if (primary == null || hit.weight() > primary.weight()) primary = hit;

            double dealt = damageAt(hit.weight()) * multiplier * charge;
            skillHit(player, victim, dealt, source);
            if (hit.center()) centerDamage += dealt;

            Vector away = victim.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0);
            if (away.lengthSquared() < 1e-6) away = forward.clone().setY(0);
            if (away.lengthSquared() > 1e-6) {
                away.normalize();
                Vector push = away.clone().multiply(knock * ratio);
                victim.setVelocity(new Vector(push.getX(), Math.min(0.0, vy), push.getZ()));
                if (qEmpowered) watchWall(victim, away);
            }

            hitVisual(victim, hit.center() ? (qEmpowered ? MAX_HIT : HEAVY_HIT) : (qEmpowered ? NORMAL : EDGE), away, qEmpowered);
        }

        if (primary == null) {
            airBurst(player.getWorld(), center, qEmpowered);
        } else {
            impactSound(primary.target(), primary.center() ? (qEmpowered ? MAX_HIT : HEAVY_HIT) : (qEmpowered ? HEAVY_HIT : NORMAL));
            lowHealthPress(victims(hits), true);
        }

        if (centerDamage > 0) sustain(player, centerDamage, qEmpowered);

        release(player.getUniqueId());
    }

    private void sustain(Player player, double dealt, boolean qEmpowered) {
        AttributeInstance maxAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxAttr == null || player.isDead()) return;
        double max = maxAttr.getValue();

        double heal = Math.min(dealt * config.empower_heal_ratio, max * config.empower_heal_cap);
        player.setHealth(Math.min(max, player.getHealth() + heal));

        if (!qEmpowered) return;
        double shield = Math.min(dealt * config.q_empower_shield_ratio, max * config.q_empower_shield_cap);
        if (shield <= 0) return;
        int amplifier = Math.max(0, (int) Math.ceil(shield / 4.0) - 1);
        player.removePotionEffect(PotionEffectType.ABSORPTION);
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,
                (int) (config.q_empower_shield_duration / 50L), amplifier, false, false, true));
        player.setAbsorptionAmount(shield);
    }

    private void watchWall(LivingEntity target, Vector dir) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks > 12 || !target.isValid() || target.isDead()) {
                    cancel();
                    return;
                }

                Vector vel = target.getVelocity().setY(0);
                if (ticks > 4 && vel.lengthSquared() < 0.0025 && wallBlock(target, dir) == null) {
                    cancel();
                    return;
                }

                Block wall = wallBlock(target, dir);
                if (wall != null) {
                    cancel();
                    new Stun(target, config.q_empower_wallStun).applyEffect(target);

                    World world = target.getWorld();
                    Location body = target.getBoundingBox().getCenter().toLocation(world);
                    Location contact = body.clone().add(dir.clone().multiply(target.getBoundingBox().getWidthX() / 2.0));
                    world.spawnParticle(Particle.CRIT, body, 14, 0.3, 0.4, 0.3, 0.3);
                    world.spawnParticle(Particle.BLOCK, contact, 24, 0.25, 0.45, 0.25, 0.15, wall.getBlockData());
                    world.playSound(body, Sound.ENTITY_GOAT_RAM_IMPACT, 1.0f, 0.7f);
                    world.playSound(body, Sound.ENTITY_GENERIC_BIG_FALL, 0.9f, 0.6f);
                    world.playSound(contact, wall.getBlockData().getSoundGroup().getBreakSound(), 1.0f, 0.8f);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private Block wallBlock(LivingEntity target, Vector dir) {
        BoundingBox box = target.getBoundingBox();
        double reach = box.getWidthX() / 2.0 + 0.25;
        Location feet = target.getLocation();
        double[] heights = {0.2, Math.max(0.3, box.getHeight() * 0.8)};
        for (double h : heights) {
            Block block = feet.clone().add(dir.getX() * reach, h, dir.getZ() * reach).getBlock();
            if (!block.isPassable()) return block;
        }
        return null;
    }

    boolean isValidTarget(Player player, Entity entity) {
        if (!(entity instanceof LivingEntity target)) return false;
        if (entity.equals(player) || target.isDead() || entity instanceof ArmorStand) return false;
        return !(entity instanceof Player p && p.getGameMode() == GameMode.SPECTATOR);
    }

    void skillHit(Player player, LivingEntity target, double damage, DamageSource source) {
        config.skillDamaging.add(player.getUniqueId());
        try {
            new ForceDamage(target, damage, source, true).applyEffect(player);
        } finally {
            config.skillDamaging.remove(player.getUniqueId());
        }
    }

    static int impactTick(double[] keys) {
        for (int i = 0; i < keys.length - 1; i++) {
            if (keys[i + 1] >= 0.5) return i;
        }
        return keys.length - 2;
    }

    void playSwingTrail(Player player, PivotPath path, Vector forward, Vector right, Vector up,
                        double tiltDeg, double sweepDeg, boolean downward, double length, Palette palette,
                        double[] keys, boolean heavy, Runnable onImpact) {
        int impactTick = impactTick(keys);
        World world = player.getWorld();

        double tilt = Math.toRadians(tiltDeg);
        Vector slant = right.clone().multiply(Math.cos(tilt)).add(up.clone().multiply(Math.sin(tilt))).normalize();

        double maxAngle = Math.toRadians(sweepDeg);
        double thick = heavy ? 1.3 : 1.0;
        double size = heavy ? 1.1 : 1.0;
        DoubleFunction<Location> pivotOf = a -> path.at(progressAt(maxAngle, downward, a));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= keys.length - 1 || !player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                double from = keys[ticks];
                double to = keys[ticks + 1];
                boolean recoil = to < from;
                double a0 = angleAt(maxAngle, downward, from);
                double a1 = angleAt(maxAngle, downward, to);

                if (recoil) {
                    drawStroke(world, pivotOf, forward, slant, a0, a1, length * 0.97, thick * 0.5, 0,
                            palette, size * 0.8, 1.5, false, 0);
                } else {
                    double progress = Math.max(0.0, Math.min(1.0, (from + to) / 2.0));
                    drawStroke(world, pivotOf, forward, slant, a0, a1, length, thick * (0.55 + 0.45 * progress),
                            length * 0.3, palette, size, 1.0, heavy, 0);

                    Vector tangent = forward.clone().multiply(-Math.sin(a1)).add(slant.clone().multiply(Math.cos(a1)));
                    if (downward) tangent.multiply(-1);
                    Vector tipDir = forward.clone().multiply(Math.cos(a1)).add(slant.clone().multiply(Math.sin(a1)));
                    headSparks(world, pivotOf.apply(a1).clone().add(tipDir.multiply(length)), tangent, heavy ? 4 : 3);
                }

                if (ticks == impactTick) {
                    if (heavy) {
                        drawStroke(world, pivotOf, forward, slant, angleAt(maxAngle, downward, 0), a1,
                                length, 0.45, 0, palette, size * 1.05, 1.3, false, 0);
                    }
                    if (onImpact != null) onImpact.run();
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static double angleAt(double maxAngle, boolean downward, double t) {
        return -maxAngle + 2 * maxAngle * (downward ? 1.0 - t : t);
    }

    private static double progressAt(double maxAngle, boolean downward, double angle) {
        double t = (angle + maxAngle) / (2 * maxAngle);
        return downward ? 1.0 - t : t;
    }

    private static Location point(Location pivot, Vector forward, Vector slant, double angle, double radius) {
        return pivot.clone().add(forward.clone().multiply(Math.cos(angle) * radius)
                .add(slant.clone().multiply(Math.sin(angle) * radius)));
    }

    private void drawStroke(World world, DoubleFunction<Location> pivotOf, Vector forward, Vector slant, double a0, double a1,
                            double rim, double thick, double hazeFrom, Palette palette, double size,
                            double density, boolean sparks, double speckle) {
        double lo = Math.min(a0, a1);
        double hi = Math.max(a0, a1);
        Vector normal = forward.clone().crossProduct(slant).normalize();
        double strokeIn = rim - 0.6 * thick;

        if (hazeFrom > 0) {
            for (double r = hazeFrom + Math.random() * 0.15; r < strokeIn - 0.1; r += 0.22) {
                double step = 0.3 * density / r;
                for (double a = lo + Math.random() * step; a <= hi; a += step) {
                    if (Math.random() < 0.38) continue;
                    Location p = point(pivotOf.apply(a), forward, slant, a, r);
                    world.spawnParticle(Particle.DUST, p, 1, 0.06, 0.06, 0.06, 0, palette.haze());
                    if (speckle > 0 && Math.random() < speckle * 0.06) {
                        world.spawnParticle(Particle.DUST, p, 1, 0.05, 0.05, 0.05, 0, new Particle.DustOptions(BLOOD, (float) (0.45 * size)));
                    }
                }
            }
        }

        Particle.DustOptions[] cells = {
                new Particle.DustOptions(palette.core().getColor(), (float) (1.15 * size)),
                new Particle.DustOptions(palette.edge().getColor(), (float) size),
                new Particle.DustOptions(palette.inner().getColor(), (float) (0.85 * size))
        };
        Particle.DustOptions[] blood = {
                new Particle.DustOptions(BLOOD, (float) (0.75 * size)),
                new Particle.DustOptions(BLOOD, (float) (0.65 * size)),
                new Particle.DustOptions(BLOOD, (float) (0.55 * size))
        };
        double step = 0.1 * density / rim;
        for (double a = lo + Math.random() * step; a <= hi; a += step) {
            Location pivot = pivotOf.apply(a);
            Vector dir = forward.clone().multiply(Math.cos(a)).add(slant.clone().multiply(Math.sin(a)));
            for (double[] cell : SECTION) {
                if (cell[0] >= 0.6 && Math.random() < 0.5) continue;
                Location p = pivot.clone().add(dir.clone().multiply(rim - cell[0] * thick))
                        .add(normal.clone().multiply(cell[1] * thick));
                world.spawnParticle(Particle.DUST, p, 1, 0.02, 0.02, 0.02, 0, cells[(int) cell[2]]);
                if (speckle > 0 && Math.random() < speckle * Math.max(0.1, 1.0 - cell[0] * 1.5)) {
                    world.spawnParticle(Particle.DUST, p, 1, 0.03, 0.03, 0.03, 0, blood[(int) cell[2]]);
                }
            }
            if (sparks && Math.random() < 0.25) {
                world.spawnParticle(Particle.CRIT, pivot.clone().add(dir.clone().multiply(rim + 0.2)), 1, 0.04, 0.04, 0.04, 0.02);
            }
        }
    }

    private void drawGhost(World world, Location pivot, Vector forward, Vector slant, double a0, double a1, double rim) {
        double lo = Math.min(a0, a1);
        double hi = Math.max(a0, a1);
        for (double r = rim * 0.72; r <= rim + 1e-6; r += 0.14) {
            Particle.DustOptions opt = r > rim * 0.92 ? GHOST_TIP : GHOST;
            double step = 0.22 / r;
            for (double a = lo + Math.random() * step; a <= hi; a += step) {
                if (Math.random() < 0.45) continue;
                world.spawnParticle(Particle.DUST, point(pivot, forward, slant, a, r), 1, 0.01, 0.01, 0.01, 0, opt);
            }
        }
    }

    private void headSparks(World world, Location tip, Vector tangent, int count) {
        if (tangent.lengthSquared() < 1e-6) return;
        Vector dir = tangent.clone().normalize();
        for (int i = 0; i < count; i++) {
            Location at = tip.clone().subtract(dir.clone().multiply(0.12 * i));
            world.spawnParticle(Particle.CRIT, at, 0, dir.getX(), dir.getY(), dir.getZ(), 0.45);
        }
    }

    private static List<LivingEntity> victims(List<Hit> hits) {
        List<LivingEntity> list = new ArrayList<>();
        for (Hit hit : hits) list.add(hit.target());
        return list;
    }

    private record Hit(LivingEntity target, double weight, boolean center) {
    }

    @FunctionalInterface
    interface PivotPath {
        Location at(double progress);
    }

    record Palette(Particle.DustOptions faint, Particle.DustOptions inner,
                   Particle.DustOptions edge, Particle.DustOptions core, Particle.DustOptions haze) {
    }
}