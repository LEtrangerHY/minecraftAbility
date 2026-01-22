package org.core.coreSystem.cores.VOL2.Rose.Skill;

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
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Rose.coreSystem.Rose;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Q implements SkillBase {

    private final Rose config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private static final Particle.DustOptions DUST_DARK = new Particle.DustOptions(Color.fromRGB(70, 10, 10), 0.5f);

    public Q(Rose config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();

        player.swingMainHand();

        Location startLocation = player.getLocation();
        Vector direction = startLocation.getDirection().normalize().multiply(config.q_Skill_dash);

        player.setVelocity(direction);
        player.getWorld().playSound(startLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        Invulnerable invulnerable = new Invulnerable(player, 700);
        invulnerable.applyEffect(player);

        detect(player);
    }

    public void detect(Player player) {
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        config.qskill_using.put(uuid, true);

        Set<Entity> damagedSet = new HashSet<>();
        config.damaged.put(uuid, damagedSet);

        double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks > 7 || player.isDead()) {
                    config.qskill_using.remove(uuid);
                    config.damaged.remove(uuid);
                    cancel();
                    return;
                }

                Location pLoc = player.getLocation().add(0, 1, 0);
                world.spawnParticle(Particle.DUST, pLoc, 77, 0.3, 0, 0.3, 0.08, DUST_DARK);

                List<Entity> nearbyEntities = player.getNearbyEntities(0.7, 0.7, 0.7);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player) {
                        if (!damagedSet.contains(target)) {

                            config.atk.put(player.getUniqueId(), "S");
                            ForceDamage forceDamage = new ForceDamage(target, damage, source);
                            forceDamage.applyEffect(player);
                            target.setVelocity(new Vector(0, 0, 0));
                            config.atk.remove(player.getUniqueId());
                            world.spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1.4, 0), 4, 0.3, 0.3, 0.3, Bukkit.createBlockData(Material.REDSTONE_BLOCK));

                            damagedSet.add(target);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
