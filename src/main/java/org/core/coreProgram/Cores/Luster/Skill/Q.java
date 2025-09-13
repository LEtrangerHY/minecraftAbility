package org.core.coreProgram.Cores.Luster.Skill;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.Luster.coreSystem.Luster;

import java.util.*;

public class Q implements SkillBase {
    private final Luster config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private static final Set<Material> ironRelatedBlocks = new HashSet<>();
    private static boolean initialized = false;

    private final List<FallingBlock> liftedBlocks = new ArrayList<>();
    private final List<LivingEntity> liftedEntities = new ArrayList<>();

    public Q(Luster config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;

        if (!initialized) {
            initIronRelatedBlocks();
            initialized = true;
        }
    }

    private void initIronRelatedBlocks() {
        for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext(); ) {
            Recipe recipe = it.next();

            if (recipe instanceof ShapedRecipe shaped) {
                for (RecipeChoice choice : shaped.getChoiceMap().values()) {
                    if (choice != null) {
                        if (choice instanceof RecipeChoice.MaterialChoice matChoice) {
                            if (matChoice.getChoices().contains(Material.IRON_INGOT)) {
                                ironRelatedBlocks.add(shaped.getResult().getType());
                            }
                        } else if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
                            for (ItemStack item : exactChoice.getChoices()) {
                                if (item.getType() == Material.IRON_INGOT) {
                                    ironRelatedBlocks.add(shaped.getResult().getType());
                                }
                            }
                        }
                    }
                }
            } else if (recipe instanceof ShapelessRecipe shapeless) {
                for (RecipeChoice choice : shapeless.getChoiceList()) {
                    if (choice != null) {
                        if (choice instanceof RecipeChoice.MaterialChoice matChoice) {
                            if (matChoice.getChoices().contains(Material.IRON_INGOT)) {
                                ironRelatedBlocks.add(shapeless.getResult().getType());
                            }
                        } else if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
                            for (ItemStack item : exactChoice.getChoices()) {
                                if (item.getType() == Material.IRON_INGOT) {
                                    ironRelatedBlocks.add(shapeless.getResult().getType());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void Trigger(Player player) {
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);

        Entity target = getTargetedEntity(player, 13, 0.3);
        if (target == null) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 1, 1);
            long cools = 300L;
            cool.updateCooldown(player, "Q", cools);
            return;
        }

        Upward(player, (LivingEntity) target);
    }

    public void Upward(Player player, LivingEntity target) {
        player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1, 1);

        int range = 6;
        Location center = player.getLocation();
        World world = center.getWorld();
        if (world == null) return;

        boolean hasIronRecipeBlock = false;

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    if (ironRelatedBlocks.contains(block.getType())) {
                        hasIronRecipeBlock = true;

                        Material originalType = block.getType();
                        BlockData data = block.getBlockData();
                        Location blockLoc = block.getLocation();

                        block.setType(Material.AIR);

                        FallingBlock falling = world.spawn(
                                blockLoc.add(0.5, 0, 0.5),
                                FallingBlock.class,
                                entity -> {
                                    entity.setBlockData(data);
                                    entity.setDropItem(false);
                                    entity.setHurtEntities(false);
                                    entity.setGravity(false);
                                    entity.setPersistent(false);
                                }
                        );
                        falling.setVelocity(new Vector(0, 0.5, 0));
                        liftedBlocks.add(falling);
                    }
                }
            }
        }

        if (!hasIronRecipeBlock) return;

        Set<Material> checkMats = Set.of(
                Material.IRON_SWORD, Material.IRON_AXE, Material.IRON_PICKAXE, Material.IRON_SHOVEL, Material.IRON_HOE,
                Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
                Material.NETHERITE_SWORD, Material.NETHERITE_AXE, Material.NETHERITE_PICKAXE, Material.NETHERITE_SHOVEL, Material.NETHERITE_HOE,
                Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
                Material.COPPER_INGOT
        );

        double radius = 9.0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living.equals(target)) continue;

            boolean shouldLift = false;

            for (ItemStack item : living.getEquipment().getArmorContents()) {
                if (item != null && checkMats.contains(item.getType())) {
                    shouldLift = true;
                    break;
                }
            }

            ItemStack main = living.getEquipment().getItemInMainHand();
            if (main != null && checkMats.contains(main.getType())) shouldLift = true;

            if (shouldLift) {
                living.setVelocity(new Vector(0, 3, 0));
                liftedEntities.add(living);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                Attack(player, target);
            }
        }.runTaskLater(plugin, 23L);
    }

    public void Attack(Player player, LivingEntity target) {
        if (target == null) return;

        BlockData iron = Material.IRON_BLOCK.createBlockData();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1f);

        double projectileSpeed = 1.7;

        for (FallingBlock block : liftedBlocks) {
            if (block.isDead()) continue;

            Vector direction = target.getLocation().toVector()
                    .subtract(block.getLocation().toVector())
                    .normalize()
                    .multiply(projectileSpeed);
            block.setVelocity(direction);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (block.isDead()) {
                        cancel();
                        return;
                    }

                    for (Entity entity : block.getLocation().getNearbyEntities(1.0, 1.0, 1.0)) {
                        if (!(entity instanceof LivingEntity living)) continue;
                        if (living.equals(player)) continue;

                        living.getWorld().spawnParticle(Particle.BLOCK, living.getLocation().clone().add(0, 1, 0), 6, 0.2, 0.2, 0.2,
                                iron);

                        living.getWorld().playSound(living.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);

                        ForceDamage forceDamage = new ForceDamage(living, config.q_Skill_Damage);
                        forceDamage.applyEffect(player);

                        block.remove();
                        cancel();
                        break;
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        for (LivingEntity entity : liftedEntities) {
            if (entity.isDead()) continue;

            Vector direction = target.getLocation().toVector()
                    .subtract(entity.getLocation().toVector())
                    .normalize()
                    .multiply(projectileSpeed);
            entity.setVelocity(direction);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (entity.isDead()) {
                        cancel();
                        return;
                    }

                    for (Entity nearby : entity.getLocation().getNearbyEntities(1.0, 1.0, 1.0)) {
                        if (!(nearby instanceof LivingEntity living)) continue;
                        if (living.equals(player)) continue;

                        living.getWorld().spawnParticle(Particle.BLOCK, living.getLocation().clone().add(0, 1, 0), 6, 0.2, 0.2, 0.2,
                                iron);

                        living.getWorld().spawnParticle(Particle.EXPLOSION, living.getLocation().clone().add(0, 1, 0), 1, 0, 0 ,0, 1);

                        living.getWorld().playSound(living.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);

                        ForceDamage forceDamage = new ForceDamage(living, config.q_Skill_Damage);
                        forceDamage.applyEffect(player);

                        cancel();
                        break;
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        liftedBlocks.clear();
        liftedEntities.clear();
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