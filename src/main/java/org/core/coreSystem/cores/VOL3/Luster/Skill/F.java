package org.core.coreSystem.cores.VOL3.Luster.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL3.Luster.coreSystem.Luster;

import java.time.Duration;
import java.util.*;

public class F implements SkillBase {
    private final Luster config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Luster config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {

        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

        if (offhandItem.getType() == Material.LODESTONE || (offhandItem.getType() == Material.IRON_INGOT && offhandItem.getAmount() > 18)) {
            final World world = player.getWorld();
            Entity target = getTargetedEntity(player, 13, 0.3);

            final int golemCount = 2;
            final double radius = 4;
            final double yOffset = 0;
            Location center = player.getLocation();

            world.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1, 1);
            world.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1, 1);

            Set<IronGolem> currentGolems = config.golems.getOrDefault(player, new HashSet<>());
            boolean hasAlive = currentGolems.stream().anyMatch(g -> !g.isDead());

            BlockData iron = Material.IRON_BLOCK.createBlockData();

            if (!hasAlive) {
                for (int i = 0; i < golemCount; i++) {
                    double angle = 2 * Math.PI / golemCount * i;

                    double x = center.getX() + radius * Math.cos(angle);
                    double y = center.getY() + yOffset;
                    double z = center.getZ() + radius * Math.sin(angle);

                    Location spawnLoc = new Location(world, x, y, z);
                    Entity golemEntity = world.spawnEntity(spawnLoc, EntityType.IRON_GOLEM);

                    golemEntity.setGlowing(true);

                    boolean isObstructed = false;
                    for (double dx = -0.7; dx <= 0.7; dx += 0.7) {
                        for (double dz = -0.7; dz <= 0.7; dz += 0.7) {
                            for (double dy = 0; dy <= 2.7; dy += 0.7) {
                                Location check = spawnLoc.clone().add(dx, dy, dz);
                                if (check.getBlock().getType().isSolid()) {
                                    isObstructed = true;
                                    break;
                                }
                            }
                            if (isObstructed) break;
                        }
                        if (isObstructed) break;
                    }

                    if (isObstructed) {
                        int clearRadius = 3;
                        for (int bx = -clearRadius; bx <= clearRadius; bx++) {
                            for (int by = -clearRadius; by <= clearRadius; by++) {
                                for (int bz = -clearRadius; bz <= clearRadius; bz++) {
                                    Location checkLoc = spawnLoc.clone().add(bx, by, bz);
                                    Block block = checkLoc.getBlock();

                                    if (!block.getType().isSolid()) continue;
                                    if (block.getType() == Material.BEDROCK || block.getType() == Material.BARRIER)
                                        continue;

                                    block.breakNaturally();
                                }
                            }
                        }
                    }

                    if (golemEntity instanceof IronGolem ironGolem) {
                        config.golems.computeIfAbsent(player, k -> new HashSet<>()).add(ironGolem);
                        ironGolem.setPlayerCreated(true);

                        if (target instanceof LivingEntity livingTarget) {
                            ironGolem.setTarget(livingTarget);
                        }
                    }

                    world.spawnParticle(Particle.ENCHANTED_HIT, spawnLoc, 44, 0.4, 0.4, 0.4, 1);
                    golemEntity.getWorld().spawnParticle(
                            Particle.BLOCK,
                            golemEntity.getLocation().clone().add(0, 1, 0),
                            44, 0.4, 0.4, 0.4,
                            iron
                    );
                }

                if(offhandItem.getType() == Material.IRON_INGOT && offhandItem.getAmount() >= 18) {
                    offhandItem.setAmount(offhandItem.getAmount() - 18);
                }

            } else if(target != null){
                long cools = 4000L;
                cool.updateCooldown(player, "F", cools);

                world.playSound(center, Sound.ENTITY_IRON_GOLEM_REPAIR, 1, 1);

                world.spawnParticle(
                        Particle.BLOCK,
                        target.getLocation().clone().add(0, 1, 0),
                        44, 0.4, 0.4, 0.4,
                        Material.IRON_BLOCK.createBlockData()
                );

                for (IronGolem golem : currentGolems) {
                    LivingEntity livingTarget = (LivingEntity) target;
                    golem.setTarget(livingTarget);
                }
            }

            if(offhandItem.getType() == Material.IRON_INGOT && offhandItem.getAmount() > 18) {
                offhandItem.setAmount(offhandItem.getAmount() - 18);
            }
        }else {
            player.playSound(player.getLocation(), Sound.BLOCK_IRON_PLACE, 1, 1);

            Title title = Title.title(
                    Component.empty(),
                    Component.text("iron needed").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);

            long cools = 500L;
            cool.updateCooldown(player, "F", cools);
        }
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

            if (result != null) candidates.add((LivingEntity) entity);
        }

        return candidates.stream()
                .min(Comparator.comparingDouble(Damageable::getHealth))
                .orElse(null);
    }
}