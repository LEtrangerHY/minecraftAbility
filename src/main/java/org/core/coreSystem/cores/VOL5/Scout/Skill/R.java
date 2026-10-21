package org.core.coreSystem.cores.VOL5.Scout.Skill;

import org.core.models.coreObj.sctBombModel;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL5.Scout.coreSystem.Scout;
import org.core.effect.crowdControl.ForceDamage;

import java.util.UUID;

public class R implements SkillBase {
    private final Scout config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R(Scout config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();

        if (config.activeBombs.containsKey(uuid) && config.activeBombs.get(uuid).isValid()) {
            detonateBomb(player, uuid, false);
        }
        else {
            throwBomb(player, uuid);
        }
    }

    private void throwBomb(Player player, UUID uuid) {
        World world = player.getWorld();
        Location spawnLoc = player.getEyeLocation().clone();
        Vector velocity = spawnLoc.getDirection().normalize().multiply(1.1).setY(spawnLoc.getDirection().getY() + 0.2);

        world.playSound(spawnLoc, Sound.ENTITY_SNOWBALL_THROW, 1.0f, 0.5f);
        world.playSound(spawnLoc, Sound.BLOCK_PISTON_EXTEND, 0.5f, 1.5f);

        ItemDisplay bomb = new sctBombModel(world, spawnLoc).getDisplay();

        config.activeBombs.put(uuid, bomb);

        cool.setCooldown(player, 250L, "R");

        BukkitRunnable task = new BukkitRunnable() {
            int life = 120;
            Location currentLoc = spawnLoc.clone();
            boolean stuck = false;
            float spin = 0f;

            @Override
            public void run() {
                if (!bomb.isValid()) {
                    this.cancel();
                    return;
                }

                if (life-- <= 0) {
                    detonateBomb(player, uuid, true);
                    this.cancel();
                    return;
                }

                if (life % Math.max(5, (life / 20) * 5) == 0) {
                    world.spawnParticle(Particle.DUST, currentLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 1.0f));
                    world.playSound(currentLoc, Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 2.0f);
                }

                if (!stuck) {
                    double speed = velocity.length();
                    RayTraceResult hitResult = world.rayTraceBlocks(currentLoc, velocity.clone().normalize(), speed, FluidCollisionMode.NEVER, true);

                    if (hitResult != null && hitResult.getHitBlock() != null) {
                        currentLoc = hitResult.getHitPosition().toLocation(world);
                        stuck = true;
                        velocity.zero();
                        world.playSound(currentLoc, Sound.BLOCK_SLIME_BLOCK_PLACE, 1.0f, 1.2f);
                        world.playSound(currentLoc, Sound.BLOCK_METAL_PLACE, 0.8f, 1.0f);
                    } else {
                        currentLoc.add(velocity);
                        velocity.multiply(0.95);
                        velocity.setY(velocity.getY() - 0.05);

                        spin = (spin + 20) % 360;
                        currentLoc.setYaw(spin);
                        currentLoc.setPitch(spin);
                    }
                    bomb.teleport(currentLoc);
                }
            }
        };

        task.runTaskTimer(plugin, 0L, 1L);
        config.bombTasks.put(uuid, task);
    }

    private void detonateBomb(Player player, UUID uuid, boolean isSelfDetonation) {
        ItemDisplay bomb = config.activeBombs.get(uuid);
        if (bomb == null || !bomb.isValid()) return;

        Location detonateLoc = bomb.getLocation().clone();

        bomb.remove();
        config.activeBombs.remove(uuid);
        if (config.bombTasks.containsKey(uuid)) {
            config.bombTasks.get(uuid).cancel();
            config.bombTasks.remove(uuid);
        }

        cool.setCooldown(player, config.r_Skill_Cool, "R");

        World world = detonateLoc.getWorld();

        world.playSound(detonateLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
        world.playSound(detonateLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 1.2f);

        world.spawnParticle(Particle.EXPLOSION, detonateLoc, 3);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, detonateLoc, 1);
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, detonateLoc, 50, 2.0, 2.0, 2.0, 0.1);
        world.spawnParticle(Particle.LAVA, detonateLoc, 30, 1.5, 1.5, 1.5, 0);

        NamespacedKey rKey = new NamespacedKey(plugin, "R");
        long level = player.getPersistentDataContainer().getOrDefault(rKey, PersistentDataType.LONG, 0L);
        double rAmp = config.r_Skill_amp * level;
        double finalDamage = config.r_Skill_damage * (1.0 + rAmp);

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        for (Entity e : world.getNearbyEntities(detonateLoc, 5.0, 5.0, 5.0)) {
            if (e instanceof LivingEntity target && !target.isDead() && !(target instanceof org.bukkit.entity.ArmorStand) && !target.hasMetadata("NPC")) {

                Vector pushVec = target.getLocation().toVector().subtract(detonateLoc.toVector());
                if (pushVec.lengthSquared() < 0.0001) {
                    pushVec = new Vector(0, 1, 0);
                } else {
                    pushVec.normalize();
                }
                pushVec.multiply(1.3).setY(0.7);

                if (target.equals(player)) {
                    if (isSelfDetonation) {
                        double selfDamage = finalDamage * 0.5;
                        AttributeInstance maxHp = player.getAttribute(Attribute.MAX_HEALTH);
                        if (maxHp != null) {
                            double newHp = Math.max(1.0, player.getHealth() - selfDamage);
                            player.setHealth(newHp);
                            player.damage(0.1);
                            world.spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0);
                        }
                    }
                    player.setVelocity(player.getVelocity().add(pushVec.multiply(1.2)));
                }
                else if (!target.isInvulnerable()) {
                    ForceDamage forceDamage = new ForceDamage(target, finalDamage, source, true);
                    forceDamage.applyEffect(player);

                    target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 80, 0, false, false, false));
                    target.setVelocity(target.getVelocity().add(pushVec));
                }
            }
        }
    }
}