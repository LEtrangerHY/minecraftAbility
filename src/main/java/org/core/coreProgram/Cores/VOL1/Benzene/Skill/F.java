package org.core.coreProgram.Cores.VOL1.Benzene.Skill;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Damageable;
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
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.Effect.Invulnerable;
import org.core.coreProgram.Cores.VOL1.Benzene.Passive.ChainCalc;
import org.core.coreProgram.Cores.VOL1.Benzene.coreSystem.Benzene;
import org.core.coreProgram.AbsCoreSystem.SkillBase;

import java.util.*;

public class F implements SkillBase {

    private final Benzene config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final ChainCalc chainCalc;

    public F(Benzene config, JavaPlugin plugin, Cool cool, ChainCalc chainCalc) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.chainCalc = chainCalc;
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

        config.damaged.put(player.getUniqueId(), new HashSet<>());

        double amp = config.f_Skill_Amp * player.getPersistentDataContainer()
                .getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_Damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Entity target = getTargetedEntity(player, slashLength, 0.3);
        Location origin = player.getEyeLocation().clone().add(0, -0.6, 0);

        Vector forward = player.getLocation().getDirection().normalize();
        Vector worldUp = new Vector(0, 1, 0);
        if (Math.abs(forward.dot(worldUp)) > 0.95) worldUp = new Vector(1, 0, 0);
        Vector right = forward.clone().crossProduct(worldUp).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        Particle.DustOptions dustOption_slash = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.6f);
        Particle.DustOptions dustOption_slash_gra = new Particle.DustOptions(Color.fromRGB(111, 111, 111), 0.6f);

        if (target != null) {
            world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.6f, 1.0f);
            world.spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().clone().add(0, 1, 0), 22, 0.6, 0, 0.6, 1);
            if (!target.isDead()) {
                chainCalc.increase(player, target);
                if (target.isDead()) chainCalc.decrease(target);
            }
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (ticks < 2)
                    world.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.6f, 1.0f);

                if (ticks > maxTicks || player.isDead()) {
                    int chainNum = config.Chain.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).size();
                    config.damaged.remove(player.getUniqueId());

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

                    this.cancel();
                    return;
                }

                double progressAngle = ((double) ticks / maxTicks) * (maxAngle * 2) - maxAngle;
                progressAngle = -progressAngle;

                double startAngle = maxAngle;
                double endAngle = -maxAngle;
                double step = -Math.toRadians(2);

                for (double length = 0; length <= slashLength; length += 0.1) {
                    for (double angle = startAngle; angle >= endAngle; angle += step) {

                        double xOffset = Math.sin(angle + progressAngle) * length;
                        double zOffset = Math.cos(angle + progressAngle) * length;
                        double yOffset = 0;

                        Vector local = right.clone().multiply(xOffset)
                                .add(forward.clone().multiply(zOffset))
                                .add(up.clone().multiply(yOffset));

                        Location particleLocation = origin.clone().add(local);
                        double distanceFromOrigin = particleLocation.distance(origin);

                        if (config.canBlockBreak.getOrDefault(player.getUniqueId(), false)) {
                            Block blockDown = particleLocation.clone().getBlock();
                            Block blockUp = particleLocation.clone().add(0, 1, 0).getBlock();
                            if (blockDown.getType() != Material.AIR && blockDown.getType() != Material.WATER && blockDown.getType() != Material.LAVA)
                                breakBlockSafely(player, blockDown);
                            if (blockUp.getType() != Material.AIR && blockUp.getType() != Material.WATER && blockUp.getType() != Material.LAVA)
                                breakBlockSafely(player, blockUp);
                        }

                        if (distanceFromOrigin >= innerRadius) {
                            if (Math.random() < 0.11)
                                world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOption_slash);
                            else
                                world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOption_slash_gra);
                        }

                        for (Entity entity : world.getNearbyEntities(particleLocation, 0.6, 0.6, 0.6)) {
                            if (entity instanceof LivingEntity target && entity != player && !config.damaged.get(player.getUniqueId()).contains(entity)) {

                                config.damaged.get(player.getUniqueId()).add(entity);

                                ForceDamage forceDamage = new ForceDamage(target, damage, source);
                                forceDamage.applyEffect(player);
                                target.setVelocity(new Vector(0, 0, 0));

                                if (config.canBlockBreak.getOrDefault(player.getUniqueId(), false)) {
                                    if (!target.isDead()) {
                                        chainCalc.increase(player, target);
                                        if (target.isDead()) chainCalc.decrease(target);
                                    }
                                }
                            }
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static final Set<Material> UNBREAKABLE_BLOCKS = Set.of(
            Material.BEDROCK,
            Material.BARRIER,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.END_PORTAL_FRAME,
            Material.END_PORTAL,
            Material.NETHER_PORTAL,
            Material.STRUCTURE_BLOCK,
            Material.JIGSAW
    );

    public void breakBlockSafely(Player player, Block block) {
        if (UNBREAKABLE_BLOCKS.contains(block.getType())) {
            return;
        }

        if(!config.blockBreak.getOrDefault(player.getUniqueId(), false)) {
            config.blockBreak.put(player.getUniqueId(), true);
        }

        block.getWorld().spawnParticle(
                Particle.BLOCK,
                block.getLocation().add(0.5, 0.5, 0.5),
                6,
                0.3, 0.3, 0.3,
                block.getBlockData()
        );

        block.breakNaturally(new ItemStack(Material.IRON_SWORD));
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
                    config.damaged.remove(player.getUniqueId());
                    config.fskill_using.remove(player.getUniqueId());

                    if(!isSafe(player.getLocation())){
                        player.teleport(firstLocation);
                    }

                    player.setGameMode(playerGameMode);

                    this.cancel();
                    return;
                }

                world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);
                world.spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation().clone().add(0, 1.2, 0), 22, 0.6, 0.6, 0.6, 1);

                teleportBehind(player, playerGameMode, entity, -5.0);

                double height = - 0.2 * tick;

                Slash(player, height);

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    public void Slash(Player player, double height) {

        config.damaged.put(player.getUniqueId(), new HashSet<>());

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

        double amp = config.f_Skill_Amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_Damage * 3 * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Location origin = player.getEyeLocation().add(0, 0, 0);
        Vector direction = player.getLocation().getDirection().clone().setY(0).normalize();

        Particle.DustOptions dustOptions1 = new Particle.DustOptions(Color.fromRGB(111, 111, 111), 0.4f);
        Particle.DustOptions dustOptions2 = new Particle.DustOptions(Color.fromRGB(101, 101, 101), 0.5f);
        Particle.DustOptions dustOptions3 = new Particle.DustOptions(Color.fromRGB(99, 99, 99), 0.6f);
        Particle.DustOptions dustOptions_gra = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.6f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if(ticks < 1){
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.2f, 1.0f);
                }

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

                        Location particleLocation = origin.clone().add(particleOffset).clone().add(0, height, 0);

                        if(length < innerRadius + 0.2){
                            world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOptions1);
                        }else if(length < innerRadius + 0.3){
                            world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOptions2);
                        }else{
                            if(Math.random() <= 0.66){
                                world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOptions3);
                            }else{
                                world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOptions_gra);
                            }
                        }

                        for (Entity entity : world.getNearbyEntities(particleLocation, 1.2, 1.2, 1.2)) {
                            if (entity instanceof LivingEntity target && entity != player) {

                                if(!config.damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                                    ForceDamage forceDamage = new ForceDamage(target, damage, source);
                                    forceDamage.applyEffect(player);
                                    target.setVelocity(new Vector(0, 0, 0));

                                    config.damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).add(entity);
                                }
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
