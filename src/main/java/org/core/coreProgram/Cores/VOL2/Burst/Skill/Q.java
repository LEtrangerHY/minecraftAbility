package org.core.coreProgram.Cores.VOL2.Burst.Skill;

import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.Effect.Invulnerable;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.VOL2.Burst.coreSystem.Burst;

import java.util.HashSet;
import java.util.List;

public class Q implements SkillBase {
    private final Burst config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public Q(Burst config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){
        World world = player.getWorld();

        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), 4, 0.3, 0.3, 0.3, 1);

        Vector upward = new Vector(0, config.q_Skill_Jump, 0);
        Vector upward2 = new Vector(0, config.q_Skill_Jump * ((double) 3 /4), 0);

        for (Entity entity : world.getNearbyEntities(player.getLocation(), 4, 4, 4)) {
            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

            entity.setVelocity(upward2);

        }

        world.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.0f);

        player.setVelocity(upward);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.getPersistentDataContainer().set(new NamespacedKey(plugin, "noFallDamage"), PersistentDataType.BOOLEAN, true);
        }, 1L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Dash(player);
        }, 13L);
    }

    public void Dash(Player player){
        player.swingMainHand();

        Location startLocation = player.getLocation();

        Vector direction = startLocation.getDirection().normalize().multiply(config.q_Skill_dash);

        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        Invulnerable invulnerable = new Invulnerable(player, 600);
        invulnerable.applyEffect(player);

        detect(player);
    }

    public void detect(Player player){

        World world = player.getWorld();

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.6f);

        double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_Damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_EXPLOSION)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            private double ticks = 0;

            @Override
            public void run() {

                if (ticks > 6 || player.isDead()) {

                    Location playerLoc = player.getLocation().clone();

                    world.playSound(playerLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1, 1);
                    world.playSound(playerLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
                    world.spawnParticle(Particle.EXPLOSION, playerLoc.add(0, 0.6, 0), 3, 0.3, 0.3, 0.3, 1.0);
                    world.spawnParticle(Particle.FLAME, playerLoc.add(0, 0.6, 0), 44, 0.1, 0.1, 0.1, 0.8);
                    world.spawnParticle(Particle.SMOKE, playerLoc.add(0, 0.6, 0), 44, 0.1, 0.1, 0.1, 0.8);

                    for (Entity entity : world.getNearbyEntities(playerLoc, 4, 4, 4)) {
                        if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

                        world.spawnParticle(Particle.EXPLOSION, entity.getLocation().clone().add(0, 1, 0), 1, 0, 0, 0, 0);

                        ForceDamage forceDamage = new ForceDamage((LivingEntity) entity, damage, source);
                        forceDamage.applyEffect(player);

                        Vector direction = entity.getLocation().toVector().subtract(playerLoc.toVector()).normalize().multiply(1.4);
                        direction.setY(1.0);

                        entity.setVelocity(direction);
                    }

                    cancel();
                    return;
                }

                world.spawnParticle(Particle.DUST, player.getLocation().clone().add(0, 1, 0), 120, 0.3, 0, 0.3, 0.08, dustOptions);

                List<Entity> nearbyEntities = player.getNearbyEntities(0.6, 0.6, 0.6);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player) {

                    }
                }


                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
