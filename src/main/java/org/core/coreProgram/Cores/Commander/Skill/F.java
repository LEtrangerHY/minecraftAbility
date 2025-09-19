package org.core.coreProgram.Cores.Commander.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.Commander.coreSystem.Commander;

import java.util.HashSet;

public class F implements SkillBase {

    private final Commander config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Commander config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){
        if(!config.comBlocks.getOrDefault(player.getUniqueId(), new HashSet<>()).isEmpty()){
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            for(FallingBlock fb : config.comBlocks.getOrDefault(player.getUniqueId(), new HashSet<>())){
                circleParticle(player, fb.getLocation().clone().add(0, 0.5, 0));
                commandReceiver_1(player, fb);
            }
        }else{
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            player.sendActionBar(Component.text("com-block uninstalled").color(NamedTextColor.RED));
            long cools = 100L;
            cool.updateCooldown(player, "F", cools);
        }
    }

    public void circleParticle(Player player, Location center){

        double Length = 6.0;
        double maxAngle = Math.toRadians(180);
        long tickDelay = 0L;
        int maxTicks = 5;
        double innerRadius = 5.8;

        Vector direction = center.getDirection().clone().setY(0).normalize();

        Particle.DustOptions dustOption = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 0.7f);

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

    public void commandReceiver_1(Player player, FallingBlock fb) {
        World world = player.getWorld();
        Location center = fb.getLocation();

        for (Entity entity : world.getNearbyEntities(center, 6, 6, 6)) {
            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

            fb.getWorld().playSound(fb.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);

            Location start = fb.getLocation().clone().add(0, 0.5, 0);

            Vector dir = entity.getLocation().clone().add(0, 1.2, 0).toVector().subtract(start.toVector()).normalize();

            double maxDistance = start.distance(entity.getLocation().clone().add(0, 1, 0));

            attackLine(player, maxDistance, start, dir);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (world.getNearbyEntities(center, 1, 1, 1).contains(entity) || !fb.isValid()) {
                        if(fb.isValid()) {
                            commandReceiver_2(player, center, entity);
                        }
                        this.cancel();
                    } else {
                        Vector direction = center.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(1.0);
                        entity.setVelocity(direction);
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    public void commandReceiver_2(Player player, Location center, Entity entity) {
        World world = player.getWorld();

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = 4 * (1 + amp);

        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 70, 0.2, 0.2, 0.2, 0.4);
        player.getWorld().playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1, 1);

        ForceDamage forceDamage = new ForceDamage((LivingEntity) entity, damage);
        forceDamage.applyEffect(player);
        entity.setVelocity(new Vector(0, 0, 0));
    }

    public void attackLine(Player player, double maxDistance, Location start, Vector direction){

        double step = 0.2;

        Particle.DustOptions dustOptions_gra = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 0.7f);

        for (double i = 0; i <= maxDistance; i += step) {
            Location point = start.clone().add(direction.clone().multiply(i));
            player.spawnParticle(Particle.DUST, point, 2, 0.05, 0.05, 0.05, 0, dustOptions_gra);
        }
    }

}
