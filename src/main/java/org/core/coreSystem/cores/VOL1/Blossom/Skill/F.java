package org.core.coreSystem.cores.VOL1.Blossom.Skill;

import org.core.models.absModelSystem.modelManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Blossom.coreSystem.Blossom;

import java.time.Duration;
import java.util.*;

public class F implements SkillBase {
    private final Blossom config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Blossom config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    private boolean isUniqueBlock(Material mat) {
        String name = mat.name();
        return name.endsWith("DIAMOND_BLOCK") || name.endsWith("NETHERITE_BLOCK") ||
                name.endsWith("EMERALD_BLOCK") || name.endsWith("GOLD_BLOCK") ||
                name.endsWith("IRON_BLOCK") || name.endsWith("LAPIS_BLOCK") ||
                name.endsWith("REDSTONE_BLOCK") || name.endsWith("COAL_BLOCK") ||
                name.endsWith("COPPER_BLOCK") || name.equals("ANCIENT_DEBRIS") ||
                name.endsWith("QUARTZ_BLOCK") || name.endsWith("AMETHYST_BLOCK") ||
                name.endsWith("ORE") || name.contains("RAW_IRON") ||
                name.contains("RAW_GOLD") || name.contains("RAW_COPPER");
    }

    @Override
    public void Trigger(Player player) {
        Block targetBlock = getCustomTargetBlock(player, 15);
        if (targetBlock == null) {
            cool.updateCooldown(player, "F", 500L);
            return;
        }

        if (isUniqueBlock(targetBlock.getType())) {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
            Title title = Title.title(
                    Component.empty(),
                    Component.text("this is unique block!").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);
            cool.updateCooldown(player, "F", 500L);
            return;
        }

        BlockData originalData = targetBlock.getBlockData();
        targetBlock.setType(Material.DIRT);

        World world = player.getWorld();
        Location baseLoc = targetBlock.getLocation().add(0, 1, 0);
        config.treeLoc.put(player.getUniqueId(), baseLoc);

        // 벚나무는 생성 시 꽃잎이 반경 넓게 퍼지므로 범위를 9로 설정
        Set<Location> beforeBlocks = getNearbyNonAirBlocks(baseLoc, 9);

        java.util.Random random = new java.util.Random();
        boolean generated = false;

        // API의 벚나무 시스템 생성을 우선 시도
        for (int i = 0; i < 10; i++) {
            if (world.generateTree(baseLoc, random, TreeType.CHERRY)) {
                generated = true;
                break;
            }
        }

        // [비파괴적 수동 스폰] 시스템 생성이 막힌 경우, 벽을 파괴하지 않고 빈 공간에만 벚나무 생성
        if (!generated) {
            int treeHeight = 5;
            for (int y = 0; y < treeHeight; y++) {
                Block b = baseLoc.clone().add(0, y, 0).getBlock();
                if (!b.getType().isSolid() || b.getType().name().endsWith("LEAVES")) {
                    b.setType(Material.CHERRY_LOG);
                }
            }

            for (int y = 2; y <= treeHeight + 1; y++) {
                int radius = (y <= 4) ? 3 : (y == 5 ? 2 : 1);

                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x == 0 && z == 0 && y < treeHeight) continue;
                        if (Math.abs(x) == radius && Math.abs(z) == radius && radius > 1) {
                            if (random.nextBoolean()) continue;
                        }

                        Block b = baseLoc.clone().add(x, y, z).getBlock();
                        if (!b.getType().isSolid()) {
                            b.setType(Material.CHERRY_LEAVES);
                        }
                    }
                }
            }
        }

        Set<Location> afterBlocks = getNearbyNonAirBlocks(baseLoc, 9);
        afterBlocks.removeAll(beforeBlocks);

        modelManager.trackBlock(targetBlock, originalData);
        BlockData airData = Material.AIR.createBlockData();
        for (Location loc : afterBlocks) {
            modelManager.trackBlock(loc.getBlock(), airData);
        }

        for (Location loc : afterBlocks) {
            Block block = loc.getBlock();
            if (block.getBlockData() instanceof Leaves leaves) {
                leaves.setPersistent(true);
                block.setBlockData(leaves);
            }
        }

        world.playSound(baseLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.4f, 1);
        world.playSound(baseLoc, Sound.BLOCK_GRASS_PLACE, 1.4f, 1);

        cool.setCooldown(player, 25500L, "Tree Duration", "boss");

        new BukkitRunnable() {
            @Override
            public void run() {
                removeTreeBlocks(afterBlocks, targetBlock, originalData);
                cool.updateCooldown(player, "Tree Duration", 0L, "boss");
            }
        }.runTaskLater(plugin, 510L);

        startEffectLogic(player, baseLoc, afterBlocks, targetBlock, originalData);
    }

    private void startEffectLogic(Player owner, Location treeCenter, Set<Location> treeBlocks, Block targetBlock, BlockData originalData) {

        World world = owner.getWorld();

        int radius = 7;

        double amp = config.f_Skill_amp * owner.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(owner)
                .withDirectEntity(owner)
                .build();

        new BukkitRunnable() {

            int timer = 30;

            @Override
            public void run() {

                if (timer <= 0 || !owner.isOnline() || owner.isDead() || !owner.getWorld().equals(treeCenter.getWorld())) {
                    removeTreeBlocks(treeBlocks, targetBlock, originalData);
                    config.treeLoc.remove(owner.getUniqueId());
                    cool.updateCooldown(owner, "Tree Duration", 0L, "boss");
                    cancel();
                    return;
                }

                treeCenter.getWorld().spawnParticle(Particle.CHERRY_LEAVES, treeCenter.clone().add(0, 4, 0), 17, 3, 2, 3, 0.1);
                treeCenter.getWorld().spawnParticle(Particle.END_ROD, treeCenter.clone().add(0, 3, 0), 17, 1, 1, 1, 0.02);

                for (Entity entity : treeCenter.getWorld().getNearbyEntities(treeCenter, radius, radius, radius)) {

                    if (entity instanceof Player p && p.equals(owner)) {
                        p.addPotionEffect(PotionEffectType.REGENERATION.createEffect(40, 2));
                        p.addPotionEffect(PotionEffectType.RESISTANCE.createEffect(40, 1));
                        p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 1.4, 0), 3, 0.5, 0.5, 0.5, 0.01);
                        p.getWorld().spawnParticle(Particle.CHERRY_LEAVES, p.getLocation().add(0, 1.5, 0), 7, 0.8, 0.8, 0.8, 0.1);
                        continue;
                    }

                    if (entity instanceof LivingEntity target && !target.equals(owner)) {
                        Particle.DustOptions pinkDust = new Particle.DustOptions(Color.fromRGB(255, 175, 185), 1.1f);

                        world.spawnParticle(Particle.DUST, target.getLocation().add(0, 1.4, 0), 17, 0.5, 0.5, 0.5, pinkDust);
                        world.spawnParticle(Particle.ENCHANT, target.getLocation().add(0, 1.4, 0), 17, 0.4, 0.7, 0.4, 0.05);
                        world.spawnParticle(Particle.FALLING_DUST, target.getLocation().add(0, 1.4, 0), 17, 0.6, 0.6, 0.6, Material.PINK_CONCRETE.createBlockData());

                        ForceDamage forceDamage = new ForceDamage(target, damage, source, false);
                        forceDamage.applyEffect(owner);
                    }
                }

                timer--;
            }

        }.runTaskTimer(plugin, 0L, 17L);
    }

    private void removeTreeBlocks(Set<Location> blocks, Block targetBlock, BlockData originalData) {
        for (Location loc : blocks) {
            Block block = loc.getBlock();
            modelManager.untrackBlock(block);
            Material type = block.getType();

            if (type.name().contains("CHERRY") ||
                    type == Material.PINK_PETALS ||
                    type == Material.BEE_NEST) {

                block.setType(Material.AIR, false);
            }
        }

        if (targetBlock != null) {
            modelManager.untrackBlock(targetBlock);
            if (targetBlock.getType() == Material.DIRT) {
                targetBlock.setBlockData(originalData);
            }
        }
    }

    private Set<Location> getNearbyNonAirBlocks(Location center, int radius) {
        Set<Location> blocks = new HashSet<>();
        World world = center.getWorld();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 15; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location loc = center.clone().add(x, y, z);
                    if (world.getBlockAt(loc).getType() != Material.AIR) {
                        blocks.add(loc.getBlock().getLocation());
                    }
                }
            }
        }
        return blocks;
    }

    public static Block getCustomTargetBlock(Player player, int range) {
        RayTraceResult result = player.rayTraceBlocks(range, FluidCollisionMode.NEVER);

        if (result == null || result.getHitBlock() == null)
            return null;

        Block hitBlock = result.getHitBlock();

        if (hitBlock.getType() == Material.WATER) {
            Block below = hitBlock.getRelative(BlockFace.DOWN);
            if (below.getType().isSolid()) return below;
            else return null;
        }

        if (hitBlock.getType().isSolid())
            return hitBlock;

        return null;
    }
}