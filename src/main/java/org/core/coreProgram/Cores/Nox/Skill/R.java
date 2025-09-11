package org.core.coreProgram.Cores.Nox.Skill;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.Effect.Invulnerable;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.Nox.Passive.Dream;
import org.core.coreProgram.Cores.Nox.coreSystem.Nox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class R implements SkillBase {

    private final Nox config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Dream dream;

    public R(Nox config, JavaPlugin plugin, Cool cool, Dream dream) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.dream = dream;
    }

    @Override
    public void Trigger(Player player) {

        player.swingMainHand();

        Location startLocation = player.getLocation();

        Vector direction = startLocation.getDirection().normalize().multiply(config.r_Skill_dash);

        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        Invulnerable invulnerable = new Invulnerable(player, 600);
        invulnerable.applyEffect(player);

        long cools = 3000 - (long) (config.dreamPoint.getOrDefault(player.getUniqueId(), new HashMap<>()).getOrDefault("R", 6.0) * 1000);
        if(3000 - (long) (config.dreamPoint.getOrDefault(player.getUniqueId(), new HashMap<>()).getOrDefault("R", 6.0) * 1000) > 600) {
            cool.updateCooldown(player, "R", cools);
        }

        detect(player);
    }

    public void detect(Player player){

        config.rskill_using.put(player.getUniqueId(), true);

        config.damaged.put(player.getUniqueId(), new HashSet<>());

        new BukkitRunnable() {
            private double ticks = 0;

            @Override
            public void run() {

                if (ticks > 6 || player.isDead()) {
                    config.rskill_using.remove(player.getUniqueId());
                    config.damaged.remove(player.getUniqueId());

                    dream.wanderersDream(player, "R");

                    cancel();
                    return;
                }

                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.6f);
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 120, 0.3, 0, 0.3, 0.08, dustOptions);

                List<Entity> nearbyEntities = player.getNearbyEntities(0.6, 0.6, 0.6);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player && !config.damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {
                        ForceDamage forceDamage = new ForceDamage(target, config.r_Skill_damage * (config.dreamPoint.getOrDefault(player.getUniqueId(), new HashMap<>()).getOrDefault("R", 1.0)));
                        forceDamage.applyEffect(player);
                        target.setVelocity(new Vector(0, 0, 0));
                        config.damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).add(target);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
