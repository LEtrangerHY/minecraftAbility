package org.core.coreSystem.cores.VOL2.Undead.Skill.DeadlyWeapons;

import org.bukkit.*;
import org.bukkit.block.data.BlockData;
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
import org.core.coreSystem.cores.VOL2.Undead.coreSystem.Undead;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;
import org.core.effect.crowdControl.Stun;

import java.util.HashSet;

public class R_axe implements SkillBase {
    private final Undead config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final NamespacedKey keyR;

    private static final Particle.DustOptions DUST_SLASH = new Particle.DustOptions(Color.fromRGB(160, 160, 160), 0.6f);
    private static final Particle.DustOptions DUST_SLASH_GRA = new Particle.DustOptions(Color.fromRGB(130, 130, 130), 0.6f);
    private static final BlockData BLOOD = Material.REDSTONE_BLOCK.createBlockData();

    public R_axe(Undead config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.keyR = new NamespacedKey(plugin, "R");
    }

    @Override
    public void Trigger(Player player) {
        Material handMat = player.getInventory().getItemInMainHand().getType();
        String coolKey_re = config.getWeaponRCoolKey(handMat);
        String coolKey = config.getWeaponCoolKey(handMat);

        if (coolKey == null || coolKey_re == null) return;

        if (cool.isReloading(player, coolKey_re)) {
            cool.updateCooldown(player, coolKey_re, 0L, "boss");
            cool.setCooldown(player, config.r_Skill_Cool, coolKey);
            cool.setCooldown(player, config.r_Skill_Cool, "R");
            Reuse(player);
        } else {
            Effect(player, coolKey, coolKey_re, handMat);
        }
    }

    public void Effect(Player player, String coolKey, String coolKey_re, Material formalHand) {
        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyR, PersistentDataType.LONG, 0L);
        double damage = 3.0 * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Invulnerable invulnerable = new Invulnerable(player, 400);
        invulnerable.applyEffect(player);

        Vector dashDir = player.getLocation().getDirection().normalize().multiply(1.8);
        player.setVelocity(dashDir);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 0.8f);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;
            boolean hit = false;

            @Override
            public void run() {
                if (ticks > 8 || player.isDead() || hit) {
                    Material handMat = player.getInventory().getItemInMainHand().getType();
                    if (handMat == formalHand) cool.setCooldown(player, 0L, "R");

                    cool.setCooldown(player, 0L, coolKey);
                    cool.setCooldown(player, config.r_Skill_Cool_re, coolKey_re, "boss");

                    final long expireTime = System.currentTimeMillis() + config.r_Skill_Cool_re;

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!player.isOnline() || !player.isValid()) {
                                this.cancel();
                                return;
                            }

                            if (System.currentTimeMillis() >= expireTime) {

                                Material handMat_2 = player.getInventory().getItemInMainHand().getType();
                                if (handMat_2 == formalHand) cool.setCooldown(player, config.r_Skill_Cool, "R");

                                cool.updateCooldown(player, coolKey_re, 0L, "boss");
                                cool.setCooldown(player, config.r_Skill_Cool, coolKey);

                                this.cancel();
                            }

                            if (!cool.isReloading(player, coolKey_re)) {
                                this.cancel();
                                return;
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 1L);

                    this.cancel();
                    return;
                }

                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1.0, 0), 5, 0.2, 0.2, 0.2, 0.05);

                for (Entity entity : player.getNearbyEntities(1.3, 1.3, 1.3)) {
                    if (entity instanceof LivingEntity target && entity != player) {

                        ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                        forceDamage.applyEffect(player);

                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 5, false, false, true));

                        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.2f, 0.9f);
                        player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.0, 0), 20, 0.4, 0.4, 0.4, 0.1);
                        player.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1.0, 0), 15, 0.3, 0.4, 0.3, BLOOD);

                        player.setVelocity(new Vector(0, 0, 0));

                        hit = true;
                        break;
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void Reuse(Player player) {

        World world = player.getWorld();
        player.swingMainHand();

        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.5f);

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyR, PersistentDataType.LONG, 0L);
        double damage = 6.0 * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        double slashLength = 4.4;
        double maxTicks = 4;
        double innerRadius = 2.0;

        Location origin = player.getEyeLocation().add(0, -0.4, 0);
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        double dirX = direction.getX();
        double dirZ = direction.getZ();
        double rightX = -dirZ;
        double rightZ = dirX;

        double originX = origin.getX();
        double originY = origin.getY();
        double originZ = origin.getZ();

        HashSet<Entity> damagedSet = new HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks < 2)
                    world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 0.6f);

                if (ticks >= maxTicks || player.isDead()) {
                    this.cancel();
                    return;
                }

                double progress = Math.toRadians(80) - (ticks * Math.toRadians(110) / maxTicks);
                double cosP = Math.cos(progress);
                double sinP = Math.sin(progress);

                for (double length = innerRadius; length <= slashLength; length += 0.18) {
                    for (double angle = -Math.toRadians(10); angle <= Math.toRadians(10); angle += Math.toRadians(2.0)) {

                        double fComp = cosP * length;
                        double yComp = sinP * length;
                        double rComp = Math.sin(angle) * 0.45;

                        double pX = originX + (dirX * fComp) + (rightX * rComp);
                        double pY = originY + yComp;
                        double pZ = originZ + (dirZ * fComp) + (rightZ * rComp);

                        Particle.DustOptions opt = (Math.random() < 0.15) ? DUST_SLASH : DUST_SLASH_GRA;
                        world.spawnParticle(Particle.DUST, pX, pY, pZ, 3, 0.13, 0.4, 0.13, 0, opt);
                    }
                }

                if (ticks == maxTicks - 1) {
                    Location impactLoc = player.getLocation().add(direction.clone().multiply(2.5));

                    world.playSound(impactLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.5f);
                    world.playSound(impactLoc, Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.2f, 0.6f);

                    world.spawnParticle(Particle.CRIT, impactLoc.clone().add(0, 0.5, 0), 20, 1.0, 0.5, 1.0, 0.1);

                    Material groundMat = impactLoc.clone().subtract(0, 0.1, 0).getBlock().getType();
                    if (groundMat.isSolid()) {
                        world.spawnParticle(Particle.BLOCK, impactLoc, 45, 1.2, 0.4, 1.2, groundMat.createBlockData());
                    }

                    for (Entity e : world.getNearbyEntities(impactLoc, 2.0, 1.8, 2.0)) {
                        if (!(e instanceof LivingEntity target) || e == player) continue;
                        if (damagedSet.contains(target)) continue;

                        ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                        forceDamage.applyEffect(player);

                        Stun stun = new Stun(target, 2000L);
                        stun.applyEffect(player);

                        damagedSet.add(target);

                        world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.2f, 0.7f);

                        world.spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1.0, 0), 40, 0.4, 0.5, 0.4, BLOOD);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}