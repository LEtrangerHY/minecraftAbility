package org.core.coreSystem.cores.VOL2.Rose.Skill;

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
import org.core.coreSystem.cores.VOL2.Rose.Passive.bloodPetal;
import org.core.coreSystem.cores.VOL2.Rose.coreSystem.Rose;
import org.core.effect.crowdControl.ForceDamage;

public class F implements SkillBase {

    private final Rose config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final bloodPetal petal;
    private final NamespacedKey keyF;

    private final BlockData blood = Material.REDSTONE_BLOCK.createBlockData();
    private static final Particle.DustOptions DUST_BLOOD = new Particle.DustOptions(Color.fromRGB(160, 10, 10), 0.5f);
    private static final Particle.DustOptions DUST_DARK = new Particle.DustOptions(Color.fromRGB(60, 0, 0), 0.4f);

    public F(Rose config, JavaPlugin plugin, Cool cool, bloodPetal petal) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.petal = petal;
        this.keyF = new NamespacedKey(plugin, "F");
    }

    @Override
    public void Trigger(Player player) {
        World world = player.getWorld();

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyF, PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            private double ticks = 0;

            @Override
            public void run() {
                if (ticks > 7 || player.isDead()) {
                    cancel();
                    return;
                }

                world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
                world.spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().clone().add(0, 1.0, 0), 7, 2, 2, 2, 1);
                world.spawnParticle(Particle.DUST, player.getLocation().clone().add(0, 1.0, 0), 44, 2, 2, 2, 0, DUST_BLOOD);
                world.spawnParticle(Particle.DUST, player.getLocation().clone().add(0, 1.0, 0), 22, 2, 2, 2, 0, DUST_DARK);

                petal.dropPetal(player, player, damage);

                for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                    if (entity instanceof LivingEntity target && entity != player) {
                        world.spawnParticle(Particle.CRIT, target.getLocation().clone().add(0, 1.0, 0), 4, 0.5, 0.5, 0.5, 1);
                        world.spawnParticle(Particle.BLOCK, entity.getLocation().clone().add(0, 1.2, 0), 4, 0.3, 0.3, 0.3, blood);
                        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);

                        config.atk.put(player.getUniqueId(), "S");
                        ForceDamage forceDamage = new ForceDamage(target, damage, source);
                        forceDamage.applyEffect(player);
                        target.setVelocity(new Vector(0, 0, 0));
                        config.atk.remove(player.getUniqueId());

                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }
}