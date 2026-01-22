package org.core.coreSystem.cores.VOL2.Rose.Passive;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.cores.VOL2.Rose.coreSystem.Rose;
import org.core.effect.crowdControl.ForceDamage;
import org.core.main.coreConfig;

import java.util.*;

public class bloodPetal implements Listener {

    private final Rose config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final coreConfig tag;

    private final NamespacedKey keyId;
    private final NamespacedKey keyHeal;

    public bloodPetal(Rose config, coreConfig tag, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.tag = tag;
        this.keyId = new NamespacedKey(plugin, "bloodPetal");
        this.keyHeal = new NamespacedKey(plugin, "bloodHealAmount");
    }

    public void dropPetal(Player player, Entity entity, double damage){
        World world = player.getWorld();
        Location entityLoc = entity.getLocation().clone().add(0, 1.4, 0);

        ItemStack itemStack = new ItemStack(Material.RED_DYE);
        ItemMeta meta = itemStack.getItemMeta();

        meta.getPersistentDataContainer().set(keyId, PersistentDataType.STRING, UUID.randomUUID().toString());

        double healAmount = damage * 0.44;
        meta.getPersistentDataContainer().set(keyHeal, PersistentDataType.DOUBLE, healAmount);

        meta.lore(List.of(Component.text("Heal: " + String.format("%.2f", healAmount)).color(NamedTextColor.RED)));

        itemStack.setItemMeta(meta);

        Item item = world.dropItem(entityLoc, itemStack);
        item.setPickupDelay(14);
        item.setGravity(true);
    }

    private final Map<UUID, Boolean> crossDirection = new HashMap<>();

    private static final Particle.DustOptions DUST_BLOOD = new Particle.DustOptions(Color.fromRGB(160, 10, 10), 0.5f);
    private static final Particle.DustOptions DUST_DARK = new Particle.DustOptions(Color.fromRGB(60, 0, 0), 0.5f);

    public void Sweep(Player player, double healAmount) {

        long currentLeft = cool.getRemainCooldown(player, "Q");
        long reducedTime = Math.max(0, currentLeft - 1000);
        cool.updateCooldown(player, "Q", reducedTime);

        if (healAmount > 0) {
            double maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
            double currentHealth = player.getHealth();
            player.setHealth(Math.min(maxHealth, currentHealth + healAmount));
        }

        player.setFoodLevel(Math.min(20, player.getFoodLevel() + 1));
        repairHandItem(player, true);
        repairHandItem(player, false);

        boolean isQActive = config.qskill_using.getOrDefault(player.getUniqueId(), false);
        boolean isRightDiagonal = crossDirection.compute(player.getUniqueId(), (k, v) -> v == null || !v);

        double maxAngle;
        double tiltRad;
        double slashLength;
        double innerRadius;
        int fillSteps;
        int maxTicks;

        if (isQActive) {
            maxAngle = Math.toRadians(180);
            double randomTiltDeg = (Math.random() * 80) - 40;
            tiltRad = Math.toRadians(randomTiltDeg);

            slashLength = 3.4;
            innerRadius = 2.0;
            fillSteps = 44;

            maxTicks = 6;

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);
        } else {
            maxAngle = Math.toRadians(77);
            tiltRad = Math.toRadians(isRightDiagonal ? 27 : -27);

            slashLength = 5.0;
            innerRadius = 2.0;
            fillSteps = 27;

            maxTicks = 3;
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_GRASS_STEP, 2.0f, 1.0f);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Location eyeLoc = player.getEyeLocation();
        Vector forward = eyeLoc.getDirection().normalize();
        Vector worldUp = new Vector(0, 1, 0);
        if (Math.abs(forward.dot(worldUp)) > 0.95) worldUp = new Vector(1, 0, 0);
        Vector right = forward.clone().crossProduct(worldUp).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        double cosTilt = Math.cos(tiltRad);
        double sinTilt = Math.sin(tiltRad);

        Vector tiltedRight = right.clone().multiply(cosTilt).add(up.clone().multiply(sinTilt)).normalize();
        Vector tiltedUp = right.clone().multiply(-sinTilt).add(up.clone().multiply(cosTilt)).normalize();

        final double f_slashLength = slashLength;
        final double f_innerRadius = innerRadius;
        final int f_fillSteps = fillSteps;
        final double f_maxAngle = maxAngle;
        final boolean f_isRightDiagonal = isRightDiagonal;

