package org.core.coreProgram.Cores.Commander.Skill;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.coreProgram.Abs.SkillBase;
import org.core.coreProgram.Cores.Commander.coreSystem.Commander;

import java.util.HashSet;
import java.util.LinkedHashMap;

public class Q implements SkillBase {

    private final Commander config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public Q(Commander config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        for(FallingBlock fb : config.comBlocks.getOrDefault(player.getUniqueId(), new HashSet<>())){
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, fb.getLocation().clone().add(0, 0.5, 0), 30, 0.2, 0.2, 0.2, 1);
            circleParticle(player, fb.getLocation().clone().add(0, 0.5, 0));
            commandReceiver(player, fb);
        }
    }

    public void circleParticle(Player player, Location center){

        double Length = 3.5;
        double maxAngle = Math.toRadians(180);
        long tickDelay = 0L;
        int maxTicks = 5;
        double innerRadius = 3.4;

        Vector direction = center.getDirection().clone().setY(0).normalize();

        Particle.DustOptions dustOption = new Particle.DustOptions(Color.fromRGB(0, 0, 255), 0.7f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }

                double progress = (ticks + 1) * (maxAngle * 2 / maxTicks) - maxAngle;
                Vector rotatedDir = direction.clone().rotateAroundY(progress);

                for (double length = 0; length <= Length; length += 0.1) {
                    for (double angle = -maxAngle; angle <= maxAngle; angle += Math.toRadians(2)) {
                        Vector angleDir = rotatedDir.clone().rotateAroundY(angle);
                        Vector particleOffset = angleDir.clone().multiply(length);

                        Location particleLocation = center.clone().add(particleOffset);

                        double distanceFromOrigin = particleLocation.distance(center);

                        if (distanceFromOrigin >= innerRadius) {
                            player.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOption);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, tickDelay, 1L);
    }

    public void commandReceiver(Player player, FallingBlock fb) {
        World world = player.getWorld();
        Location center = fb.getLocation();

        for (Entity entity : world.getNearbyEntities(center, 3.5, 3.5, 3.5)) {
            if (!(entity instanceof LivingEntity)) continue;

            if(!entity.equals(player)) {
                ForceDamage forceDamage = new ForceDamage((LivingEntity) entity, 2);
                forceDamage.applyEffect(player);
                entity.setVelocity(new Vector(0, 0, 0));

                PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS, 20 * 4, 1, false, false);
                ((LivingEntity) entity).addPotionEffect(slowness);

                fb.getWorld().playSound(fb.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);

                Location start = fb.getLocation().clone().add(0.5, 0.5, 0.5);

                Vector dir = entity.getLocation().clone().add(0, 1.2, 0).toVector().subtract(start.toVector()).normalize();

                double maxDistance = start.distance(entity.getLocation().clone().add(0, 1, 0));

                attackLine(player, maxDistance, start, dir);
            }else{
                ((LivingEntity) entity).heal(2);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f, 1);
                player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().clone().add(0, 1.2, 0), 5, 0.2, 0.2, 0.2, 0);
            }
        }
    }

    public void attackLine(Player player, double maxDistance, Location start, Vector direction){

        double step = 0.2;

        Particle.DustOptions dustOptions_gra = new Particle.DustOptions(Color.fromRGB(0, 0, 255), 0.7f);

        for (double i = 0; i <= maxDistance; i += step) {
            Location point = start.clone().add(direction.clone().multiply(i));
            player.spawnParticle(Particle.DUST, point, 2, 0.05, 0.05, 0.05, 0, dustOptions_gra);
        }
    }
}
