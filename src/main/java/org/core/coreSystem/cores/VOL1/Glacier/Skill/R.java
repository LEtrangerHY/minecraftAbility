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
import java.util.Set;

public class R implements SkillBase {
    private final Glacier config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R(Glacier config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

        if (offhandItem.getType() == Material.BLUE_ICE && offhandItem.getAmount() > 1) {
            World world = player.getWorld();
            Location playerLocation = player.getLocation();
            Vector direction = playerLocation.getDirection().normalize().multiply(1.4);

            world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1, 1);
            config.Rcollision.put(player.getUniqueId(), false);
            config.entityCollision.put(player.getUniqueId(), false);

            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (ticks >= 12 || config.Rcollision.getOrDefault(player.getUniqueId(), true)) {
                        config.Rcollision.remove(player.getUniqueId());

                        Location endLocation = playerLocation.clone()
                                .add(direction.clone().multiply(Math.min(ticks, 16) * 1.5))
                                .add(0, 1.4, 0);

                        freeze(player, endLocation);
                        this.cancel();
                        return;
                    }

                    Location particleLocation = playerLocation.clone()
                            .add(direction.clone().multiply(ticks * 1.5))
                            .add(0, 1.4, 0);

                    for (Entity entity : particleLocation.getWorld().getNearbyEntities(particleLocation, 1, 1, 1)) {
                        if (entity instanceof LivingEntity target && !target.equals(player)) {

                            config.entityCollision.put(player.getUniqueId(), true);
                            config.Rcollision.put(player.getUniqueId(), true);

                            cool.updateCooldown(player, "R", 36000L);

                            freeze(player, target.getLocation());
                            return;
                        }
                    }

                    Block block = particleLocation.getBlock();

                    boolean playerInWater = player.getLocation().getBlock().getType() == Material.WATER;

                    boolean waterCollision = block.getType() == Material.WATER && !playerInWater;

                    if (!block.isPassable() || waterCollision) {
                        config.Rcollision.put(player.getUniqueId(), true);
                        return;
                    }

                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 0.6f);
                    world.spawnParticle(Particle.DUST, particleLocation, 5, 0.1, 0.1, 0.1, 0, dustOptions);

                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);

            offhandItem.setAmount(offhandItem.getAmount() - 1);
        }else{
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, 1);

            Title title = Title.title(
                    Component.empty(),
                    Component.text("Blue Ice needed").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);

            long cools = 500L;
            cool.updateCooldown(player, "R", cools);
        }
    }

    private static final Set<Biome> COLD_BIOMES = Set.of(
            Biome.SNOWY_PLAINS,
            Biome.SNOWY_TAIGA,
            Biome.SNOWY_BEACH,
            Biome.ICE_SPIKES,
            Biome.FROZEN_RIVER,
            Biome.FROZEN_OCEAN,
            Biome.DEEP_FROZEN_OCEAN
    );

    public void freeze(Player player, Location center) {
        World world = center.getWorld();
        int radius = 3;
        int radiusSquared = radius * radius;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        Biome biome = world.getBiome(center);
        Material iceType = COLD_BIOMES.contains(biome) ? Material.BLUE_ICE : Material.FROSTED_ICE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    if (x * x + y * y + z * z > radiusSquared) continue;

                    Block block = world.getBlockAt(cx + x, cy + y, cz + z);
                    if (block.getType() == Material.WATER) {
                        block.setType(iceType);
                    }

                    if(config.entityCollision.getOrDefault(player.getUniqueId(), true)){
                        if (block.getType() == Material.AIR || block.isPassable()) {
                            block.setType(iceType);
                        }
                    }
                }
            }
        }

        config.entityCollision.remove(player.getUniqueId());

        world.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.1f);
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.1f);
        world.spawnParticle(Particle.SNOWFLAKE, center, 80, 1.5, 1.5, 1.5, 0.1);
    }
}