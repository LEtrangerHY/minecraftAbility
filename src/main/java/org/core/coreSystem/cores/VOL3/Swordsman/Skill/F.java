package org.core.coreSystem.cores.VOL3.Swordsman.Skill;

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
import org.core.coreSystem.cores.VOL3.Swordsman.Passive.Iaido;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;
import org.core.effect.crowdControl.Stiff;
import org.core.effect.crowdControl.Stun;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL3.Swordsman.coreSystem.Swordsman;

import java.util.HashSet;
import java.util.UUID;

public class F implements SkillBase {
    private final Swordsman config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Iaido laido;
    private final NamespacedKey keyF;

    private static final Particle.DustOptions DUST_DASH = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.6f);
    private static final Particle.DustOptions DUST_SLASH = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 0.6f);

    public F(Swordsman config, JavaPlugin plugin, Cool cool, Iaido laido) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.laido = laido;
        this.keyF = new NamespacedKey(plugin, "F");
    }

    @Override
    public void Trigger(Player player) {
        World world = player.getWorld();
        player.swingMainHand();

        if (!config.laidoSlash.getOrDefault(player.getUniqueId(), false)) {
            world.spawnParticle(Particle.SPIT, player.getLocation().add(0, 1.0, 0), 20, 0.2, 0.3, 0.2, 0.5);
            world.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.4f);

            laido.Sheath(player);

            cool.updateCooldown(player, "R", 0L);
            config.r_Skill_count.remove(player.getUniqueId());
            cool.updateCooldown(player, "Q", 0L);

        } else {
            Slash(player);
            Dash(player);
            laido.Draw(player);
        }
    }

    public void Dash(Player player) {
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        Location startLocation = player.getLocation();
        Vector direction = startLocation.getDirection().normalize().multiply(config.f_Skill_dash);

        player.setVelocity(direction);

        Invulnerable invulnerable = new Invulnerable(player, 200);
        invulnerable.applyEffect(player);

        config.f_skillUsing.put(uuid, true);

        world.spawnParticle(Particle.SPIT, player.getLocation().add(0, 1.0, 0), 20, 0.2, 0.3, 0.2, 0.5);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 4 || player.isDead()) {
                    Stiff stiff = new Stiff(player, 1000L);
                    stiff.applyEffect(player);

                    Invulnerable invulnerable = new Invulnerable(player, 2000);
                    invulnerable.applyEffect(player);

                    laidoChainSlash(player);

                    cancel();
                    return;
                }

                world.spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 100, 0.3, 0, 0.3, 0.08, DUST_DASH);
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1L);
    }

    public void Slash(Player player) {
        player.swingMainHand();
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);

        double slashLength = 3.3;
        double maxAngle = Math.toRadians(45);
        int maxTicks = 4;
        double innerRadius = 1.1;

        double hitThreshold = Math.cos(maxAngle + 0.1);

        HashSet<Entity> damagedSet = new HashSet<>();
        config.f_damaged.put(uuid, damagedSet);

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyF, PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Location origin = player.getEyeLocation().add(0, -0.6, 0);
        Vector direction = player.getLocation().getDirection().setY(0).normalize();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= maxTicks || player.isDead()) {
                    config.f_damaged.remove(uuid);
                    this.cancel();
                    return;
                }

                double progress = (ticks + 1) * (maxAngle * 2 / maxTicks) - maxAngle;
                Vector rotatedDir = direction.clone().rotateAroundY(progress);

                for (Entity e : world.getNearbyEntities(origin, slashLength + 0.5, slashLength + 0.5, slashLength + 0.5)) {
                    if (e instanceof LivingEntity target && e != player) {
                        if (!damagedSet.contains(target)) {

                            Vector toEntity = target.getLocation().toVector().subtract(origin.toVector());
                            double distSq = toEntity.lengthSquared();

                            if (distSq <= (slashLength + 0.5) * (slashLength + 0.5)) {

                                Vector toEntityDir = toEntity.clone().setY(0).normalize();
                                double dotProduct = rotatedDir.dot(toEntityDir);

                                if (dotProduct >= hitThreshold) {
                                    Stun stun = new Stun(target, config.f_Skill_stun);
                                    stun.applyEffect(player);

                                    ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                                    forceDamage.applyEffect(player);

                                    damagedSet.add(target);
                                }
                            }
                        }
                    }
                }

                Location particleLocation = origin.clone();
                double originX = origin.getX();
                double originY = origin.getY();
                double originZ = origin.getZ();

                for (double length = 0; length <= slashLength; length += 0.1) {
                    if (length < innerRadius) continue;

                    for (double angle = -maxAngle; angle <= maxAngle; angle += Math.toRadians(2)) {
                        Vector angleDir = rotatedDir.clone().rotateAroundY(angle);

                        double valX = angleDir.getX() * length;
                        double valY = angleDir.getY() * length;
                        double valZ = angleDir.getZ() * length;

                        particleLocation.setX(originX + valX);
                        particleLocation.setY(originY + valY);
                        particleLocation.setZ(originZ + valZ);

                        world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, DUST_SLASH);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void laidoChainSlash(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World world = player.getWorld();
            UUID uuid = player.getUniqueId();

            double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyF, PersistentDataType.LONG, 0L);
            double damage = (config.f_Skill_damage / 3) * (1 + amp);

            DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                    .withCausingEntity(player)
                    .withDirectEntity(player)
                    .build();

            laido.Sheath(player);

            new BukkitRunnable() {
                private double ticks = 0;

                @Override
                public void run() {
                    if (ticks > 7 || player.isDead()) {
                        config.f_skillUsing.remove(uuid);
                        cancel();
                        return;
                    }

                    world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
                    world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.0f);
                    world.spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1.0, 0), 20, 2, 2, 2, 1);

                    for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                        if (entity instanceof LivingEntity target && entity != player) {
                            world.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.0, 0), 20, 0.5, 0.5, 0.5, 1);

                            ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                            forceDamage.applyEffect(player);
                        }
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, 0, 2);
        }, 5L);
    }
}