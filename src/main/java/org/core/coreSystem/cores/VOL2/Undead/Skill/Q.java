package org.core.coreSystem.cores.VOL2.Undead.Skill;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Undead.coreSystem.Undead;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Q implements SkillBase {
    private final Undead config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private final NamespacedKey keyQ;

    private static final Particle.DustOptions DUST_IRON = new Particle.DustOptions(Color.fromRGB(160, 160, 160), 0.6f);
    private static final Particle.DustOptions DUST_MID = new Particle.DustOptions(Color.fromRGB(140, 140, 140), 0.6f);
    private static final Particle.DustOptions DUST_DECAY = new Particle.DustOptions(Color.fromRGB(60, 60, 60), 0.3f);

    public Q(Undead config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.keyQ = new NamespacedKey(plugin, "Q");
    }

    @Override
    public void Trigger(Player player) {
        ItemStack mainItemForCheck = player.getInventory().getItemInMainHand();
        ItemStack offItemForCheck = player.getInventory().getItemInOffHand();

        Material oldMainMat = mainItemForCheck != null ? mainItemForCheck.getType() : Material.AIR;
        Material newMainMat = offItemForCheck != null ? offItemForCheck.getType() : Material.AIR;

        String qKey = config.getWeaponQCoolKey(newMainMat);
        Material messageMat = newMainMat;
        if (qKey == null) {
            qKey = config.getWeaponQCoolKey(oldMainMat);
            messageMat = oldMainMat;
        }

        if (qKey != null) {
            if (cool.isReloading(player, qKey)) return;
        }

        if (qKey != null) {
            cool.setCooldown(player, config.q_Skill_Cool, qKey);
            cool.setCooldown(player, config.q_Skill_Cool, "Q");
        } else {
            cool.updateCooldown(player, "Q", 0L);
        }

        String rKey = config.getWeaponCoolKey(newMainMat);
        if (rKey != null) {
            cool.updateCooldown(player, "R", cool.getRemainCooldown(player, rKey));
        } else {
            cool.updateCooldown(player, "R", 0L);
        }

        UUID uuid = player.getUniqueId();
        config.is_swapping.add(uuid);

        final Material finalMessageMat = messageMat;
        String finalQKey = qKey;
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack actualMain = player.getInventory().getItemInMainHand();
            actualMain = (actualMain != null) ? actualMain.clone() : null;

            ItemStack actualOff = player.getInventory().getItemInOffHand();
            actualOff = (actualOff != null) ? actualOff.clone() : null;

            if (finalQKey != null) {
                CrossSweep(player, 4.0, 1.6, actualMain, actualOff, finalMessageMat);
            } else {
                try {
                    player.getInventory().setItemInMainHand(null);
                    player.getInventory().setItemInOffHand(null);

                    player.getInventory().setItemInOffHand(actualMain);
                    player.getInventory().setItemInMainHand(actualOff);

                    player.updateInventory();
                } finally {
                    config.is_swapping.remove(uuid);
                }
            }
        });
    }

    private void CrossSweep(Player player, double slashLength, double innerRadius, ItemStack actualMain, ItemStack actualOff, Material messageMat) {
        Location eyeLoc = player.getEyeLocation();
        Vector forward = eyeLoc.getDirection().normalize();
        Vector worldUp = new Vector(0, 1, 0);
        if (Math.abs(forward.dot(worldUp)) > 0.95) worldUp = new Vector(1, 0, 0);
        Vector right = forward.clone().crossProduct(worldUp).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        boolean isMainEmpty = actualMain == null || actualMain.getType() == Material.AIR;
        boolean isOffEmpty = actualOff == null || actualOff.getType() == Material.AIR;

        boolean draw1 = !isMainEmpty && actualMain.getType() != Material.IRON_HORSE_ARMOR;
        boolean draw2 = !isOffEmpty && actualOff.getType() != Material.IRON_HORSE_ARMOR;

        long qLevel = player.getPersistentDataContainer().getOrDefault(keyQ, org.bukkit.persistence.PersistentDataType.LONG, 0L);
        double amp = config.q_Skill_amp * qLevel;

        double damage1 = draw1 ? getWeaponDamage(actualMain) * (1 + amp) : 0;
        double damage2 = draw2 ? getWeaponDamage(actualOff) * (1 + amp) : 0;

        double tiltRad1 = Math.toRadians((draw1 && draw2) ? 25 : 0);
        double tiltRad2 = Math.toRadians((draw1 && draw2) ? -25 : 0);

        double maxAngle = Math.toRadians(90);
        int maxTicks = 5;
        int fillSteps = 25;

        double cos1 = Math.cos(tiltRad1), sin1 = Math.sin(tiltRad1);
        double cos2 = Math.cos(tiltRad2), sin2 = Math.sin(tiltRad2);

        Vector tiltedRight1 = right.clone().multiply(cos1).add(up.clone().multiply(sin1)).normalize();
        Vector tiltedUp1 = right.clone().multiply(-sin1).add(up.clone().multiply(cos1)).normalize();

        Vector tiltedRight2 = right.clone().multiply(cos2).add(up.clone().multiply(sin2)).normalize();
        Vector tiltedUp2 = right.clone().multiply(-sin2).add(up.clone().multiply(cos2)).normalize();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.4f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 0.8f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 0.4f, 0.5f);

        Set<Entity> localDamagedSet = new HashSet<>();

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isValid() || player.isDead()) {
                    try {
                        if (player.isValid() && !player.isDead()) {
                            player.getInventory().setItemInMainHand(null);
                            player.getInventory().setItemInOffHand(null);

                            player.getInventory().setItemInOffHand(actualMain);
                            player.getInventory().setItemInMainHand(actualOff);

                            player.updateInventory();
                        }
                    } finally {
                        config.is_swapping.remove(player.getUniqueId());
                    }
                    this.cancel();
                    return;
                }

                double progress = (double) ticks / (maxTicks - 1);
                double prevProgress = (double) Math.max(0, ticks - 1) / (maxTicks - 1);

                double currentAngle1 = maxAngle + (-maxAngle - maxAngle) * progress;
                double prevAngle1 = maxAngle + (-maxAngle - maxAngle) * prevProgress;

                double currentAngle2 = -maxAngle + (maxAngle - -maxAngle) * progress;
                double prevAngle2 = -maxAngle + (maxAngle - -maxAngle) * prevProgress;

                double sweepWidth = Math.abs(currentAngle1 - prevAngle1) / 2.0;
                if (ticks == 0) sweepWidth = 0.1;
                double hitThreshold = Math.cos(sweepWidth + 0.2);

                Location origin = eyeLoc.clone().add(0, -0.4, 0);

                for (Entity entity : player.getWorld().getNearbyEntities(origin, slashLength, slashLength, slashLength)) {
                    if (!(entity instanceof LivingEntity target) || entity == player) continue;
                    if (localDamagedSet.contains(entity)) continue;

                    Vector toEntity = target.getEyeLocation().toVector().subtract(origin.toVector());
                    if (toEntity.lengthSquared() > (slashLength + 0.8) * (slashLength + 0.8)) continue;

                    boolean hit1 = false;
                    if (draw1 && Math.abs(toEntity.dot(tiltedUp1)) <= 1.5) {
                        Vector toEntityPlanar1 = toEntity.clone().subtract(tiltedUp1.clone().multiply(toEntity.dot(tiltedUp1))).normalize();
                        Vector midDir1 = forward.clone().multiply(Math.cos((currentAngle1 + prevAngle1) / 2.0)).add(tiltedRight1.clone().multiply(Math.sin((currentAngle1 + prevAngle1) / 2.0))).normalize();
                        if (toEntityPlanar1.dot(midDir1) >= hitThreshold) hit1 = true;
                    }

                    boolean hit2 = false;
                    if (draw2 && Math.abs(toEntity.dot(tiltedUp2)) <= 1.5) {
                        Vector toEntityPlanar2 = toEntity.clone().subtract(tiltedUp2.clone().multiply(toEntity.dot(tiltedUp2))).normalize();
                        Vector midDir2 = forward.clone().multiply(Math.cos((currentAngle2 + prevAngle2) / 2.0)).add(tiltedRight2.clone().multiply(Math.sin((currentAngle2 + prevAngle2) / 2.0))).normalize();
                        if (toEntityPlanar2.dot(midDir2) >= hitThreshold) hit2 = true;
                    }

                    if (hit1 || hit2) {
                        localDamagedSet.add(entity);

                        double totalDamage = 0.0;
                        if (hit1) totalDamage += damage1;
                        if (hit2) totalDamage += damage2;

                        if (totalDamage > 0) {
                            ForceDamage forceDamage = new ForceDamage(target, totalDamage, source, true);
                            forceDamage.applyEffect(player);

                            player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.3f);

                            player.getWorld().spawnParticle(Particle.ASH, target.getLocation().add(0, 1.2, 0), 16, 0.3, 0.3, 0.3, 0.05);
                        }
                    }
                }

                if (draw1) drawSweep(origin, forward, tiltedRight1, currentAngle1, prevAngle1, slashLength, innerRadius, fillSteps);
                if (draw2) drawSweep(origin, forward, tiltedRight2, currentAngle2, prevAngle2, slashLength, innerRadius, fillSteps);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawSweep(Location origin, Vector forward, Vector tiltedRight, double currentAngle, double prevAngle, double slashLength, double innerRadius, int fillSteps) {
        Vector currentDir = forward.clone().multiply(Math.cos(currentAngle)).add(tiltedRight.clone().multiply(Math.sin(currentAngle))).normalize();
        double range = slashLength - innerRadius;

        for (double len = 0.5; len <= slashLength; len += 0.04) {
            Vector point = currentDir.clone().multiply(len);
            Location loc = origin.clone().add(point);
            if (len > innerRadius) {
                Particle.DustOptions opt;
                if (len < innerRadius + (range * 0.66)) opt = DUST_IRON;
                else opt = DUST_MID;

                opt = (Math.random() < 0.22) ? DUST_DECAY : opt;
                origin.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, opt);
            }
        }

        if (currentAngle != prevAngle) {
            for (int i = 1; i < fillSteps; i++) {
                double subAngle = prevAngle + (currentAngle - prevAngle) * (i / (double) fillSteps);
                Vector subDir = forward.clone().multiply(Math.cos(subAngle)).add(tiltedRight.clone().multiply(Math.sin(subAngle))).normalize();

                for (double len = innerRadius; len <= slashLength; len += 0.07) {
                    Particle.DustOptions opt;
                    if (len < innerRadius + (range * 0.66)) opt = DUST_IRON;
                    else opt = DUST_MID;

                    opt = (Math.random() < 0.22) ? DUST_DECAY : opt;
                    origin.getWorld().spawnParticle(Particle.DUST, origin.clone().add(subDir.clone().multiply(len)), 1, 0, 0, 0, 0, opt);
                }
            }
        }
    }

    private double getWeaponDamage(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 1.0;

        if (item.hasItemMeta() && item.getItemMeta().hasAttributeModifiers()) {
            var modifiers = item.getItemMeta().getAttributeModifiers(Attribute.ATTACK_DAMAGE);
            if (modifiers != null && !modifiers.isEmpty()) {
                double damage = 1.0;
                for (AttributeModifier mod : modifiers) {
                    damage += mod.getAmount();
                }
                return damage;
            }
        }

        return switch (item.getType()) {
            case IRON_AXE -> 9.0;
            case IRON_SHOVEL -> 4.5;
            case IRON_PICKAXE -> 4.0;
            case IRON_HOE -> 1.0;
            default -> 1.0;
        };
    }
}