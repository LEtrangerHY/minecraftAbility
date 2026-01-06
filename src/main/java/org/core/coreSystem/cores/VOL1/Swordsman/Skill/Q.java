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
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Swordsman.Passive.Laido;
import org.core.coreSystem.cores.VOL1.Swordsman.coreSystem.Swordsman;

import java.util.HashSet;

public class Q implements SkillBase {
    private final Swordsman config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Laido laido;

    public Q(Swordsman config, JavaPlugin plugin, Cool cool, Laido laido) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.laido = laido;
    }

    @Override
    public void Trigger(Player player) {

        int slashTimes = (config.laidoSlash.getOrDefault(player.getUniqueId(), false)) ? 2 : 1;

        long maxTicks = (slashTimes > 1) ? 3 : 7;

        config.q_skillUsing.put(player.getUniqueId(), true);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= slashTimes || player.isDead()) {
                    config.q_skillUsing.remove(player.getUniqueId());
                    config.q_damaged.remove(player.getUniqueId());

                    this.cancel();
                    return;
                }

                double height = - 0.8 + tick * 0.4;

                tick++;

                Slash(player, slashTimes, tick, height);
            }
        }.runTaskTimer(plugin, 0L, maxTicks);

    }

    public void Slash(Player player, int slashTimes, int slashCount, double height){

        World world = player.getWorld();

        boolean duelSlash = (slashTimes > 1);

        if(duelSlash && slashCount == 1) laido.Draw(player);
        player.swingMainHand();
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

        double slashLength = 4.4;
        double maxAngle = (duelSlash) ? Math.toRadians(45) : Math.toRadians(100);
        if(!(slashCount > 1)) maxAngle = -maxAngle;
        double angleIncrease = (!(slashCount > 1)) ? -Math.toRadians(2) : Math.toRadians(2);
        double maxTicks = (duelSlash) ? 3 : 7;
        double innerRadius = 2.4;

        config.q_damaged.put(player.getUniqueId(), new HashSet<>());

        double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Location origin = player.getEyeLocation().clone().add(0, height, 0);
        Vector direction = player.getLocation().getDirection().clone().setY(0).normalize();

        Particle.DustOptions dustOption_slash = new Particle.DustOptions(Color.fromRGB(127, 127, 127), 0.6f);

        Particle.DustOptions dustOption_duelSlash = (slashCount == 2) ? new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.6f) : new Particle.DustOptions(Color.fromRGB(0, 0, 0), 0.6f);

        double finalMaxAngle = maxAngle;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (ticks >= maxTicks || player.isDead()) {
                    this.cancel();
                    return;
                }

                double progress = (ticks + 1) * (finalMaxAngle * 2 / maxTicks) - finalMaxAngle;
                Vector rotatedDir = direction.clone().rotateAroundY(progress);

                for (double length = 0; length <= slashLength; length += 0.1) {
                    for (double angle = -finalMaxAngle; (slashCount == 2) ? angle <= finalMaxAngle : angle >= finalMaxAngle; angle += angleIncrease) {
                        Vector angleDir = rotatedDir.clone().rotateAroundY(angle);
                        Vector particleOffset = angleDir.clone().multiply(length);

                        Location particleLocation = origin.clone().add(particleOffset);

                        double distanceFromOrigin = particleLocation.distance(origin);


                        if (distanceFromOrigin >= innerRadius) {
                            if(duelSlash) {
                                world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOption_duelSlash);
                            }else{
                                world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOption_slash);
                            }
                        }

                        for (Entity entity : world.getNearbyEntities(particleLocation, 0.4, 0.4, 0.4)) {
                            if (entity instanceof LivingEntity target && entity != player
                                    && !config.q_damaged.get(player.getUniqueId()).contains(entity)) {

                                ForceDamage forceDamage = new ForceDamage(target, damage, source);
                                forceDamage.applyEffect(player);

                                target.setVelocity(new Vector(0, 0, 0));

                                int currentAbsorption = player.getAbsorptionAmount() > 0 ? (int) player.getAbsorptionAmount() : 0;
                                int newAbsorption = Math.min(currentAbsorption + 1, 8);
                                int amplifier = (newAbsorption / 2) - 1;
                                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, Integer.MAX_VALUE, amplifier, false, false, false));

                                config.q_damaged.get(player.getUniqueId()).add(entity);
                            }
                        }

                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}