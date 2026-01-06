package org.core.coreSystem.cores.VOL2.Burst.Skill;

import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.core.coreSystem.cores.VOL2.Burst.coreSystem.Burst;

public class F implements SkillBase {
    private final Burst config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Burst config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){
        World world = player.getWorld();
        Location playerLoc = player.getLocation().clone();

        world.spawnParticle(Particle.EXPLOSION, playerLoc.add(0, 1, 0), 4, 0.3, 0.3, 0.3, 1);
        world.playSound(playerLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_Damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_EXPLOSION)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        for (Entity entity : world.getNearbyEntities(playerLoc, 3, 3, 3)) {
            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

            world.spawnParticle(Particle.EXPLOSION, entity.getLocation().clone().add(0, 1, 0), 1, 0, 0, 0, 0);

            ForceDamage forceDamage = new ForceDamage((LivingEntity) entity, damage / 2, source);
            forceDamage.applyEffect(player);

            Vector direction = entity.getLocation().toVector().subtract(playerLoc.toVector()).normalize().multiply(1.0);
            direction.setY(0.6);

            entity.setVelocity(direction);
        }

        Vector upward = new Vector(0, config.f_Skill_Jump, 0);

        player.setVelocity(upward);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setVelocity(new Vector(0, 0, 0));
            SaturationBomb(player);
        }, 6L);
    }

    public void SaturationBomb(Player player){
        World world = player.getWorld();

        new BukkitRunnable() {
            private double ticks = 0;

            @Override
            public void run() {

                if (ticks >= 18 || player.isDead()) {

                    Invulnerable invulnerable = new Invulnerable(player, 2000);
                    invulnerable.applyEffect(player);

                    cancel();
                    return;
                }

                Location currentLocation = player.getLocation();
                Vector direction = currentLocation.getDirection().normalize().multiply(config.f_Skill_dash);
                player.setVelocity(direction);

                Vector rightVector = currentLocation.getDirection().crossProduct(new Vector(0, 1, 0)).normalize().multiply(2.0);

                Location leftLoc = currentLocation.clone().subtract(rightVector);
                Location rightLoc = currentLocation.clone().add(rightVector);

                spawnTNT(leftLoc);
                spawnTNT(rightLoc);

                ticks += 3;
            }

            private void spawnTNT(Location loc) {
                world.playSound(loc, Sound.ENTITY_TNT_PRIMED, 2.0f, 1.0f);

                TNTPrimed tnt = (TNTPrimed) world.spawnEntity(loc, EntityType.TNT);
                tnt.setFuseTicks(40);
                tnt.setYield(4.0f);
                tnt.setIsIncendiary(true);
            }

        }.runTaskTimer(plugin, 0, 3);
    }
}
