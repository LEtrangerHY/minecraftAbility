package org.core.coreSystem.cores.VOL2.Lavender.Skill;

import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Lavender.coreSystem.Lavender;
import org.core.effect.crowdControl.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class F implements SkillBase {

    private final Lavender config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final R r;

    public F(Lavender config, JavaPlugin plugin, Cool cool, R r) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.r = r;
    }

    @Override
    public void Trigger(Player player) {
        if (!config.activeWalls.containsValue(player.getUniqueId())) {
            r.Retract(player);
            cool.updateCooldown(player, "Q", 0L);
            cool.updateCooldown(player, "R", 0L);
            Stiff.breakStiff(player);

            World world = player.getWorld();
            Location startLocation = player.getLocation();

            config.transportPos.put(player.getUniqueId(), startLocation);

            LivingEntity target = getTargetedEntity(player, 4.0, 0.4);
            Vector direction = startLocation.getDirection().normalize().multiply(config.f_Skill_dash);

            player.setVelocity(direction);
            if (target != null) {
                BlockData amethyst = Material.AMETHYST_BLOCK.createBlockData();
                target.setVelocity(direction);
                world.spawnParticle(Particle.BLOCK, target.getLocation().clone().add(0, 1.2, 0), 9, 0.6, 0.6, 0.6, amethyst);
            }
            world.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.0f);

            Invulnerable invulnerable = new Invulnerable(player, 1000);
            invulnerable.applyEffect(player);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Walls(player);
            }, 10L);

        } else if (config.transportPos.containsKey(player.getUniqueId())) {
            World world = player.getWorld();
            BlockData amethyst = Material.AMETHYST_BLOCK.createBlockData();

            player.teleport(config.transportPos.getOrDefault(player.getUniqueId(), player.getLocation()));

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                world.spawnParticle(Particle.BLOCK, player.getLocation().clone().add(0, -1.0, 0), 12, 0.6, 0.6, 0.6, amethyst);
                world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.6f, 1.0f);
                world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.6f, 1.0f);
            }, 2L);

            config.transportPos.remove(player.getUniqueId());
        }
    }

    public void Walls(Player player) {
        World world = player.getWorld();
        Location center = player.getLocation();
        UUID uuid = player.getUniqueId();

        List<Location> createdBlocks = new ArrayList<>();

        int radius = 3;
        int height = 4;
        long durationTicks = 120L;

        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.6f, 1.0f);
        world.playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.6f, 1.0f);

        world.getNearbyEntities(center, 6, 6, 6).forEach(entity -> {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                entity.teleport(player.getLocation());
                world.playSound(entity.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.6f, 1.0f);
                lavender_fSkill_Particle_Effect(player, entity, 60);
                Grounding ground = new Grounding(entity, config.f_Skill_ground);
                ground.applyEffect(player);
            }
        });

        for (double x = -radius - 0.5; x <= radius + 0.5; x++) {
            for (double z = -radius - 0.5; z <= radius + 0.5; z++) {
                double distance = Math.sqrt(x * x + z * z);

                if (distance <= radius + 0.5) {
                    Location floorLoc = center.clone().add(x, -1, z).getBlock().getLocation();
                    if (floorLoc.getBlock().isPassable()) {
                        floorLoc.getBlock().setType(Material.AMETHYST_BLOCK);
                        config.activeWalls.put(floorLoc, uuid);
                        createdBlocks.add(floorLoc);
                    }
                }

                if (distance >= radius - 0.5 && distance <= radius + 0.5) {
                    for (int y = 0; y < height; y++) {
                        Location blockLoc = center.clone().add(x, y, z).getBlock().getLocation();
                        if (blockLoc.getBlock().isPassable()) {
                            blockLoc.getBlock().setType(Material.AMETHYST_BLOCK);
                            config.activeWalls.put(blockLoc, uuid);
                            createdBlocks.add(blockLoc);
                        }
                    }
                }
            }
        }

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cleanup();
                    this.cancel();
                    return;
                }

                if (ticksElapsed >= durationTicks) {
                    cleanup();
                    cool.updateCooldown(player, "F", config.f_Skill_Cool);
                    this.cancel();
                    return;
                }

                ticksElapsed++;
            }

            private void cleanup() {
                config.transportPos.remove(uuid);

                for (Location loc : createdBlocks) {
                    if (uuid.equals(config.activeWalls.get(loc))) {
                        loc.getBlock().setType(Material.AIR);
                        config.activeWalls.remove(loc);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        cool.updateCooldown(player, "F", 0L);
    }

    public static LivingEntity getTargetedEntity(Player player, double range, double raySize) {
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        List<LivingEntity> candidates = new ArrayList<>();

        for (Entity entity : world.getNearbyEntities(eyeLocation, range, range, range)) {
            if (!(entity instanceof LivingEntity) || entity.equals(player) || entity.isInvulnerable()) continue;
            RayTraceResult result = world.rayTraceEntities(eyeLocation, direction, range, raySize, e -> e.equals(entity));
            if (result != null) candidates.add((LivingEntity) entity);
        }

        return candidates.stream().min(Comparator.comparingDouble(Damageable::getHealth)).orElse(null);
    }

    public void lavender_fSkill_Particle_Effect(Player player, Entity entity, int time){
        World world = player.getWorld();
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(230, 230, 250), 0.5f);

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > time || entity.isDead()) {
                    this.cancel();
                    return;
                }
                for(int i = 0; i < 33; i++){
                    world.spawnParticle(Particle.DUST, entity.getLocation().add(0, ((double) i) / 10, 0), 1, 0, 0, 0, 0, dustOptions);
                    if(i % 3 == 0){
                        world.spawnParticle(Particle.WITCH, entity.getLocation().add(0, (3.3 - (((double) i * 1.2) / 10)), 0), 1, 0, 0, 0, 0);
                    }
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}