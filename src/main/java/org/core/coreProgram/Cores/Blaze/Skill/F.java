package org.core.coreProgram.Cores.Blaze.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Debuff.Burn;
import org.core.Effect.ForceDamage;
import org.core.Effect.Stun;
import org.core.coreProgram.Abs.SkillBase;
import org.core.coreProgram.Cores.Blaze.Passive.BlueFlame;
import org.core.coreProgram.Cores.Blaze.coreSystem.Blaze;
import org.core.coreProgram.Cores.Dagger.Passive.DamageStroke;
import org.core.coreProgram.Cores.Dagger.coreSystem.Dagger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class F implements SkillBase {
    private final Blaze config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final BlueFlame blueFlame;

    public F(Blaze config, JavaPlugin plugin, Cool cool, BlueFlame blueFlame) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.blueFlame = blueFlame;
    }

    @Override
    public void Trigger(Player player){

        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

        if (offhandItem.getType() == Material.SOUL_LANTERN || ((offhandItem.getType() == Material.SOUL_SAND || offhandItem.getType() == Material.SOUL_SOIL) && offhandItem.getAmount() >= 30)) {
            World world = player.getWorld();
            Location center = player.getLocation().clone();

            setBiome(world, center, 21);

            PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, 20 * 20, 2, false, false);
            player.addPotionEffect(speed);

            player.spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().clone().add(0, 0.6, 0), 666, 0.1, 0.1, 0.1, 0.8);

            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_BURN, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1, 1);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 1);

            List<Entity> entities = new ArrayList<>(world.getNearbyEntities(center, 13, 13, 13));
            new BukkitRunnable() {
                int index = 0;

                @Override
                public void run() {
                    if (index >= entities.size()) {
                        cancel();
                        return;
                    }

                    Entity entity = entities.get(index);

                    if (!entity.equals(player) && entity instanceof LivingEntity && !entity.isDead()) {
                        blueFlameInitiate(player, (LivingEntity) entity);
                    }

                    index++;
                }
            }.runTaskTimer(plugin, 0L, 4L);


            if((offhandItem.getType() == Material.SOUL_SAND || offhandItem.getType() == Material.SOUL_SOIL) && offhandItem.getAmount() >= 30) {
                offhandItem.setAmount(offhandItem.getAmount() - 30);
            }else if(offhandItem.getType() == Material.SOUL_LANTERN){
                cool.setCooldown(player, 13000L, "R");
                cool.setCooldown(player, 13000L, "Q");
            }
        }else{
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1, 1);
            player.sendActionBar(Component.text("Soul needed").color(NamedTextColor.RED));
            long cools = 100L;
            cool.updateCooldown(player, "F", cools);
        }

    }

    public void blueFlameInitiate(Player player, LivingEntity victim){

        player.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0f, 1.0f);
        player.getWorld().playSound(victim.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);

        new BukkitRunnable(){
            int tick = 0;

            @Override
            public void run(){

                if(tick > 13 || victim.isDead() || player.isDead()){

                    blueFlamePool(player, victim);

                    cancel();
                    return;
                }

                Location bottom = victim.getLocation().clone().add(0, 0.2, 0);

                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, bottom, 4, 0.4, 0.3, 0.4, 0);
                player.getWorld().spawnParticle(Particle.SOUL, bottom, 4, 0.4, 0.3, 0.4, 0.04);

                tick++;

            }
        }.runTaskTimer(plugin, 0L, 1L);

    }

    public void blueFlamePool(Player player, LivingEntity victim){

        player.spawnParticle(Particle.SOUL_FIRE_FLAME, victim.getLocation().clone().add(0, 0.6, 0), 20, 0.1, 0.1, 0.1, 0.8);
        player.spawnParticle(Particle.FLAME, victim.getLocation().clone().add(0, 0.6, 0), 6, 0.1, 0.1, 0.1, 0.8);

        player.playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        for (Entity entity : victim.getWorld().getNearbyEntities(victim.getLocation().clone().add(0, 0.2, 0), 0.3, 13, 0.3)) {
            if (entity instanceof LivingEntity target && entity != player) {

                ForceDamage forceDamage = new ForceDamage(target, 13);
                forceDamage.applyEffect(player);

                Stun stun = new Stun(victim, 2000);
                stun.applyEffect(player);

                Burn burn = new Burn(target, 6000L);
                burn.applyEffect(player);
                PotionEffect wither = new PotionEffect(PotionEffectType.WITHER, 20 * 6, 3, false, false);
                target.addPotionEffect(wither);

            }
        }

        new BukkitRunnable(){
            int tick = 0;
            @Override
            public void run(){

                if(tick > 40 || victim.isDead() || player.isDead()){
                    cancel();
                    return;
                }

                for(int i = 0; i < 77; i++){
                    Location particleLoc = victim.getLocation().clone().add(0, i / 10.0, 0);
                    player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0.04);
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void setBiome(World world, Location center, int radius) {
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        int radiusSquared = radius * radius;
        int minY = Math.max(world.getMinHeight(), cy - radius);
        int maxY = Math.min(world.getMaxHeight(), cy + radius);

        Set<Material> sandLike = Set.of(Material.SAND, Material.GRAVEL);
        Set<Material> ores = Set.of(
                Material.DIAMOND_ORE, Material.IRON_ORE, Material.COPPER_ORE, Material.GOLD_ORE,
                Material.COAL_ORE, Material.REDSTONE_ORE, Material.LAPIS_ORE, Material.EMERALD_ORE,
                Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_IRON_ORE,
                Material.DEEPSLATE_COPPER_ORE, Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_REDSTONE_ORE,
                Material.DEEPSLATE_LAPIS_ORE, Material.DEEPSLATE_EMERALD_ORE
        );
        Set<Material> replaceable = Set.of(
                Material.STONE, Material.DEEPSLATE, Material.END_STONE, Material.NETHERRACK, Material.DIRT,
                Material.GRASS_BLOCK, Material.DIRT_PATH, Material.FARMLAND, Material.MUD, Material.CLAY,
                Material.GRANITE, Material.ANDESITE, Material.DIORITE, Material.TUFF,
                Material.CRIMSON_NYLIUM, Material.WARPED_NYLIUM, Material.SANDSTONE
        );

        Set<Chunk> modifiedChunks = new HashSet<>();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    int dx = x - cx;
                    int dy = y - cy;
                    int dz = z - cz;

                    double distanceSquared = dx * dx + dy * dy + dz * dz;
                    if (distanceSquared > radiusSquared) continue;

                    boolean isEdge = distanceSquared >= (radius - 1) * (radius - 1);

                    world.setBiome(x, y, z, Biome.PALE_GARDEN);

                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();

                    if (sandLike.contains(type)) {
                        block.setType(Material.SOUL_SAND);
                        if (Math.random() < 0.4 || isEdge) {
                            Block above = block.getRelative(BlockFace.UP);
                            if (above.getType() == Material.AIR) {
                                above.setType(Material.FIRE);
                            }
                        }
                    } else if (ores.contains(type)) {
                        block.setType(Material.BONE_BLOCK);
                    } else if (replaceable.contains(type)) {
                        block.setType(Material.SOUL_SOIL);
                        if (Math.random() < 0.4 || isEdge) {
                            Block above = block.getRelative(BlockFace.UP);
                            if (above.getType() == Material.AIR) {
                                above.setType(Material.FIRE);
                            }
                        }
                    }

                    modifiedChunks.add(block.getChunk());
                }
            }
        }

        for (Chunk chunk : modifiedChunks) {
            world.refreshChunk(chunk.getX(), chunk.getZ());
        }
    }
}
