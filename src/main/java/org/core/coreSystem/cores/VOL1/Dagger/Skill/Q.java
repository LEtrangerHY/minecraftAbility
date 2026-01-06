package org.core.coreSystem.cores.VOL1.Dagger.Skill;

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
import org.core.coreSystem.cores.VOL1.Dagger.Passive.DamageStroke;
import org.core.coreSystem.cores.VOL1.Dagger.coreSystem.Dagger;

import java.util.HashSet;
import java.util.List;

public class Q implements SkillBase {
    private final Dagger config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final DamageStroke damagestroker;

    public Q(Dagger config, JavaPlugin plugin, Cool cool, DamageStroke damagestroker) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.damagestroker = damagestroker;
    }

    @Override
    public void Trigger(Player player) {
        player.swingMainHand();

        Location startLocation = player.getLocation();

        Vector direction = startLocation.getDirection().normalize().multiply(config.q_Skill_dash);

        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        Invulnerable invulnerable = new Invulnerable(player, 500);
        invulnerable.applyEffect(player);

        detect_1(player);
    }

    public void detect_1(Player player){
        config.f_damaged_2.put(player.getUniqueId(), new HashSet<>());

        double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_Damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            private double ticks = 0;

            @Override
            public void run() {

                if (ticks > 5 || player.isDead()) {
                    config.f_damaged_2.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.6f);
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 60, 0.3, 0, 0.3, 0.08, dustOptions);

                List<Entity> nearbyEntities = player.getNearbyEntities(0.6, 0.6, 0.6);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player && !config.f_damaged_2.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                        config.f_damaging.put(player.getUniqueId(), true);
                        damagestroker.damageStroke(player, target);

                        ForceDamage forceDamage = new ForceDamage(target, damage, source);
                        forceDamage.applyEffect(player);
                        target.setVelocity(new Vector(0, 0, 0));
                        config.f_damaging.remove(player.getUniqueId());

                        player.setVelocity(new Vector(0, 0, 0));

                        largeDash(player);

                        config.f_damaged_2.remove(player.getUniqueId());
                        this.cancel();
                        break;
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void largeDash(Player player){
        player.swingMainHand();

        Location startLocation = player.getLocation();

        Vector direction = startLocation.getDirection().normalize().multiply(config.q_Skill_dash_2);

        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, 1.0f, 1.0f);
        player.spawnParticle(Particle.SPIT, player.getLocation().add(0, 1.0, 0),10, 0.1, 0.2, 0.1, 0.5);

        Invulnerable invulnerable = new Invulnerable(player, 800);
        invulnerable.applyEffect(player);

        detect_2(player);
    }

    public void detect_2(Player player){

        config.q_damaged.put(player.getUniqueId(), new HashSet<>());

        double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_Damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            private double ticks = 0;

            @Override
            public void run() {

                if (ticks > 8 || player.isDead()) {
                    long coolTime = config.q_Skill_Cool_2 - config.q_Skill_Cool_decrease * config.q_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).size();
                    if(coolTime > 50) {
                        cool.updateCooldown(player, "Q", coolTime);
                    }else{
                        cool.updateCooldown(player, "Q", 50L);
                    }
                    config.q_damaged.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.8f);
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 25, 0.6, 0.1, 0.6, 0.08, dustOptions);
                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1.0, 0), 25, 0.4, 0.1, 0.4, 0);
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1.0, 0), 100, 1.0, 0, 1.0, 0);

                List<Entity> nearbyEntities = player.getNearbyEntities(1.0, 1.0, 1.0);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player && !config.q_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                        config.f_damaging.put(player.getUniqueId(), true);
                        damagestroker.damageStroke(player, target);

                        ForceDamage forceDamage = new ForceDamage(target, damage / 2, source);
                        forceDamage.applyEffect(player);
                        target.setVelocity(new Vector(0, 0, 0));
                        config.f_damaging.remove(player.getUniqueId());

                        config.q_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).add(target);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}