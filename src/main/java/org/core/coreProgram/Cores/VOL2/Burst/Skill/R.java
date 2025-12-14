package org.core.coreProgram.Cores.VOL2.Burst.Skill;

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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.VOL2.Burst.coreSystem.Burst;

import java.util.ArrayList;
import java.util.List;

public class R implements SkillBase {
    private final Burst config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R(Burst config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){
        World world = player.getWorld();
        Block targetBlock = getCustomTargetBlock(player, 14);
        if (targetBlock == null || !targetBlock.getType().isSolid()) {
            player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1, 1);
            cool.updateCooldown(player, "R", 500L);
            return;
        }

        world.playSound(targetBlock.getLocation().clone(), Sound.ENTITY_TNT_PRIMED, 1f, 1f);

        Location particleLoc = targetBlock.getLocation().add(0.5, 1.5, 0.5);

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_Damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_EXPLOSION)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || tick > 40) {

                    world.playSound(particleLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1, 1);
                    world.playSound(particleLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
                    world.spawnParticle(Particle.EXPLOSION, particleLoc.clone().add(0, 0.6, 0), 3, 0.3, 0.3, 0.3, 1.0);
                    world.spawnParticle(Particle.FLAME, particleLoc.clone().add(0, 0.6, 0), 44, 0.1, 0.1, 0.1, 0.8);
                    world.spawnParticle(Particle.SMOKE, particleLoc.clone().add(0, 0.6, 0), 44, 0.1, 0.1, 0.1, 0.8);

                    for (Entity entity : world.getNearbyEntities(particleLoc, 4, 4, 4)) {
                        if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

                        world.spawnParticle(Particle.EXPLOSION, entity.getLocation().clone().add(0, 1, 0), 1, 0, 0, 0, 0);

                        ForceDamage forceDamage = new ForceDamage((LivingEntity) entity, damage, source);
                        forceDamage.applyEffect(player);

                        Vector direction = entity.getLocation().toVector().subtract(particleLoc.toVector()).normalize().multiply(1.4);
                        direction.setY(1.0);

                        entity.setVelocity(direction);
                    }

                    cancel();
                    return;
                }

                world.spawnParticle(Particle.FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0.04);

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
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
