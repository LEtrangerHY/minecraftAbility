package org.core.coreSystem.cores.VOL5.Apocalypse.Skill;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL5.Apocalypse.coreSystem.Apocalypse;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;
import org.core.effect.crowdControl.Stiff;
import org.core.effect.crowdControl.Stun;
import org.core.effect.effectPlugin.swingEchoGuard;

import java.util.*;

public class R implements SkillBase {

    private final Apocalypse config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final NamespacedKey keyR;

    private final Map<UUID, BukkitTask> windows = new HashMap<>();
    private final Set<UUID> hadInvisibility = new HashSet<>();

    static final Color CUT_CORE = Color.fromRGB(234, 226, 210);
    static final Color CUT_FADE = Color.fromRGB(190, 166, 124);
    static final Color CUT_SHADOW = Color.fromRGB(88, 70, 54);
    private static final Particle.DustOptions ARC_DUST = new Particle.DustOptions(Color.fromRGB(220, 208, 186), 0.6f);
    private static final Particle.DustOptions CUT_AFTER = new Particle.DustOptions(Color.fromRGB(214, 200, 174), 0.5f);

    public R(Apocalypse config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.keyR = new NamespacedKey(plugin, "R");
    }

    @Override
    public void Trigger(Player player) {
        if (isWindowOpen(player)) {
            judgementCut(player);
        } else {
            blink(player);
        }
    }

    public boolean isWindowOpen(Player player) {
        return windows.containsKey(player.getUniqueId());
    }

