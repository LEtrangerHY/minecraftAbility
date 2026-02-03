package org.core.coreSystem.cores.KEY.PLAYER.Skill;

import org.bukkit.*;
import org.bukkit.block.data.BlockData;
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
import org.core.coreSystem.cores.KEY.PLAYER.coreSystem.PLAYER;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class R implements SkillBase {

    private final PLAYER config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final NamespacedKey keyR;

    private static final Particle.DustOptions DUST_OPT = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.5f);

    public R(PLAYER config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.keyR = new NamespacedKey(plugin, "R");
    }

    @Override
    public void Trigger(Player player) {
        player.swingMainHand();

        Location startLocation = player.getLocation();
        Vector direction = startLocation.getDirection().normalize().multiply(config.r_Skill_dash);

        player.setVelocity(direction);
        player.getWorld().playSound(startLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        Invulnerable invulnerable = new Invulnerable(player, 300);
        invulnerable.applyEffect(player);

        detect(player);
    }

    public void detect(Player player) {
        config.rskill_using.put(player.getUniqueId(), true);
        config.damaged_1.put(player.getUniqueId(), new HashSet<>());

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyR, PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.GENERIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks > 6 || player.isDead()) {
                    config.rskill_using.remove(player.getUniqueId());
                    config.damaged_1.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                Location pLoc = player.getLocation();
                World world = player.getWorld();

                pLoc.add(0, 1, 0);
                world.spawnParticle(Particle.DUST, pLoc, 60, 0.3, 0, 0.3, 0.08, DUST_OPT);

                List<Entity> nearbyEntities = player.getNearbyEntities(1.2, 1.2, 1.2);
                Set<Entity> damagedSet = config.damaged_1.get(player.getUniqueId());

                if (damagedSet != null) {
                    for (Entity entity : nearbyEntities) {
                        if (entity instanceof LivingEntity target && entity != player && !damagedSet.contains(entity)) {
                            ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                            forceDamage.applyEffect(player);

                            damagedSet.add(target);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}