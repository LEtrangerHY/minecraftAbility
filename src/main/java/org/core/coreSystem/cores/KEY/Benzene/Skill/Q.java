package org.core.coreSystem.cores.KEY.Benzene.Skill;

import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.Grounding;
import org.core.coreSystem.cores.KEY.Benzene.coreSystem.Benzene;
import org.core.coreSystem.absCoreSystem.SkillBase;

public class Q implements SkillBase {

    private final Benzene config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private static final Particle.DustOptions DUST_CHAIN = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 1.0f);
    private static final BlockData CHAIN_BLOCK_DATA = Material.IRON_CHAIN.createBlockData();
    private static final PotionEffect GLOW_EFFECT = new PotionEffect(PotionEffectType.GLOWING, 40, 2, false, false);

    public Q(Benzene config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        player.swingOffHand();
        World world = player.getWorld();

        LivingEntity entity = getTargetedEntity(player, 12, 0.6);

        world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.6f, 1.0f);

        if (entity != null) {
            handleSingleTarget(player, world, entity);
        } else {
            handleAreaTarget(player, world);
        }
    }

    private void handleSingleTarget(Player player, World world, LivingEntity entity) {
        chain_qSkill_Particle_Effect(player, entity, 40);

        Grounding grounding = new Grounding(entity, 2000);
        entity.setVelocity(new Vector(0, 0, 0));
        grounding.applyEffect(entity);

        entity.addPotionEffect(GLOW_EFFECT);

        config.q_Skill_effect_1.put(player.getUniqueId(), entity);
        world.playSound(entity.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.6f, 1.0f);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= 3 || player.isDead()) {
                    cancel();
                    return;
                }
                Location loc = entity.getLocation();
                world.playSound(loc, Sound.BLOCK_CHAIN_PLACE, 1.6f, 1.0f);
                world.spawnParticle(Particle.BLOCK, loc.add(0, 1.2, 0), 6, 0.6, 0.6, 0.6, CHAIN_BLOCK_DATA);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        config.atkCount.put(player.getUniqueId(), 3);
        cool.updateCooldown(player, "R", 0L);
        cool.resumeCooldown(player, "R");
    }

    private void handleAreaTarget(Player player, World world) {
        Location pLoc = player.getLocation();
        world.playSound(pLoc, Sound.ITEM_TRIDENT_HIT_GROUND, 1.0f, 1.0f);

        int hitCount = 0;

        for (Entity rangeTarget : world.getNearbyEntities(pLoc, 6.0, 6.0, 6.0)) {
            if (rangeTarget instanceof LivingEntity target && rangeTarget != player) {

                if (target.getLocation().distanceSquared(pLoc) > 36.0) continue;

                Location tLoc = target.getLocation();
                world.playSound(tLoc, Sound.ITEM_TRIDENT_HIT_GROUND, 1.6f, 1.0f);
                world.playSound(tLoc, Sound.BLOCK_CHAIN_PLACE, 1.6f, 1.0f);
                world.spawnParticle(Particle.BLOCK, tLoc.add(0, 1.2, 0), 12, 0.6, 0.6, 0.6, CHAIN_BLOCK_DATA);

                chain_qSkill_Particle_Effect(player, target, 40);

                Grounding grounding = new Grounding(target, 2000);
                grounding.applyEffect(player);
                target.setVelocity(new Vector(0, 0, 0));

                target.addPotionEffect(GLOW_EFFECT);

                config.q_Skill_effect_2.put(player.getUniqueId(), target);
                hitCount++;
            }
        }

        if (hitCount > 0) {
            int currentCount = config.atkCount.getOrDefault(player.getUniqueId(), 0);
            int newCount = Math.min(3, currentCount + hitCount);
            config.atkCount.put(player.getUniqueId(), newCount);

            if (newCount == 3) {
                cool.updateCooldown(player, "R", 0L);
                cool.resumeCooldown(player, "R");
            } else if (newCount == 2) {
                cool.updateCooldown(player, "R", 2000L);
                cool.pauseCooldown(player, "R");
            } else if (newCount == 1) {
                cool.updateCooldown(player, "R", 4000L);
                cool.pauseCooldown(player, "R");
            }
        }
    }

    public void chain_qSkill_Particle_Effect(Player player, Entity entity, int time) {
        World world = player.getWorld();

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick > time || entity.isDead()) {
                    config.q_Skill_effect_1.remove(player.getUniqueId(), entity);
                    config.q_Skill_effect_2.remove(player.getUniqueId(), entity);
                    cancel();
                    return;
                }

                Location baseLoc = entity.getLocation();
                for (int i = 0; i < 33; i += 2) {
                    double yOffset = i / 10.0;
                    world.spawnParticle(Particle.DUST, baseLoc.clone().add(0, yOffset, 0), 1, 0, 0, 0, 0, DUST_CHAIN);

                    if (i % 3 == 0) {
                        double hitY = 3.3 - (i * 0.12);
                        world.spawnParticle(Particle.ENCHANTED_HIT, baseLoc.clone().add(0, hitY, 0), 1, 0, 0, 0, 0);
                    }
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static LivingEntity getTargetedEntity(Player player, double range, double raySize) {
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        RayTraceResult result = world.rayTraceEntities(eyeLocation, direction, range, raySize,
                e -> e instanceof LivingEntity && !e.equals(player));

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            return target;
        }
        return null;
    }
}