    private void blink(Player player) {
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        swing(player);

        Location from = player.getLocation();
        world.spawnParticle(Particle.LARGE_SMOKE, from.clone().add(0, 1, 0), 12, 0.25, 0.5, 0.25, 0.02);
        world.spawnParticle(Particle.GUST, from.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
        world.playSound(from, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.7f, 1.25f);
        world.playSound(from, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.6f);

        teleportForward(player);

        Location to = player.getLocation();
        world.spawnParticle(Particle.SMOKE, to.clone().add(0, 1, 0), 10, 0.2, 0.4, 0.2, 0.01);

        new Stiff(player, config.r_Skill_duration).applyEffect(player);
        new Invulnerable(player, config.r_Skill_duration).applyEffect(player);
        hide(player);

        cool.updateCooldown(player, "R", config.r_reuse_Delay);

        long ticks = Math.max(1L, config.r_Skill_duration / 50L);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) {
                windows.remove(uuid);
                hadInvisibility.remove(uuid);
                return;
            }
            endWindow(p, true);
        }, ticks);

        windows.put(uuid, task);
    }

    private void teleportForward(Player player) {
        World world = player.getWorld();

        Location eyeLoc = player.getEyeLocation();
        Location feetLoc = player.getLocation();
        Vector direction = eyeLoc.getDirection().normalize();
        double eyeHeight = player.getEyeHeight();

        double safeDistance = -1;

        for (double d = config.r_Skill_teleport; d >= 0; d -= 0.2) {
            double x = eyeLoc.getX() + direction.getX() * d;
            double y = eyeLoc.getY() + direction.getY() * d;
            double z = eyeLoc.getZ() + direction.getZ() * d;

            Location checkHeadLoc = new Location(world, x, y, z);
            Location checkFeetLoc = new Location(world, x, y - eyeHeight, z);

            Block headBlock = checkHeadLoc.getBlock();
            Block feetBlock = checkFeetLoc.getBlock();

            if (!headBlock.isPassable()) continue;

            boolean isFeetSafe = false;
            if (feetBlock.isPassable()) {
                isFeetSafe = true;
            } else {
                BoundingBox bb = feetBlock.getBoundingBox();
                if (checkFeetLoc.getY() >= bb.getMaxY() - 0.05) isFeetSafe = true;
            }

            if (isFeetSafe) {
                safeDistance = d;
                break;
            }
        }

        double moveDistance = (safeDistance == -1) ? 0 : safeDistance;
        if (moveDistance <= 0) return;

        Location targetFeetLoc = eyeLoc.clone().add(direction.clone().multiply(moveDistance)).subtract(0, eyeHeight, 0);

        Block feetBlock = targetFeetLoc.getBlock();
        if (feetBlock.getType().isSolid()) {
            BoundingBox bb = feetBlock.getBoundingBox();
            if (targetFeetLoc.getY() < bb.getMaxY()) targetFeetLoc.setY(bb.getMaxY());
        } else {
            Block belowBlock = targetFeetLoc.clone().subtract(0, 0.05, 0).getBlock();
            if (belowBlock.getType().isSolid()) {
                BoundingBox bb = belowBlock.getBoundingBox();
                if (targetFeetLoc.getY() < bb.getMaxY()) targetFeetLoc.setY(bb.getMaxY());
            }
        }

        targetFeetLoc.setYaw(feetLoc.getYaw());
        targetFeetLoc.setPitch(feetLoc.getPitch());

        player.teleport(targetFeetLoc);
    }

    public void endWindow(Player player, boolean applyCooldown) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = windows.remove(uuid);
        if (task == null) return;

        task.cancel();

        reveal(player);
        Stiff.breakStiff(player);
        if (Invulnerable.isInvulnerable(player)) {
            new Invulnerable(player, 0L).removeEffect(player);
        }

        if (applyCooldown) {
            cool.updateCooldown(player, "R", config.r_Skill_Cool);
        }
    }

    public void forceCleanup(Player player) {
        endWindow(player, false);
    }

    private void hide(Player player) {
        UUID uuid = player.getUniqueId();

        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            hadInvisibility.add(uuid);
        } else {
            hadInvisibility.remove(uuid);
            int ticks = (int) Math.max(1L, config.r_Skill_duration / 50L);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, ticks, 0, false, false));
        }

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) viewer.hidePlayer(plugin, player);
        }
    }

    private void reveal(Player player) {
        UUID uuid = player.getUniqueId();

        if (!hadInvisibility.remove(uuid)) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) viewer.showPlayer(plugin, player);
        }
    }

    public void hideFrom(Player viewer) {
        for (UUID uuid : windows.keySet()) {
            Player hidden = Bukkit.getPlayer(uuid);
            if (hidden != null && !hidden.equals(viewer)) viewer.hidePlayer(plugin, hidden);
        }
    }

    private void judgementCut(Player player) {
        endWindow(player, false);
        swing(player);

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ITEM_MACE_SMASH_AIR, 0.7f, 1.5f);
        Location origin = player.getEyeLocation();

        Vector forward = origin.getDirection().normalize();
        double yawRad = Math.toRadians(origin.getYaw());
        Vector right = new Vector(-Math.cos(yawRad), 0, -Math.sin(yawRad)).normalize();
        Vector normal = right.clone().crossProduct(forward).normalize();

        double radius = config.r_Skill_radius;
        double halfAngle = Math.toRadians(config.r_Skill_angle / 2.0);
        double reach = radius + config.r_Skill_heightBase + config.r_Skill_heightGrow + 2.0;

        long level = player.getPersistentDataContainer().getOrDefault(keyR, PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_damage * (1 + config.r_Skill_amp * level);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        List<LivingEntity> hits = new ArrayList<>();

        for (Entity entity : world.getNearbyEntities(origin, reach, reach, reach)) {
            if (!(entity instanceof LivingEntity target)) continue;
            if (entity.equals(player) || target.isDead() || entity instanceof ArmorStand) continue;
            if (entity instanceof Player p && p.getGameMode() == GameMode.SPECTATOR) continue;

            if (isInsideCut(origin, forward, right, normal, halfAngle, radius, target.getBoundingBox())) {
                hits.add(target);
            }
        }

        for (LivingEntity target : hits) {
            config.skillDamaging.add(player.getUniqueId());
            try {
                new ForceDamage(target, damage, source, true).applyEffect(player);
            } finally {
                config.skillDamaging.remove(player.getUniqueId());
            }
            new Stun(target, config.r_Skill_stun).applyEffect(target);
        }

        playCutEffect(player, origin, forward, right, normal, halfAngle, radius, hits);
    }

    private boolean isInsideCut(Location origin, Vector forward, Vector right, Vector normal,
                                double halfAngle, double radius, BoundingBox box) {
        Vector v = box.getCenter().subtract(origin.toVector());

        double f = v.dot(forward);
        double r = v.dot(right);
        double n = v.dot(normal);

        double planeDist = Math.sqrt(f * f + r * r);
        double halfWidth = box.getWidthX() / 2.0;
        double halfHeight = box.getHeight() / 2.0;

        if (planeDist - halfWidth > radius) return false;

        if (planeDist > 0.8) {
            double angle = Math.atan2(Math.abs(r), f);
            double slack = Math.atan2(halfWidth, planeDist);
            if (angle > halfAngle + slack) return false;
        }

        double t = Math.max(0.0, Math.min(1.0, planeDist / radius));
        double band = config.r_Skill_heightBase + config.r_Skill_heightGrow * t;
        double ny = Math.abs(normal.getY());
        double bodySlack = halfHeight * ny + halfWidth * Math.sqrt(Math.max(0.0, 1.0 - ny * ny));

        return Math.abs(n) <= band + bodySlack;
    }

    private Location planePoint(Location origin, Vector forward, Vector right, double dist, double angle) {
        return origin.clone()
                .add(forward.clone().multiply(dist * Math.cos(angle)))
                .add(right.clone().multiply(dist * Math.sin(angle)));
    }

    private void playCutEffect(Player player, Location origin, Vector forward, Vector right, Vector normal,
                               double halfAngle, double radius, List<LivingEntity> hits) {
        World world = player.getWorld();
        Random random = new Random();

        world.playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.75f);
        world.playSound(origin, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 0.7f);
        world.playSound(origin, Sound.ENTITY_BREEZE_WIND_BURST, 0.7f, 1.3f);

        Location fx = origin.clone().subtract(0, 0.6, 0);

        List<CutLine> lines = new ArrayList<>();

        for (LivingEntity target : hits) {
            Vector v = target.getBoundingBox().getCenter().subtract(origin.toVector());
            double f = v.dot(forward);
            double r = v.dot(right);
            double dist = Math.sqrt(f * f + r * r);
            double angle = Math.atan2(r, f);
            CutLine line = createLine(planePoint(fx, forward, right, dist, angle), dist,
                    forward, right, normal, 2.8 + random.nextDouble() * 0.8, target, random);
            line.primary = lines.isEmpty();
            lines.add(line);
        }

        int rows = 5;
        int cols = 3;
        double minDist = 2.0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double dist = minDist + (row + 0.2 + 0.6 * random.nextDouble()) / rows * (radius - minDist);
                double angle = -halfAngle + (col + 0.2 + 0.6 * random.nextDouble()) / cols * (halfAngle * 2);
                lines.add(createLine(planePoint(fx, forward, right, dist, angle), dist,
                        forward, right, normal, 2.4 + random.nextDouble() * 1.4, null, random));
            }
        }

        int waveTicks = 5;

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick < waveTicks) {
                    double front = radius * (tick + 1) / waveTicks;
                    drawWaveArc(world, fx, forward, right, halfAngle, front);

                    for (CutLine line : lines) {
                        if (line.frame < 0 && (line.dist <= front || tick == waveTicks - 1)) line.frame = 0;
                    }
                }

                boolean active = false;
                for (CutLine line : lines) {
                    if (line.frame < 0 || line.frame > 4) {
                        if (line.frame < 0) active = true;
                        continue;
                    }
                    playLineFrame(world, line, random);
                    line.frame++;
                    active = true;
                }

                tick++;
                if (!active || tick > waveTicks + 8) cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private CutLine createLine(Location center, double dist, Vector forward, Vector right, Vector normal,
                               double length, LivingEntity target, Random random) {
        double phi = random.nextDouble() * Math.PI;
        double tilt = (random.nextDouble() - 0.5) * 0.5;
        Vector dir = forward.clone().multiply(Math.cos(phi))
                .add(right.clone().multiply(Math.sin(phi)))
                .add(normal.clone().multiply(tilt))
                .normalize();

        Location shifted = center.clone().add(normal.clone().multiply((random.nextDouble() - 0.5) * 0.3));

        Vector inPlaneSide = dir.clone().crossProduct(normal);
        if (inPlaneSide.lengthSquared() < 1e-6) inPlaneSide = right.clone();
        inPlaneSide.normalize().multiply(random.nextBoolean() ? 1 : -1);

        Vector bend = inPlaneSide.add(normal.clone().multiply((random.nextDouble() - 0.5) * 0.8));
        bend.subtract(dir.clone().multiply(bend.dot(dir))).normalize();
        bend.multiply(length * (0.10 + 0.10 * random.nextDouble()));

        return new CutLine(shifted, dist, dir, length, bend, target);
    }

    private void playLineFrame(World world, CutLine line, Random random) {
        switch (line.frame) {
            case 0 -> {
                drawCutSegment(world, line, -0.5, -0.5 + 0.45, 0.6f);
                world.spawnParticle(Particle.CRIT, line.point(-0.05), 2, 0.03, 0.03, 0.03, 0.05);
            }
            case 1 -> {
                drawCutSegment(world, line, -0.5, 0.5, 1.0f);
                world.spawnParticle(Particle.SWEEP_ATTACK, line.point(0), 1, 0, 0, 0, 0);
                world.playSound(line.center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.55f, 1.3f + random.nextFloat() * 0.4f);
            }
            case 2 -> drawCutSegment(world, line, -0.5, 0.5, 0.75f);
            case 3 -> {
                drawCutSegment(world, line, -0.35, 0.35, 0.4f);
                world.spawnParticle(Particle.CRIT, line.point(0), 4, 0.15, 0.15, 0.15, 0.25);

                if (line.target != null) {
                    Location body = line.target.getBoundingBox().getCenter().toLocation(world);
                    world.spawnParticle(Particle.CRIT, body, 10, 0.3, 0.4, 0.3, 0.3);
                    world.spawnParticle(Particle.SMALL_GUST, body, 1, 0.1, 0.2, 0.1, 0);
                    float vol = line.primary ? 1.0f : 0.6f;
                    world.playSound(body, Sound.ENTITY_PLAYER_ATTACK_CRIT, vol, 1.05f);
                    world.playSound(body, Sound.ENTITY_PLAYER_ATTACK_STRONG, vol * 0.9f, 0.8f);
                    world.playSound(body, Sound.ENTITY_PLAYER_ATTACK_SWEEP, vol * 0.7f, 1.2f);
                    if (line.primary) world.playSound(body, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.8f, 0.9f);
                }
            }
            case 4 -> {
                for (int i = 0; i < 4; i++) {
                    Location p = line.point(random.nextDouble() - 0.5);
                    world.spawnParticle(Particle.DUST, p, 1, 0.03, 0.03, 0.03, 0, CUT_AFTER);
                }
                if (random.nextDouble() < 0.35) {
                    world.spawnParticle(Particle.SMALL_GUST, line.point(0), 1, 0.2, 0.1, 0.2, 0);
                }
            }
            default -> {
            }
        }
    }

    private void drawCutSegment(World world, CutLine line, double from, double to, float intensity) {
        Vector side = line.dir.clone().crossProduct(new Vector(0, 1, 0));
        if (side.lengthSquared() < 1e-6) side = new Vector(1, 0, 0);
        side.normalize().multiply(0.07);

        double step = 0.08 / line.length;
        int index = 0;
        for (double s = from; s <= to + 1e-6; s += step, index++) {
            double edge = 1.0 - Math.abs(s) * 2.0;
            if (edge < 0) continue;

            float size = (float) ((0.3 + 0.9 * edge) * intensity);
            Location p = line.point(s);

            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, p, 1, 0, 0, 0, 0,
                    new Particle.DustTransition(CUT_CORE, CUT_FADE, Math.max(0.2f, size)));

            if (index % 2 == 0 && edge > 0.15 && intensity > 0.5f) {
                world.spawnParticle(Particle.DUST, p.clone().add(side), 1, 0, 0, 0, 0,
                        new Particle.DustOptions(CUT_SHADOW, (float) (0.25 + 0.3 * edge)));
            }
        }
    }

    private void drawWaveArc(World world, Location origin, Vector forward, Vector right, double halfAngle, double dist) {
        double step = 0.4 / Math.max(1.0, dist);
        for (double a = -halfAngle; a <= halfAngle + 1e-6; a += step) {
            world.spawnParticle(Particle.DUST, planePoint(origin, forward, right, dist, a), 1, 0, 0, 0, 0, ARC_DUST);
        }
    }

    private static final class CutLine {
        final Location center;
        final double dist;
        final Vector dir;
        final double length;
        final Vector bend;
        final LivingEntity target;
        int frame = -1;
        boolean primary;

        CutLine(Location center, double dist, Vector dir, double length, Vector bend, LivingEntity target) {
            this.center = center;
            this.dist = dist;
            this.dir = dir;
            this.length = length;
            this.bend = bend;
            this.target = target;
        }

        Location point(double s) {
            double curve = 1.0 - 4.0 * s * s;
            return center.clone()
                    .add(dir.clone().multiply(s * length))
                    .add(bend.clone().multiply(curve));
        }
    }

    private void swing(Player player) {
        if (player.getInventory().getItemInMainHand().getType() == Apocalypse.SWORD) {
            swingEchoGuard.swingMainHand(player);
        } else {
            swingEchoGuard.swingOffHand(player);
        }
    }
}