package org.core.coreSystem.cores.VOL1.Glacier.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Glacier.coreSystem.Glacier;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class F implements SkillBase {

    private final Glacier config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Glacier config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){

        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

        if (offhandItem.getType() == Material.BLUE_ICE && offhandItem.getAmount() > 20) {
            World world = player.getWorld();
            Location center = player.getLocation().clone();

            processBiomeChangeDistributed(world, center, 15);

            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 0.6f);
            world.spawnParticle(Particle.DUST, center.clone().add(0, 1, 0), 1000, 8, 8, 8, 0, dustOptions);

            world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
            world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1, 1);
            world.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.0f);

            for (Entity entity : world.getNearbyEntities(center, 4, 4, 4)) {
                if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

                world.spawnParticle(Particle.EXPLOSION, entity.getLocation().clone().add(0, 1, 0), 1, 0, 0, 0, 0);

                Vector direction = entity.getLocation().toVector().subtract(center.toVector()).normalize().multiply(2.2);
                direction.setY(0.4);

                entity.setVelocity(direction);
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                FreezeEntity(player, center, 2);
            }, 6);

            offhandItem.setAmount(offhandItem.getAmount() - 20);
        } else {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, 1);

            Title title = Title.title(
                    Component.empty(),
                    Component.text("blue ice needed").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);

            long cools = 500L;
            cool.updateCooldown(player, "F", cools);
        }

    }

    private void processBiomeChangeDistributed(World world, Location center, int radius) {
        final int cx = center.getBlockX();
        final int cy = center.getBlockY();
        final int cz = center.getBlockZ();
        final int radiusSquared = radius * radius;
        final int minY = Math.max(world.getMinHeight(), cy - radius);
        final int maxY = Math.min(world.getMaxHeight(), cy + radius);

        new BukkitRunnable() {
            int currentX = cx - radius;
            final int endX = cx + radius;
            final int blocksPerTick = 2500;

            final Set<Chunk> modifiedChunks = new HashSet<>();

            @Override
            public void run() {
                int blocksProcessed = 0;

                while (currentX <= endX) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = cz - radius; z <= cz + radius; z++) {

                            int dx = currentX - cx;
                            int dy = y - cy;
                            int dz = z - cz;

                            if (dx * dx + dy * dy + dz * dz > radiusSquared) continue;

                            blocksProcessed++;

                            world.setBiome(currentX, y, z, Biome.SNOWY_PLAINS);

                            if (blocksProcessed % 64 == 0) {
                                modifiedChunks.add(world.getBlockAt(currentX, y, z).getChunk());
                            }
                        }
                    }
                    currentX++;

                    if (blocksProcessed >= blocksPerTick) {
                        return;
                    }
                }

                for (Chunk chunk : modifiedChunks) {
                    world.refreshChunk(chunk.getX(), chunk.getZ());
                }
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void FreezeEntity(Player player, Location center, int radius) {
        World world = player.getWorld();
        int radiusSquared = radius * radius;

        Set<Block> blocksToFreeze = new HashSet<>();

        for (Entity rangeTarget : world.getNearbyEntities(player.getLocation(), 15.0, 15.0, 15.0)) {
            if (rangeTarget instanceof LivingEntity target && rangeTarget != player) {

                Location TLoc = target.getLocation();
                int cx = TLoc.getBlockX();
                int cy = TLoc.getBlockY();
                int cz = TLoc.getBlockZ();

                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {

                            if (x * x + y * y + z * z > radiusSquared) continue;

                            Block block = world.getBlockAt(cx + x, cy + y, cz + z);

                            if (block.isPassable() || block.getType() == Material.AIR) {
                                blocksToFreeze.add(block);
                            }
                        }
                    }
                }
            }
        }

        for (Block block : blocksToFreeze) {
            block.setType(Material.BLUE_ICE, false);
        }

        world.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.1f);
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.1f);
        world.spawnParticle(Particle.SNOWFLAKE, center, 80, 1.5, 1.5, 1.5, 0.1);
    }
}