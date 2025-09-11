package org.core.coreProgram.Cores.Glacier.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.Glacier.coreSystem.Glacier;

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

        if (offhandItem.getType() == Material.BLUE_ICE && offhandItem.getAmount() >= 20) {
            World world = player.getWorld();
            Location center = player.getLocation().clone();

            SetBiome(world, center, 15);

            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 0.6f);
            player.spawnParticle(Particle.DUST, center.add(0, 1, 0), 1000, 8, 8, 8, 0, dustOptions);

            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1, 1);

            player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.0f);

            for (Entity entity : world.getNearbyEntities(center, 4, 4, 4)) {
                if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

                player.spawnParticle(Particle.EXPLOSION, entity.getLocation().clone().add(0, 1, 0), 1, 0, 0, 0, 0);

                Vector direction = entity.getLocation().toVector().subtract(center.toVector()).normalize().multiply(2.2);
                direction.setY(0.4);

                entity.setVelocity(direction);
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                FreezeEntity(player, center, 2);
            }, 6);

            offhandItem.setAmount(offhandItem.getAmount() - 20);
        }else{
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, 1);
            player.sendActionBar(Component.text("Blue Ice needed").color(NamedTextColor.RED));
            long cools = 100L;
            cool.updateCooldown(player, "F", cools);
        }

    }

    public void SetBiome(World world, Location center, int radius) {
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        int radiusSquared = radius * radius;

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = cy - radius; y <= cy + radius; y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    int dx = x - cx;
                    int dy = y - cy;
                    int dz = z - cz;
                    if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                        world.setBiome(x, y, z, Biome.SNOWY_PLAINS);
                    }
                }
            }
        }

        for (Player cplayer : world.getNearbyPlayers(center, radius + 16)) {
            int chunkX = cplayer.getLocation().getBlockX() >> 4;
            int chunkZ = cplayer.getLocation().getBlockZ() >> 4;
            world.refreshChunk(chunkX, chunkZ);
        }
    }

    public void FreezeEntity(Player player, Location center, int radius) {
        World world = player.getWorld();
        int radiusSquared = radius * radius;

        for (Entity rangeTarget : world.getNearbyEntities(player.getLocation(), 15.0, 15.0, 15.0)) {
            if (rangeTarget instanceof LivingEntity target && rangeTarget != player) {

                Location TLoc = target.getLocation().clone();

                int cx = TLoc.getBlockX();
                int cy = TLoc.getBlockY();
                int cz = TLoc.getBlockZ();

                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {

                            if (x * x + y * y + z * z > radiusSquared) continue;

                            Block block = world.getBlockAt(cx + x, cy + y, cz + z);

                            if (block.isPassable() || block.getType() == Material.AIR) {
                                block.setType(Material.BLUE_ICE);
                            }
                        }
                    }
                }

            }
        }

        world.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.1f);
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.1f);
        world.spawnParticle(Particle.SNOWFLAKE, center, 80, 1.5, 1.5, 1.5, 0.1);
    }

}
