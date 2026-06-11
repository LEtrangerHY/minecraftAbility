package org.core.coreSystem.cores.VOL3.Charlotte.Skill;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL3.Charlotte.coreSystem.Charlotte;
import org.core.effect.crowdControl.ForceDamage;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Q implements SkillBase {

    private final Charlotte config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final NamespacedKey keyQ;

    private static final BlockData WHITE_STAINED_GLASS = Material.WHITE_STAINED_GLASS.createBlockData();
    private static final Map<UUID, QSession> glassSessions = new HashMap<>();

    public Q(Charlotte config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.keyQ = new NamespacedKey(plugin, "Q");

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onBlockBreak(BlockBreakEvent event) {
                if (isProtectedBlock(event.getBlock())) {
                    event.setCancelled(true);
                }
            }
            @EventHandler
            public void onEntityExplode(EntityExplodeEvent event) {
                event.blockList().removeIf(this::isProtectedBlock);
            }
            @EventHandler
            public void onBlockExplode(BlockExplodeEvent event) {
                event.blockList().removeIf(this::isProtectedBlock);
            }

            private boolean isProtectedBlock(Block b) {
                for (QSession session : glassSessions.values()) {
                    for (PlacedBlock pb : session.placedBlocks) {
                        if (pb.block.equals(b)) return true;
                    }
                }
                return false;
            }
        }, plugin);
    }

    public static boolean isRecastPhase(Player player) {
        QSession session = glassSessions.get(player.getUniqueId());
        return session != null && session.landed;
    }

    public static boolean isMoving(Player player) {
        QSession session = glassSessions.get(player.getUniqueId());
        return session != null && !session.landed;
    }

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();
        QSession session = glassSessions.get(uuid);

        if (!cool.isReloading(player, "Q Reuse")) {
            throwGlassWall(player);
        } else {
            if (session != null && session.landed) {
                detonateGlassWall(player, session);
            }
        }
    }

    private void throwGlassWall(Player player) {
        UUID uuid = player.getUniqueId();
        World world = player.getWorld();
        Location startLoc = player.getLocation();

        Vector forward = startLoc.getDirection().clone();
        forward.setY(0).normalize();

        Vector up = new Vector(0, 1, 0);
        Vector right = forward.clone().crossProduct(up).normalize();

        BlockDisplay[] displays = new BlockDisplay[15];
        for (int i = 0; i < 15; i++) {
            BlockDisplay display = (BlockDisplay) world.spawnEntity(startLoc, EntityType.BLOCK_DISPLAY);
            // [수정] 꽉 찬 색유리 블럭 세팅
            display.setBlock(WHITE_STAINED_GLASS);
            display.setTeleportDuration(1);
            display.setTransformation(new Transformation(
                    new Vector3f(-0.5f, -0.5f, -0.5f), new Quaternionf(), new Vector3f(1f, 1f, 1f), new Quaternionf()
            ));
            displays[i] = display;
        }

        world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.5f, 0.8f);
        world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.2f);

        QSession session = new QSession(displays, forward, right, up, WHITE_STAINED_GLASS);
        glassSessions.put(uuid, session);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 12;
            final double speed = 6.0 / maxTicks;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !glassSessions.containsKey(uuid)) {
                    cleanupSession(uuid, 0L);
                    this.cancel();
                    return;
                }

                Location currentCenter = startLoc.clone().add(forward.clone().multiply(ticks * speed));
                updateDisplays(session, currentCenter);

                handleWallCollision(player, currentCenter, forward, right, up);

                if (ticks >= maxTicks) {
                    placeBlocks(session, currentCenter, world);
                    session.landed = true;
                    session.centerLoc = currentCenter;

                    cool.setCooldown(player, 12000L, "Q Reuse", "boss");

                    session.timeoutTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            cleanupSession(uuid, config.q_Skill_Cool);
                        }
                    }.runTaskLater(plugin, 240L);

                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void detonateGlassWall(Player player, QSession session) {
        UUID uuid = player.getUniqueId();
        World world = player.getWorld();

        if (session.timeoutTask != null) session.timeoutTask.cancel();
        cool.updateCooldown(player, "Q Reuse", 0L, "boss");

        revertBlocks(session);

        for (int i = 0; i < 15; i++) {
            BlockDisplay display = (BlockDisplay) world.spawnEntity(session.centerLoc, EntityType.BLOCK_DISPLAY);
            display.setBlock(session.paneData);
            display.setTeleportDuration(1);
            display.setTransformation(new Transformation(
                    new Vector3f(-0.5f, -0.5f, -0.5f), new Quaternionf(), new Vector3f(1f, 1f, 1f), new Quaternionf()
            ));
            session.displays[i] = display;
        }

        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.5f, 0.8f);
        Location baseStartLoc = session.centerLoc.clone();

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 12;
            final double speed = 6.0 / maxTicks;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cleanupSession(uuid, config.q_Skill_Cool);
                    this.cancel();
                    return;
                }

                Location currentCenter = baseStartLoc.clone().add(session.forward.clone().multiply(ticks * speed));
                updateDisplays(session, currentCenter);
                handleWallCollision(player, currentCenter, session.forward, session.right, session.up);

                if (ticks >= maxTicks) {
                    triggerBurst(player, session, currentCenter);

                    cleanupSession(uuid, config.q_Skill_Cool);
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void triggerBurst(Player player, QSession session, Location centerLoc) {
        World world = player.getWorld();

        world.playSound(centerLoc, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
        world.playSound(centerLoc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 2.0f, 1.2f);
        world.playSound(centerLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.5f, 1.5f);

        world.spawnParticle(Particle.BLOCK, centerLoc, 150, 2.5, 1.5, 2.5, 0.5, WHITE_STAINED_GLASS);
        world.spawnParticle(Particle.END_ROD, centerLoc, 80, 1.5, 1.0, 1.5, 0.4);
        world.spawnParticle(Particle.ENCHANTED_HIT, centerLoc, 100, 2.0, 1.5, 2.0, 0.5);

        long level = player.getPersistentDataContainer().getOrDefault(keyQ, PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_Damage * (1 + (config.q_Skill_amp * level));

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        for (Entity entity : world.getNearbyEntities(centerLoc, 6.0, 6.0, 6.0)) {
            if (entity instanceof LivingEntity target && !entity.equals(player)) {
                ForceDamage forceDamage = new ForceDamage(target, damage, source, false);
                forceDamage.applyEffect(player);

                Vector knockbackDir = entity.getLocation().toVector().subtract(centerLoc.toVector());
                if (knockbackDir.lengthSquared() < 0.01) {
                    knockbackDir = session.forward.clone();
                }
                knockbackDir.normalize().multiply(1.5).setY(0.4);
                target.setVelocity(knockbackDir);
            }
        }
    }

    private void handleWallCollision(Player player, Location center, Vector forward, Vector right, Vector up) {
        World world = player.getWorld();
        Location wallCenter = center.clone().add(up.clone().multiply(1.0));

        double widthLimit = 2.6;
        double heightLimit = 1.6;
        double depthLimit = 0.7;

        for (Entity entity : world.getNearbyEntities(wallCenter, 4.5, 4.5, 4.5)) {
            if (entity.equals(player)) continue;

            Vector entityCenter = entity.getLocation().toVector();
            if (entity instanceof LivingEntity) {
                entityCenter.add(new Vector(0, entity.getHeight() / 2, 0));
            }

            Vector toEntity = entityCenter.subtract(wallCenter.toVector());

            double dotRight = toEntity.dot(right);
            double dotUp = toEntity.dot(up);
            double dotForward = toEntity.dot(forward);

            if (Math.abs(dotRight) <= widthLimit && Math.abs(dotUp) <= heightLimit && Math.abs(dotForward) <= depthLimit) {
                if (entity instanceof Projectile proj) {
                    if (proj.getShooter() == player) continue;
                    world.playSound(entity.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                    world.spawnParticle(Particle.BLOCK, entity.getLocation(), 15, 0.2, 0.2, 0.2, WHITE_STAINED_GLASS);
                    proj.remove();
                } else if (entity instanceof LivingEntity) {
                    Vector pushVector = forward.clone().multiply(0.8);
                    pushVector.setY(0.2);
                    entity.setVelocity(pushVector);
                }
            }
        }
    }

    private void updateDisplays(QSession session, Location center) {
        float playerYaw = center.getYaw();
        float playerPitch = center.getPitch();

        int index = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = -2; col <= 2; col++) {
                Vector offset = session.right.clone().multiply(col).add(session.up.clone().multiply(row));
                Location targetLoc = center.clone().add(offset);

                targetLoc.setYaw(playerYaw);
                targetLoc.setPitch(playerPitch);

                BlockDisplay display = session.displays[index];
                if (display != null && display.isValid()) {
                    display.teleport(targetLoc);
                }
                index++;
            }
        }
    }

    private void placeBlocks(QSession session, Location center, World world) {
        for (BlockDisplay display : session.displays) {
            if (display != null && display.isValid()) display.remove();
        }

        Set<Block> processedBlocks = new HashSet<>();

        for (int row = 0; row < 3; row++) {
            for (int col = -2; col <= 2; col++) {
                Vector offset = session.right.clone().multiply(col).add(session.up.clone().multiply(row));
                Location targetLoc = center.clone().add(offset);

                Block block = targetLoc.getBlock();

                if (!processedBlocks.add(block)) continue;

                if (block.getType() != Material.BEDROCK && block.getType() != Material.END_PORTAL_FRAME) {
                    session.placedBlocks.add(new PlacedBlock(block, block.getBlockData()));
                    block.setBlockData(session.paneData, false);
                }
            }
        }
        world.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.0f, 1.0f);
    }

    private void revertBlocks(QSession session) {
        for (PlacedBlock pb : session.placedBlocks) {
            pb.block.setBlockData(pb.oldData, false);
        }
        session.placedBlocks.clear();
    }

    private void cleanupSession(UUID uuid, long finalCooldown) {
        QSession session = glassSessions.remove(uuid);
        if (session != null) {
            if (session.timeoutTask != null) session.timeoutTask.cancel();
            revertBlocks(session);

            for (BlockDisplay display : session.displays) {
                if (display != null && display.isValid()) display.remove();
            }

            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                cool.updateCooldown(player, "Q Reuse", 0L, "boss");
                if (finalCooldown > 0) {
                    cool.setCooldown(player, finalCooldown, "Q");
                }
            }
        }
    }

    private static class PlacedBlock {
        Block block;
        BlockData oldData;
        public PlacedBlock(Block block, BlockData oldData) {
            this.block = block;
            this.oldData = oldData;
        }
    }

    private static class QSession {
        BlockDisplay[] displays;
        List<PlacedBlock> placedBlocks = new ArrayList<>();
        Vector forward, right, up;
        BlockData paneData;
        Location centerLoc;
        boolean landed = false;
        BukkitTask timeoutTask;

        public QSession(BlockDisplay[] displays, Vector forward, Vector right, Vector up, BlockData paneData) {
            this.displays = displays;
            this.forward = forward;
            this.right = right;
            this.up = up;
            this.paneData = paneData;
        }
    }
}