package org.core.coreSystem.cores.VOL2.Burst.Skill;

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
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Burst.coreSystem.Burst;

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

        world.spawnParticle(Particle.EXPLOSION, player.getLocation().clone().add(0, 1, 0), 4, 0.3, 0.3, 0.3, 1);
        world.playSound(player.getLocation().clone(), Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);

        Location playerLoc = player.getLocation().clone();

        double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_Damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_EXPLOSION)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        for (Entity entity : world.getNearbyEntities(playerLoc, 3, 3, 3)) {
            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

            world.spawnParticle(Particle.EXPLOSION, entity.getLocation().clone().add(0, 1, 0), 1, 0, 0, 0, 0);

            ForceDamage forceDamage = new ForceDamage((LivingEntity) entity, damage / 2, source);
            forceDamage.applyEffect(player);

            Vector direction = entity.getLocation().toVector().subtract(playerLoc.toVector()).normalize().multiply(1.0);
            direction.setY(0.6);

            entity.setVelocity(direction);
        }

        Vector upward = new Vector(0, config.q_Skill_Jump, 0);

        player.setVelocity(upward);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setVelocity(new Vector(0, 0, 0));
            Dash(player);
        }, 13L);
    }

    public void Dash(Player player) {
        World world = player.getWorld();

        world.spawnParticle(Particle.EXPLOSION, player.getLocation().clone().add(0, 1, 0), 4, 0.3, 0.3, 0.3, 1);
        world.playSound(player.getLocation().clone(), Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);

        Location playerLoc = player.getLocation().clone();

        double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_Damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_EXPLOSION)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        for (Entity entity : world.getNearbyEntities(playerLoc, 3, 3, 3)) {
            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

            world.spawnParticle(Particle.EXPLOSION, entity.getLocation().clone().add(0, 1, 0), 1, 0, 0, 0, 0);

            ForceDamage forceDamage = new ForceDamage((LivingEntity) entity, damage / 2, source);
            forceDamage.applyEffect(player);

            Vector direction = entity.getLocation().toVector().subtract(playerLoc.toVector()).normalize().multiply(1.0);
            direction.setY(0.6);

            entity.setVelocity(direction);
        }

        Invulnerable invulnerable = new Invulnerable(player, 1300);
        invulnerable.applyEffect(player);

        detect(player);
    }


    public void detect(Player player){
        World world = player.getWorld();

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

                if (ticks > 7 || player.isDead()) {

                    player.setVelocity(new Vector(0, 0, 0));

                    Location playerLoc = player.getLocation().clone();

                    world.playSound(playerLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2, 1);
                    world.playSound(playerLoc, Sound.ENTITY_GENERIC_EXPLODE, 2, 1);
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

                Location startLocation = player.getLocation().clone();
                Vector direction = startLocation.getDirection().normalize().multiply(config.q_Skill_dash);
                player.setVelocity(direction);

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
