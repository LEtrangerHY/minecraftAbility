package org.core.coreSystem.cores.KEY.Benzene.Skill;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;
import org.core.coreSystem.cores.KEY.Benzene.Passive.chainResonance;
import org.core.coreSystem.cores.KEY.Benzene.coreSystem.Benzene;
import org.core.coreSystem.absCoreSystem.SkillBase;

import java.util.*;

public class F implements SkillBase {

    private final Benzene config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final chainResonance chainResonance;

    private static final Particle.DustOptions DUST_SLASH = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.5f);
    private static final Particle.DustOptions DUST_SLASH_GRA = new Particle.DustOptions(Color.fromRGB(111, 111, 111), 0.5f);

    private static final Particle.DustOptions DUST_OPT_1 = new Particle.DustOptions(Color.fromRGB(111, 111, 111), 0.3f);
    private static final Particle.DustOptions DUST_OPT_2 = new Particle.DustOptions(Color.fromRGB(101, 101, 101), 0.4f);
    private static final Particle.DustOptions DUST_OPT_3 = new Particle.DustOptions(Color.fromRGB(99, 99, 99), 0.5f);
    private static final Particle.DustOptions DUST_OPT_GRA = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.6f);

    private static final Set<Material> UNBREAKABLE_BLOCKS = Set.of(
            Material.BEDROCK, Material.BARRIER, Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK, Material.END_PORTAL_FRAME, Material.END_PORTAL,
            Material.NETHER_PORTAL, Material.STRUCTURE_BLOCK, Material.JIGSAW, Material.AIR, Material.WATER, Material.LAVA
    );

    public F(Benzene config, JavaPlugin plugin, Cool cool, chainResonance chainResonance) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.chainResonance = chainResonance;
    }

    @Override
    public void Trigger(Player player) {
        player.swingMainHand();
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

        double slashLength = 4.8;
        double maxAngle = Math.toRadians(45);
        int maxTicks = 3;
        double innerRadius = 2.6;

        double hitThreshold = Math.cos(maxAngle + 0.2);

        config.damaged_2.put(player.getUniqueId(), new HashSet<>());

        double amp = config.f_Skill_Amp * player.getPersistentDataContainer()
                .getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_Damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Entity initialTarget = getTargetedEntity(player, 5.0, 0.3);
        Location eyeLoc = player.getEyeLocation();
        Location origin = eyeLoc.clone().add(0, -0.6, 0);

        Vector forward = player.getLocation().getDirection().normalize();
        Vector worldUp = new Vector(0, 1, 0);
        if (Math.abs(forward.dot(worldUp)) > 0.95) worldUp = new Vector(1, 0, 0);
        Vector right = forward.clone().crossProduct(worldUp).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        if (initialTarget != null) {
            world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.6f, 1.0f);
            world.spawnParticle(Particle.ENCHANTED_HIT, initialTarget.getLocation().add(0, 1, 0), 22, 0.6, 0, 0.6, 1);
            if (!initialTarget.isDead()) {
                chainResonance.increase(player, initialTarget);
                if (initialTarget.isDead()) chainResonance.decrease(initialTarget);
            }
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks < 2)
                    world.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.6f, 1.0f);

                if (ticks > maxTicks || player.isDead()) {
                    finishSkill(player, initialTarget);
                    this.cancel();
                    return;
                }

                double progressAngle = ((double) ticks / maxTicks) * (maxAngle * 2) - maxAngle;
                progressAngle = -progressAngle;

                double startAngle = maxAngle;
                double endAngle = -maxAngle;
                double step = -Math.toRadians(2);

                boolean canBreakBlock = config.canBlockBreak.getOrDefault(player.getUniqueId(), false);
                Set<Entity> damagedEntities = config.damaged_2.get(player.getUniqueId());

                Vector currentTickDirection = forward.clone().rotateAroundAxis(up, progressAngle);

                for (Entity entity : world.getNearbyEntities(origin, slashLength, slashLength, slashLength)) {
                    if (!(entity instanceof LivingEntity target) || entity == player || damagedEntities.contains(entity)) continue;

                    Vector toEntity = target.getLocation().add(0, target.getHeight() / 2.0, 0).toVector().subtract(origin.toVector());
                    double distSq = toEntity.lengthSquared();

                    if (distSq > (slashLength + 1.0) * (slashLength + 1.0)) continue;

                    Vector toEntityDir = toEntity.normalize();
                    double dotProduct = currentTickDirection.dot(toEntityDir);

                    if (dotProduct > hitThreshold) {
                        damagedEntities.add(entity);

                        ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                        forceDamage.applyEffect(player);

                        if (canBreakBlock) {
                            if (!target.isDead()) {
                                chainResonance.increase(player, target);
                                if (target.isDead()) chainResonance.decrease(target);
                            }
                        }
                    }
                }

                int particleCount = 0;

                for (double angle = startAngle; angle >= endAngle; angle += step) {
                    double sinA = Math.sin(angle + progressAngle);
                    double cosA = Math.cos(angle + progressAngle);

                    double dirX = right.getX() * sinA + forward.getX() * cosA;
                    double dirY = right.getY() * sinA + forward.getY() * cosA;
                    double dirZ = right.getZ() * sinA + forward.getZ() * cosA;

                    for (double length = 0; length <= slashLength; length += 0.1) {
                        double x = origin.getX() + dirX * length;
                        double y = origin.getY() + dirY * length;
                        double z = origin.getZ() + dirZ * length;

                        if (canBreakBlock && length >= 1.0 && particleCount % 5 == 0) {
                            Block block = new Location(world, x, y, z).getBlock();
                            if (!UNBREAKABLE_BLOCKS.contains(block.getType())) breakBlockSafely(player, block);
                            Block blockUp = block.getRelative(0, 1, 0);
                            if (!UNBREAKABLE_BLOCKS.contains(blockUp.getType())) breakBlockSafely(player, blockUp);
                        }

                        if (length >= innerRadius) {
                            Particle.DustOptions opt = (Math.random() < 0.11) ? DUST_SLASH : DUST_SLASH_GRA;
                            world.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, opt);
                        }
                        particleCount++;
                    }
                }
                ticks++;
            }

            private void finishSkill(Player player, Entity target) {
                int chainNum = config.chainRes.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).size();
                config.damaged_2.remove(player.getUniqueId());

                if (target != null && chainNum >= 2) {
                    Special_Attack(player, player.getLocation(), player.getGameMode(), target, chainNum);
                }

                if (config.blockBreak.getOrDefault(player.getUniqueId(), false)) {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    ItemMeta meta = mainHand.getItemMeta();
                    if (meta instanceof org.bukkit.inventory.meta.Damageable dm) {
                        int newDamage = dm.getDamage() + 6;
                        dm.setDamage(newDamage);
                        mainHand.setItemMeta(meta);
                        if (newDamage >= mainHand.getType().getMaxDurability())
                            player.getInventory().setItemInMainHand(null);
                    }
                    config.blockBreak.remove(player.getUniqueId());
                }
                config.canBlockBreak.remove(player.getUniqueId());
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void breakBlockSafely(Player player, Block block) {
        if (UNBREAKABLE_BLOCKS.contains(block.getType())) return;

        if (!config.blockBreak.getOrDefault(player.getUniqueId(), false)) {
            config.blockBreak.put(player.getUniqueId(), true);
        }

        block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 6, 0.3, 0.3, 0.3, block.getBlockData());
        block.breakNaturally(new ItemStack(Material.IRON_SWORD));
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

    public void Special_Attack(Player player, Location firstLocation, GameMode playerGameMode, Entity entity, int chainNum) {

        long cools = config.f_Skill_Cool * chainNum;
        cool.updateCooldown(player, "F", cools);

        Invulnerable invulnerable = new Invulnerable(player, 150L * chainNum);
        invulnerable.applyEffect(player);

        World world = player.getWorld();

        config.fskill_using.put(player.getUniqueId(), true);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= chainNum || player.isDead()) {
                    config.damaged_2.remove(player.getUniqueId());
                    config.fskill_using.remove(player.getUniqueId());

                    if(!isSafe(player.getLocation())){
                        player.teleport(firstLocation);
                    }

                    player.setGameMode(playerGameMode);

                    this.cancel();
                    return;
                }

                teleportBehind(player, playerGameMode, entity, -5.0);

                world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);
                world.spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation().clone().add(0, 1.2, 0), 22, 0.6, 0.6, 0.6, 1);

                double height = - 0.2 * tick;

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
        double maxAngleDeg = 36.0;
        double maxAngleRad = Math.toRadians(maxAngleDeg);
        double innerRadius = 5.0;
        int maxTicks = 3;

        double angleThreshold = Math.cos(maxAngleRad);

        Random rand = new Random();
        int random = rand.nextInt(4);
        double tiltAngle = switch (random) {
            case 0 -> Math.toRadians(6);
            case 1 -> Math.toRadians(-6);
            case 2 -> Math.toRadians(12);
            case 3 -> Math.toRadians(-12);
            default -> Math.toRadians(0);
        };

        double amp = config.f_Skill_Amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_Damage * 3 * (1 + amp);

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
                if (ticks < 1) {
                    world.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.2f, 1.0f);
                }

                if (ticks >= maxTicks || player.isDead()) {
                    this.cancel();
                    return;
                }

                for (Entity entity : world.getNearbyEntities(player.getLocation(), slashLength, slashLength, slashLength)) {

                    if (!(entity instanceof LivingEntity target) || entity == player) continue;
                    if (config.damaged_2.get(player.getUniqueId()).contains(entity)) continue;

                    Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector());
                    toTarget.setY(0);

                    if (toTarget.lengthSquared() > slashLength * slashLength) continue;

                    Vector directionToTarget = toTarget.normalize();
                    double dotProduct = direction.dot(directionToTarget);

                    if (dotProduct >= angleThreshold) {
                        ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                        forceDamage.applyEffect(player);

                        config.damaged_2.get(player.getUniqueId()).add(entity);
                    }
                }

                double progress = (ticks + 1) * (maxAngleRad * 2 / maxTicks) - maxAngleRad;
                Vector rotatedDir = direction.clone().rotateAroundY(progress);

                for (double length = innerRadius; length <= slashLength; length += 0.1) {
                    for (double angle = -maxAngleRad; angle <= maxAngleRad; angle += Math.toRadians(1)) {

                        Vector angleDir = rotatedDir.clone().rotateAroundY(angle);
                        Vector particleOffset = angleDir.clone().multiply(length);

                        double cosTilt = Math.cos(tiltAngle);
                        double sinTilt = Math.sin(tiltAngle);
                        double tiltedY = particleOffset.getY() * cosTilt - particleOffset.getZ() * sinTilt;
                        double tiltedZ = particleOffset.getY() * sinTilt + particleOffset.getZ() * cosTilt;
                        particleOffset.setY(tiltedY);
                        particleOffset.setZ(tiltedZ);

                        Location particleLocation = origin.clone().add(particleOffset).add(0, height, 0);

                        if (length < innerRadius + 0.2) {
                            world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, DUST_OPT_1);
                        } else if (length < innerRadius + 0.3) {
                            world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, DUST_OPT_2);
                        } else {
                            if (Math.random() <= 0.66) {
                                world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, DUST_OPT_3);
                            } else {
                                world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, DUST_OPT_GRA);
                            }
                        }
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