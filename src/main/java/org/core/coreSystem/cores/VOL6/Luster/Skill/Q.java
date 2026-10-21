package org.core.coreSystem.cores.VOL6.Luster.Skill;

import org.core.models.coreObj.lustLiftedBlockModel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL6.Luster.coreSystem.Luster;

import java.time.Duration;
import java.util.*;

public class Q implements SkillBase {
    private final Luster config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private static final Set<Material> ironRelatedMaterials = new HashSet<>();
    private static boolean initialized = false;

    public Q(Luster config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;

        if (!initialized) {
            initIronRelatedMaterials();
            initialized = true;
        }
    }

    private void initIronRelatedMaterials() {
        for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext(); ) {
            Recipe recipe = it.next();

            if (recipe instanceof ShapedRecipe shaped) {
                for (RecipeChoice choice : shaped.getChoiceMap().values()) {
                    if (choice != null) {
                        if (choice instanceof RecipeChoice.MaterialChoice matChoice) {
                            if (matChoice.getChoices().contains(Material.IRON_INGOT)) {
                                ironRelatedMaterials.add(shaped.getResult().getType());
                            }
                        } else if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
                            for (ItemStack item : exactChoice.getChoices()) {
                                if (item.getType() == Material.IRON_INGOT) {
                                    ironRelatedMaterials.add(shaped.getResult().getType());
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
                                ironRelatedMaterials.add(shapeless.getResult().getType());
                            }
                        } else if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
                            for (ItemStack item : exactChoice.getChoices()) {
                                if (item.getType() == Material.IRON_INGOT) {
                                    ironRelatedMaterials.add(shapeless.getResult().getType());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void playLiftParticles(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(200, 200, 200), 1.7f);
        Particle.DustOptions dustOptions_gra = new Particle.DustOptions(Color.fromRGB(244, 244, 244), 1.4f);

        try {
            world.spawnParticle(Particle.valueOf("ENCHANTMENT_TABLE"), loc, 25, 0.4, 0.4, 0.4, 0.1);
        } catch (IllegalArgumentException e) {
            try {
                world.spawnParticle(Particle.valueOf("ENCHANT"), loc, 25, 0.4, 0.4, 0.4, 0.1);
            } catch (IllegalArgumentException ignored) {}
        }

        world.spawnParticle(Particle.DUST, loc, 15, 0.4, 0.4, 0.4, 0, dustOptions);
        world.spawnParticle(Particle.DUST, loc, 15, 0.4, 0.4, 0.4, 0, dustOptions_gra);
    }

    @Override
    public void Trigger(Player player) {
        Entity target = getTargetedEntity(player, 13, 0.3);
        if (target == null) {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
            Title title = Title.title(
                    Component.empty(),
                    Component.text("not designated").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);
            cool.updateCooldown(player, "Q", 500L);
            return;
        }

        Upward(player, (LivingEntity) target);
    }

    public void Upward(Player player, LivingEntity target) {
        final List<BlockDisplay> liftedBlocks = new ArrayList<>();
        final List<LivingEntity> liftedEntities = new ArrayList<>();
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1, 1);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.4f, 1.0f);

        int range = 6;
        Location center = player.getLocation();
        World world = center.getWorld();
        if (world == null) return;

        boolean hasIronRecipeBlock = false;

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    if (ironRelatedMaterials.contains(block.getType())) {
                        hasIronRecipeBlock = true;

                        BlockData data = block.getBlockData();
                        Location blockLoc = block.getLocation();

                        block.setType(Material.AIR);

                        double offsetX = (Math.random() * 0.2) - 0.1;
                        double offsetZ = (Math.random() * 0.2) - 0.1;
                        float randomYaw = (float) (Math.random() * 360);
                        float randomPitch = (float) (Math.random() * 360);

                        Location spawnLoc = blockLoc.clone().add(0.5 + offsetX, 0, 0.5 + offsetZ);
                        spawnLoc.setYaw(randomYaw);
                        spawnLoc.setPitch(randomPitch);

                        BlockDisplay display = new lustLiftedBlockModel(world, spawnLoc, data, null).getDisplay();

                        liftedBlocks.add(display);
                        playLiftParticles(spawnLoc);
                    }
                }
            }
        }

        int naturalBlockCount = liftedBlocks.size();

        if (!hasIronRecipeBlock) {
            ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);
            boolean hasLodestone = offhandItem != null && offhandItem.getType() == Material.LODESTONE;
            boolean hasIngots = offhandItem != null && offhandItem.getType() == Material.IRON_INGOT && offhandItem.getAmount() >= 8;

            if (hasLodestone || hasIngots) {
                if (hasIngots && !hasLodestone) {
                    offhandItem.setAmount(offhandItem.getAmount() - 8);
                }
                hasIronRecipeBlock = true;

                int spawned = 0;
                for (int i = 0; i < 50; i++) {
                    double rx = center.getX() + (Math.random() * 8 - 4);
                    double ry = center.getY() + (Math.random() * 4 - 2);
                    double rz = center.getZ() + (Math.random() * 8 - 4);
                    Location randLoc = new Location(world, rx, ry, rz);

                    Block targetBlock = randLoc.getBlock();
                    if (targetBlock.isPassable() || targetBlock.getType().isAir()) {

                        double offsetX = (Math.random() * 0.2) - 0.1;
                        double offsetZ = (Math.random() * 0.2) - 0.1;
                        float randomYaw = (float) (Math.random() * 360);
                        float randomPitch = (float) (Math.random() * 360);

                        Location spawnLoc = targetBlock.getLocation().add(0.5 + offsetX, 0, 0.5 + offsetZ);
                        spawnLoc.setYaw(randomYaw);
                        spawnLoc.setPitch(randomPitch);

                        BlockDisplay display = new lustLiftedBlockModel(world, spawnLoc, Material.IRON_BLOCK.createBlockData(), new NamespacedKey(plugin, "summoned_block")).getDisplay();

                        liftedBlocks.add(display);
                        playLiftParticles(spawnLoc);

                        spawned++;
                        if (spawned >= 4) break;
                    }
                }
            } else {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_IRON_PLACE, 1, 1);
                Title title = Title.title(
                        Component.empty(),
                        Component.text("iron ingot needed").color(NamedTextColor.RED),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
                );
                player.showTitle(title);
                cool.updateCooldown(player, "Q", 500L);
                return;
            }
        }

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 23) {
                    this.cancel();
                    return;
                }
                for (BlockDisplay bd : liftedBlocks) {
                    if (bd.isValid()) {
                        Location loc = bd.getLocation();
                        loc.add(0, 0.3, 0);
                        loc.setYaw(loc.getYaw() + 7);
                        loc.setPitch(loc.getPitch() + 5);
                        bd.teleport(loc);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        Set<Material> checkMats = Set.of(
                Material.IRON_SWORD, Material.IRON_AXE, Material.IRON_PICKAXE, Material.IRON_SHOVEL, Material.IRON_HOE,
                Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS
        );

        double radius = 9.0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living.equals(target)) continue;

            boolean shouldLift = false;

            if (living.getType() == EntityType.IRON_GOLEM) {
                shouldLift = true;
            }

            if (!shouldLift) {
                for (ItemStack item : living.getEquipment().getArmorContents()) {
                    if (item != null && (checkMats.contains(item.getType()) || ironRelatedMaterials.contains(item.getType()))) {
                        shouldLift = true;
                        break;
                    }
                }

                if (!shouldLift) {
                    ItemStack main = living.getEquipment().getItemInMainHand();
                    if (main != null && (checkMats.contains(main.getType()) || ironRelatedMaterials.contains(main.getType()))) {
                        shouldLift = true;
                    }
                }

                if (!shouldLift) {
                    ItemStack off = living.getEquipment().getItemInOffHand();
                    if (off != null && (checkMats.contains(off.getType()) || ironRelatedMaterials.contains(off.getType()))) {
                        shouldLift = true;
                    }
                }
            }

            if (shouldLift) {
                living.setVelocity(new Vector(0, 1.5, 0));
                liftedEntities.add(living);

                if (living.getPersistentDataContainer().has(new NamespacedKey(plugin, "ally"), PersistentDataType.BYTE)) {
                    living.setInvulnerable(true);
                }
            }
        }

        int entityCount = liftedEntities.size();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (target == null || target.isDead() || !target.isValid() || !player.isOnline()) {
                    cancelAttackAndDrop(player, liftedBlocks, liftedEntities);
                    return;
                }
                Attack(player, target, naturalBlockCount, entityCount, liftedBlocks, liftedEntities);
            }
        }.runTaskLater(plugin, 23L);
    }

    private void cancelAttackAndDrop(Player player, List<BlockDisplay> liftedBlocks, List<LivingEntity> liftedEntities) {
        for (BlockDisplay bd : liftedBlocks) {
            if (bd != null && !bd.isDead()) {
                if (bd.getPersistentDataContainer().has(new NamespacedKey(plugin, "summoned_block"), PersistentDataType.BYTE)) {
                    bd.remove();
                } else {
                    Material mat = bd.getBlock().getMaterial();
                    bd.getWorld().dropItemNaturally(bd.getLocation(), new ItemStack(mat));
                    bd.remove();
                }
            }
        }
        liftedBlocks.clear();

        for (LivingEntity le : liftedEntities) {
            if (le != null && !le.isDead()) {
                Vector v = le.getVelocity();
                le.setVelocity(new Vector(v.getX(), 0, v.getZ()));
                if (le.getPersistentDataContainer().has(new NamespacedKey(plugin, "ally"), PersistentDataType.BYTE)) {
                    le.setInvulnerable(false);
                }
            }
        }
        liftedEntities.clear();
    }

    public void Attack(Player player, LivingEntity target, int naturalBlockCount, int entityCount, List<BlockDisplay> liftedBlocks, List<LivingEntity> liftedEntities) {
        if (target == null) return;

        if (naturalBlockCount > 0 || entityCount > 0) {
            long currentCd = cool.getRemainCooldown(player, "Q");
            if (currentCd > 0) {
                long reduceAmount = (naturalBlockCount * 2000L) + (entityCount * 1000L);
                long newCd = Math.max(0, currentCd - reduceAmount);
                cool.updateCooldown(player, "Q", newCd);
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1f);

        double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_Damage * (1 + amp);

        List<Object> projectiles = new ArrayList<>();
        projectiles.addAll(liftedBlocks);
        projectiles.addAll(liftedEntities);

        liftedBlocks.clear();
        liftedEntities.clear();

        Collections.shuffle(projectiles);

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index >= projectiles.size()) {
                    cancel();
                    return;
                }

                if (target.isDead() || !target.isValid() || !player.isOnline()) {
                    for (int i = index; i < projectiles.size(); i++) {
                        Object remaining = projectiles.get(i);
                        if (remaining instanceof BlockDisplay bd) {
                            if (bd.isValid() && !bd.isDead()) {
                                if (bd.getPersistentDataContainer().has(new NamespacedKey(plugin, "summoned_block"), PersistentDataType.BYTE)) {
                                    bd.remove();
                                } else {
                                    Material mat = bd.getBlock().getMaterial();
                                    bd.getWorld().dropItemNaturally(bd.getLocation(), new ItemStack(mat));
                                    bd.remove();
                                }
                            }
                        } else if (remaining instanceof LivingEntity le) {
                            if (le.isValid() && !le.isDead()) {
                                Vector v = le.getVelocity();
                                le.setVelocity(new Vector(v.getX(), 0, v.getZ()));
                                if (le.getPersistentDataContainer().has(new NamespacedKey(plugin, "ally"), PersistentDataType.BYTE)) {
                                    le.setInvulnerable(false);
                                }
                            }
                        }
                    }
                    cancel();
                    return;
                }

                Object proj = projectiles.get(index++);
                fireProjectile(player, target, proj, damage);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void fireProjectile(Player player, LivingEntity target, Object projectile, double damage) {
        double projectileSpeed = 1.7;
        BlockData iron = Material.IRON_BLOCK.createBlockData();

        if (projectile instanceof BlockDisplay display) {
            if (display.isDead()) return;

            BlockData data = display.getBlock();
            boolean isSummonedBlock = display.getPersistentDataContainer().has(new NamespacedKey(plugin, "summoned_block"), PersistentDataType.BYTE);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!display.isValid() || target.isDead() || !target.isValid()) {
                        if (!isSummonedBlock && display.isValid()) {
                            display.getWorld().dropItemNaturally(display.getLocation(), new ItemStack(data.getMaterial()));
                        }
                        display.remove();
                        cancel();
                        return;
                    }

                    Location tLoc = target.getLocation().add(0, target.getHeight() / 2.0, 0);
                    Vector dir = tLoc.toVector().subtract(display.getLocation().toVector());
                    double dist = dir.length();

                    if (dist < projectileSpeed) {
                        display.teleport(tLoc);
                        target.getWorld().spawnParticle(Particle.BLOCK, tLoc, 10, 0.2, 0.2, 0.2, data);
                        target.getWorld().playSound(tLoc, Sound.BLOCK_ANVIL_LAND, 1f, 1f);

                        DamageSource source = DamageSource.builder(DamageType.MOB_PROJECTILE)
                                .withCausingEntity(player)
                                .withDirectEntity(display)
                                .withDamageLocation(display.getLocation())
                                .build();

                        ForceDamage forceDamage = new ForceDamage(target, damage, source, false);
                        forceDamage.applyEffect(player);

                        display.remove();
                        cancel();
                    } else {
                        dir.normalize().multiply(projectileSpeed);
                        Location newLoc = display.getLocation().add(dir);

                        // 발사되어 날아가는 동안에도 회전(Spin) 효과 유지
                        newLoc.setYaw(newLoc.getYaw() + 15);
                        newLoc.setPitch(newLoc.getPitch() + 15);
                        display.teleport(newLoc);
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);

        } else if (projectile instanceof LivingEntity le) {
            if (le.isDead()) return;

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (le.isDead() || target.isDead() || !target.isValid()) {
                        if (le.getPersistentDataContainer().has(new NamespacedKey(plugin, "ally"), PersistentDataType.BYTE)) {
                            le.setInvulnerable(false);
                        }
                        cancel();
                        return;
                    }

                    Location tLoc = target.getLocation().add(0, target.getHeight() / 2.0, 0);
                    Vector dir = tLoc.toVector().subtract(le.getLocation().toVector());
                    double dist = dir.length();

                    if (dist < projectileSpeed * 2.0) {
                        le.getWorld().spawnParticle(Particle.BLOCK, le.getLocation().add(0, 1, 0), 6, 0.2, 0.2, 0.2, iron);
                        le.getWorld().spawnParticle(Particle.EXPLOSION, le.getLocation().add(0, 1, 0), 1, 0, 0, 0, 1);
                        le.getWorld().playSound(le.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);

                        DamageSource leSource = DamageSource.builder(DamageType.MOB_PROJECTILE)
                                .withCausingEntity(player)
                                .withDirectEntity(le)
                                .withDamageLocation(le.getLocation())
                                .build();

                        ForceDamage forceDamage = new ForceDamage(target, damage, leSource, false);
                        forceDamage.applyEffect(player);

                        if (le.getPersistentDataContainer().has(new NamespacedKey(plugin, "ally"), PersistentDataType.BYTE)) {
                            le.setInvulnerable(false);
                        }
                        cancel();
                    } else {
                        le.setVelocity(dir.normalize().multiply(projectileSpeed));
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    public static LivingEntity getTargetedEntity(Player player, double range, double raySize) {
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        List<LivingEntity> candidates = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(eyeLocation, range, range, range)) {
            if (!(entity instanceof LivingEntity) || entity.equals(player) || entity.isInvulnerable()) continue;

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