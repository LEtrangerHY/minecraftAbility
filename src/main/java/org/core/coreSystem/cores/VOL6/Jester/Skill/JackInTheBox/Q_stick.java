package org.core.coreSystem.cores.VOL6.Jester.Skill.JackInTheBox;

import org.core.effect.effectPlugin.swingEchoGuard;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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
import org.core.coreSystem.cores.VOL6.Jester.coreSystem.Jester;
import org.core.effect.crowdControl.ForceDamage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

public class Q_stick implements SkillBase {

    private final Jester config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Consumer<Player> onSkipStage;
    private final NamespacedKey keyQ;

    private final Map<UUID, Integer> comboCount = new HashMap<>();
    private final Map<UUID, Long> lastCastTime = new HashMap<>();

    public static final Map<UUID, Integer> stickChargeTicks = new HashMap<>();

    private static final Particle.DustOptions DUST_PURPLE = new Particle.DustOptions(Color.fromRGB(180, 0, 255), 0.8f);
    private static final Particle.DustOptions DUST_LIME = new Particle.DustOptions(Color.fromRGB(50, 255, 50), 0.8f);
    private static final Particle.DustOptions DUST_YELLOW = new Particle.DustOptions(Color.fromRGB(255, 220, 0), 0.8f);
    private static final Particle.DustOptions[] CLOWN_COLORS = {DUST_PURPLE, DUST_LIME, DUST_YELLOW};

    private static final BlockData BLOOD_DATA = Material.REDSTONE_BLOCK.createBlockData();
    private static final Particle.DustOptions CELL_RED = new Particle.DustOptions(Color.fromRGB(180, 20, 20), 1.2f);

