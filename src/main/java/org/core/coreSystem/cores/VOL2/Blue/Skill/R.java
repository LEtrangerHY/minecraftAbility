package org.core.coreSystem.cores.VOL2.Blue.Skill;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Blue.coreSystem.Blue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class R implements SkillBase {
    private final Blue config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R(Blue config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){

        World world = player.getWorld();

        if(cool.isReloading(player, "R Reuse")) {
            world.spawnParticle(Particle.WITCH, player.getLocation().clone().add(0, 1, 0), 80, 1.5, 1.5, 1.5, 0.1);
            world.spawnParticle(Particle.SMOKE, player.getLocation().clone().add(0, 1, 0), 80, 1.5, 1.5, 1.5, 1);

            world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.6f, 1.0f);
            world.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.3f, 1.0f);
            cool.updateCooldown(player, "R Reuse", 0L, "boss");
            slowFlower(player, config.Flower.getOrDefault(player.getUniqueId(), new ArrayList<>()));
            return;

        }else{
            world.spawnParticle(Particle.WITCH, player.getLocation().clone().add(0, 1, 0), 80, 1.5, 1.5, 1.5, 0.1);
            world.spawnParticle(Particle.SMOKE, player.getLocation().clone().add(0, 1, 0), 80, 1.5, 1.5, 1.5, 1);

            world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);
            world.playSound(player.getLocation(), Sound.BLOCK_GRASS_PLACE, 1, 1);
            cool.updateCooldown(player, "R", 0L);
            cool.setCooldown(player, 4000L, "R Reuse", "boss");
            placeWitherFlower(player, 10.0, 44.0);
        }

    }

    public void placeWitherFlower(Player player, double radius, double angleDegrees) {
        Location playerLoc = player.getLocation();
        World world = player.getWorld();

        Vector forward = playerLoc.getDirection().setY(0).normalize();
        Vector origin = new Vector(playerLoc.getX() + 0.5, playerLoc.getY(), playerLoc.getZ() + 0.5);

        double halfAngleRad = Math.toRadians(angleDegrees / 2);

        int minX = (int) Math.floor(playerLoc.getX() - radius);
        int maxX = (int) Math.ceil(playerLoc.getX() + radius);
        int minZ = (int) Math.floor(playerLoc.getZ() - radius);
        int maxZ = (int) Math.ceil(playerLoc.getZ() + radius);
        int playerY = playerLoc.getBlockY();

        config.Flower.putIfAbsent(player.getUniqueId(), new ArrayList<>());

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Vector blockPos = new Vector(x + 0.5, playerY, z + 0.5);
                Vector directionToBlock = blockPos.clone().subtract(origin);
                directionToBlock.setY(0);
                double distance = directionToBlock.length();

                if (distance == 0 || distance > radius) continue;

                directionToBlock.normalize();
                double dot = forward.dot(directionToBlock);
                dot = Math.min(1.0, Math.max(-1.0, dot));
                double angleBetween = Math.acos(dot);

                if (angleBetween <= halfAngleRad) {
                    int foundY = -1;
                    for (int y = playerY + 2; y >= playerY - 7; y--) {
                        Block baseBlock = world.getBlockAt(x, y, z);
                        if (baseBlock.getType().isSolid() && !baseBlock.isPassable()) {
                            foundY = y + 1;
                            break;
                        }
                    }

                    int foundUpperY = -1;
                    for (int y = playerY + 1; y <= playerY + 8; y++) {
                        Block baseBlock = world.getBlockAt(x, y, z);
                        if (baseBlock.getType().isSolid() && !baseBlock.isPassable()) {
                            foundUpperY = y + 1;
                            break;
                        }
                    }

                    Location playerBlockLoc = playerLoc.getBlock().getLocation();
                    int px = playerBlockLoc.getBlockX();
                    int py = playerBlockLoc.getBlockY();
                    int pz = playerBlockLoc.getBlockZ();

                    if (foundY != -1 && !(x == px && foundY == py && z == pz)) {
                        Block aboveBlock = world.getBlockAt(x, foundY, z);
                        if (aboveBlock.isPassable() || aboveBlock.getType() == Material.AIR) {
                            config.Flower.get(player.getUniqueId()).add(aboveBlock);
                        }
                    }

                    if (foundUpperY != -1 && foundUpperY != foundY && !(x == px && foundUpperY == py && z == pz)) {
                        Block aboveBlock = world.getBlockAt(x, foundUpperY, z);
                        if (aboveBlock.isPassable() || aboveBlock.getType() == Material.AIR) {
                            config.Flower.get(player.getUniqueId()).add(aboveBlock);
                        }
                    }
                }
            }
        }

        config.Flower.get(player.getUniqueId()).sort(Comparator.comparingDouble(b -> b.getLocation().distance(playerLoc)));
        List<Block> flower = config.Flower.get(player.getUniqueId());

        damageTimer(player);

        new BukkitRunnable() {
            int index = 0;
            @Override
            public void run() {
                int perTick = 3;
                for (int i = 0; i < perTick && index < flower.size(); i++, index++) {
                    Block block = flower.get(index);
                    block.setType(Material.WITHER_ROSE, false);

                    world.spawnParticle(Particle.SOUL, block.getLocation().clone().add(0.5, 0.5, 0.5), 3, 0.1, 0.1, 0.1, 0.02);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            cool.setCooldown(player, config.r_Skill_Cool, "R");
                            if (block.getType() == Material.WITHER_ROSE) {
                                block.setType(Material.AIR, false);
                                world.spawnParticle(Particle.SMOKE, block.getLocation().add(0.5, 0.2, 0.5), 5, 0.1, 0.1, 0.1, 0.02);

                                if(!config.Flower.isEmpty()) {
                                    config.Flower.remove(player.getUniqueId());
                                }
                            }
                        }
                    }.runTaskLater(plugin, 80L);
                }

                if (index >= flower.size()){
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void damageTimer(Player player){
        new BukkitRunnable(){

            int tick = 0;

            @Override
            public void run(){
                if(!player.isOnline() || player.isDead() || tick > 20 * 4){
                    config.rReuseDamage.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                config.rReuseDamage.put(player.getUniqueId(), (double) (tick / 10));

                tick++;
            }

        }.runTaskTimer(plugin, 0L, 1L);

    }

    public void slowFlower(Player player, List<Block> blocksToPlace){

        World world = player.getWorld();

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        double damage = config.rReuseDamage.getOrDefault(player.getUniqueId(), 0.0) * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        config.r_damaged.putIfAbsent(player.getUniqueId(), new HashSet<>());

        new BukkitRunnable() {
            int index = 0;
            @Override
            public void run() {
                int perTick = 3;
                for (int i = 0; i < perTick && index < blocksToPlace.size(); i++, index++) {

                    List<Entity> nearbyEntities = (List<Entity>) blocksToPlace.get(index).getLocation().getNearbyEntities(0.5, 0.5, 0.5);

                    for (Entity entity : nearbyEntities) {
                        if (entity instanceof LivingEntity target && entity != player && !config.r_damaged.get(player.getUniqueId()).contains(target)) {

                            PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS, 80, 3, false, false);
                            target.addPotionEffect(slowness);

                            ForceDamage forceDamage = new ForceDamage(target, damage, source, false);
                            forceDamage.applyEffect(player);

                            config.r_damaged.get(player.getUniqueId()).add(target);
                        }
                    }

                    world.spawnParticle(Particle.SOUL, blocksToPlace.get(index).getLocation().add(0.5, 0.5, 0.5), 3, 0.1, 0.1, 0.1, 0.02);
                    world.spawnParticle(Particle.SMOKE, blocksToPlace.get(index).getLocation().add(0.5, 0.2, 0.5), 5, 0.1, 0.1, 0.1, 1);
                }

                if (index >= blocksToPlace.size()){
                    config.r_damaged.remove(player.getUniqueId());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}