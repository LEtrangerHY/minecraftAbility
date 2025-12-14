package org.core.coreProgram.Cores.VOL1.Swordsman.Skill;

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
import org.core.Effect.Stun;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.VOL1.Swordsman.Passive.Laido;
import org.core.coreProgram.Cores.VOL1.Swordsman.coreSystem.Swordsman;

import java.util.HashSet;
import java.util.List;

public class R implements SkillBase {
    private final Swordsman config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Laido laido;

    public R(Swordsman config, JavaPlugin plugin, Cool cool, Laido laido) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.laido = laido;
    }

    @Override
    public void Trigger(Player player){

        World world = player.getWorld();

        player.swingMainHand();

        Location startLocation = player.getLocation();

        Vector direction = startLocation.getDirection().normalize().multiply(config.r_Skill_dash);

        player.setVelocity(direction);
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        Invulnerable invulnerable = new Invulnerable(player, 550);
        invulnerable.applyEffect(player);

        if(!config.laidoSlash.getOrDefault(player.getUniqueId(), false)) {

            Rapid(player);

            int count = config.r_Skill_count.getOrDefault(player.getUniqueId(), 0);
            config.r_Skill_count.put(player.getUniqueId(), count + 1);

            if(config.r_Skill_count.getOrDefault(player.getUniqueId(), 0) >= 2){
                cool.updateCooldown(player, "R", 8900L);
                config.r_Skill_count.remove(player.getUniqueId());
            }else{
                cool.updateCooldown(player, "R", 550L);
            }
        }else{
            int count = config.r_Skill_count.getOrDefault(player.getUniqueId(), 0);
            Quick(player, count);

            config.r_Skill_count.put(player.getUniqueId(), count + 2);

            cool.updateCooldown(player, "R", 10000L);
            config.r_Skill_count.remove(player.getUniqueId());

            laido.Draw(player);
        }
    }

    public void Rapid(Player player){

        World world = player.getWorld();

        config.r_skillUsing.put(player.getUniqueId(), true);
        config.r_damaged.put(player.getUniqueId(), new HashSet<>());

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .build();

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.6f);

        new BukkitRunnable(){
            int ticks = 0;

            @Override
            public void run() {

                if (ticks > 5 || player.isDead()) {
                    config.r_skillUsing.remove(player.getUniqueId());
                    config.r_damaged.remove(player.getUniqueId());

                    cancel();
                    return;
                }

                world.spawnParticle(Particle.DUST, player.getLocation().clone().add(0, 1, 0), 100, 0.3, 0, 0.3, 0.08, dustOptions);

                List<Entity> nearbyEntities = player.getNearbyEntities(0.5, 0.5, 0.5);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player && !config.r_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                        ForceDamage forceDamage = new ForceDamage(target, damage, source);
                        forceDamage.applyEffect(player);
                        target.setVelocity(new Vector(0, 0, 0));

                        config.r_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).add(target);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1L);
    }

    public void Quick(Player player, int count){

        World world = player.getWorld();

        Location firstLoc = player.getLocation().clone();

        config.q_skillUsing.put(player.getUniqueId(), true);
        config.r_damaged.put(player.getUniqueId(), new HashSet<>());

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .build();

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.6f);

        new BukkitRunnable() {
            private double ticks = 0;

            @Override
            public void run() {

                if (ticks > 5 || player.isDead()) {
                    config.q_skillUsing.remove(player.getUniqueId());
                    config.r_damaged.remove(player.getUniqueId());

                    Location secondLoc = player.getLocation().clone();
                    if(!player.isDead()) {
                        Draw(player, firstLoc, secondLoc, count);
                    }

                    cancel();
                    return;
                }

                world.spawnParticle(Particle.DUST, player.getLocation().clone().add(0, 1, 0), 100, 0.3, 0, 0.3, 0.08, dustOptions);

                List<Entity> nearbyEntities = player.getNearbyEntities(0.5, 0.5, 0.5);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player && !config.r_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                        Stun stun = new Stun(entity, config.r_Skill_stun);
                        stun.applyEffect(player);

                        ForceDamage forceDamage = new ForceDamage(target, damage, source);
                        forceDamage.applyEffect(player);
                        target.setVelocity(new Vector(0, 0, 0));

                        config.r_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).add(target);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void Draw(Player player, Location first, Location second, int count) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            World world = player.getWorld();

            config.r_damaged_2.put(player.getUniqueId(), new HashSet<>());
            world.playSound(second.clone().add(0, 1.0, 0), Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);
            world.playSound(second.clone().add(0, 1.0, 0), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

            boolean isFirst = (count < 1);
            double additional_damage = (isFirst) ? config.r_Skill_add_damage : config.r_Skill_add_damage / 3;
            double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
            additional_damage = additional_damage * (1 + amp);

            DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                    .withCausingEntity(player)
                    .build();

            Vector path = second.clone().toVector().subtract(first.toVector());
            double distance = path.length();
            path.normalize();

            double step = 0.7;

            for (double d = 0; d <= distance; d += step) {
                Location point = first.clone().add(path.clone().multiply(d));

                world.spawnParticle(Particle.SWEEP_ATTACK, point.clone().add(0, 1.0, 0), 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.SMOKE, point.clone().add(0, 1.0, 0), (isFirst) ? 20 : 10, 0.5, 0.0, 0.5, 0);

                List<Entity> nearbyEntities = world.getNearbyEntities(point, 0.5, 0.5, 0.5).stream().toList();
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player
                            && !config.r_damaged_2.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                        world.spawnParticle(Particle.CRIT, target.getLocation().clone().add(0, 1.0, 0), 20, 0.4, 0.4, 0.4, 1);
                        if (isFirst) world.spawnParticle(Particle.SPIT, target.getLocation().clone().add(0, 1.0, 0), 20, 0.2, 0.3, 0.2, 0.5);

                        ForceDamage forceDamage = new ForceDamage(target, additional_damage, source);
                        forceDamage.applyEffect(player);
                        target.setVelocity(new Vector(0, 0, 0));

                        config.r_damaged_2.get(player.getUniqueId()).add(target);
                    }
                }
            }

            config.r_damaged_2.remove(player.getUniqueId());
        }, 5L);
    }

}