package org.core.coreProgram.Cores.Benzene.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.Effect.Invulnerable;
import org.core.coreProgram.Cores.Benzene.Passive.ChainCalc;
import org.core.coreProgram.Cores.Benzene.coreSystem.Benzene;
import org.core.coreProgram.Abs.SkillBase;

import java.util.HashSet;
import java.util.List;

public class R implements SkillBase {

    private final Benzene config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final ChainCalc chainCalc;

    public R(Benzene config, JavaPlugin plugin, Cool cool, ChainCalc chainCalc) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.chainCalc = chainCalc;
    }

    @Override
    public void Trigger(Player player) {

        player.swingMainHand();

        Location startLocation = player.getLocation();

        Vector direction = startLocation.getDirection().normalize().multiply(config.r_Skill_dash);

        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        Invulnerable invulnerable = new Invulnerable(600);
        invulnerable.applyEffect(player);

        detect(player);

    }

    public void detect(Player player){

        config.rskill_using.put(player.getUniqueId(), true);
        config.damaged_2.put(player.getUniqueId(), new HashSet<>());

        config.atkCount.put(player.getUniqueId(), 0);
        player.sendActionBar(Component.text("Attack Count : " + config.atkCount.getOrDefault(player.getUniqueId(), 0)).color(NamedTextColor.GRAY));

        new BukkitRunnable() {
            private double ticks = 0;

            @Override
            public void run() {

                if(ticks < 4){
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.0f, 1.0f);
                    player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().clone().add(0, 1.2, 0), 44, 0.3, 0.3, 0.3,
                            Material.CHAIN.createBlockData());
                }

                if (ticks > 6 || player.isDead()) {
                    config.rskill_using.remove(player.getUniqueId());
                    config.damaged_2.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(111, 111, 111), 0.6f);
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 111, 0.3, 0, 0.3, 0.08, dustOptions);

                Particle.DustOptions dustOptions_small = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.6f);
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 66, 0.3, 0, 0.3, 0.08, dustOptions_small);

                List<Entity> nearbyEntities = player.getNearbyEntities(1.2, 1.2, 1.2);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player && !config.damaged_2.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {
                        ForceDamage forceDamage = new ForceDamage(target, config.r_Skill_damage);
                        forceDamage.applyEffect(player);
                        config.damaged_2.getOrDefault(player.getUniqueId(), new HashSet<>()).add(target);
                        target.setVelocity(new Vector(0, 0, 0));
                        if(!target.isDead()){
                            chainCalc.increase(player, target);
                            if(target.isDead()){
                                chainCalc.decrease(target);
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
