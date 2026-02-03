package org.core.coreSystem.cores.KEY.PLAYER.Skill;

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
import org.core.coreSystem.cores.KEY.PLAYER.coreSystem.PLAYER;
import org.core.effect.crowdControl.ForceDamage;

import java.util.HashSet;
import java.util.Set;

public class F implements SkillBase {

    private final PLAYER config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private static final Particle.DustOptions DUST_SLASH = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.5f);

    public F(PLAYER config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        player.swingMainHand();
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

        double slashLength = 4.8;
        double maxAngle = Math.toRadians(45);
        int maxTicks = 3;
        double innerRadius = 2.6;

        double hitThreshold = Math.cos(maxAngle + 0.2);

        config.damaged_2.put(player.getUniqueId(), new HashSet<>());

        double amp = config.f_Skill_Amp * player.getPersistentDataContainer()
                .getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_Damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Location eyeLoc = player.getEyeLocation();
        Location origin = eyeLoc.clone().add(0, -0.6, 0);

        Vector forward = player.getLocation().getDirection().normalize();
        Vector worldUp = new Vector(0, 1, 0);
        if (Math.abs(forward.dot(worldUp)) > 0.95) worldUp = new Vector(1, 0, 0);
        Vector right = forward.clone().crossProduct(worldUp).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (ticks > maxTicks || player.isDead()) {
                    config.damaged_2.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                double progressAngle = ((double) ticks / maxTicks) * (maxAngle * 2) - maxAngle;
                progressAngle = -progressAngle;

                double startAngle = maxAngle;
                double endAngle = -maxAngle;
                double step = -Math.toRadians(2);

                Set<Entity> damagedEntities = config.damaged_2.get(player.getUniqueId());

                Vector currentTickDirection = forward.clone().rotateAroundAxis(up, progressAngle);

                for (Entity entity : world.getNearbyEntities(origin, slashLength, slashLength, slashLength)) {
                    if (!(entity instanceof LivingEntity target) || entity == player || damagedEntities.contains(entity)) continue;

                    Vector toEntity = target.getLocation().add(0, target.getHeight() / 2.0, 0).toVector().subtract(origin.toVector());
                    double distSq = toEntity.lengthSquared();

                    if (distSq > (slashLength + 1.0) * (slashLength + 1.0)) continue;

                    Vector toEntityDir = toEntity.normalize();
                    double dotProduct = currentTickDirection.dot(toEntityDir);

                    if (dotProduct > hitThreshold) {
                        damagedEntities.add(entity);

                        ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                        forceDamage.applyEffect(player);

                        long currentLeft = cool.getRemainCooldown(player, "R");
                        long reducedTime = Math.max(0, currentLeft - 1000);

                        cool.updateCooldown(player, "R", reducedTime);
                    }
                }

                for (double angle = startAngle; angle >= endAngle; angle += step) {
                    double sinA = Math.sin(angle + progressAngle);
                    double cosA = Math.cos(angle + progressAngle);

                    double dirX = right.getX() * sinA + forward.getX() * cosA;
                    double dirY = right.getY() * sinA + forward.getY() * cosA;
                    double dirZ = right.getZ() * sinA + forward.getZ() * cosA;

                    for (double length = 0; length <= slashLength; length += 0.1) {
                        double x = origin.getX() + dirX * length;
                        double y = origin.getY() + dirY * length;
                        double z = origin.getZ() + dirZ * length;

                        if (length >= innerRadius) world.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, DUST_SLASH);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}