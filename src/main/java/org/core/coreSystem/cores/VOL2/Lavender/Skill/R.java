package org.core.coreSystem.cores.VOL2.Lavender.Skill;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Lavender.coreSystem.Lavender;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Stiff;
import org.core.effect.crowdControl.Stun;

import java.util.*;

public class R implements SkillBase {

    private final Lavender config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private final Map<UUID, SwordData> activeSwords = new HashMap<>();

    public R(Lavender config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        if (activeSwords.containsKey(player.getUniqueId())) {
            SwordData data = activeSwords.get(player.getUniqueId());
            if (!data.isRetracting) {
                player.swingMainHand();
                Stiff.breakStiff(player);
                cool.updateCooldown(player, "R", config.r_re_Skill_Cool);
                Retract(player);
            }
        } else {
            player.swingMainHand();
            Ballistic(player);
        }
    }

    public void Ballistic(Player player) {
        World world = player.getWorld();

        Stiff stiff = new Stiff(player, 3000L);
        stiff.applyEffect(player);

        List<Location> pathPoints = calculateReflectionPath(player);

        SwordData data = new SwordData(pathPoints);

        BukkitTask task = new BukkitRunnable() {
            int tickCount = 0;
            int drawnSegments = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    forceCleanup(player);
                    return;
                }

                if (tickCount % 2 == 0) {
                    if (drawnSegments < pathPoints.size() - 1) {
                        drawnSegments++;

                        Location hitLoc = pathPoints.get(drawnSegments);
                        world.playSound(hitLoc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.6f, 1f);
                        BlockData amethyst = Material.AMETHYST_BLOCK.createBlockData();
                        world.spawnParticle(Particle.BLOCK, hitLoc, 9, 0.3, 0.3, 0.3, amethyst);
                    }
                }

                data.lastDrawnIndex = drawnSegments;

                drawSwordParticles(pathPoints, drawnSegments);

                checkCollision(pathPoints, drawnSegments, player, data);

                tickCount++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        data.setTask(task);
        activeSwords.put(player.getUniqueId(), data);

        BukkitTask autoRetract = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeSwords.containsKey(player.getUniqueId())) {
                    SwordData currentData = activeSwords.get(player.getUniqueId());
                    if (!currentData.isRetracting) {
                        Retract(player);
                    }
                }
            }
        }.runTaskLater(plugin, 60L);

        data.setAutoRetractTask(autoRetract);

        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1f);
    }

    public void Retract(Player player) {
        UUID uid = player.getUniqueId();
        if (!activeSwords.containsKey(uid)) return;

        SwordData data = activeSwords.get(uid);
        cancelActiveTasks(data);
        data.isRetracting = true;
        startRetractionAnimation(player, data);
    }

    private void cancelActiveTasks(SwordData data) {
        if (data.getAutoRetractTask() != null) {
            data.getAutoRetractTask().cancel();
            data.setAutoRetractTask(null);
        }
        if (data.getTask() != null) {
            data.getTask().cancel();
            data.setTask(null);
        }
    }

    private void startRetractionAnimation(Player player, SwordData data) {
        new BukkitRunnable() {
            int currentSegment = data.lastDrawnIndex;

            @Override
            public void run() {
                if (!player.isOnline() || currentSegment <= 0) {
                    finishRetraction(player);
                    cancel();
                    return;
                }

                player.getWorld().playSound(data.path.get(currentSegment), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.2f, 1f);
                checkRetractionHit(data.path, currentSegment - 1, player);
                currentSegment--;
                drawSwordParticles(data.path, currentSegment);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void finishRetraction(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        activeSwords.remove(player.getUniqueId());
    }

    private void forceCleanup(Player player) {
        if(activeSwords.containsKey(player.getUniqueId())) {
            SwordData data = activeSwords.get(player.getUniqueId());
            cancelActiveTasks(data);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            activeSwords.remove(player.getUniqueId());
        }
    }

    private List<Location> calculateReflectionPath(Player player) {
        List<Location> points = new ArrayList<>();
        Location eyeLoc = player.getEyeLocation().add(0, -0.6, 0).clone();
        Vector direction = eyeLoc.getDirection().normalize();
        Location origin = eyeLoc.clone().add(direction.clone().multiply(0.6));
        points.add(origin.clone());

        if (!origin.getBlock().isPassable()) {
            points.add(origin.clone().add(direction.clone().multiply(0.1)));
            return points;
        }

        Location currentStart = origin.clone();
        Vector currentDir = direction.clone();

        double maxRangePerSegment = 9.0;
        int maxBounces = 6;

        for (int i = 0; i < maxBounces; i++) {
            RayTraceResult result = player.getWorld().rayTraceBlocks(
                    currentStart, currentDir, maxRangePerSegment, FluidCollisionMode.NEVER, true
            );

            if (result == null || result.getHitBlock() == null) {
                Location endPoint = currentStart.clone().add(currentDir.clone().multiply(maxRangePerSegment));
                points.add(endPoint);
                break;
            } else {
                Location hitPoint = result.getHitPosition().toLocation(player.getWorld());
                points.add(hitPoint);
                BlockFace face = result.getHitBlockFace();
                if (face != null) {
                    if (face.getModX() != 0) currentDir.setX(-currentDir.getX());
                    if (face.getModY() != 0) currentDir.setY(-currentDir.getY());
                    if (face.getModZ() != 0) currentDir.setZ(-currentDir.getZ());
                }
                currentStart = hitPoint.clone().add(currentDir.clone().multiply(0.01));
            }
        }
        return points;
    }

    private void drawSwordParticles(List<Location> points, int maxSegments) {
        if (points.size() < 2) return;
        Particle.DustOptions dust = new Particle.DustOptions(Color.SILVER, 0.6f);

        for (int i = 0; i < maxSegments; i++) {
            if (i >= points.size() - 1) break;
            Location start = points.get(i);
            Location end = points.get(i + 1);
            double dist = start.distance(end);
            Vector dir = end.toVector().subtract(start.toVector()).normalize();
            Vector right = dir.getCrossProduct(new Vector(0, 1, 0));
            if (right.lengthSquared() < 0.01) right = dir.getCrossProduct(new Vector(1, 0, 0));
            right.normalize().multiply(0.09);

            for (double d = 0; d < dist; d += 0.06) {
                Location center = start.clone().add(dir.clone().multiply(d));
                Location leftSide = center.clone().add(right);
                Location rightSide = center.clone().subtract(right);
                World w = center.getWorld();
                w.spawnParticle(Particle.DUST, center, 1, 0, 0, 0, 0, dust);
                w.spawnParticle(Particle.DUST, leftSide, 1, 0, 0, 0, 0, dust);
                w.spawnParticle(Particle.DUST, rightSide, 1, 0, 0, 0, 0, dust);
            }
        }
    }

    private void checkCollision(List<Location> points, int maxSegments, Player player, SwordData data) {
        if (points.size() < 2) return;

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        for (int i = 0; i < maxSegments; i++) {
            if (i >= points.size() - 1) break;

            Location start = points.get(i);
            Location end = points.get(i + 1);
            Vector direction = end.toVector().subtract(start.toVector());
            double dist = direction.length();

            BoundingBox searchBox = BoundingBox.of(start, end).expand(0.5);

            Collection<Entity> nearbyEntities = player.getWorld().getNearbyEntities(searchBox,
                    entity -> entity != player && entity instanceof LivingEntity);

            for (Entity entity : nearbyEntities) {
                LivingEntity target = (LivingEntity) entity;

                BoundingBox targetBox = target.getBoundingBox().expand(0.3);
                RayTraceResult hit = targetBox.rayTrace(start.toVector(), direction.normalize(), dist);

                if (hit != null) {
                    Set<Integer> hitSegments = data.segmentHits.computeIfAbsent(target.getUniqueId(), k -> new HashSet<>());
                    if (!hitSegments.contains(i)) {
                        ForceDamage forceDamage = new ForceDamage(target, 3.0, source);
                        forceDamage.applyEffect(player);
                        Stun stun = new Stun(target, 3000L);
                        stun.applyEffect(player);
                        hitSegments.add(i);
                    }
                }
            }
        }
    }

    private void checkRetractionHit(List<Location> points, int segmentIndex, Player player) {
        if (segmentIndex < 0 || segmentIndex >= points.size() - 1) return;

        Location start = points.get(segmentIndex);
        Location end = points.get(segmentIndex + 1);
        Vector direction = end.toVector().subtract(start.toVector());
        double dist = direction.length();

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        BoundingBox searchBox = BoundingBox.of(start, end).expand(0.5);

        Collection<Entity> nearbyEntities = player.getWorld().getNearbyEntities(searchBox,
                entity -> entity != player && entity instanceof LivingEntity);

        for (Entity entity : nearbyEntities) {
            LivingEntity target = (LivingEntity) entity;
            BoundingBox targetBox = target.getBoundingBox().expand(0.3);
            RayTraceResult hit = targetBox.rayTrace(start.toVector(), direction.normalize(), dist);

            if (hit != null) {
                ForceDamage forceDamage = new ForceDamage(target, 3.0, source);
                forceDamage.applyEffect(player);
                target.removePotionEffect(PotionEffectType.SLOWNESS);
            }
        }
    }

    private static class SwordData {
        private final List<Location> path;
        private final Map<UUID, Set<Integer>> segmentHits = new HashMap<>();
        private BukkitTask task;
        private BukkitTask autoRetractTask;
        public int lastDrawnIndex = 0;
        public boolean isRetracting = false;

        public SwordData(List<Location> path) {
            this.path = path;
        }

        public void setTask(BukkitTask task) { this.task = task; }
        public BukkitTask getTask() { return task; }
        public void setAutoRetractTask(BukkitTask task) { this.autoRetractTask = task; }
        public BukkitTask getAutoRetractTask() { return autoRetractTask; }
    }
}