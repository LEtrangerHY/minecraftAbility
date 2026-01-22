package org.core.coreSystem.cores.VOL1.Swordsman.Skill;

import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Swordsman.Passive.Laido;
import org.core.coreSystem.cores.VOL1.Swordsman.coreSystem.Swordsman;

import java.util.*;

public class Q implements SkillBase {
    private final Swordsman config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Laido laido;
    private final NamespacedKey keyQ;

    private static final Particle.DustOptions DUST_GRAY = new Particle.DustOptions(Color.fromRGB(127, 127, 127), 0.5f);
    private static final Particle.DustOptions DUST_WHITE = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.5f);
    private static final Particle.DustOptions DUST_BLACK = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 0.6f);

    public Q(Swordsman config, JavaPlugin plugin, Cool cool, Laido laido) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.laido = laido;
        this.keyQ = new NamespacedKey(plugin, "Q");
    }

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();
        int slashTimes = config.laidoSlash.getOrDefault(uuid, false) ? 2 : 1;
        long maxTicks = (slashTimes > 1) ? 3 : 7;

        config.q_skillUsing.put(uuid, true);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= slashTimes || player.isDead()) {
                    config.q_skillUsing.remove(uuid);
                    config.q_damaged.remove(uuid);
                    this.cancel();
                    return;
                }

                double height = -0.8 + tick * 0.4;
                tick++;
                Slash(player, slashTimes, tick, height);
            }
        }.runTaskTimer(plugin, 0L, maxTicks);
    }

    public void Slash(Player player, int slashTimes, int slashCount, double height) {
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();
        boolean duelSlash = (slashTimes > 1);

        if (duelSlash && slashCount == 1) laido.Draw(player);
        player.swingMainHand();
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

        double slashLength = 4.4;
        double maxAngleRad = (duelSlash) ? Math.toRadians(45) : Math.toRadians(100);
        if (slashCount <= 1) maxAngleRad = -maxAngleRad;

        double angleIncrease = (slashCount <= 1) ? -Math.toRadians(2) : Math.toRadians(2);
        double maxTicks = (duelSlash) ? 3 : 7;
        double innerRadius = 2.4;

        double absMaxAngle = Math.abs(maxAngleRad);
        double hitThreshold = Math.cos(absMaxAngle + 0.25);

        HashSet<Entity> damagedSet = new HashSet<>();
        config.q_damaged.put(uuid, damagedSet);

        double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyQ, PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Location origin = player.getEyeLocation().add(0, height, 0);
        Vector direction = player.getLocation().getDirection().setY(0).normalize();

        Particle.DustOptions dustOption;
        if (duelSlash) {
            dustOption = (slashCount == 2) ? DUST_WHITE : DUST_BLACK;
        } else {
            dustOption = DUST_GRAY;
        }

        double finalMaxAngle = maxAngleRad;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= maxTicks || player.isDead()) {
                    this.cancel();
                    return;
                }

                double progress = (ticks + 1) * (finalMaxAngle * 2 / maxTicks) - finalMaxAngle;
                Vector rotatedDir = direction.clone().rotateAroundY(progress);

                for (Entity e : world.getNearbyEntities(origin, slashLength + 0.5, slashLength + 0.5, slashLength + 0.5)) {
                    if (e instanceof LivingEntity target && e != player) {
                        if (!damagedSet.contains(target)) {

                            Vector toEntity = target.getLocation().toVector().subtract(origin.toVector());

                            if (toEntity.lengthSquared() <= (slashLength + 0.5) * (slashLength + 0.5)) {

                                Vector toEntityDir = toEntity.clone().setY(0).normalize(); // 평면 각도 계산을 위해 Y=0
                                double dotProduct = rotatedDir.dot(toEntityDir);

                                if (dotProduct >= hitThreshold) {
                                    ForceDamage forceDamage = new ForceDamage(target, damage, source);
                                    forceDamage.applyEffect(player);
                                    target.setVelocity(new Vector(0, 0, 0));

                                    int currentAbsorption = player.getAbsorptionAmount() > 0 ? (int) player.getAbsorptionAmount() : 0;
                                    int newAbsorption = Math.min(currentAbsorption + 1, 8);
                                    int amplifier = (newAbsorption / 2) - 1;
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, Integer.MAX_VALUE, amplifier, false, false, false));

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

                    for (double angle = -finalMaxAngle; (slashCount == 2) ? angle <= finalMaxAngle : angle >= finalMaxAngle; angle += angleIncrease) {
                        Vector angleDir = rotatedDir.clone().rotateAroundY(angle);

                        double valX = angleDir.getX() * length;
                        double valY = angleDir.getY() * length;
                        double valZ = angleDir.getZ() * length;

                        particleLocation.setX(originX + valX);
                        particleLocation.setY(originY + valY);
                        particleLocation.setZ(originZ + valZ);

                        world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOption);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}