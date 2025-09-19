package org.core.coreProgram.Cores.Nox.Skill;

import org.bukkit.*;
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

        boolean diff = (config.dreamSkill.containsKey(player.getUniqueId()) && !config.dreamSkill.getOrDefault(player.getUniqueId(), "").equals("R"));

        dream.dreamPoint(player, config.r_Skill_Cool, "R");

        player.swingMainHand();

        Location startLocation = player.getLocation();

        Vector direction = startLocation.getDirection().normalize().multiply(config.r_Skill_dash);

        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        Invulnerable invulnerable = new Invulnerable(player, 600);
        invulnerable.applyEffect(player);

        detect(player, diff);
    }

    public void detect(Player player, boolean diff){

        World world = player.getWorld();

        config.rskill_using.put(player.getUniqueId(), true);

        config.damaged.put(player.getUniqueId(), new HashSet<>());

        double damage = diff ? config.r_Skill_damage * 3 : config.r_Skill_damage;

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        damage = damage * (1 + amp);

        double finalDamage = damage;
        new BukkitRunnable() {
            private double ticks = 0;

            @Override
            public void run() {

                if (ticks > 6 || player.isDead()) {
                    config.rskill_using.remove(player.getUniqueId());
                    config.damaged.remove(player.getUniqueId());

                    cancel();
                    return;
                }

                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.6f);
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 120, 0.3, 0, 0.3, 0.08, dustOptions);

                List<Entity> nearbyEntities = player.getNearbyEntities(0.6, 0.6, 0.6);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player && !config.damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                        if(diff) {
                            world.playSound(target.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.6f, 1.0f);
                            world.spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().clone().add(0, 1.2, 0), 33, 0.6, 0.6, 0.6, 1);
                        }

                        ForceDamage forceDamage = new ForceDamage(target, finalDamage);
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
