package org.core.coreSystem.cores.VOL3.Claud.Skill;

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
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL3.Claud.coreSystem.Claud;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Q implements SkillBase {
    private final Claud config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final NamespacedKey keyQ;

    private static final Particle.DustOptions DUST_DARK = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.5f);

    public Q(Claud config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.keyQ = new NamespacedKey(plugin, "Q");
    }

    @Override
    public void Trigger(Player player) {
        Location startLocation = player.getLocation();
        Vector direction = startLocation.getDirection().normalize().multiply(config.q_Skill_dash_b);

        player.setVelocity(direction);
        player.getWorld().playSound(startLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        Slash(player);

        Invulnerable invulnerable = new Invulnerable(player, 200);
        invulnerable.applyEffect(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setVelocity(new Vector(0, 0, 0));
            Dash(player);
        }, 12L);
    }


    public void Slash(Player player) {
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        player.swingMainHand();
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

        double slashLength = 5.0;
        double maxAngleRad = Math.toRadians(100);
        maxAngleRad = -maxAngleRad;

        double angleIncrease = -Math.toRadians(2);
        double maxTicks = 6;
        double innerRadius = 2.0;

        double absMaxAngle = Math.abs(maxAngleRad);
        double hitThreshold = Math.cos(absMaxAngle + 0.25);

        HashSet<Entity> damagedSet = new HashSet<>();
        config.damaged.put(uuid, damagedSet);
        config.q_Skill_dash_f.put(uuid, 0);

        double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyQ, PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_Damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Location origin = player.getEyeLocation().add(0, -0.6, 0);
        Vector direction = player.getLocation().getDirection().setY(0).normalize();

        double finalMaxAngle = maxAngleRad;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= maxTicks || player.isDead()) {

                    int dash = config.q_Skill_dash_f.getOrDefault(player.getUniqueId(), 0);
                    if (dash < 6) config.q_Skill_dash_f.put(player.getUniqueId(), dash + damagedSet.size());

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

                                Vector toEntityDir = toEntity.clone().setY(0).normalize();
                                double dotProduct = rotatedDir.dot(toEntityDir);

                                if (dotProduct >= hitThreshold) {
                                    ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                                    forceDamage.applyEffect(player);

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

                    for (double angle = -finalMaxAngle; angle >= finalMaxAngle; angle += angleIncrease) {
                        Vector angleDir = rotatedDir.clone().rotateAroundY(angle);

                        double valX = angleDir.getX() * length;
                        double valY = angleDir.getY() * length;
                        double valZ = angleDir.getZ() * length;

                        particleLocation.setX(originX + valX);
                        particleLocation.setY(originY + valY);
                        particleLocation.setZ(originZ + valZ);

                        world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, DUST_DARK);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void Dash(Player player){
        World world = player.getWorld();

        int dash = config.q_Skill_dash_f.getOrDefault(player.getUniqueId(), 0);
        cool.updateCooldown(player, "Q", 1000L * dash);

        if(dash > 1) {
            Location startLocation = player.getLocation();
            Vector direction = startLocation.getDirection().normalize().multiply(dash * 0.6);

            player.setVelocity(direction);

            world.playSound(startLocation, Sound.ITEM_SPEAR_LUNGE_3, 1.6f, 1.0f);
            world.playSound(startLocation, Sound.ITEM_TRIDENT_THROW, 1.6f, 1.0f);

            Invulnerable invulnerable = new Invulnerable(player, dash * 100L);
            invulnerable.applyEffect(player);

            detect(player, dash);
        }else{
            config.q_Skill_dash_f.remove(player.getUniqueId());
        }
    }

    public void detect(Player player, int dash) {
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        config.qskill_using.put(uuid, true);

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks > dash * 2 || player.isDead()) {
                    config.qskill_using.remove(uuid);
                    cancel();
                    return;
                }

                Location pLoc = player.getLocation().add(0, 1, 0);
                world.spawnParticle(Particle.DUST, pLoc, 66, 0.3, 0, 0.3, 0.08, DUST_DARK);

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}