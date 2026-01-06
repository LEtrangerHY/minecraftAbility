package org.core.coreSystem.cores.VOL1.Bloom.Skill;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Bloom.coreSystem.Bloom;

import java.util.*;

public class F implements SkillBase {
    private final Bloom config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Bloom config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        Block targetBlock = getCustomTargetBlock(player, 15);
        if (targetBlock == null) {
            cool.updateCooldown(player, "F", 500L);
            return;
        }

        World world = player.getWorld();
        Location baseLoc = targetBlock.getLocation().add(0, 1, 0);
        config.treeLoc.put(player.getUniqueId(), baseLoc);

        Set<Location> beforeBlocks = getNearbyNonAirBlocks(baseLoc, 8);

        if (!world.generateTree(baseLoc, TreeType.CHERRY)) {
            cool.updateCooldown(player, "F", 500L);
            return;
        }

        Set<Location> afterBlocks = getNearbyNonAirBlocks(baseLoc, 8);
        afterBlocks.removeAll(beforeBlocks);

        world.playSound(baseLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.4f, 1);
        world.playSound(baseLoc, Sound.BLOCK_GRASS_PLACE, 1.4f, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                removeTreeBlocks(afterBlocks);
            }
        }.runTaskLater(plugin, 20L * 30);

        startEffectLogic(player, baseLoc, afterBlocks);
    }

    private void startEffectLogic(Player owner, Location treeCenter, Set<Location> treeBlocks) {

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
                    removeTreeBlocks(treeBlocks);
                    config.treeLoc.remove(owner.getUniqueId());
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

                        ForceDamage forceDamage = new ForceDamage(target, damage, source);
                        forceDamage.applyEffect(owner);
                        target.setVelocity(new Vector(0, 0, 0));
                    }
                }

                timer--;
            }

        }.runTaskTimer(plugin, 0L, 17L);
    }

    private void removeTreeBlocks(Set<Location> blocks) {
        for (Location loc : blocks) {
            Block block = loc.getBlock();
            Material type = block.getType();

            if (type == Material.CHERRY_LOG ||
                    type == Material.CHERRY_LEAVES ||
                    type == Material.CHERRY_SAPLING) {
                block.setType(Material.AIR, false);
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
