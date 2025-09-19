package org.core.coreProgram.Cores.Carpenter.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.core.Effect.Stun;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.Carpenter.coreSystem.Carpenter;

import java.util.*;
import java.util.List;

public class R implements SkillBase {

    private final Carpenter config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R(Carpenter config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {

        Location startLocation = player.getLocation();

        Vector direction = startLocation.getDirection().normalize().multiply(config.r_Skill_dash);

        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.0f);

        Invulnerable invulnerable = new Invulnerable(player,500);
        invulnerable.applyEffect(player);

        detect_1(player);
    }

    public void detect_1(Player player){
        config.r_damaged.put(player.getUniqueId(), new HashSet<>());

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_damage * (1 + amp);

        new BukkitRunnable() {
            private double ticks = 0;

            @Override
            public void run() {

                if (ticks > 5 || player.isDead()) {
                    config.r_damaged.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                double velocity_x = player.getVelocity().getX();
                double velocity_y = player.getVelocity().getY();
                double velocity_z = player.getVelocity().getZ();

                double velocity = Math.sqrt(Math.pow(velocity_x, 2) + Math.pow(velocity_y, 2) + Math.pow(velocity_z, 2));

                player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1.0, 0), 20, 0.3, 0.2, 0.3, 0);

                List<Entity> nearbyEntities = player.getNearbyEntities(0.5, 0.5, 0.5);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player && !config.r_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                        player.swingMainHand();

                        Stun stun = new Stun(entity, (long) (config.r_Stun * velocity));
                        stun.applyEffect(player);

                        config.r_damaging.put(player.getUniqueId(), true);
                        ForceDamage forceDamage = new ForceDamage(target, damage * velocity);
                        forceDamage.applyEffect(player);
                        target.setVelocity(new Vector(0, 0, 0));
                        config.r_damaging.remove(player.getUniqueId());

                        player.setVelocity(new Vector(0, 0, 0));

                        if(config.r_Skill_damage * velocity >= config.r_Skill_damage){
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
                            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 0), 1.0f);
                            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 25, 0.4, 0.3, 0.4, 0.08, dustOptions);
                            String send = String.format("%.2f", damage * velocity);
                            player.sendActionBar(Component.text(send).color(NamedTextColor.YELLOW));
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);
                        }else {
                            String send = String.format("%.2f", damage * velocity);
                            player.sendActionBar(Component.text(send).color(NamedTextColor.WHITE));
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 1.0f, 1.0f);
                        }

                        if(config.r_Skill_damage * velocity > 10){
                            player.spawnParticle(Particle.CRIT, player.getLocation().add(0, 1.0, 0), 50, 0.3, 0.3, 0.3, 0);
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                        }

                        config.r_damaged.remove(player.getUniqueId());
                        this.cancel();
                        break;
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

}
