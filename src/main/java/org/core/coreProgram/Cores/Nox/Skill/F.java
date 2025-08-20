package org.core.coreProgram.Cores.Nox.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.coreProgram.Abs.SkillBase;
import org.core.coreProgram.Cores.Nox.Passive.Dream;
import org.core.coreProgram.Cores.Nox.coreSystem.Nox;

import java.util.*;

public class F implements SkillBase {

    private final Nox config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Dream dream;

    public F(Nox config, JavaPlugin plugin, Cool cool, Dream dream) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.dream = dream;
    }

    @Override
    public void Trigger(Player player) {

        player.swingMainHand();
        World world = player.getWorld();

        Location firstLocation = player.getLocation();

        GameMode playerGameMode = player.getGameMode();

        Entity target = getTargetedEntity(player,4.8, 0.3);

        if(target != null){
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);
            Special_Attack(player, firstLocation, playerGameMode, target, (config.dreamPoint.getOrDefault(player.getUniqueId(), new HashMap<>()).getOrDefault("F", 2.0) + 0.6) / 2);
        }else{
            world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 1, 1);
            long cools = 250L;
            cool.updateCooldown(player, "F", cools);
        }
    }

    public static LivingEntity getTargetedEntity(Player player, double range, double raySize) {
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        List<LivingEntity> candidates = new ArrayList<>();

        for (Entity entity : world.getNearbyEntities(eyeLocation, range, range, range)) {
            if (!(entity instanceof LivingEntity) || entity.equals(player)) continue;

            RayTraceResult result = world.rayTraceEntities(
                    eyeLocation, direction, range, raySize, e -> e.equals(entity)
            );

            if (result != null) {
                candidates.add((LivingEntity) entity);
            }
        }

        return candidates.stream()
                .min(Comparator.comparingDouble(Damageable::getHealth))
                .orElse(null);
    }

    public void Special_Attack(Player player, Location firstLocation, GameMode playerGameMode, Entity entity, double times) {

        int slashCount = (int) times;

        config.fskill_using.put(player.getUniqueId(), true);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= slashCount || player.isDead()) {
                    config.damaged.remove(player.getUniqueId());
                    config.fskill_using.remove(player.getUniqueId());

                    if(!isSafe(player.getLocation())){
                        player.teleport(firstLocation);
                    }

                    player.setGameMode(playerGameMode);

                    dream.wanderersDream(player, "F");
                    this.cancel();
                    return;
                }
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);

                teleportBehind(player, playerGameMode, entity, -5.0);

                double height = 0.3 - (0.15 * tick);

                Slash(player, height);

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    public void Slash(Player player, double height) {

        config.damaged_2.put(player.getUniqueId(), new HashSet<>());

        player.swingMainHand();
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

        double slashLength = 5.4;
        double maxAngle = Math.toRadians(36);
        long tickDelay = 0L;
        int maxTicks = 3;

        double innerRadius = 5.0;

        Random rand = new Random();
        int random = rand.nextInt(2);

        double tiltAngle = switch (random) {
            case 0 -> Math.toRadians(8);
            case 1 -> Math.toRadians(-8);
            default -> Math.toRadians(0);
        };

        Location origin = player.getEyeLocation().add(0, -0.6, 0);
        Vector direction = player.getLocation().getDirection().clone().setY(0).normalize();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (ticks >= maxTicks || player.isDead()) {
                    this.cancel();
                    return;
                }

                double progress = (ticks + 1) * (maxAngle * 2 / maxTicks) - maxAngle;
                Vector rotatedDir = direction.clone().rotateAroundY(progress);

                for (double length = innerRadius; length <= slashLength; length += 0.1) {
                    for (double angle = -maxAngle; angle <= maxAngle; angle += Math.toRadians(1)) {
                        Vector angleDir = rotatedDir.clone().rotateAroundY(angle);
                        Vector particleOffset = angleDir.clone().multiply(length);

                        double cosTilt = Math.cos(tiltAngle);
                        double sinTilt = Math.sin(tiltAngle);
                        double tiltedY = particleOffset.getY() * cosTilt - particleOffset.getZ() * sinTilt;
                        double tiltedZ = particleOffset.getY() * sinTilt + particleOffset.getZ() * cosTilt;
                        particleOffset.setY(tiltedY);
                        particleOffset.setZ(tiltedZ);

                        Location particleLocation = origin.clone().add(particleOffset);

                        if(length < innerRadius + 0.2){
                            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(199, 199, 199), 0.4f);
                            world.spawnParticle(Particle.DUST, particleLocation.add(0, height, 0), 1, 0, 0, 0, 0, dustOptions);
                        }else if(length < innerRadius + 0.3){
                            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(222, 222, 222), 0.5f);
                            world.spawnParticle(Particle.DUST, particleLocation.add(0, height, 0), 1, 0, 0, 0, 0, dustOptions);
                        }else{
                            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.6f);
                            world.spawnParticle(Particle.DUST, particleLocation.add(0, height, 0), 1, 0, 0, 0, 0, dustOptions);
                        }

                        for (Entity entity : world.getNearbyEntities(particleLocation, 1.2, 1.2, 1.2)) {
                            if (entity instanceof LivingEntity target && entity != player && !config.damaged_2.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                                ForceDamage forceDamage = new ForceDamage(target, config.f_Skill_damage * (config.dreamPoint.getOrDefault(player.getUniqueId(), new HashMap<>()).getOrDefault("F", 1.0) / 2));
                                forceDamage.applyEffect(player);
                                target.setVelocity(new Vector(0, 0, 0));

                                config.damaged_2.getOrDefault(player.getUniqueId(), new HashSet<>()).add(entity);
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, tickDelay, 1L);
    }

    public static void teleportBehind(Player player, GameMode playerGameMode, Entity target, double distance) {

        Location enemyLocation = target.getLocation();
        Location playerLocation = player.getLocation();

        Vector directionToPlayer = playerLocation.toVector().subtract(enemyLocation.toVector()).normalize();

        Location teleportLocation = enemyLocation.clone().add(directionToPlayer.multiply(distance));
        teleportLocation.setY(enemyLocation.getY());

        float yaw = getYawToFace(teleportLocation, enemyLocation);
        teleportLocation.setYaw(yaw);

        player.teleport(teleportLocation);

        if(isSafe(teleportLocation)){
            player.setGameMode(playerGameMode);
        }else{
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    private static boolean isSafe(Location loc) {
        Block aboveHead = loc.clone().add(0, 1, 0).getBlock();

        return !aboveHead.getType().isSolid();
    }

    private static float getYawToFace(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }
}