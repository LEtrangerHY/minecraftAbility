package org.core.coreSystem.cores.VOL3.Darmes.Skill;

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
import org.core.coreSystem.cores.VOL3.Darmes.coreSystem.Darmes;
import org.core.effect.crowdControl.ForceDamage;

import java.util.HashSet;
import java.util.UUID;

public class Q implements SkillBase {

    private final Darmes config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final NamespacedKey keyQ;

    public Q(Darmes config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.keyQ = new NamespacedKey(plugin, "Q");
    }

    private static final Particle.DustOptions DUST_SLASH = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.5f);
    private static final Particle.DustOptions DUST_SLASH_GRA = new Particle.DustOptions(Color.fromRGB(111, 111, 111), 0.5f);

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();

        if(config.q_reuse.getOrDefault(uuid, false)) {
            SlamDown(player);
            config.q_reuse.remove(uuid);
        } else {
            cool.updateCooldown(player, "Q", 300L);
            Sweep(player);
        }
    }

    public void Sweep(Player player) {
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "q_initial_y"), PersistentDataType.DOUBLE, player.getLocation().getY());

        world.playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1, 1);

        double slashLength = 4.8;
        double maxAngleRad = Math.toRadians(90);
        double maxTicks = 6;
        double innerRadius = 2.6;

        double hitThreshold = Math.cos(maxAngleRad + 0.25);

        HashSet<Entity> damagedSet = new HashSet<>();
        config.q_damaged.put(uuid, damagedSet);

        double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyQ, PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_Damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Location origin = player.getEyeLocation().add(0, -0.6, 0);

        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        double dirX = direction.getX();
        double dirZ = direction.getZ();

        double originX = origin.getX();
        double originY = origin.getY();
        double originZ = origin.getZ();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks < 4)
                    world.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.6f, 1.0f);

                if (ticks >= maxTicks || player.isDead()) {
                    Jump(player);
                    this.cancel();
                    return;
                }

                for (Entity e : world.getNearbyEntities(origin, slashLength + 0.5, slashLength + 0.5, slashLength + 0.5)) {
                    if (!(e instanceof LivingEntity target) || e == player || damagedSet.contains(target)) continue;

                    Vector toTarget = target.getLocation().toVector().subtract(origin.toVector());
                    toTarget.setY(0);

                    if (toTarget.lengthSquared() <= (slashLength + 0.5) * (slashLength + 0.5)) {
                        Vector toTargetDir = toTarget.normalize();
                        double dotProduct = direction.dot(toTargetDir);

                        if (dotProduct >= hitThreshold) {
                            double jumpUpdate = config.q_Skill_Jump.getOrDefault(uuid, 1.6) + config.q_Skill_Jump_Int;
                            jumpUpdate = Math.min(6.6, jumpUpdate);
                            config.q_Skill_Jump.put(uuid, jumpUpdate);

                            ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                            forceDamage.applyEffect(player);
                            damagedSet.add(target);

                            world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);
                        }
                    }
                }

                double progress = (ticks + 1) * (maxAngleRad * 2 / maxTicks) - maxAngleRad;

                for (double length = innerRadius; length <= slashLength; length += 0.15) {
                    for (double angle = -maxAngleRad; angle <= maxAngleRad; angle += Math.toRadians(2.5)) {

                        double totalAngle = progress + angle;
                        double sinA = Math.sin(totalAngle);
                        double cosA = Math.cos(totalAngle);

                        double finalX = dirX * cosA - dirZ * sinA;
                        double finalZ = dirX * sinA + dirZ * cosA;

                        double pX = originX + finalX * length;
                        double pZ = originZ + finalZ * length;

                        Particle.DustOptions opt = (Math.random() < 0.11) ? DUST_SLASH : DUST_SLASH_GRA;
                        world.spawnParticle(Particle.DUST, pX, originY, pZ, 1, 0.3, 0.3, 0.3, 0, opt);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void Jump(Player player) {
        World world = player.getWorld();

        world.spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), 4, 0.3, 0.3, 0.3, 1);

        double jumpUpdate = config.q_Skill_Jump.getOrDefault(player.getUniqueId(), 1.6);
        Vector upward = new Vector(0, jumpUpdate, 0);

        world.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.0f);
        player.setVelocity(upward);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            config.q_reuse.put(player.getUniqueId(), true);
            player.getPersistentDataContainer().set(new NamespacedKey(plugin, "noFallDamage"), PersistentDataType.BOOLEAN, true);
        }, 3L);
    }

    public void SlamDown(Player player) {
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        NamespacedKey initialYKey = new NamespacedKey(plugin, "q_initial_y");
        double initialY = player.getPersistentDataContainer().getOrDefault(initialYKey, PersistentDataType.DOUBLE, player.getLocation().getY());
        player.getPersistentDataContainer().remove(initialYKey);

        double jumpedHeight = Math.max(0, player.getLocation().getY() - initialY);
        double ratio = Math.min(jumpedHeight / 26.0, 1.0);

        long finalCooldown = 12000L + (long) (54000L * Math.pow(ratio, 2));
        cool.updateCooldown(player, "Q", finalCooldown);

        double jumpUpdate = config.q_Skill_Jump.getOrDefault(uuid, 1.6);
        Vector downward = new Vector(0, -jumpUpdate * 1.6, 0);

        world.playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.0f);
        player.setVelocity(downward);

        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "q_slam_active"), PersistentDataType.BOOLEAN, true);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "q_slam_start_y"), PersistentDataType.DOUBLE, player.getLocation().getY());

        config.q_Skill_Jump.remove(uuid);
    }
}