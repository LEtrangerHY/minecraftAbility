package org.core.coreSystem.cores.VOL2.Undead.Skill.DeadlyWeapons;

import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Undead.coreSystem.Undead;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;

import java.util.HashSet;
import java.util.Random;

public class R_shovel implements SkillBase {
    private final Undead config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final NamespacedKey keyR;

    private final Random random = new Random();

    private static final Particle.DustOptions DUST_IRON = new Particle.DustOptions(Color.fromRGB(160, 160, 160), 0.6f);
    private static final Particle.DustOptions DUST_MID = new Particle.DustOptions(Color.fromRGB(140, 140, 140), 0.6f);
    private static final Particle.DustOptions DUST_DECAY = new Particle.DustOptions(Color.fromRGB(60, 60, 60), 0.3f);

    public R_shovel(Undead config, JavaPlugin plugin, Cool cool) {
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
            Reuse(player, coolKey, coolKey_re);
        } else {
            Effect(player);
            cool.setCooldown(player, config.r_Skill_Cool_re, coolKey_re, "boss");
            cool.setCooldown(player, 0L, coolKey);
            cool.setCooldown(player, 0L, "R");

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
                        if (handMat_2 == handMat) cool.setCooldown(player, config.r_Skill_Cool, "R");

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
        }
    }

    public void Effect(Player player) {
        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyR, PersistentDataType.LONG, 0L);
        double damage = 1.5 * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        World world = player.getWorld();

        Invulnerable invulnerable = new Invulnerable(player, 400);
        invulnerable.applyEffect(player);

        Vector dashDir = player.getLocation().getDirection().normalize().multiply(1.6).setY(0.2);
        player.setVelocity(dashDir);

        world.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.2f, 0.8f);
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.9f);

        HashSet<Entity> damagedSet = new HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 8 || player.isDead()) {
                    this.cancel();
                    return;
                }
                world.spawnParticle(Particle.CRIT, player.getLocation().add(0, 1.0, 0), 5, 0.2, 0.2, 0.2, 0.1);
                world.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 0.2, 0), 3, 0.2, 0.1, 0.2, 0.05);

                for (Entity entity : player.getNearbyEntities(1.8, 1.8, 1.8)) {
                    if (entity instanceof LivingEntity target && entity != player) {
                        if (!damagedSet.contains(target)) {
                            ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                            forceDamage.applyEffect(player);

                            world.playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.8f, 1.2f);
                            damagedSet.add(target);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void Reuse(Player player, String coolKey, String coolKey_re) {

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyR, PersistentDataType.LONG, 0L);
        double damage = 4.5 * (1 + amp) * 1.2;

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        World world = player.getWorld();
        player.swingMainHand();
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.7f);
        world.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 0.6f, 0.8f);

        double slashLength = 4.4;
        double maxAngleDeg = 80.0;
        double maxAngleRad = Math.toRadians(maxAngleDeg);
        double innerRadius = 1.4;
        int maxTicks = 5;

        double angleThreshold = Math.cos(maxAngleRad + 0.1);

        Location origin = player.getEyeLocation().add(0, -0.4, 0);
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        if (direction.lengthSquared() == 0) direction = new Vector(1, 0, 0);

        HashSet<Entity> damagedSet = new HashSet<>();

        Vector finalDirection = direction;
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= maxTicks || player.isDead()) {
                    this.cancel();
                    return;
                }

                for (Entity entity : world.getNearbyEntities(player.getLocation(), slashLength, slashLength, slashLength)) {
                    if (!(entity instanceof LivingEntity target) || entity == player) continue;
                    if (damagedSet.contains(target)) continue;

                    Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector());
                    toTarget.setY(0);

                    if (toTarget.lengthSquared() > slashLength * slashLength) continue;

                    Vector directionToTarget = toTarget.normalize();
                    double dotProduct = finalDirection.dot(directionToTarget);

                    if (dotProduct >= angleThreshold) {
                        ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                        forceDamage.applyEffect(player);

                        Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.1).setY(0.3);
                        target.setVelocity(push);

                        world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 1.0f);
                        world.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.2, 0), 15, 0.4, 0.4, 0.4, 0.1);

                        disarmMainHand(target);

                        damagedSet.add(target);
                    }
                }

                double sweepPerTick = (maxAngleRad * 2) / maxTicks;
                double startDeg = -maxAngleRad + (ticks * sweepPerTick);
                double endDeg = -maxAngleRad + ((ticks + 1) * sweepPerTick);

                int steps = 6;
                double range = slashLength - innerRadius;

                for (int i = 0; i <= steps; i++) {
                    double currentDeg = startDeg + (endDeg - startDeg) * ((double) i / steps);

                    for (double len = innerRadius; len <= slashLength; len += 0.06) {
                        Vector offset = finalDirection.clone().rotateAroundY(currentDeg).multiply(len);
                        Location pLoc = origin.clone().add(offset);

                        Particle.DustOptions opt;
                        if (len < innerRadius + (range * 0.66)) opt = DUST_IRON;
                        else opt = DUST_MID;

                        opt = (Math.random() < 0.22) ? DUST_DECAY : opt;
                        world.spawnParticle(Particle.DUST, pLoc, 1, 0.13, 0.13, 0.13, 0, opt);
                    }

                    if (Math.random() < 0.22) {
                        Vector sweepOffset = finalDirection.clone().rotateAroundY(currentDeg).multiply(slashLength);
                        world.spawnParticle(Particle.SWEEP_ATTACK, origin.clone().add(sweepOffset), 1, 0, 0, 0, 0);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void disarmMainHand(LivingEntity target) {
        EntityEquipment equipment = target.getEquipment();
        if (equipment == null) return;

        ItemStack mainHandItem = equipment.getItemInMainHand();

        if (mainHandItem.getType() != Material.AIR) {
            ItemStack droppedItem = mainHandItem.clone();

            equipment.setItemInMainHand(null);

            Location dropLoc = target.getLocation().add(0, 1.0, 0);
            Item itemEntity = target.getWorld().dropItem(dropLoc, droppedItem);

            itemEntity.setPickupDelay(40);

            itemEntity.setVelocity(new Vector(
                    (random.nextDouble() - 0.5) * 0.4,
                    0.5,
                    (random.nextDouble() - 0.5) * 0.4
            ));

            target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.2f, 0.7f);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.9f);
            target.getWorld().spawnParticle(Particle.ITEM, target.getLocation().add(0, 1.2, 0), 25, 0.3, 0.4, 0.3, 0.1, droppedItem);
        }
    }
}