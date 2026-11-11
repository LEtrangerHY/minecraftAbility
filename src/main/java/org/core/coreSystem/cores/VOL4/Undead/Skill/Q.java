package org.core.coreSystem.cores.VOL4.Undead.Skill;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL4.Undead.Passive.handSwap;
import org.core.coreSystem.cores.VOL4.Undead.Passive.deepWound;
import org.core.coreSystem.cores.VOL4.Undead.Passive.stickyBomb;
import org.core.coreSystem.cores.VOL4.Undead.coreSystem.Undead;
import org.core.effect.crowdControl.Invulnerable;

import java.util.function.Consumer;

public class Q implements SkillBase {

    private final Undead config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final handSwap swap;
    private final stickyBomb bomb;
    private final deepWound wound;

    public Q(Undead config, JavaPlugin plugin, Cool cool, handSwap swap, stickyBomb bomb, deepWound wound) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.swap = swap;
        this.bomb = bomb;
        this.wound = wound;
    }

    @Override
    public void Trigger(Player player) {
        if (swap.isFirearm(player)) {
            firearm(player);
        } else {
            melee(player);
        }
    }

    private void melee(Player player) {
        dash(player);
        seek(player, config.melee_q_Skill_Range, target -> wound.mark(player, target));
    }

    private void firearm(Player player) {
        dash(player);
        seek(player, config.gun_q_Skill_Range, target -> bomb.attach(player, target));
    }

    private void seek(Player player, double range, Consumer<LivingEntity> action) {
        World world = player.getWorld();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks++ >= config.q_Skill_Dash_Ticks) {
                    cancel();
                    return;
                }

                LivingEntity nearest = null;
                double best = Double.MAX_VALUE;
                Vector center = player.getBoundingBox().getCenter();

                for (Entity entity : world.getNearbyEntities(player.getLocation(), range + 1, range + 1, range + 1)) {
                    if (!isTarget(player, entity)) continue;
                    LivingEntity target = (LivingEntity) entity;
                    double distance = target.getBoundingBox().getCenter().distance(center);
                    if (distance <= range && distance < best && player.hasLineOfSight(target)) {
                        best = distance;
                        nearest = target;
                    }
                }

                if (nearest != null) {
                    action.accept(nearest);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void dash(Player player) {
        Vector direction = player.getLocation().getDirection().normalize().multiply(config.q_Skill_Dash);
        player.setVelocity(direction);

        new Invulnerable(player, config.q_Skill_Dash_Ticks * 50L).applyEffect(player);

        World world = player.getWorld();
        world.playSound(player, Sound.ENTITY_BREEZE_WIND_BURST, 1.2f, 0.8f);
        world.playSound(player, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 1.3f);
        world.playSound(player, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.2f);
        world.playSound(player, Sound.ITEM_MACE_SMASH_AIR, 0.9f, 1.1f);
        world.playSound(player, Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 0.7f);
        world.spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.2, 0), 8, 0.3, 0.05, 0.3, 0.02);
    }

    private boolean isTarget(Player player, Entity entity) {
        return entity instanceof LivingEntity && entity != player && !(entity instanceof ArmorStand) && !entity.isDead();
    }
}