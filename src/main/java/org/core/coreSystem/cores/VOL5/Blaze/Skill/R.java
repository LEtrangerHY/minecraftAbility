package org.core.coreSystem.cores.VOL5.Blaze.Skill;

import org.core.models.absModelSystem.modelManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL5.Blaze.Passive.BlueFlame;
import org.core.coreSystem.cores.VOL5.Blaze.coreSystem.Blaze;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class R implements SkillBase{
    private final Blaze config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final BlueFlame blueFlame;

    private final NamespacedKey meteorKey;

    private static final double TARGET_RANGE = 44.0;
    private static final double APEX_HEIGHT = 16.0;
    private static final double ZONE_RADIUS = 6.0;
    private static final int CHARGE_TICKS = 20;
    private static final int METEOR_COUNT = 16;
    private static final int METEOR_INTERVAL = 2;
    private static final double METEOR_SPEED = 1.3;
    private static final double GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5));

    private static final Particle.DustTransition ORB_DUST = new Particle.DustTransition(Color.fromRGB(200, 250, 255), Color.fromRGB(60, 170, 255), 1.6f);
    private static final Particle.DustOptions SPARKLE_DUST = new Particle.DustOptions(Color.fromRGB(235, 250, 255), 0.6f);
    private static final Particle.DustOptions ZONE_DUST = new Particle.DustOptions(Color.fromRGB(80, 200, 255), 1.0f);

    public R(Blaze config, JavaPlugin plugin, Cool cool, BlueFlame blueFlame) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.blueFlame = blueFlame;
        this.meteorKey = new NamespacedKey(plugin, "blazeMeteor");
    }

    @Override
    public void Trigger(Player player) {
        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

        if (!(offhandItem.getType() == Material.SOUL_LANTERN || ((offhandItem.getType() == Material.SOUL_SAND || offhandItem.getType() == Material.SOUL_SOIL) && offhandItem.getAmount() >= 7))) {
            fail(player, "soul sand needed");
            return;
        }

        LivingEntity target = findTarget(player);
        if (target == null) {
            fail(player, "not designated");
            return;
        }

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_PARROT_IMITATE_BLAZE, 1, 1);
        world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_BURN, 1, 1);
        world.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1, 0.7f);
        world.playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1, 0.7f);

        new BukkitRunnable() {
            int tick = 0;
            int launched = 0;
            Location lastTargetLoc = target.getLocation();
            Location center;
            Location apex;
            List<Location> impacts;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                if (tick < CHARGE_TICKS) {
                    if (target.isValid() && !target.isDead() && target.getWorld().equals(world)) {
                        lastTargetLoc = target.getLocation();
                    }
                    Location followApex = lastTargetLoc.clone().add(0, getApexHeight(world, lastTargetLoc), 0);
                    chargeEffect(world, player, lastTargetLoc, followApex, tick);
                } else {
                    if (center == null) {
                        center = lastTargetLoc.clone();
                        apex = center.clone().add(0, getApexHeight(world, center), 0);
                        impacts = buildImpacts(center);
                        world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);
                    }

                    int launchTick = tick - CHARGE_TICKS;
                    if (launchTick % 4 == 0) {
                        drawZone(world, center, launchTick);
                    }
                    if (launchTick % METEOR_INTERVAL == 0 && launched < METEOR_COUNT) {
                        launchMeteor(world, player, apex, impacts.get(launched));
                        launched++;
                    }
                    orbEffect(world, apex, 1.0 - (double) launched / METEOR_COUNT);

                    if (launched >= METEOR_COUNT) {
                        world.spawnParticle(Particle.SOUL, apex, 20, 0.6, 0.6, 0.6, 0.05);
                        world.playSound(apex, Sound.BLOCK_FIRE_EXTINGUISH, 1.5f, 0.6f);
                        cancel();
                        return;
                    }
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        if((offhandItem.getType() == Material.SOUL_SAND || offhandItem.getType() == Material.SOUL_SOIL) && offhandItem.getAmount() >= 7) {
            offhandItem.setAmount(offhandItem.getAmount() - 7);
        }
    }

    private void fail(Player player, String message) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1, 1);

        Title title = Title.title(
                Component.empty(),
                Component.text(message).color(NamedTextColor.RED),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
        );
        player.showTitle(title);

        long cools = 500L;
        cool.updateCooldown(player, "R", cools);
    }

    private LivingEntity findTarget(Player player) {
        Location eye = player.getEyeLocation();
        RayTraceResult result = player.getWorld().rayTrace(eye, eye.getDirection(), TARGET_RANGE,
                FluidCollisionMode.NEVER, true, 0.6,
                entity -> entity instanceof LivingEntity && entity != player && !(entity instanceof ArmorStand) && !entity.isDead());
        if (result == null || !(result.getHitEntity() instanceof LivingEntity living)) return null;
        return living;
    }

    private double getApexHeight(World world, Location center) {
        Location eye = center.clone().add(0, 1.8, 0);
        RayTraceResult hit = world.rayTraceBlocks(eye, new Vector(0, 1, 0), APEX_HEIGHT, FluidCollisionMode.NEVER, true);
        if (hit == null) return APEX_HEIGHT;
        return Math.max(3.0, hit.getHitPosition().getY() - center.getY() - 1.0);
    }

    private void chargeEffect(World world, Player player, Location center, Location apex, int tick) {
        double progress = (double) tick / CHARGE_TICKS;
        Location body = player.getLocation().add(0, 1, 0);

        double yaw = Math.toRadians(player.getLocation().getYaw());
        Vector flatForward = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector handSide = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
        if (player.getMainHand() == org.bukkit.inventory.MainHand.LEFT) handSide.multiply(-1);
        Location mouth = player.getLocation().add(0, 1.0, 0)
                .add(flatForward.multiply(0.45))
                .add(handSide.multiply(0.35));
        if (Math.random() < 0.3 + 0.5 * progress) {
            world.spawnParticle(Particle.END_ROD, mouth, 1, 0.08, 0.08, 0.08, 0.01);
        }
        if (tick % 3 == 0) {
            world.spawnParticle(Particle.WAX_OFF, mouth, 2, 0.12, 0.12, 0.12, 0);
        }
        world.spawnParticle(Particle.DUST, mouth, 1, 0.05, 0.05, 0.05, 0, SPARKLE_DUST);
        if (tick == CHARGE_TICKS / 2) {
            world.spawnParticle(Particle.END_ROD, mouth, 10, 0.05, 0.05, 0.05, 0.08);
            world.playSound(mouth, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.6f);
        }

        Location base = center.clone().add(0, 1, 0);
        Vector toApex = apex.toVector().subtract(base.toVector());
        double length = toApex.length();
        Vector step = toApex.clone().normalize();
        double reach = length * Math.min(1.0, progress * 1.6);
        for (double d = 0; d <= reach; d += 0.6) {
            if (Math.random() < 0.5) {
                Location p = base.clone().add(step.clone().multiply(d));
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 0, step.getX(), step.getY(), step.getZ(), 0.05);
            }
        }

        if (progress > 0.4) {
            orbEffect(world, apex, (progress - 0.4) / 0.6);
        }

        if (tick % 2 == 0) {
            drawZone(world, center, tick);
        }

        if (tick == CHARGE_TICKS / 2) {
            world.playSound(body, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.6f);
        }
        if (tick == CHARGE_TICKS - 1) {
            world.playSound(apex, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.5f);
            world.playSound(body, Sound.ENTITY_BLAZE_AMBIENT, 1.2f, 0.5f);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, apex, 60, 0.2, 0.2, 0.2, 0.35);
        }
    }

    private void drawZone(World world, Location center, int tick) {
        int points = 40;
        for (int i = 0; i < points; i++) {
            double theta = Math.PI * 2 * i / points + tick * 0.05;
            Location outer = center.clone().add(Math.cos(theta) * ZONE_RADIUS, 0.15, Math.sin(theta) * ZONE_RADIUS);
            world.spawnParticle(Particle.DUST, outer, 1, 0, 0, 0, 0, ZONE_DUST);
            if (i % 4 == 0) {
                Location inner = center.clone().add(Math.cos(-theta) * 1.0, 0.15, Math.sin(-theta) * 1.0);
                world.spawnParticle(Particle.DUST, inner, 1, 0, 0, 0, 0, ZONE_DUST);
            }
        }
    }

    private void orbEffect(World world, Location apex, double scale) {
        if (scale <= 0) return;
        double size = 0.3 + 0.9 * scale;
        world.spawnParticle(Particle.DUST_COLOR_TRANSITION, apex, (int) (6 + 10 * scale), size * 0.5, size * 0.5, size * 0.5, 0, ORB_DUST);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, apex, (int) (3 + 6 * scale), size * 0.4, size * 0.4, size * 0.4, 0.02);
    }

    private List<Location> buildImpacts(Location center) {
        List<Location> impacts = new ArrayList<>(METEOR_COUNT);
        double offset = Math.random() * Math.PI * 2;
        for (int i = 0; i < METEOR_COUNT; i++) {
            double t = (i + 0.5) / METEOR_COUNT;
            double radius = ZONE_RADIUS * Math.sqrt(t) + (Math.random() - 0.5) * 0.6;
            radius = Math.max(0, Math.min(ZONE_RADIUS, radius));
            double theta = offset + i * GOLDEN_ANGLE + Math.random() * 0.3;
            impacts.add(center.clone().add(Math.cos(theta) * radius, 0, Math.sin(theta) * radius));
        }
        Collections.shuffle(impacts);
        return impacts;
    }

    private void launchMeteor(World world, Player player, Location apex, Location target) {
        Vector dir = target.toVector().subtract(apex.toVector()).normalize();

        Location spawnLoc = apex.clone().add(dir.clone().multiply(0.8));

        Fireball meteor = world.spawn(spawnLoc, Fireball.class, fb -> {
            fb.setShooter(player);
            fb.setDirection(dir);
            fb.setVelocity(dir.clone().multiply(METEOR_SPEED));
            fb.getPersistentDataContainer().set(meteorKey, PersistentDataType.BYTE, (byte) 1);
        });
        modelManager.track(meteor);

        world.playSound(apex, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f + (float) Math.random() * 0.4f);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, spawnLoc, 12, 0.1, 0.1, 0.1, 0.12);

        new BukkitRunnable() {
            int life = 0;

            @Override
            public void run() {
                if (!meteor.isValid() || life > 80) {
                    if (meteor.isValid()) meteor.remove();
                    cancel();
                    return;
                }

                Location loc = meteor.getLocation();
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 4, 0.15, 0.15, 0.15, 0.01);
                world.spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, 2, 0.1, 0.1, 0.1, 0, ORB_DUST);
                if (life % 2 == 0) {
                    world.spawnParticle(Particle.SOUL, loc, 1, 0.1, 0.1, 0.1, 0.01);
                }

                life++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}