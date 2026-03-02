package org.core.coreSystem.cores.VOL3.Luster.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
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

    private final Map<UUID, BukkitRunnable> golemLifespanTasks = new HashMap<>();
    private final Map<UUID, Integer> golemLifespanTicks = new HashMap<>();
    private final Map<UUID, Boolean> golemLifespanPaused = new HashMap<>();

    public F(Luster config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);
        UUID pUUID = player.getUniqueId();

        if (offhandItem.getType() == Material.LODESTONE || (offhandItem.getType() == Material.IRON_INGOT && offhandItem.getAmount() > 18)) {
            final World world = player.getWorld();
            Entity target = getTargetedEntity(player, 13, 0.3);

            if (target != null && isPlayerOwnedGolem(target, player)) {
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
                Title title = Title.title(
                        Component.empty(),
                        Component.text("cannot target allies").color(NamedTextColor.RED),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
                );
                player.showTitle(title);
                cool.updateCooldown(player, "F", 500L);
                return;
            }

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
                Set<IronGolem> spawnedGolems = new HashSet<>();

                long playerLevel = player.getPersistentDataContainer().getOrDefault(
                        new NamespacedKey(plugin, "level"), PersistentDataType.LONG, 0L
                );
                int pLevelInt = (int) playerLevel;

                for (int i = 0; i < golemCount; i++) {
                    double angle = 2 * Math.PI / golemCount * i;

                    double x = center.getX() + radius * Math.cos(angle);
                    double y = center.getY() + yOffset;
                    double z = center.getZ() + radius * Math.sin(angle);

                    Location spawnLoc = new Location(world, x, y, z);
                    Entity golemEntity = world.spawnEntity(spawnLoc, EntityType.IRON_GOLEM);

                    golemEntity.setGlowing(true);

                    PersistentDataContainer golemData = golemEntity.getPersistentDataContainer();
                    golemData.set(new NamespacedKey(plugin, "entity_level"), PersistentDataType.INTEGER, pLevelInt);
                    golemData.set(new NamespacedKey(plugin, "ally"), PersistentDataType.BYTE, (byte) 1);

                    if (golemEntity instanceof LivingEntity livingGolem) {
                        AttributeInstance healthAttr = livingGolem.getAttribute(Attribute.MAX_HEALTH);
                        if (healthAttr != null) {
                            double baseHealth = healthAttr.getBaseValue();
                            double newHealth = baseHealth * 0.66;

                            if (pLevelInt > 0) {
                                double p = (0.005 * pLevelInt * pLevelInt + 0.055 * pLevelInt) * 1.44;
                                newHealth += (baseHealth * p * 0.44);
                            }

                            healthAttr.setBaseValue(newHealth);
                            livingGolem.setHealth(newHealth);
                        }
                    }

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
                        spawnedGolems.add(ironGolem);
                        ironGolem.setPlayerCreated(true);

                        if (target instanceof LivingEntity livingTarget && !isPlayerOwnedGolem(livingTarget, player)) {
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

                cool.setCooldown(player, 44000L, "Golem Duration", "boss");
                golemLifespanTicks.put(pUUID, 0);
                golemLifespanPaused.put(pUUID, false);

                if (golemLifespanTasks.containsKey(pUUID)) {
                    golemLifespanTasks.get(pUUID).cancel();
                }

                BukkitRunnable lifespanTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline() || player.isDead()) {
                            killGolems(spawnedGolems, world, iron);
                            cool.updateCooldown(player, "Golem Duration", 0L, "boss");
                            golemLifespanTasks.remove(pUUID);
                            cancel();
                            return;
                        }

                        if (golemLifespanPaused.getOrDefault(pUUID, false)) {
                            return;
                        }

                        int currentTicks = golemLifespanTicks.getOrDefault(pUUID, 0);
                        if (currentTicks >= 880) {
                            killGolems(spawnedGolems, world, iron);
                            cool.updateCooldown(player, "Golem Duration", 0L, "boss");
                            cool.updateCooldown(player, "F", 60000L);
                            golemLifespanTasks.remove(pUUID);
                            cancel();
                            return;
                        }

                        golemLifespanTicks.put(pUUID, currentTicks + 1);
                    }
                };

                lifespanTask.runTaskTimer(plugin, 0L, 1L);
                golemLifespanTasks.put(pUUID, lifespanTask);

                if(offhandItem.getType() == Material.IRON_INGOT && offhandItem.getAmount() >= 18) {
                    offhandItem.setAmount(offhandItem.getAmount() - 18);
                }

            } else if(target != null) {
                long cools = 4000L;
                cool.updateCooldown(player, "F", cools);

                world.playSound(center, Sound.ENTITY_IRON_GOLEM_REPAIR, 1, 1);
                world.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.2f);

                world.spawnParticle(
                        Particle.BLOCK,
                        target.getLocation().clone().add(0, 1, 0),
                        44, 0.4, 0.4, 0.4,
                        Material.IRON_BLOCK.createBlockData()
                );

                List<IronGolem> activeGolems = new ArrayList<>();
                for (IronGolem golem : currentGolems) {
                    if (golem != null && !golem.isDead()) {
                        activeGolems.add(golem);
                    }
                }

                if (activeGolems.isEmpty()) {
                    return;
                }

                long currentDuration = cool.getRemainCooldown(player, "Golem Duration");
                if (currentDuration > 0) {
                    long newDuration = Math.max(0, currentDuration - 4000L);
                    cool.updateCooldown(player, "Golem Duration", newDuration, "boss");
                }

                int currentTicks = golemLifespanTicks.getOrDefault(pUUID, 0);
                golemLifespanTicks.put(pUUID, currentTicks + 80);

                cool.pauseCooldown(player, "Golem Duration");
                golemLifespanPaused.put(pUUID, true);

                int totalGolems = activeGolems.size();
                int[] completedGolems = {0};

                for (IronGolem golem : activeGolems) {
                    double health = golem.getHealth();
                    if (health <= 13.0) {
                        golem.setHealth(0);
                        completedGolems[0]++;
                        if (completedGolems[0] >= totalGolems) finalizeGolemCharge(player, pUUID);
                        continue;
                    } else {
                        golem.setHealth(health - 13.0);
                    }

                    LivingEntity livingTarget = (LivingEntity) target;

                    golem.setAI(false);
                    golem.setGravity(false);
                    golem.setCollidable(false);
                    golem.setInvulnerable(true);

                    new BukkitRunnable() {
                        int chargeTicks = 0;
                        @Override
                        public void run() {
                            if (golem.isDead() || livingTarget.isDead() || !player.isOnline()) {
                                restoreGolem(golem, livingTarget);
                                completedGolems[0]++;
                                if (completedGolems[0] >= totalGolems) finalizeGolemCharge(player, pUUID);
                                cancel();
                                return;
                            }

                            if (chargeTicks < 20) {
                                Location gLoc = golem.getLocation().add(0, golem.getHeight() / 2.0, 0);
                                Particle.DustOptions chargeDust = new Particle.DustOptions(Color.fromRGB(220, 220, 220), 1.2f);

                                for (int p = 0; p < 8; p++) {
                                    double offsetX = (Math.random() - 0.5) * 4.0;
                                    double offsetY = (Math.random() - 0.5) * 4.0;
                                    double offsetZ = (Math.random() - 0.5) * 4.0;

                                    Location startParticleLoc = gLoc.clone().add(offsetX, offsetY, offsetZ);
                                    Vector particleDir = gLoc.toVector().subtract(startParticleLoc.toVector()).normalize().multiply(0.3);

                                    world.spawnParticle(Particle.DUST, startParticleLoc, 0, particleDir.getX(), particleDir.getY(), particleDir.getZ(), 1, chargeDust);
                                }

                                if (chargeTicks % 5 == 0) {
                                    world.playSound(gLoc, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.5f + (chargeTicks * 0.02f));
                                }

                                chargeTicks++;
                            } else {
                                world.playSound(golem.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1, 1);
                                world.spawnParticle(Particle.EXPLOSION, golem.getLocation().add(0, golem.getHeight() / 2.0, 0), 1, 0, 0, 0, 0);

                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        if (golem.isDead() || livingTarget.isDead() || !player.isOnline()) {
                                            restoreGolem(golem, livingTarget);
                                            completedGolems[0]++;
                                            if (completedGolems[0] >= totalGolems) finalizeGolemCharge(player, pUUID);
                                            cancel();
                                            return;
                                        }

                                        Location tLoc = livingTarget.getLocation().add(0, livingTarget.getHeight() / 2.0, 0);
                                        Location gLoc = golem.getLocation().add(0, golem.getHeight() / 2.0, 0);
                                        Vector dir = tLoc.toVector().subtract(gLoc.toVector());
                                        double dist = dir.length();

                                        double speed = 1.8;

                                        if (dist < speed * 1.2) {
                                            golem.teleport(livingTarget.getLocation());
                                            restoreGolem(golem, livingTarget);

                                            world.spawnParticle(Particle.EXPLOSION, golem.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                                            world.playSound(golem.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1);

                                            completedGolems[0]++;
                                            if (completedGolems[0] >= totalGolems) finalizeGolemCharge(player, pUUID);
                                            cancel();
                                        } else {
                                            Vector step = dir.normalize().multiply(speed);
                                            Location next = golem.getLocation().add(step);
                                            next.setDirection(step);
                                            golem.teleport(next);

                                            world.spawnParticle(Particle.DUST, golem.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0, new Particle.DustOptions(Color.fromRGB(200, 200, 200), 1.5f));
                                        }
                                    }
                                }.runTaskTimer(plugin, 0L, 1L);

                                cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 1L);
                }
            }

            if(offhandItem.getType() == Material.IRON_INGOT && offhandItem.getAmount() > 18) {
                offhandItem.setAmount(offhandItem.getAmount() - 18);
            }
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_IRON_PLACE, 1, 1);

            Title title = Title.title(
                    Component.empty(),
                    Component.text("iron ingot needed").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);

            long cools = 500L;
            cool.updateCooldown(player, "F", cools);
        }
    }

    private void finalizeGolemCharge(Player player, UUID pUUID) {
        if (!player.isOnline()) return;

        cool.resumeCooldown(player, "Golem Duration");
        golemLifespanPaused.put(pUUID, false);
    }

    private void killGolems(Set<IronGolem> golems, World world, BlockData iron) {
        for (IronGolem golem : golems) {
            if (golem != null && !golem.isDead()) {
                world.spawnParticle(Particle.BLOCK, golem.getLocation().clone().add(0, 1, 0), 44, 0.4, 0.4, 0.4, iron);
                world.playSound(golem.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1, 1);
                golem.remove();
            }
        }
    }

    private void restoreGolem(IronGolem golem, LivingEntity target) {
        if (golem != null && golem.isValid() && !golem.isDead()) {
            golem.setAI(true);
            golem.setGravity(true);
            golem.setCollidable(true);
            golem.setInvulnerable(false);
            if (target != null && target.isValid() && !target.isDead()) {
                golem.setTarget(target);
            }
        }
    }

    private boolean isPlayerOwnedGolem(Entity entity, Player player) {
        if (!(entity instanceof IronGolem golem)) return false;
        Set<IronGolem> ownedGolems = config.golems.get(player);
        return ownedGolems != null && ownedGolems.contains(golem);
    }

    public LivingEntity getTargetedEntity(Player player, double range, double raySize) {
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