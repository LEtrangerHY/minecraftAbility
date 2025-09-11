package org.core.coreProgram.Cores.Nox.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.Nox.Passive.Dream;
import org.core.coreProgram.Cores.Nox.coreSystem.Nox;

import java.util.HashMap;
import java.util.HashSet;

public class Q implements SkillBase {

    private final Nox config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Dream dream;

    public Q(Nox config, JavaPlugin plugin, Cool cool, Dream dream) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.dream = dream;
    }

    @Override
    public void Trigger(Player player) {

        TeleportPos(player, 0);

    }

    public void TeleportPos(Player player, double loop){

        World world = player.getWorld();

        Location start = player.getLocation().clone();
        Vector direction = start.getDirection().normalize();
        double maxDistance = 6 - loop;

        Location targetLocation = start.clone().add(direction.clone().multiply(maxDistance));

        Block feetBlock = targetLocation.getBlock();
        Block headBlock = targetLocation.clone().add(0, 1, 0).getBlock();

        if (feetBlock.isPassable() && headBlock.isPassable()) {
            world.spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().clone().add(0, 1.2, 0), 33, 0.6, 0.6, 0.6, 1);
            player.teleport(targetLocation);
            world.spawnParticle(Particle.SPIT, player.getLocation().clone(), 33, 0.2, 0.3, 0.2, 1);

            long cools = 6000 - (long) (config.dreamPoint.getOrDefault(player.getUniqueId(), new HashMap<>()).getOrDefault("Q", 6.0) * 1000);
            if(6000 - (long) (config.dreamPoint.getOrDefault(player.getUniqueId(), new HashMap<>()).getOrDefault("Q", 6.0) * 1000) > 600) {
                cool.updateCooldown(player, "Q", cools);
            }

            TeleportSlash(player, maxDistance, start, direction);

        } else {
            if(maxDistance < 0){
                world.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
                player.sendActionBar(Component.text("failed").color(NamedTextColor.RED));
            }else{
                TeleportPos(player, loop + 0.2);
            }
        }
    }

    public void TeleportSlash(Player player, double maxDistance, Location start, Vector direction){

        World world = player.getWorld();

        double step = 0.5;
        config.damaged_3.put(player.getUniqueId(), new HashSet<>());

        boolean hit = false;

        for (double i = 0; i <= maxDistance; i += step) {
            Location point = start.clone().add(direction.clone().multiply(i));
            for (Entity entity : world.getNearbyEntities(point, 1.2, 1.2, 1.2)) {
                if (entity instanceof LivingEntity target && entity != player) {
                    if (!config.damaged_3.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(target)) {
                        hit = true;
                        new BukkitRunnable() {
                            int tick = 0;

                            @Override
                            public void run() {
                                if (tick > config.dreamPoint.getOrDefault(player.getUniqueId(), new HashMap<>()).getOrDefault("Q", 1.0)
                                        || player.isDead()) {
                                    this.cancel();
                                    return;
                                }
                                world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                                world.playSound(target.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1f, 1f);

                                ForceDamage forceDamage = new ForceDamage(target, config.q_Skill_damage);
                                forceDamage.applyEffect(player);
                                target.setVelocity(new Vector(0, 0, 0));

                                world.spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().clone().add(0, 1.2, 0), 3, 0.6, 0.6, 0.6, 1);
                                world.spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().clone().add(0, 1.2, 0), 11, 0.6, 0.6, 0.6, 1);

                                tick++;
                            }
                        }.runTaskTimer(plugin, 0L, 2L);

                        config.damaged_3.getOrDefault(player.getUniqueId(), new HashSet<>()).add(target);
                    }
                }
            }
        }

        if (hit) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                world.playSound(start, Sound.ENTITY_WITHER_SHOOT, 1f, 1f);
                dream.wanderersDream(player, "Q");
            }, config.dreamPoint
                    .getOrDefault(player.getUniqueId(), new HashMap<>())
                    .getOrDefault("Q", 1.0)
                    .longValue() * 2 + 1);
        } else {
            world.playSound(start, Sound.ENTITY_WITHER_SHOOT, 1f, 1f);
            dream.wanderersDream(player, "Q");
        }
    }
}
