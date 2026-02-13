package org.core.coreSystem.cores.KEY.Nightel.Skill;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.KEY.Nightel.Passive.Chain;
import org.core.coreSystem.cores.KEY.Nightel.coreSystem.Nightel;

import java.util.*;

public class F implements SkillBase {

    private final Nightel config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Chain chain;
    private final NamespacedKey keyF;

    private static final Particle.DustOptions DUST_1 = new Particle.DustOptions(Color.fromRGB(199, 199, 199), 0.3f);
    private static final Particle.DustOptions DUST_2 = new Particle.DustOptions(Color.fromRGB(222, 222, 222), 0.4f);
    private static final Particle.DustOptions DUST_3 = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.5f);

    public F(Nightel config, JavaPlugin plugin, Cool cool, Chain chain) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.chain = chain;
        this.keyF = new NamespacedKey(plugin, "F");
    }

    @Override
    public void Trigger(Player player) {
        player.swingMainHand();
        World world = player.getWorld();

        LivingEntity target = getTargetedEntity(player, 4.8, 0.3);

        if (target != null) {
            Location firstLocation = player.getLocation();
            GameMode playerGameMode = player.getGameMode();

            world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);
            world.spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1.2, 0), 22, 0.6, 0.6, 0.6, 1);

            int slashCount = config.chainCount.getOrDefault(player.getUniqueId(), 0);

            boolean justTeleport = !(slashCount > 1.0);
            if (slashCount < 6 && !justTeleport) chain.removePoint(player);

            Special_Attack(player, firstLocation, playerGameMode, target, slashCount, justTeleport);
        } else {
            world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 1, 1);
            cool.updateCooldown(player, "F", 250L);
        }
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

    public void Special_Attack(Player player, Location firstLocation, GameMode playerGameMode, Entity entity, int slashCount, boolean justTeleport) {
        World world = player.getWorld();

        if (!justTeleport) config.fskill_using.put(player.getUniqueId(), true);

        Invulnerable invulnerable = new Invulnerable(player, 150L * slashCount);
        invulnerable.applyEffect(player);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= slashCount || player.isDead()) {
                    config.damaged_2.remove(player.getUniqueId());
                    config.fskill_using.remove(player.getUniqueId());

                    if (!isSafe(player.getLocation())) player.teleport(firstLocation);

                    player.setGameMode(playerGameMode);

                    this.cancel();
                    return;
                }

                teleportBehind(player, playerGameMode, entity, -5.0);

                Location targetLoc = entity.getLocation().add(0, 1.2, 0);
                if (slashCount == 6) {
                    world.spawnParticle(Particle.ENCHANTED_HIT, targetLoc, 22, 0.6, 0.6, 0.6, 1);
                    world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);
                } else {
                    world.spawnParticle(Particle.CRIT, targetLoc, 22, 0.6, 0.6, 0.6, 1);
                    world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1, 1);
                }

                double height = -0.2 * tick;
                if (!justTeleport) Slash(player, height, slashCount);

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    public void Slash(Player player, double height, int slashCount) {
        config.damaged_2.put(player.getUniqueId(), new HashSet<>());

        player.swingMainHand();
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

        double slashLength = 5.4;
        double maxAngle = Math.toRadians(36);
        int maxTicks = 3;
        double innerRadius = 5.0;

        Random rand = new Random();
        int random = rand.nextInt(4);

        double tiltAngle = switch (random) {
            case 0 -> Math.toRadians(6);
            case 1 -> Math.toRadians(-6);
            case 2 -> Math.toRadians(12);
            case 3 -> Math.toRadians(-12);
            default -> Math.toRadians(0);
        };

        double cosTilt = Math.cos(tiltAngle);
        double sinTilt = Math.sin(tiltAngle);

        double baseDamage = (slashCount == 6) ? 6 : config.f_Skill_damage;
        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyF, PersistentDataType.LONG, 0L);
        double damage = baseDamage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Location origin = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection().setY(0).normalize();

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

                Set<Entity> damagedEntities = config.damaged_2.get(player.getUniqueId());

                List<LivingEntity> potentialTargets = new ArrayList<>();
                for (Entity entity : world.getNearbyEntities(origin, slashLength, slashLength, slashLength)) {
                    if (entity instanceof LivingEntity target && entity != player) {
                        if (damagedEntities == null || !damagedEntities.contains(target)) {
                            potentialTargets.add(target);
                        }
                    }
                }

                if (!potentialTargets.isEmpty()) {
                    Iterator<LivingEntity> iterator = potentialTargets.iterator();
                    while (iterator.hasNext()) {
                        LivingEntity target = iterator.next();
                        Vector toEntity = target.getLocation().toVector().subtract(origin.toVector());
                        double distSq = toEntity.lengthSquared();

                        if (distSq <= (slashLength + 0.5) * (slashLength + 0.5)) {
                            Vector toEntityDir = toEntity.clone().normalize();
                            double angle = rotatedDir.angle(toEntityDir);

                            if (angle < maxAngle + 0.3) {
                                if (damagedEntities != null) damagedEntities.add(target);

                                ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                                forceDamage.applyEffect(player);
                                iterator.remove();
                            }
                        }
                    }
                }

                Location particleLocation = origin.clone();
                double originX = origin.getX();
                double originY = origin.getY();
                double originZ = origin.getZ();

                for (double length = innerRadius; length <= slashLength; length += 0.1) {
                    for (double angle = -maxAngle; angle <= maxAngle; angle += Math.toRadians(1)) {
                        Vector angleDir = rotatedDir.clone().rotateAroundY(angle);

                        double valX = angleDir.getX() * length;
                        double valY = angleDir.getY() * length;
                        double valZ = angleDir.getZ() * length;

                        double tiltedY = valY * cosTilt - valZ * sinTilt;
                        double tiltedZ = valY * sinTilt + valZ * cosTilt;

                        particleLocation.setX(originX + valX);
                        particleLocation.setY(originY + tiltedY + height);
                        particleLocation.setZ(originZ + tiltedZ);

                        Particle.DustOptions opt;
                        if (length < innerRadius + 0.2) opt = DUST_1;
                        else if (length < innerRadius + 0.3) opt = DUST_2;
                        else opt = DUST_3;

                        world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, opt);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
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

        if (isSafe(teleportLocation)) {
            player.setGameMode(playerGameMode);
        } else {
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