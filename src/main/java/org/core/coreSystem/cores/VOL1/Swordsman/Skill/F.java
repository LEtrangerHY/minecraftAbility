package org.core.coreSystem.cores.VOL1.Swordsman.Skill;

import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;
import org.core.effect.crowdControl.Stun;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Swordsman.Passive.Laido;
import org.core.coreSystem.cores.VOL1.Swordsman.coreSystem.Swordsman;

import java.util.HashSet;
import java.util.List;

public class F implements SkillBase {
    private final Swordsman config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Laido laido;

    public F(Swordsman config, JavaPlugin plugin, Cool cool, Laido laido) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.laido = laido;
    }

    @Override
    public void Trigger(Player player){

        World world = player.getWorld();

        player.swingMainHand();

        if(!config.laidoSlash.getOrDefault(player.getUniqueId(), false)){

            world.spawnParticle(Particle.SPIT, player.getLocation().clone().add(0, 1.0, 0), 20, 0.2, 0.3, 0.2, 0.5);
            world.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.4f);
            laido.Sheath(player);
            cool.updateCooldown(player, "R", 0L);
            config.r_Skill_count.remove(player.getUniqueId());
            cool.updateCooldown(player, "Q", 0L);

        }else{

            Slash(player);
            Dash(player);
            laido.Draw(player);
        }
    }

    public void Dash(Player player){
        World world = player.getWorld();

        Location startLocation = player.getLocation();

        Vector direction = startLocation.getDirection().normalize().multiply(config.f_Skill_dash);

        player.setVelocity(direction);

        Invulnerable invulnerable = new Invulnerable(player,  200);
        invulnerable.applyEffect(player);

        config.f_skillUsing.put(player.getUniqueId(), true);

        world.spawnParticle(Particle.SPIT, player.getLocation().clone().add(0, 1.0, 0), 20, 0.2, 0.3, 0.2, 0.5);

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.6f);

        new BukkitRunnable(){
            int ticks = 0;

            @Override
            public void run() {

                if (ticks > 4 || player.isDead()) {
                    PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS, 20, 255, false, false);
                    player.addPotionEffect(slowness);

                    Invulnerable invulnerable = new Invulnerable(player, 2000);
                    invulnerable.applyEffect(player);

                    laidoChainSlash(player);

                    cancel();
                    return;
                }

                world.spawnParticle(Particle.DUST, player.getLocation().clone().add(0, 1, 0), 100, 0.3, 0, 0.3, 0.08, dustOptions);

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1L);
    }

    public void Slash(Player player) {

        player.swingMainHand();
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);

        double slashLength = 3.3;
        double maxAngle = Math.toRadians(45);
        int maxTicks = 4;
        double innerRadius = 1.1;

        config.f_damaged.put(player.getUniqueId(), new HashSet<>());

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Location origin = player.getEyeLocation().add(0, -0.6, 0);
        Vector direction = player.getLocation().getDirection().clone().setY(0).normalize();

        Particle.DustOptions dustOption_slash = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 0.6f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (ticks >= maxTicks || player.isDead()) {

                    config.f_damaged.remove(player.getUniqueId());

                    this.cancel();
                    return;
                }

                double progress = (ticks + 1) * (maxAngle * 2 / maxTicks) - maxAngle;
                Vector rotatedDir = direction.clone().rotateAroundY(progress);

                for (double length = 0; length <= slashLength; length += 0.1) {
                    for (double angle = -maxAngle; angle <= maxAngle; angle += Math.toRadians(2)) {
                        Vector angleDir = rotatedDir.clone().rotateAroundY(angle);
                        Vector particleOffset = angleDir.clone().multiply(length);

                        Location particleLocation = origin.clone().add(particleOffset);

                        double distanceFromOrigin = particleLocation.distance(origin);

                        if (distanceFromOrigin >= innerRadius) {
                            world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOption_slash);
                        }

                        for (Entity entity : world.getNearbyEntities(particleLocation, 0.5, 0.5, 0.5)) {
                            if (entity instanceof LivingEntity target && entity != player && !config.f_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {
                                Stun stun = new Stun(entity, config.f_Skill_stun);
                                stun.applyEffect(player);

                                ForceDamage forceDamage = new ForceDamage(target, damage, source);
                                forceDamage.applyEffect(player);
                                target.setVelocity(new Vector(0, 0, 0));

                                config.f_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).add(entity);
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void laidoChainSlash(Player player){
        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            World world = player.getWorld();

            double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
            double damage = (config.f_Skill_damage / 3) * (1 + amp);

            DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                    .withCausingEntity(player)
                    .build();

            laido.Sheath(player);

            new BukkitRunnable() {
                private double ticks = 0;

                @Override
                public void run() {

                    if (ticks > 7 || player.isDead()) {
                        config.f_skillUsing.remove(player.getUniqueId());

                        cancel();
                        return;
                    }

                    world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
                    world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.0f);

                    world.spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1.0, 0), 20, 2, 2, 2, 1);

                    List<Entity> nearbyEntities = player.getNearbyEntities(5, 5, 5);
                    for (Entity entity : nearbyEntities) {
                        if (entity instanceof LivingEntity target && entity != player) {

                            world.spawnParticle(Particle.CRIT, target.getLocation().clone().add(0, 1.0, 0), 20, 0.5, 0.5, 0.5, 1);

                            ForceDamage forceDamage = new ForceDamage(target, damage, source);
                            forceDamage.applyEffect(player);
                            target.setVelocity(new Vector(0, 0, 0));
                        }
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, 0, 2);
        }, 5L);
    }
}
