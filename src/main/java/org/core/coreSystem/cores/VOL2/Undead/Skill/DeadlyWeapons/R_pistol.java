package org.core.coreSystem.cores.VOL2.Undead.Skill.DeadlyWeapons;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Undead.coreSystem.Undead;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Stun;

public class R_pistol implements SkillBase {
    private final Undead config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R_pistol(Undead config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {

        Material handMat = player.getInventory().getItemInMainHand().getType();
        String coolKey = config.getWeaponCoolKey(handMat);

        if (coolKey == null) return;

        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        double maxHealth = maxHealthAttr.getValue();
        double cost = maxHealth * 0.13;

        if (player.getHealth() <= cost) {
            player.sendMessage("§c[!] 체력 부족.");
            cool.setCooldown(player, 0L, coolKey);
            cool.setCooldown(player, 0L, "R");
            return;
        }

        player.setHealth(player.getHealth() - cost);

        World world = player.getWorld();
        Vector dir = player.getEyeLocation().getDirection().normalize();

        Vector knockback = dir.clone().multiply(-0.3).setY(0.1);
        player.setVelocity(player.getVelocity().add(knockback));

        new BukkitRunnable() {
            int ticks = 0;
            float currentRecoil = 0f;
            float targetRecoil = -5.0f;

            @Override
            public void run() {
                if (ticks > 4 || !player.isOnline() || player.isDead()) {
                    if (Math.abs(currentRecoil) > 0.01f) {
                        Location loc = player.getLocation();
                        loc.setPitch(loc.getPitch() - currentRecoil);
                        Vector velocity = player.getVelocity();
                        player.teleport(loc);
                        player.setVelocity(velocity);
                    }
                    this.cancel();
                    return;
                }

                float nextRecoil;
                if (ticks < 1) {
                    nextRecoil = currentRecoil + (targetRecoil - currentRecoil) * 0.9f;
                } else {
                    targetRecoil = 0f;
                    nextRecoil = currentRecoil + (targetRecoil - currentRecoil) * 0.55f;
                }

                float delta = nextRecoil - currentRecoil;
                currentRecoil = nextRecoil;

                if (Math.abs(delta) > 0.05f) {
                    Location loc = player.getLocation();
                    loc.setPitch(loc.getPitch() + delta);

                    Vector velocity = player.getVelocity();
                    player.teleport(loc);
                    player.setVelocity(velocity);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        Vector spawnOffset = dir.clone().multiply(0.8).add(new Vector(0, -0.3, 0));
        Location spawnLoc = player.getEyeLocation().add(spawnOffset);

        BlockDisplay bulletDisplay = world.spawn(spawnLoc, BlockDisplay.class, entity -> {
            entity.setBlock(Material.IRON_BLOCK.createBlockData());
            entity.setTeleportDuration(1);

            Transformation transform = entity.getTransformation();
            transform.getScale().set(0.3f, 0.2f, 0.3f);
            transform.getTranslation().set(0f, 0f, 0f);
            entity.setTransformation(transform);
        });

        double speed = 4.0;
        Vector velocity = dir.clone().multiply(speed);

        world.playSound(spawnLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 2.0f);
        world.spawnParticle(Particle.SMOKE, spawnLoc, 6, 0.1, 0.1, 0.1, 0.05);

        DamageSource source = DamageSource.builder(DamageType.MOB_PROJECTILE)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            int life = 60;
            Location currentLoc = spawnLoc.clone();

            @Override
            public void run() {
                if (!bulletDisplay.isValid()) {
                    this.cancel();
                    return;
                }

                if (life-- <= 0) {
                    bulletDisplay.remove();
                    this.cancel();
                    return;
                }

                world.spawnParticle(Particle.CRIT, currentLoc, 1, 0, 0, 0, 0);

                for (Entity nearby : world.getNearbyEntities(currentLoc, 1.3, 1.3, 1.3)) {
                    if (nearby instanceof LivingEntity target && nearby != player) {

                        double targetCurrentHp = target.getHealth();
                        double damage = targetCurrentHp * 0.25;

                        ForceDamage forceDamage = new ForceDamage(target, damage, source, false);
                        forceDamage.applyEffect(player);

                        Stun stun = new Stun(target, 2000L);
                        stun.applyEffect(player);

                        world.playSound(currentLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.3f, 1.2f);
                        world.playSound(currentLoc, Sound.BLOCK_ANVIL_LAND, 1.1f, 1.5f);
                        world.spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1.0, 0), 20, 0.3, 0.3, 0.3, Material.REDSTONE_BLOCK.createBlockData());

                        world.spawnParticle(Particle.BLOCK, currentLoc, 15, 0.2, 0.2, 0.2, Material.IRON_BLOCK.createBlockData());

                        bulletDisplay.remove();
                        this.cancel();
                        return;
                    }
                }

                RayTraceResult hitBlockResult = world.rayTraceBlocks(currentLoc, dir, speed, FluidCollisionMode.NEVER, true);

                if (hitBlockResult != null && hitBlockResult.getHitBlock() != null) {
                    Location hitPos = hitBlockResult.getHitPosition().toLocation(world);

                    world.playSound(hitPos, Sound.BLOCK_STONE_BREAK, 1.0f, 1.5f);
                    world.spawnParticle(Particle.BLOCK, hitPos, 15, 0.2, 0.2, 0.2, hitBlockResult.getHitBlock().getBlockData());

                    bulletDisplay.remove();
                    this.cancel();
                    return;
                }

                currentLoc.add(velocity);
                bulletDisplay.teleport(currentLoc);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}