    public Q_stick(Jester config, JavaPlugin plugin, Cool cool, Consumer<Player> onSkipStage) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.onSkipStage = onSkipStage;
        this.keyQ = new NamespacedKey(plugin, "Q");
    }

    @Override
    public void Trigger(Player player) {
        if (cool.isReloading(player, "Q_stick_internal")) return;

        UUID uuid = player.getUniqueId();
        int ticks = stickChargeTicks.getOrDefault(uuid, 0);
        stickChargeTicks.remove(uuid);

        executeAttack(player, ticks);
    }

    private void executeAttack(Player player, int ticks) {
        UUID uuid = player.getUniqueId();
        cool.setCooldown(player, 600L, "Q_stick_internal");

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCastTime.getOrDefault(uuid, 0L) > 6500L) {
            comboCount.put(uuid, 0);
        }
        lastCastTime.put(uuid, currentTime);

        int count = comboCount.getOrDefault(uuid, 0);
        comboCount.put(uuid, count + 1);

        if (ticks < 12) {
            Slam(player);
        } else {
            Sweep(player);
        }

        if (count + 1 >= 6) {
            comboCount.remove(uuid);
            onSkipStage.accept(player);
        }
    }

    private void Slam(Player player) {
        World world = player.getWorld();
        swingEchoGuard.swingMainHand(player);

        world.playSound(player.getLocation(), Sound.ENTITY_WITCH_THROW, 1.2f, 1.5f);

        long level = player.getPersistentDataContainer().getOrDefault(keyQ, PersistentDataType.LONG, 0L);
        double ampMultiplier = 1.0 + (config.q_Skill_amp * level);
        double damage = config.q_Skill_damage * ampMultiplier;

        // 💡 추가된 피흡 로직: 최대 회복량 설정
        final double maxHeal = 4.0 * ampMultiplier;
        final double[] currentHealed = {0.0};

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player).withDirectEntity(player).build();

        Location origin = player.getEyeLocation().subtract(0, 0.3, 0);
        Vector dir = origin.getDirection().setY(0).normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());

        double slashLength = 4.0;
        int maxTicks = 4;
        Random rand = new Random();

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= maxTicks || player.isDead()) {
                    this.cancel();
                    return;
                }

                double progress = Math.toRadians(70) - (ticks * Math.toRadians(110) / maxTicks);
                double cosP = Math.cos(progress);
                double sinP = Math.sin(progress);

                for (double length = 1.0; length <= slashLength; length += 0.2) {
                    for (double angle = -Math.toRadians(15); angle <= Math.toRadians(15); angle += Math.toRadians(5.0)) {
                        double fComp = cosP * length;
                        double yComp = sinP * length;
                        double rComp = Math.sin(angle) * 0.5;

                        double pX = origin.getX() + (dir.getX() * fComp) + (right.getX() * rComp);
                        double pY = origin.getY() + yComp;
                        double pZ = origin.getZ() + (dir.getZ() * fComp) + (right.getZ() * rComp);

                        Particle.DustOptions opt = CLOWN_COLORS[rand.nextInt(CLOWN_COLORS.length)];
                        world.spawnParticle(Particle.DUST, pX, pY, pZ, 1, 0.2, 0.2, 0.2, 0, opt);
                    }
                }

                if (ticks == maxTicks - 1) {
                    Location impactLoc = player.getLocation().add(dir.clone().multiply(2.5));
                    world.playSound(impactLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 1.0f);
                    world.spawnParticle(Particle.ENCHANTED_HIT, impactLoc.clone().add(0, 0.5, 0), 20, 1.0, 0.5, 1.0, 0.1);
                    world.spawnParticle(Particle.WITCH, impactLoc.clone().add(0, 0.5, 0), 15, 1.0, 0.5, 1.0, 0);

                    for (Entity e : world.getNearbyEntities(impactLoc, 2.0, 2.0, 2.0)) {
                        if (e instanceof LivingEntity target && e != player) {
                            ForceDamage fd = new ForceDamage(target, damage, source, true);
                            fd.applyEffect(player);

                            // 💡 추가된 피흡 로직 적용 (피해량의 50%)
                            if (currentHealed[0] < maxHeal) {
                                double healAmount = damage * 0.5;
                                double actualHeal = Math.min(healAmount, maxHeal - currentHealed[0]);
                                applyLifesteal(player, actualHeal);
                                currentHealed[0] += actualHeal;
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void Sweep(Player player) {
        World world = player.getWorld();
        swingEchoGuard.swingMainHand(player);

        world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 1.2f);
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.8f);

        long level = player.getPersistentDataContainer().getOrDefault(keyQ, PersistentDataType.LONG, 0L);
        double ampMultiplier = 1.0 + (config.q_Skill_amp * level);
        double damage = config.q_Skill_damage * 2 * ampMultiplier;

        final double maxHeal = 4.0 * ampMultiplier;
        final double[] currentHealed = {0.0};

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player).withDirectEntity(player).build();

        Location origin = player.getEyeLocation().subtract(0, 0.2, 0);
        Vector dir = origin.getDirection().setY(0).normalize();

        double slashLength = 5.0;
        int maxTicks = 4;
        double maxAngle = Math.toRadians(90);
        Random rand = new Random();

        final HashSet<UUID> damaged = new HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= maxTicks || player.isDead()) {
                    this.cancel();
                    return;
                }

                double startAngle = maxAngle - (ticks * (maxAngle * 2) / maxTicks);
                double endAngle = maxAngle - ((ticks + 1) * (maxAngle * 2) / maxTicks);

                for (double angle = startAngle; angle >= endAngle; angle -= Math.toRadians(4)) {
                    Vector rotatedDir = dir.clone().rotateAroundY(angle);

                    for (double length = 1.0; length <= slashLength; length += 0.3) {
                        Location pLoc = origin.clone().add(rotatedDir.clone().multiply(length));
                        Particle.DustOptions opt = CLOWN_COLORS[rand.nextInt(CLOWN_COLORS.length)];
                        world.spawnParticle(Particle.DUST, pLoc, 1, 0.1, 0.1, 0.1, 0, opt);

                        if(length > slashLength - 0.5) {
                            world.spawnParticle(Particle.FIREWORK, pLoc, 1, 0.1, 0.1, 0.1, 0.02);
                        }
                    }
                }

                for (Entity e : world.getNearbyEntities(origin, slashLength, slashLength, slashLength)) {
                    if (e instanceof LivingEntity target && e != player && !damaged.contains(target.getUniqueId())) {
                        Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize();

                        if (dir.dot(toTarget) >= 0) {
                            ForceDamage fd = new ForceDamage(target, damage, source, true);
                            fd.applyEffect(player);

                            Vector knockback = toTarget.multiply(1.3).setY(0.4);
                            target.setVelocity(knockback);

                            damaged.add(target.getUniqueId());

                            if (currentHealed[0] < maxHeal) {
                                double healAmount = damage;
                                double actualHeal = Math.min(healAmount, maxHeal - currentHealed[0]);
                                applyLifesteal(player, actualHeal);
                                currentHealed[0] += actualHeal;
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void applyLifesteal(Player player, double healAmount) {
        if (player.isDead() || !player.isValid()) return;

        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double maxHealth = maxHealthAttr.getValue();
            player.setHealth(Math.min(maxHealth, player.getHealth() + healAmount));

            Location centerLoc = player.getLocation().add(0, 1.0, 0);
            World world = player.getWorld();

            world.spawnParticle(Particle.BLOCK, centerLoc, 6, 0.4, 0.5, 0.4, 0, BLOOD_DATA);
            world.spawnParticle(Particle.DUST, centerLoc, 4, 0.4, 0.5, 0.4, 0, CELL_RED);

            world.playSound(centerLoc, Sound.BLOCK_SLIME_BLOCK_STEP, 0.35f, 1.2f);
        }
    }
}