package org.core.coreSystem.cores.VOL3.Cheshire.Skill;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
import org.core.coreSystem.cores.VOL3.Cheshire.coreSystem.Cheshire;
import org.core.effect.crowdControl.ForceDamage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Q implements SkillBase {

    private final Cheshire config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private static final Particle.DustOptions DUST_BLOOD = new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.8f);

    public Q(Cheshire config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();

        NamespacedKey qKey = new NamespacedKey(plugin, "Q");
        double qAmp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(qKey, PersistentDataType.LONG, 0L);
        double scaledDamage = config.q_Skill_damage * (1.0 + qAmp);

        NamespacedKey levelKey = new NamespacedKey(plugin, "level");
        long playerLevel = player.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.LONG, 0L);
        if (playerLevel < 0) playerLevel = 0;
        if (playerLevel > 10) playerLevel = 10;
        double levelBonus = 0.005 * playerLevel * playerLevel + 0.055 * playerLevel;
        double levelMult = 1.0 + levelBonus;

        final double maxHeal = 6.0 * (1.0 + qAmp) * levelMult;
        final double[] currentHeal = {0.0};

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        DamageSource magicSource = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        List<Location> allSmiles = new ArrayList<>();
        if (config.smileLocations.containsKey(uuid)) allSmiles.add(config.smileLocations.get(uuid));
        if (config.activeSmiles.containsKey(uuid)) allSmiles.addAll(config.activeSmiles.get(uuid));

        boolean isEnhanced = !allSmiles.isEmpty();
        boolean hitAny = false;

        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();
        LivingEntity mainTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : player.getNearbyEntities(4.0, 4.0, 4.0)) {
            if (entity instanceof LivingEntity target && target != player) {
                Vector toTarget = target.getLocation().toVector().subtract(eyeLoc.toVector());
                if (toTarget.lengthSquared() > 0.01 && dir.dot(toTarget.normalize()) > 0.6) {
                    double dist = eyeLoc.distance(target.getEyeLocation());
                    if (dist < closestDist) {
                        closestDist = dist;
                        mainTarget = target;
                    }
                }
            }
        }

        Location biteLoc;

        if (mainTarget != null) {
            hitAny = true;

            new ForceDamage(mainTarget, scaledDamage, source, true).applyEffect(player);

            double expectedDamageDealt = scaledDamage * levelMult;
            double actualHeal = Math.min(expectedDamageDealt * 0.5, maxHeal - currentHeal[0]);
            if (actualHeal > 0) {
                applyLifesteal(player, actualHeal);
                currentHeal[0] += actualHeal;
            }

            mainTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 3, false, true));

            biteLoc = mainTarget.getLocation().add(0.0, 1.2, 0.0);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_BITE, 1.5f, 1.8f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.8f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FOX_BITE, 1.2f, 1.2f);

            final LivingEntity finalTarget = mainTarget;
            new BukkitRunnable() {
                int count = 0;
                @Override
                public void run() {
                    if (count >= 4 || finalTarget.isDead()) {
                        this.cancel();
                        return;
                    }
                    new ForceDamage(finalTarget, 1.0, magicSource, true).applyEffect(player);
                    finalTarget.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, finalTarget.getLocation().add(0.0, 1.0, 0.0), 5, 0.2, 0.2, 0.2, 0.1);
                    count++;
                }
            }.runTaskTimer(plugin, 20L, 20L);
        } else {
            biteLoc = eyeLoc.clone().add(dir.clone().multiply(2.5));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_BITE, 1.5f, 1.8f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        }

        drawAnimatedBite(biteLoc, dir);

        if (isEnhanced) {
            for (Location smileLoc : allSmiles) {
                smileLoc.getWorld().playSound(smileLoc, Sound.ENTITY_PHANTOM_BITE, 1.5f, 1.8f);
                smileLoc.getWorld().playSound(smileLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.8f);

                Vector smileDir = config.smileDirections.getOrDefault(uuid, new Vector(1.0, 0.0, 0.0));
                drawAnimatedBite(smileLoc.clone().add(0.0, 1.0, 0.0), smileDir);

                for (Entity entity : smileLoc.getWorld().getNearbyEntities(smileLoc, 3.5, 3.5, 3.5)) {
                    if (entity instanceof LivingEntity target && target != player) {
                        double magicDamage = target.getHealth() * 0.13;

                        new ForceDamage(target, scaledDamage * ((double) 2 /3), source, true).applyEffect(player);
                        new ForceDamage(target, magicDamage, magicSource, true).applyEffect(player);

                        double expectedAoeScaledDamage = (scaledDamage * ((double) 2 / 3)) * levelMult;
                        double expectedAoeMagicDamage = magicDamage * levelMult;

                        double aoeTotalDamage = expectedAoeScaledDamage + expectedAoeMagicDamage;
                        double aoeActualHeal = Math.min(aoeTotalDamage, maxHeal - currentHeal[0]);

                        if (aoeActualHeal > 0) {
                            applyLifesteal(player, aoeActualHeal);
                            currentHeal[0] += aoeActualHeal;
                        }

                        hitAny = true;

                        Location tLoc = target.getLocation();
                        tLoc.getWorld().playSound(tLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 1.5f);
                        for(double y = 0.0; y <= 2.5; y += 0.3) {
                            tLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, tLoc.clone().add(0.0, y, 0.0), 2, 0.1, 0.1, 0.1, 0.05);
                        }
                    }
                }
            }

            if (config.smileTasks.containsKey(uuid)) {
                org.bukkit.Bukkit.getScheduler().cancelTask(config.smileTasks.get(uuid));
                config.smileTasks.remove(uuid);
                config.smileLocations.remove(uuid);
            }
            if (config.activeSmileTasks.containsKey(uuid)) {
                for (int taskId : config.activeSmileTasks.get(uuid)) {
                    org.bukkit.Bukkit.getScheduler().cancelTask(taskId);
                }
                config.activeSmileTasks.remove(uuid);
                config.activeSmiles.remove(uuid);
            }
            config.fUltSmileCounts.put(uuid, 0);

            if (hitAny) {
                long remaining = cool.getRemainCooldown(player, "PASSIVE");
                if (remaining > 0) {
                    long reducedCooldown = remaining * 2 / 3;
                    cool.updateCooldown(player, "PASSIVE", reducedCooldown, "boss");
                }
            }
        }
    }

    private void drawAnimatedBite(Location center, Vector direction) {
        Vector right = new Vector(-direction.getZ(), 0.0, direction.getX());
        if (right.lengthSquared() < 0.001) right = new Vector(1.0, 0.0, 0.0);
        else right.normalize();

        Vector up = new Vector(0.0, 1.0, 0.0);

        Vector finalRight = right;
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 3) {
                    center.getWorld().spawnParticle(Particle.BLOCK, center, 30, 0.5, 0.5, 0.5, 0.0, org.bukkit.Material.REDSTONE_BLOCK.createBlockData());
                    this.cancel();
                    return;
                }

                double progress = tick / 3.0;
                double gap = (1.0 - progress) * 1.5;

                double width = 1.5;

                for (double t = -1.0; t <= 1.0; t += 0.05) {
                    double jawCurve = (t * t) * 0.4;

                    double localT;
                    if (t < -0.33) localT = t + 0.66;
                    else if (t > 0.33) localT = t - 0.66;
                    else localT = t;

                    double tooth = (0.33 - Math.abs(localT)) * 2.5;

                    double topY = gap + 0.4 + jawCurve - tooth;
                    double bottomY = -gap - 0.4 - jawCurve + tooth;

                    Vector topTooth = finalRight.clone().multiply(t * width).add(up.clone().multiply(topY));
                    Vector bottomTooth = finalRight.clone().multiply(t * width).add(up.clone().multiply(bottomY));

                    center.getWorld().spawnParticle(Particle.DUST, center.clone().add(topTooth), 1, 0.0, 0.0, 0.0, 0.0, DUST_BLOOD);
                    center.getWorld().spawnParticle(Particle.DUST, center.clone().add(bottomTooth), 1, 0.0, 0.0, 0.0, 0.0, DUST_BLOOD);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void applyLifesteal(Player player, double healAmount) {
        if (player.isDead() || !player.isValid()) return;

        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double maxHealth = maxHealthAttr.getValue();
            player.setHealth(Math.min(maxHealth, player.getHealth() + healAmount));
        }

        int foodToRestore = (int) Math.ceil(healAmount);
        if (foodToRestore > 0 && player.getFoodLevel() < 20) {
            player.setFoodLevel(Math.min(20, player.getFoodLevel() + foodToRestore));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.4f, 1.4f);
        }
    }
}