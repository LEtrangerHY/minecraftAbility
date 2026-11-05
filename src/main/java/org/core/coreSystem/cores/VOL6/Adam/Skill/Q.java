package org.core.coreSystem.cores.VOL6.Adam.Skill;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL6.Adam.Passive.stackCharge;
import org.core.coreSystem.cores.VOL6.Adam.coreSystem.Adam;
import org.core.effect.crowdControl.Grounding;
import org.core.effect.effectPlugin.swingEchoGuard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Q implements SkillBase {

    private static final Particle.DustOptions DUST_CHAIN = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.6f);
    private static final BlockData CHAIN_BLOCK_DATA = Material.IRON_CHAIN.createBlockData();
    private static final PotionEffect GLOW_EFFECT = new PotionEffect(PotionEffectType.GLOWING, 40, 2, false, false);

    private final Adam config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final stackCharge charge;

    private final Map<UUID, Pending> pending = new HashMap<>();

    private record Pending(UUID target, BukkitTask expiry) {}

    public Q(Adam config, JavaPlugin plugin, Cool cool, stackCharge charge) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.charge = charge;
    }

    @Override
    public void Trigger(Player player) {
        Pending reuse = pending.remove(player.getUniqueId());
        if (reuse != null) {
            reuse.expiry().cancel();
            Entity entity = Bukkit.getEntity(reuse.target());
            if (entity instanceof LivingEntity target && target.isValid() && !target.isDead()
                    && target.getWorld().equals(player.getWorld())) {
                swap(player, target);
            }
            cool.updateCooldown(player, "Q", config.q_Skill_Cool);
            return;
        }

        designate(player);
    }

    public void cleanup(Player player) {
        Pending reuse = pending.remove(player.getUniqueId());
        if (reuse != null) reuse.expiry().cancel();
    }

    private void designate(Player player) {
        swingEchoGuard.swingOffHand(player);
        World world = player.getWorld();

        LivingEntity target = getTargetedEntity(player, config.q_Range, config.q_Ray);

        world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.6f, 1.0f);

        if (target == null) {
            cool.updateCooldown(player, "Q", config.q_Miss_Cool);
            return;
        }

        chainEffect(player, target, 40);

        Grounding grounding = new Grounding(target, config.q_Grounding);
        target.setVelocity(new Vector(0, 0, 0));
        grounding.applyEffect(target);

        target.addPotionEffect(GLOW_EFFECT);
        world.playSound(target.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.6f, 1.0f);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= 3 || player.isDead()) {
                    cancel();
                    return;
                }
                Location loc = target.getLocation();
                world.playSound(loc, Sound.BLOCK_CHAIN_PLACE, 1.6f, 1.0f);
                world.spawnParticle(Particle.BLOCK, loc.add(0, 1.2, 0), 6, 0.6, 0.6, 0.6, CHAIN_BLOCK_DATA);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        charge.add(player, config.q_Stack);

        UUID uuid = player.getUniqueId();
        cool.updateCooldown(player, "Q", config.q_Reuse_Delay);
        BukkitTask expiry = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pending.remove(uuid) != null && player.isOnline()) {
                cool.updateCooldown(player, "Q", config.q_Skill_Cool);
            }
        }, Math.max(1L, config.q_Reuse_Window / 50L));
        pending.put(uuid, new Pending(target.getUniqueId(), expiry));
    }

    private void swap(Player player, LivingEntity target) {
        World world = player.getWorld();
        Location from = player.getLocation();
        Location to = target.getLocation();

        Location lineStart = from.clone().add(0, 1.0, 0);
        Vector line = to.clone().add(0, 1.0, 0).toVector().subtract(lineStart.toVector());
        double length = line.length();
        if (length > 1.0E-4) {
            Vector step = line.clone().normalize();
            for (double d = 0; d <= length; d += 0.3) {
                Location point = lineStart.clone().add(step.clone().multiply(d));
                world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, DUST_CHAIN);
                if (((int) (d / 0.3)) % 4 == 0) world.spawnParticle(Particle.BLOCK, point, 1, 0.05, 0.05, 0.05, 0, CHAIN_BLOCK_DATA);
            }
        }

        world.playSound(from, Sound.BLOCK_CHAIN_BREAK, 1.4f, 1.0f);
        world.playSound(to, Sound.BLOCK_CHAIN_BREAK, 1.4f, 1.0f);
        world.playSound(from, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);

        Location playerDest = to.clone();
        boolean face = from.distanceSquared(to) <= config.q_Face_Range * config.q_Face_Range;
        playerDest.setYaw(face ? (float) Math.toDegrees(Math.atan2(-(from.getX() - to.getX()), from.getZ() - to.getZ())) : from.getYaw());
        playerDest.setPitch(from.getPitch());

        Location targetDest = from.clone();
        targetDest.setYaw(to.getYaw());
        targetDest.setPitch(to.getPitch());

        Long groundedUntil = Grounding.groundedEntities.remove(target.getUniqueId());

        target.teleport(targetDest);
        player.teleport(playerDest);
        player.setFallDistance(0);
        target.setFallDistance(0);

        world.spawnParticle(Particle.BLOCK, playerDest.clone().add(0, 1.0, 0), 10, 0.3, 0.5, 0.3, 0, CHAIN_BLOCK_DATA);
        world.spawnParticle(Particle.BLOCK, targetDest.clone().add(0, 1.0, 0), 10, 0.3, 0.5, 0.3, 0, CHAIN_BLOCK_DATA);

        if (groundedUntil != null) {
            long left = groundedUntil - System.currentTimeMillis();
            if (left > 50L) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (target.isValid() && !target.isDead()) new Grounding(target, left - 50L).applyEffect(target);
                }, 1L);
            }
        }
    }

    private void chainEffect(Player player, Entity entity, int time) {
        World world = player.getWorld();

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick > time || entity.isDead() || !entity.isValid()) {
                    cancel();
                    return;
                }

                Location baseLoc = entity.getLocation();
                for (int i = 0; i < 33; i += 2) {
                    double yOffset = i / 10.0;
                    world.spawnParticle(Particle.DUST, baseLoc.clone().add(0, yOffset, 0), 1, 0, 0, 0, 0, DUST_CHAIN);

                    if (i % 3 == 0) {
                        double hitY = 3.3 - (i * 0.12);
                        world.spawnParticle(Particle.ENCHANTED_HIT, baseLoc.clone().add(0, hitY, 0), 1, 0, 0, 0, 0);
                    }
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static LivingEntity getTargetedEntity(Player player, double range, double raySize) {
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        RayTraceResult result = world.rayTraceEntities(eyeLocation, direction, range, raySize,
                e -> e instanceof LivingEntity && !e.equals(player));

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            return target;
        }
        return null;
    }
}