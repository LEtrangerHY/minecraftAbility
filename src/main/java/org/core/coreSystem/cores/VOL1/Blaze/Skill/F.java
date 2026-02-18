package org.core.coreSystem.cores.VOL1.Blaze.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.core.cool.Cool;
import org.core.effect.debuff.Burn;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Stun;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Blaze.Passive.BlueFlame;
import org.core.coreSystem.cores.VOL1.Blaze.coreSystem.Blaze;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class F implements SkillBase {
    private final Blaze config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final BlueFlame blueFlame;

    private static final Set<Material> SAND_LIKE = Set.of(Material.SAND, Material.GRAVEL);
    private static final Set<Material> ORES = Set.of(
            Material.DIAMOND_ORE, Material.IRON_ORE, Material.COPPER_ORE, Material.GOLD_ORE,
            Material.COAL_ORE, Material.REDSTONE_ORE, Material.LAPIS_ORE, Material.EMERALD_ORE,
            Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.DEEPSLATE_COPPER_ORE, Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.DEEPSLATE_LAPIS_ORE, Material.DEEPSLATE_EMERALD_ORE
    );
    private static final Set<Material> REPLACEABLE = Set.of(
            Material.STONE, Material.DEEPSLATE, Material.END_STONE, Material.NETHERRACK, Material.DIRT,
            Material.GRASS_BLOCK, Material.DIRT_PATH, Material.FARMLAND, Material.MUD, Material.CLAY,
            Material.GRANITE, Material.ANDESITE, Material.DIORITE, Material.TUFF,
            Material.CRIMSON_NYLIUM, Material.WARPED_NYLIUM, Material.SANDSTONE
    );
    private static final Set<Material> MELTABLE = Set.of(
            Material.ICE, Material.FROSTED_ICE, Material.BLUE_ICE, Material.PACKED_ICE, Material.SNOW_BLOCK, Material.POWDER_SNOW
    );

    public F(Blaze config, JavaPlugin plugin, Cool cool, BlueFlame blueFlame) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.blueFlame = blueFlame;
    }

    @Override
    public void Trigger(Player player) {
        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);
        boolean isMaterial = (offhandItem.getType() == Material.SOUL_SAND || offhandItem.getType() == Material.SOUL_SOIL) && offhandItem.getAmount() > 30;
        boolean isLantern = offhandItem.getType() == Material.SOUL_LANTERN;

        if (isLantern || isMaterial) {
            World world = player.getWorld();
            Location center = player.getLocation();

            processBiomeChangeDistributed(world, center, 21);

            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 6, 1, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 13, 1, false, false));

            player.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.6, 0), 666, 0.1, 0.1, 0.1, 0.8);

            world.playSound(center, Sound.ENTITY_BLAZE_BURN, 1.0f, 1.0f);
            world.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
            world.playSound(center, Sound.ENTITY_WITHER_SPAWN, 2.0f, 1);
            world.playSound(center, Sound.ENTITY_WITHER_DEATH, 2.0f, 1);

            for (Entity entity : world.getNearbyEntities(center.clone().add(0, 0.2, 0), 13, 13, 13)) {
                if (entity instanceof LivingEntity target && entity != player) {
                    blueFlameInitiate(player, target);
                }
            }

            if (isMaterial) {
                offhandItem.setAmount(offhandItem.getAmount() - 30);
            } else {
                cool.setCooldown(player, 13000L, "R");
                cool.setCooldown(player, 13000L, "Q");
            }
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1, 1);

            Title title = Title.title(
                    Component.empty(),
                    Component.text("Soul needed").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);

            cool.updateCooldown(player, "F", 500L);
        }
    }

    public void blueFlameInitiate(Player player, LivingEntity victim) {
        World world = player.getWorld();
        Location victimLoc = victim.getLocation();

        world.playSound(victimLoc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0f, 1.0f);
        world.playSound(victimLoc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);

        Location pollLoc = victimLoc.clone().add(0, 0.2, 0);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick > 13 || player.isDead()) {
                    blueFlamePool(player, pollLoc);
                    cancel();
                    return;
                }

                world.spawnParticle(Particle.SOUL_FIRE_FLAME, pollLoc, 4, 0.4, 0.3, 0.4, 0);
                world.spawnParticle(Particle.SOUL, pollLoc, 4, 0.4, 0.3, 0.4, 0.04);

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void blueFlamePool(Player player, Location pollLoc) {
        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = 0.6 * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        World world = player.getWorld();
        Location particleBase = pollLoc.clone().add(0, 0.6, 0);

        player.spawnParticle(Particle.SOUL_FIRE_FLAME, particleBase, 20, 0.1, 0.1, 0.1, 0.8);
        player.spawnParticle(Particle.FLAME, particleBase, 6, 0.1, 0.1, 0.1, 0.8);

        world.playSound(pollLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 1.0f);
        world.playSound(pollLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        PotionEffect wither = new PotionEffect(PotionEffectType.WITHER, 20 * 13, 3, false, false);

        for (Entity entity : world.getNearbyEntities(pollLoc, 1.3, 13, 1.3)) {
            if (entity instanceof LivingEntity target && entity != player) {
                new Stun(target, 3300L).applyEffect(player);
                new Burn(target, 13000L).applyEffect(player);
                target.addPotionEffect(wither);
            }
        }

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick > 66) {
                    cancel();
                    return;
                }

                for (double i = 0; i < 9.0; i += 0.5) {
                    Location particleLoc = pollLoc.clone().add(0, i, 0);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 2, 0.2, 0.2, 0.2, 0.06);
                    if (tick % 2 == 0) {
                        world.spawnParticle(Particle.FLAME, particleLoc, 2, 0.24, 0.24, 0.24, 0.13);
                    }
                }

                for (Entity entity : world.getNearbyEntities(pollLoc, 1.3, 13, 1.3)) {
                    if (entity instanceof LivingEntity target && entity != player) {
                        ForceDamage forceDamage = new ForceDamage(target, damage, source, false);
                        forceDamage.applyEffect(player);
                    }
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
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
            final int blocksPerTick = 2000;

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

                            int i = dx * dx + dy * dy + dz * dz;
                            if (i > radiusSquared) continue;

                            blocksProcessed++;

                            processSingleBlock(world, currentX, y, z, i, radius, modifiedChunks);
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

    private void processSingleBlock(World world, int x, int y, int z, double distSq, int radius, Set<Chunk> modifiedChunks) {
        boolean isEdge = distSq >= (radius - 1) * (radius - 1);

        world.setBiome(x, y, z, Biome.PALE_GARDEN);

        Block block = world.getBlockAt(x, y, z);
        Material type = block.getType();

        boolean changed = false;

        if (MELTABLE.contains(type)) {
            block.setType(Material.AIR);
            changed = true;
        } else if (SAND_LIKE.contains(type)) {
            block.setType(Material.SOUL_SAND);
            handleFireAbove(block, isEdge);
            changed = true;
        } else if (ORES.contains(type)) {
            block.setType(Material.BONE_BLOCK);
            changed = true;
        } else if (REPLACEABLE.contains(type)) {
            block.setType(Material.SOUL_SOIL);
            handleFireAbove(block, isEdge);
            changed = true;
        }

        if (changed) {
            modifiedChunks.add(block.getChunk());
        }
    }

    private void handleFireAbove(Block block, boolean isEdge) {
        if (ThreadLocalRandom.current().nextDouble() < 0.13 || isEdge) {
            Block above = block.getRelative(BlockFace.UP);
            if (above.getType() == Material.AIR) {
                above.setType(Material.FIRE);
            }
        }
    }
}