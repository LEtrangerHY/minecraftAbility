package org.core.coreSystem.cores.VOL3.Knight.Skill;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL3.Knight.Passive.bladeBond;
import org.core.coreSystem.cores.VOL3.Knight.coreSystem.Knight;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.effectPlugin.swingEchoGuard;
import org.core.models.coresObj.knightSwordModel;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class R implements SkillBase {
    private final Knight config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final bladeBond blades;
    private final NamespacedKey keyR;

    public R(Knight config, JavaPlugin plugin, Cool cool, bladeBond blades) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.blades = blades;
        this.keyR = new NamespacedKey(plugin, "R");
    }

    @Override
    public void Trigger(Player player) {
        List<bladeBond.Blade> origins = new ArrayList<>();
        for (bladeBond.Blade blade : blades.list(player)) {
            if (blade.isOrigin()) origins.add(blade);
        }

        if (origins.isEmpty()) {
            throwBlade(player);
        } else {
            volley(player, origins);
        }
    }

    private double amplify(Player player) {
        long level = player.getPersistentDataContainer().getOrDefault(keyR, PersistentDataType.LONG, 0L);
        return 1 + config.r_Skill_amp * level;
    }

    private DamageSource source(Player player) {
        return DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();
    }

    private void throwBlade(Player player) {
        World world = player.getWorld();
        Material material = player.getInventory().getItemInOffHand().getType();
        double damage = bladeBond.swordDamage(material) * config.blade_Throw_Ratio * amplify(player);

        swingEchoGuard.swingOffHand(player);
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1.2f);
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.5f);

        Location eye = player.getEyeLocation();
        blades.launch(player, material, eye, eye.getDirection(), config.r_Throw_Speed, config.r_Throw_Range,
                damage, source(player), true, null, null);

        bladeBond.wear(player, EquipmentSlot.OFF_HAND, 1);
    }

    private void volley(Player player, List<bladeBond.Blade> origins) {
        swingEchoGuard.swingMainHand(player);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.6f, 1.4f);

        double amp = amplify(player);
        int epoch = blades.epoch(player);
        long stagger = Math.max(0L, config.r_Reuse_Stagger / 50L);

        for (int i = 0; i < origins.size(); i++) {
            bladeBond.Blade origin = origins.get(i);
            Location center = origin.getCenter();
            boolean spiral = origin.getHost() == null;
            Material material = origin.getMaterial();
            double damage = bladeBond.swordDamage(material) / config.r_Reuse_Divide * amp;

            Location vanish = origin.getExposed();
            vanish.getWorld().spawnParticle(Particle.DUST, vanish, 12, 0.2, 0.2, 0.2, 0, bladeBond.WHITE);
            blades.discard(player, origin);

            Bukkit.getScheduler().runTaskLater(plugin, () -> summon(player, epoch, center, spiral, material, damage), stagger * i);
        }
    }

    private void summon(Player player, int epoch, Location center, boolean spiral, Material material, double damage) {
        if (!blades.isCurrent(player, epoch) || !player.isOnline()) return;

        World world = center.getWorld();
        world.playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.8f, 1.3f);

        for (int i = 0; i < 3; i++) {
            double angle = 2 * Math.PI / 3 * i;
            Location start = center.clone().add(config.r_Reuse_Radius * Math.cos(angle), 0, config.r_Reuse_Radius * Math.sin(angle));
            world.spawnParticle(Particle.ENCHANTED_HIT, start, 14, 0.3, 0.3, 0.3, 0);
            fly(player, epoch, center, start, angle, spiral, material, damage);
        }
    }

    private void fly(Player player, int epoch, Location center, Location start, double angle, boolean spiral,
                     Material material, double damage) {
        World world = center.getWorld();
        Vector tangent = new Vector(-Math.sin(angle), 0, Math.cos(angle));

        Location spawn = start.clone();
        spawn.setDirection(tangent);
        knightSwordModel model = new knightSwordModel(world, spawn, material, bladeBond.SCALE, false, false);
        DamageSource source = source(player);

        double radius = config.r_Reuse_Radius;
        long windup = Math.max(1L, config.r_Windup_Time / 50L);
        double turn = Math.toRadians(spiral ? config.r_Windup_Turn : config.r_Windup_Turn_Straight);
        double pull = spiral ? 0.0 : config.r_Windup_Pull;
        long dashTicks = (long) Math.ceil(config.r_Straight_Distance / config.r_Straight_Speed);
        double span = config.r_Straight_Speed * dashTicks;
        long spiralTicks = Math.max(1L, config.r_Spiral_Ticks);
        double spiralTurn = Math.toRadians(config.r_Spiral_Turn);
        double exit = config.r_Spiral_Exit;

        new BukkitRunnable() {
            long ticks = 0;
            Location previous = start.clone();
            Vector facing = tangent.clone();
            Location dashFrom = null;
            Vector dash = null;
            BlockDisplay line = null;
            Location trailFrom = null;
            Vector trailHeading = null;
            double trailDistance = 0;

            private void flushTrail() {
                if (trailFrom != null) bladeBond.streak(world, trailFrom, trailHeading, trailDistance);
                trailFrom = null;
            }

            private void end() {
                flushTrail();
                model.remove();
                retract(line, span);
                cancel();
            }

            @Override
            public void run() {
                if (!blades.isCurrent(player, epoch) || !player.isOnline() || !model.isValid()) {
                    model.remove();
                    if (line != null) line.remove();
                    cancel();
                    return;
                }
                ticks++;

                if (ticks <= windup) {
                    double progress = (double) ticks / windup;
                    Location next;
                    if (spiral) {
                        double theta = angle + turn * progress * progress;
                        next = center.clone().add(radius * Math.cos(theta), 0, radius * Math.sin(theta));
                        facing = new Vector(-Math.sin(theta), 0, Math.cos(theta));
                    } else {
                        double theta = angle + turn * (1 - Math.pow(1 - progress, 3));
                        double reach = radius + pull * smooth((progress - 0.5) / 0.5);
                        next = center.clone().add(reach * Math.cos(theta), 0, reach * Math.sin(theta));

                        Vector around = new Vector(-Math.sin(theta), 0, Math.cos(theta));
                        Vector inward = center.toVector().subtract(next.toVector()).setY(0);
                        if (inward.lengthSquared() > 1.0E-4) {
                            double aim = smooth(progress);
                            facing = around.multiply(1 - aim).add(inward.normalize().multiply(aim)).normalize();
                        } else {
                            facing = around;
                        }
                    }
                    next.setDirection(facing);
                    model.getDisplay().teleport(next);
                    previous = next;

                    if (!spiral && ticks == windup) line = spawnLine(world, next, center);
                    return;
                }

                long t = ticks - windup;
                Location next;
                if (spiral) {
                    if (t > spiralTicks) {
                        end();
                        return;
                    }
                    double u = (double) t / spiralTicks;
                    double current = radius * (1 - (1 + exit) * u);
                    double theta = angle + turn + spiralTurn * u;
                    next = center.clone().add(current * Math.cos(theta), 0, current * Math.sin(theta));
                } else {
                    if (t > dashTicks) {
                        end();
                        return;
                    }
                    if (dash == null) {
                        dashFrom = previous.clone();
                        dash = center.toVector().subtract(dashFrom.toVector());
                        if (dash.lengthSquared() < 1.0E-4) dash = facing.clone();
                        dash.normalize();

                        Location aimed = dashFrom.clone();
                        aimed.setDirection(dash);
                        model.getDisplay().teleport(aimed);
                        model.slide(span, (int) dashTicks);
                        stretch(line, span, (int) dashTicks);
                    }
                    next = dashFrom.clone().add(dash.clone().multiply(config.r_Straight_Speed * t));
                }

                Vector move = next.toVector().subtract(previous.toVector());
                double distance = move.length();

                if (distance > 1.0E-4) {
                    facing = move.clone().normalize();

                    RayTraceResult hit = world.rayTraceEntities(previous, move, distance, 0.3, e -> bladeBond.isTarget(player, e));
                    if (hit != null && hit.getHitEntity() instanceof LivingEntity target) {
                        cancel();
                        Location point = hit.getHitPosition().toLocation(world);
                        flushTrail();
                        bladeBond.streak(world, previous, facing, point.distance(previous));

                        if (!spiral) {
                            model.settle();
                            if (dashFrom != null) retract(line, point.distance(dashFrom));
                        }

                        new ForceDamage(target, damage, source, false).applyEffect(player);
                        world.spawnParticle(Particle.ENCHANTED_HIT, point, 12, 0.2, 0.2, 0.2, 0.2);
                        world.playSound(point, Sound.ITEM_TRIDENT_HIT, 1f, 1.2f);

                        blades.embed(player, material, false, target, hit.getHitPosition(), facing, model);
                        return;
                    }
                }

                if (spiral) {
                    next.setDirection(facing);
                    model.getDisplay().teleport(next);
                }
                flushTrail();
                if (distance > 1.0E-4) {
                    trailFrom = previous.clone();
                    trailHeading = facing.clone();
                    trailDistance = distance;
                }
                previous = next;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static double smooth(double value) {
        double x = Math.max(0.0, Math.min(1.0, value));
        return x * x * (3 - 2 * x);
    }

    private Transformation lineShape(double from, double length) {
        float width = (float) config.r_Line_Width;
        return new Transformation(
                new Vector3f(-width / 2f, -width / 2f, (float) from),
                new Quaternionf(),
                new Vector3f(width, width, (float) Math.max(0.01, length)),
                new Quaternionf()
        );
    }

    private BlockDisplay spawnLine(World world, Location from, Location center) {
        Vector direction = center.toVector().subtract(from.toVector());
        if (direction.lengthSquared() < 1.0E-4) return null;

        Location at = from.clone();
        at.setDirection(direction);
        Transformation initial = lineShape(0, 0.01);
        return world.spawn(at, BlockDisplay.class, d -> {
            d.setBlock(Material.BLACK_CONCRETE.createBlockData());
            d.setBrightness(new Display.Brightness(15, 15));
            d.setTransformation(initial);
        });
    }

    private void stretch(BlockDisplay line, double length, int ticks) {
        if (line == null || !line.isValid()) return;
        line.setInterpolationDelay(0);
        line.setInterpolationDuration(Math.max(1, ticks));
        line.setTransformation(lineShape(0, length));
    }

    private void retract(BlockDisplay line, double length) {
        if (line == null || !line.isValid()) return;
        line.setInterpolationDelay(0);
        line.setInterpolationDuration(5);
        line.setTransformation(lineShape(length, 0.01));
        Bukkit.getScheduler().runTaskLater(plugin, line::remove, 6L);
    }
}