        Set<Entity> localDamagedSet = new HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= maxTicks || player.isDead()) {
                    this.cancel();
                    return;
                }

                double startAngle = f_isRightDiagonal ? f_maxAngle : -f_maxAngle;
                double endAngle = f_isRightDiagonal ? -f_maxAngle : f_maxAngle;

                double progress = (double) ticks / (maxTicks - 1);
                double currentAngle = startAngle + (endAngle - startAngle) * progress;

                double prevProgress = (double) Math.max(0, ticks - 1) / (maxTicks - 1);
                double prevAngle = startAngle + (endAngle - startAngle) * prevProgress;

                double midAngle = (currentAngle + prevAngle) / 2.0;
                double sweepWidth = Math.abs(currentAngle - prevAngle) / 2.0;

                if (ticks == 0) sweepWidth = 0.1;

                double hitThreshold = Math.cos(sweepWidth + 0.15);

                Vector midDir = forward.clone().multiply(Math.cos(midAngle))
                        .add(tiltedRight.clone().multiply(Math.sin(midAngle))).normalize();

                Location origin = eyeLoc.clone().add(0, -0.4, 0);

                for (Entity entity : player.getWorld().getNearbyEntities(origin, f_slashLength, f_slashLength, f_slashLength)) {
                    if (!(entity instanceof LivingEntity target) || entity == player) continue;
                    if (localDamagedSet.contains(entity)) continue;

                    Vector toEntity = target.getEyeLocation().toVector().subtract(origin.toVector());
                    double distSq = toEntity.lengthSquared();

                    if (distSq > (f_slashLength + 0.8) * (f_slashLength + 0.8)) continue;

                    double distFromPlane = Math.abs(toEntity.dot(tiltedUp));
                    if (distFromPlane > 1.5) continue;

                    Vector toEntityPlanar = toEntity.clone().subtract(tiltedUp.clone().multiply(toEntity.dot(tiltedUp))).normalize();

                    double dotProduct = toEntityPlanar.dot(midDir);

                    if (dotProduct >= hitThreshold) {
                        localDamagedSet.add(entity);

                        config.atk.put(player.getUniqueId(), "P");
                        ForceDamage forceDamage = new ForceDamage(target, healAmount, source);
                        forceDamage.applyEffect(player);
                        config.atk.remove(player.getUniqueId());
                        target.setVelocity(new Vector(0,0,0));

                        player.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1.4, 0), 7, 0.3, 0.3, 0.3, Bukkit.createBlockData(Material.REDSTONE_BLOCK));
                    }
                }

                Vector currentDir = forward.clone().multiply(Math.cos(currentAngle))
                        .add(tiltedRight.clone().multiply(Math.sin(currentAngle))).normalize();

                for (double len = 0.5; len <= f_slashLength; len += 0.04) {
                    Vector point = currentDir.clone().multiply(len);
                    Location loc = origin.clone().add(point);
                    if (len > f_innerRadius) {
                        Particle.DustOptions opt = (len > f_slashLength - 1.0) ? DUST_DARK : DUST_BLOOD;
                        player.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, opt);
                    }
                }

                if (ticks > 0) {
                    for(int i = 1; i < f_fillSteps; i++){
                        double subAngle = prevAngle + (currentAngle - prevAngle) * (i / (double) f_fillSteps);
                        Vector subDir = forward.clone().multiply(Math.cos(subAngle))
                                .add(tiltedRight.clone().multiply(Math.sin(subAngle))).normalize();

                        for (double len = f_innerRadius; len <= f_slashLength; len += 0.07) {
                            player.getWorld().spawnParticle(Particle.DUST, origin.clone().add(subDir.clone().multiply(len)), 1, 0,0,0, 0, DUST_DARK);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void repairHandItem(Player player, boolean isMainHand) {

        if(!hasProperItems(player)) return;

        ItemStack item = isMainHand ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();

        if (item.getType() != Material.AIR && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
                if (damageable.getDamage() > 0) {
                    damageable.setDamage(damageable.getDamage() - 1);
                    item.setItemMeta(meta);
                }
            }
        }
    }

    private boolean hasProperItems(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return main.getType() == Material.COPPER_SWORD && off.getType() == Material.COPPER_SWORD;
    }
}