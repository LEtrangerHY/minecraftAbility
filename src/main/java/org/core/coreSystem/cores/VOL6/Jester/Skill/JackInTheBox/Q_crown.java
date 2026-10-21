package org.core.coreSystem.cores.VOL6.Jester.Skill.JackInTheBox;

import org.core.effect.effectPlugin.swingEchoGuard;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL6.Jester.coreSystem.Jester;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Grounding;
import org.core.effect.crowdControl.Stun;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Q_crown implements SkillBase {

    private final Jester config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final Consumer<Player> onSkipStage;

    private static final Particle.DustOptions DUST_CHAIN = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.6f);

    public Q_crown(Jester config, JavaPlugin plugin, Cool cool, Consumer<Player> onSkipStage) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.onSkipStage = onSkipStage;
    }

    @Override
    public void Trigger(Player player) {
        World world = player.getWorld();
        Location pLoc = player.getLocation();

        config.isExecutionTime.put(player.getUniqueId(), true);

        world.playSound(pLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.5f, 1.0f);

        for (int degree = 0; degree < 360; degree += 8) {
            double radians = Math.toRadians(degree);
            double x = Math.cos(radians) * 6.0;
            double z = Math.sin(radians) * 6.0;
            world.spawnParticle(Particle.WITCH, pLoc.clone().add(x, 0.2, z), 1, 0, 0, 0, 0);
        }
        world.spawnParticle(Particle.WITCH, pLoc.clone().add(0, 1, 0), 40, 2.0, 0.5, 2.0, 0.1);

        List<LivingEntity> executeTargets = new ArrayList<>();
        List<LivingEntity> damageTargets = new ArrayList<>();

        for (Entity e : world.getNearbyEntities(pLoc, 6.0, 6.0, 6.0)) {
            if (e instanceof LivingEntity target && e != player) {
                if (target.getLocation().distanceSquared(pLoc) > 36.0) continue;

                double maxHp = target.getAttribute(Attribute.MAX_HEALTH) != null ? target.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
                double curHp = target.getHealth();
                double hpRatio = curHp / maxHp;

                if (hpRatio < 0.22 && executeTargets.size() < 5) {
                    executeTargets.add(target);
                } else {
                    damageTargets.add(target);
                }
            }
        }

        player.setVelocity(new Vector(0, 0.6, 0));
        swingEchoGuard.swingMainHand(player);
        world.playSound(pLoc, Sound.ENTITY_BREEZE_JUMP, 1.2f, 1.2f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isDead() || !player.isOnline()) {
                    config.isExecutionTime.remove(player.getUniqueId());
                    return;
                }

                Location landLoc = player.getLocation();

                player.setVelocity(new Vector(0, -0.8, 0));
                world.playSound(landLoc, Sound.ENTITY_WITHER_BREAK_BLOCK, 0.8f, 0.6f);
                world.spawnParticle(Particle.EXPLOSION, landLoc.clone().add(0, 0.2, 0), 2, 1.0, 0.1, 1.0, 0);

                DamageSource source = DamageSource.builder(DamageType.MAGIC)
                        .withCausingEntity(player).withDirectEntity(player).build();

                int executedCount = 0;

                for (LivingEntity target : executeTargets) {
                    if (target.isDead() || !target.isValid()) continue;
                    executedCount++;

                    world.playSound(target.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.5f, 0.8f);
                    world.playSound(target.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 1.5f, 1.2f);
                    world.spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING, target.getLocation().add(0, 1, 0), 60, 0.5, 0.5, 0.5, 0.2);

                    ForceDamage fd = new ForceDamage(target, 999999, source, true);
                    fd.applyEffect(player);
                    target.setHealth(0);
                }

                for (LivingEntity target : damageTargets) {
                    if (target.isDead() || !target.isValid()) continue;

                    double damage = target.getHealth() * 0.33;
                    ForceDamage fd = new ForceDamage(target, damage, source, true);
                    fd.applyEffect(player);

                    target.setVelocity(new Vector(0, 0, 0));
                    Grounding grounding = new Grounding(target, 2000L);
                    grounding.applyEffect(player);

                    playChainEffect(target, world);
                }

                if (executeTargets.isEmpty()) {
                    double selfDamage = player.getHealth() * 0.33;
                    ForceDamage fd = new ForceDamage(player, selfDamage, source, true);
                    fd.applyEffect(player);

                    Stun stun = new Stun(player, 4000L);
                    stun.applyEffect(player);

                    cool.setCooldown(player, 20000L, "TRICK SHOW Cooldown", "boss");

                    playPenaltySpear(player, world);

                } else {
                    long baseCooldown = 20000L;
                    long reduction = executedCount * 4000L;
                    long finalCooldown = Math.max(0, baseCooldown - reduction);

                    if (finalCooldown > 0) {
                        cool.setCooldown(player, finalCooldown, "TRICK SHOW Cooldown", "boss");
                    } else {
                        cool.updateCooldown(player, "TRICK SHOW Cooldown", 0L);
                    }
                    player.playSound(landLoc, Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 1.5f);
                }

                config.isExecutionTime.remove(player.getUniqueId());
                onSkipStage.accept(player);
            }
        }.runTaskLater(plugin, 10L);
    }

    private void playChainEffect(LivingEntity target, World world) {
        Location loc = target.getLocation();
        world.playSound(loc, Sound.BLOCK_CHAIN_PLACE, 1.6f, 1.0f);
        world.spawnParticle(Particle.BLOCK, loc.clone().add(0, 1.2, 0), 12, 0.6, 0.6, 0.6, Material.IRON_CHAIN.createBlockData());

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 40 || target.isDead() || !target.isValid()) {
                    this.cancel();
                    return;
                }
                Location baseLoc = target.getLocation();
                for (int i = 0; i < 33; i += 2) {
                    double yOffset = i / 10.0;
                    world.spawnParticle(Particle.DUST, baseLoc.clone().add(0, yOffset, 0), 1, 0, 0, 0, 0, DUST_CHAIN);

                    if (i % 3 == 0) {
                        double hitY = 3.3 - (i * 0.12);
                        world.spawnParticle(Particle.ENCHANTED_HIT, baseLoc.clone().add(0, hitY, 0), 1, 0, 0, 0, 0);
                    }
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void playPenaltySpear(Player player, World world) {
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.5f, 0.5f);

        new BukkitRunnable() {
            int dropTick = 0;

            @Override
            public void run() {
                if (player.isDead() || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                Location pLoc = player.getLocation();

                if (dropTick < 3) {
                    double y = 6.0 - (dropTick * 2.0);
                    world.spawnParticle(Particle.BLOCK, pLoc.clone().add(0, y, 0), 30, 0.3, 0.8, 0.3, Material.IRON_CHAIN.createBlockData());

                } else if (dropTick == 3) {
                    world.playSound(pLoc, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.6f);
                    world.playSound(pLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 0.5f);
                    world.playSound(pLoc, Sound.ENTITY_DONKEY_HURT, 1.5f, 0.8f);
                    world.spawnParticle(Particle.EXPLOSION, pLoc.clone().add(0, 1, 0), 2, 0.5, 0.5, 0.5, 0);

                } else if (dropTick < 23) {
                    for (int i = 0; i < 33; i += 2) {
                        double yOffset = i / 10.0;
                        world.spawnParticle(Particle.DUST, pLoc.clone().add(0, yOffset, 0), 1, 0.1, 0, 0.1, 0, DUST_CHAIN);

                        if (i % 4 == 0) {
                            world.spawnParticle(Particle.ENCHANTED_HIT, pLoc.clone().add(0, yOffset, 0), 1, 0.1, 0, 0.1, 0);
                        }
                    }
                } else {
                    this.cancel();
                }

                dropTick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}