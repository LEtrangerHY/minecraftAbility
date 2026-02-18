package org.core.coreSystem.cores.VOL3.Residue.Skill;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
import org.core.coreSystem.cores.VOL3.Residue.coreSystem.Residue;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;

import java.util.HashSet;
import java.util.UUID;

public class F implements SkillBase {
    private final Residue config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final NamespacedKey keyF;

    private static final Particle.DustOptions DUST_SLASH = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.5f);
    private static final Particle.DustOptions DUST_SLASH_GRA = new Particle.DustOptions(Color.fromRGB(111, 111, 111), 0.5f);

    private static final Particle.DustOptions DUST_ASH = new Particle.DustOptions(Color.fromRGB(110, 110, 110), 0.8f);
    private static final BlockData CHAIN_DATA = Material.IRON_CHAIN.createBlockData();

    public F(Residue config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.keyF = new NamespacedKey(plugin, "F");
    }

    @Override
    public void Trigger(Player player) {
        Location startLocation = player.getLocation();
        Vector direction = startLocation.getDirection().normalize().multiply(config.f_Skill_dash_b);

        player.setVelocity(direction);
        player.getWorld().playSound(startLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        Slash(player);

        Invulnerable invulnerable = new Invulnerable(player, 200);
        invulnerable.applyEffect(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Dash(player);
        }, 12L);
    }


    public void Slash(Player player) {
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

        double slashLength = 4.8;
        double maxAngleRad = Math.toRadians(90);
        maxAngleRad = -maxAngleRad;

        double angleIncrease = -Math.toRadians(2);
        double maxTicks = 6;
        double innerRadius = 2.6;

        double absMaxAngle = Math.abs(maxAngleRad);
        double hitThreshold = Math.cos(absMaxAngle + 0.25);

        HashSet<Entity> damagedSet = new HashSet<>();
        config.damaged.put(uuid, damagedSet);
        config.f_Skill_dash_f.put(uuid, 0);

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyF, PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_Damage * (1 + amp);
        double heal = config.f_Skill_Heal * (1 + amp);

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
                if (ticks < 4)
                    world.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.6f, 1.0f);

                if (ticks >= maxTicks || player.isDead()) {

                    int hitCount = damagedSet.size();
                    int dash = config.f_Skill_dash_f.getOrDefault(player.getUniqueId(), 0);

                    if (dash < 6) config.f_Skill_dash_f.put(player.getUniqueId(), dash + hitCount);

                    player.setFoodLevel(Math.min(20, player.getFoodLevel() + hitCount * 3));

                    if (hitCount > 0 && !player.isDead()) {
                        double healAmount = Math.min(6.0, hitCount * heal);

                        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
                        if (maxHealthAttr != null) {
                            double maxHealth = maxHealthAttr.getValue();
                            player.setHealth(Math.min(maxHealth, player.getHealth() + healAmount));
                        }
                    }

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

                        Particle.DustOptions opt = (Math.random() < 0.11) ? DUST_SLASH : DUST_SLASH_GRA;
                        world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, opt);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void Dash(Player player) {
        World world = player.getWorld();

        int dash = config.f_Skill_dash_f.getOrDefault(player.getUniqueId(), 0);

        long qCooldown = cool.getRemainCooldown(player, "Q");
        cool.updateCooldown(player, "Q", Math.max(qCooldown - dash * 1000L, 0L));

        if (dash > 1) {
            cool.updateCooldown(player, "F", Math.min(1000L * dash, 3000L));

            Location startLocation = player.getLocation();
            Vector direction = startLocation.getDirection().normalize().multiply(dash * 0.6);

            player.setVelocity(new Vector(0, 0, 0));
            player.setVelocity(direction);

            world.playSound(startLocation, Sound.ITEM_SPEAR_LUNGE_3, 1.6f, 1.0f);
            world.playSound(startLocation, Sound.ITEM_TRIDENT_THROW, 1.6f, 1.0f);

            Invulnerable invulnerable = new Invulnerable(player, dash * 100L);
            invulnerable.applyEffect(player);

            detect(player, dash);
        } else {
            config.f_Skill_dash_f.remove(player.getUniqueId());
        }
    }

    public void detect(Player player, int dash) {
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        config.fskill_using.put(uuid, true);

        Vector backwardDir = player.getLocation().getDirection().multiply(-0.3);

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks > dash || player.isDead()) {
                    config.fskill_using.remove(uuid);
                    cancel();
                    return;
                }

                Location pLoc = player.getLocation().add(0, 1, 0);
                Location trailLoc = pLoc.clone().add(backwardDir);

                world.spawnParticle(Particle.DUST, pLoc, 20, 0.2, 0.2, 0.2, 0.05, DUST_SLASH);

                world.spawnParticle(Particle.LARGE_SMOKE, trailLoc, 5 + (dash * 2), 0.3, 0.3, 0.3, 0.08);
                world.spawnParticle(Particle.DUST, trailLoc, 10, 0.4, 0.4, 0.4, 0.05, DUST_ASH);

                int chainCount = 12 + (dash * 6);
                world.spawnParticle(Particle.BLOCK, trailLoc, chainCount, 0.5, 0.5, 0.5, CHAIN_DATA);

                world.spawnParticle(Particle.SWEEP_ATTACK, pLoc, 1, 0, 0, 0, 0);

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}