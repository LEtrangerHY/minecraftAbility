package org.core.coreProgram.Cores.Harvester.Skill;

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
import org.core.coreProgram.Cores.Harvester.coreSystem.Harvester;

import java.util.HashSet;
import java.util.List;

public class F implements SkillBase {

    public final Harvester config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Harvester config, JavaPlugin plugin, Cool cool){
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        player.swingMainHand();

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);

        Invulnerable invulnerable = new Invulnerable(player, 2600);
        invulnerable.applyEffect(player);

        detect(player);
    }

    public void detect(Player player){

        World world = player.getWorld();

        config.fskill_using.put(player.getUniqueId(), true);

        config.damaged.put(player.getUniqueId(), new HashSet<>());

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_damage * (1 + amp);

        new BukkitRunnable() {
            private double ticks = 0;

            @Override
            public void run() {

                if (ticks > 13 || player.isDead()) {
                    config.fskill_using.remove(player.getUniqueId());

                    cancel();
                    return;
                }

                Location startLocation = player.getLocation();

                Vector direction = startLocation.getDirection().normalize().multiply(config.f_Skill_dash);

                player.setVelocity(direction);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.0f);

                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1.3, 0), 13, 1, 1, 1, 1);

                List<Entity> nearbyEntities = player.getNearbyEntities(4, 4, 4);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player) {

                        world.spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().clone().add(0, 1.2, 0), 4, 0.4, 0.4, 0.4, 1);
                        world.spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().clone().add(0, 1.2, 0), 13, 0.6, 0.6, 0.6, 1);

                        ForceDamage forceDamage = new ForceDamage(target, damage);
                        forceDamage.applyEffect(player);
                        target.setVelocity(direction);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }
}
