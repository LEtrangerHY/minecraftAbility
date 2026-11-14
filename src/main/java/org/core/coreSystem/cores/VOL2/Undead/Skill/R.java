package org.core.coreSystem.cores.VOL2.Undead.Skill;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Undead.Passive.basicAttack;
import org.core.coreSystem.cores.VOL2.Undead.Passive.handSwap;
import org.core.coreSystem.cores.VOL2.Undead.coreSystem.Undead;

import java.util.UUID;

public class R implements SkillBase {

    private static final Particle.DustOptions BLADE = new Particle.DustOptions(Color.fromRGB(246, 248, 252), 0.75f);
    private static final Particle.DustOptions KEEN = new Particle.DustOptions(Color.fromRGB(212, 216, 224), 0.62f);
    private static final Particle.DustOptions BODY = new Particle.DustOptions(Color.fromRGB(158, 163, 172), 0.52f);
    private static final Particle.DustOptions MIST = new Particle.DustOptions(Color.fromRGB(118, 122, 130), 0.42f);

    private static final double[] FLURRY_KEYS = {0.0, 0.42, 0.9, 1.0};
    private static final double[] FLURRY_TILT = {-18, -50, 50, 18};

    private final Undead config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final handSwap swap;
    private final basicAttack attack;

    public R(Undead config, JavaPlugin plugin, Cool cool, handSwap swap, basicAttack attack) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.swap = swap;
        this.attack = attack;
    }

    private record Flurry(double reach, double sweep, double thick, double tolerance, Sound sound, float pitch,
                          boolean push, double knockback, double lift) {
    }

    @Override
    public void Trigger(Player player) {
        if (swap.isFirearm(player)) {
            firearm(player);
        } else {
            melee(player);
        }
    }

    private void melee(Player player) {
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
                entity -> attack.isTarget(player, entity));

        if (hit == null || !(hit.getHitEntity() instanceof LivingEntity target)) return null;
        return player.hasLineOfSight(target) ? target : null;
    }

    private void comboSlash(Player player, Flurry shape, double amount, int index, LivingEntity focus) {
        basicAttack.Basis b = attack.basis(player);
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
                    attack.tipSparks(world, basicAttack.point(center, axis, lean, a1, rim), tangent, 4);

                    if (ticks == 1) {
                        attack.arcHit(player, origin, b.flat(), b.forward(), slant, reach, -sweep - Math.toRadians(8), sweep + Math.toRadians(8),
                                shape.tolerance(), amount,
                                !last || shape.push(), last ? shape.knockback() : 0, last ? shape.lift() : 0);
                    }
                } else if (ticks < FLURRY_KEYS.length + 1) {
                    double step = (ticks == FLURRY_KEYS.length - 1 ? 0.14 : 0.24) / rim;
                    for (double a = -sweep + Math.random() * step; a <= sweep; a += step) {
                        world.spawnParticle(Particle.DUST, basicAttack.point(center, axis, lean, a, rim), 1, 0, 0, 0, 0, basicAttack.TRAIL);
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

            world.spawnParticle(Particle.DUST, basicAttack.point(pivot, axis, slant, a, rim), 1, 0.005, 0.005, 0.005, 0, BLADE);

            for (double depth = 0.07; depth < width; depth += 0.075) {
                double ratio = depth / width;
                Particle.DustOptions tone = ratio < 0.3 ? KEEN : ratio < 0.65 ? BODY : MIST;
                if (tone == MIST && Math.random() < 0.4) continue;

                Location at = basicAttack.point(pivot, axis, slant, a, rim - depth * 0.6).subtract(normal.clone().multiply(depth));
                world.spawnParticle(Particle.DUST, at, 1, 0.012, 0.012, 0.012, 0, tone);
            }
        }
    }

    private void firearm(Player player) {
        UUID uuid = player.getUniqueId();
        boolean[] landed = new boolean[2];
        boolean[] ended = new boolean[2];

        if (!attack.pay(player, config.gun_r_Skill_Cost_Ratio)) return;

        config.is_acting.add(uuid);
        attack.fire(player, config.gun_r_Skill_Rate, 0, config.gun_Attack_Recoil, false, hit -> {
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
                    if (!attack.pay(player, config.gun_r_Skill_Cost_Ratio)) {
                        config.is_acting.remove(uuid);
                        cancel();
                        return;
                    }

                    attack.fire(player, config.gun_r_Skill_Rate, 0, config.gun_Attack_Recoil, false, hit -> {
                        landed[1] = hit;
                        ended[1] = true;
                    });
                    return;
                }

                if (ticks < config.gun_r_Skill_Interval + config.gun_r_Skill_Finish_Delay || !ended[0] || !ended[1]) return;

                if (landed[0] && landed[1] && attack.pay(player, config.gun_r_Skill_Finish_Cost_Ratio)) {
                    attack.fire(player, config.gun_r_Skill_Finish_Rate, config.gun_r_Skill_Knockback, config.gun_r_Skill_Recoil, true, null);

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
}