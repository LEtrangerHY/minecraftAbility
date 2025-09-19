package org.core.coreProgram.Cores.Knight.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.Knight.coreSystem.Knight;

import java.util.*;

public class Q implements SkillBase {
    private final Knight config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public Q(Knight config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){

        if(!config.isFocusing.getOrDefault(player.getUniqueId(), false)) {

            Entity target = getTargetedEntity(player, 7, 0.3);

            if(target != null && !target.isDead()) {

                if (target instanceof LivingEntity ltarget) {

                    config.isFocusing.put(player.getUniqueId(), true);

                    AttributeInstance maxHealth = ltarget.getAttribute(Attribute.MAX_HEALTH);
                    if (maxHealth == null) return;

                    double originalMax = maxHealth.getBaseValue();

                    player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);
                    cool.setCooldown(player, 2000L, "Focus");
                    Focus(player, ltarget, originalMax);

                }
            }else{
                player.sendActionBar(Component.text("not designated").color(NamedTextColor.BLACK));
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
                long cools = 100L;
                cool.updateCooldown(player, "Q", cools);
            }

        }else if(config.isFocusing.getOrDefault(player.getUniqueId(), false)){
            config.isFocusCancel.put(player.getUniqueId(), true);
        }
    }

    public void Focus(Player player, LivingEntity target, double originalMax) {


        config.isFocusCancel.remove(player.getUniqueId());

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                boolean isCancelled = config.isFocusCancel.getOrDefault(player.getUniqueId(), false);

                if (ticks >= 40 || player.isDead() || isCancelled) {
                    if (!isCancelled) {
                        Slice(player, target, originalMax);
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
                        player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 1, 1);
                        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
                        player.sendActionBar(Component.text("Focus Cancelled").color(NamedTextColor.BLACK));

                        ItemStack mainHand = player.getInventory().getItemInMainHand();
                        ItemMeta meta = mainHand.getItemMeta();
                        if (meta instanceof org.bukkit.inventory.meta.Damageable && mainHand.getType().getMaxDurability() > 0) {
                            org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;
                            int newDamage = damageable.getDamage() + 1;
                            damageable.setDamage(newDamage);
                            mainHand.setItemMeta(meta);

                            if (newDamage >= mainHand.getType().getMaxDurability()) {
                                player.getInventory().setItemInMainHand(null);
                            }
                        }

                        resetTargetMaxHealth(target, originalMax);

                        cool.updateCooldown(player, "Focus", 0L);
                        cool.updateCooldown(player, "Q", 3000L);
                    }

                    config.isFocusing.remove(player.getUniqueId());
                    config.isFocusCancel.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                if (ticks % 10 == 0) {
                    PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS, 10, 2, false, false);
                    player.addPotionEffect(slowness);

                    player.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                            player.getLocation().clone().add(0, 1.3, 0), 14, 0.4, 0.4, 0.4, 1);
                    player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                            target.getLocation().clone().add(0, 1.3, 0), 2, 0.2, 0.2, 0.2, 1);

                    AttributeInstance maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
                    if (maxHealth != null && !target.isDead()) {
                        double currentMax = maxHealth.getBaseValue();
                        double newMax = Math.max(1.0, currentMax - 1);
                        maxHealth.setBaseValue(newMax);

                        if (target.getHealth() > newMax) {
                            target.setHealth(newMax);
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }


    public void Slice(Player player,LivingEntity target, double originalMax) {

        World world = player.getWorld();

        config.q_Skill_Using.put(player.getUniqueId(), true);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= 2 || player.isDead()) {
                    config.damaged.remove(player.getUniqueId());
                    resetTargetMaxHealth(target, originalMax);
                    config.q_Skill_Using.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);

                Slash(player, tick);

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    public void Slash(Player player, int atkType) {

        config.damaged.put(player.getUniqueId(), new HashSet<>());

        player.swingMainHand();
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

        double slashLength = 4.7;
        double maxAngle = Math.toRadians(49);
        long tickDelay = 0L;
        int maxTicks = 3;
        double innerRadius = 2.7;

        double amp = config.q_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_Damage * (1 + amp);

        Location origin = player.getEyeLocation().add(0, 0, 0);
        Vector direction = player.getLocation().getDirection().clone();
        Vector rightAxis = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (ticks >= maxTicks || player.isDead()) {

                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    ItemMeta meta = mainHand.getItemMeta();
                    if (meta instanceof org.bukkit.inventory.meta.Damageable && mainHand.getType().getMaxDurability() > 0) {
                        org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;
                        int newDamage = damageable.getDamage() + 1;
                        damageable.setDamage(newDamage);
                        mainHand.setItemMeta(meta);

                        if (newDamage >= mainHand.getType().getMaxDurability()) {
                            player.getInventory().setItemInMainHand(null);
                        }
                    }

                    this.cancel();
                    return;
                }

                double progress = (ticks + 1) * (maxAngle * 2 / maxTicks) - maxAngle;
                Vector rotatedDir;

                if (atkType == 0) {
                    rotatedDir = direction.clone().setY(0).normalize().rotateAroundY(progress);
                } else {
                    rotatedDir = direction.clone().rotateAroundAxis(rightAxis, progress);
                }

                for (double length = innerRadius; length <= slashLength; length += 0.1) {
                    for (double angle = -maxAngle; angle <= maxAngle; angle += Math.toRadians(1)) {
                        Vector angleDir;
                        if (atkType == 0) {
                            angleDir = rotatedDir.clone().rotateAroundY(angle);
                        } else {
                            angleDir = rotatedDir.clone().rotateAroundAxis(rightAxis, angle);
                        }

                        Vector particleOffset = angleDir.clone().multiply(length);
                        Location particleLocation = origin.clone().add(particleOffset);

                        Particle.DustOptions dustOptions = new Particle.DustOptions(
                                Math.random() < 0.17 ? Color.fromRGB(255, 255, 255) : Color.fromRGB(0, 0, 0),
                                0.7f
                        );
                        world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOptions);

                        for (Entity entity : world.getNearbyEntities(particleLocation, 0.7, 0.7, 0.7)) {
                            if (entity instanceof LivingEntity target && entity != player) {
                                if(!config.damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {
                                    ForceDamage forceDamage = new ForceDamage(target, damage);
                                    forceDamage.applyEffect(player);
                                    target.setVelocity(new Vector(0, 0, 0));
                                    config.damaged.get(player.getUniqueId()).add(entity);
                                }
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, tickDelay, 1L);
    }

    private void resetTargetMaxHealth(LivingEntity target, double originalMax) {
        AttributeInstance maxHealth = target.getAttribute(Attribute.MAX_HEALTH);

        if (maxHealth != null && !target.isDead()) {
            double newMax = Math.max(1.0, originalMax - 1);
            maxHealth.setBaseValue(newMax);

            target.heal(4);

            if (target.getHealth() > newMax) {
                target.setHealth(newMax);
            }
        }
    }

    public static LivingEntity getTargetedEntity(Player player, double range, double raySize) {
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        List<LivingEntity> candidates = new ArrayList<>();

        for (Entity entity : world.getNearbyEntities(eyeLocation, range, range, range)) {
            if (!(entity instanceof LivingEntity) || entity.equals(player)) continue;

            RayTraceResult result = world.rayTraceEntities(
                    eyeLocation, direction, range, raySize, e -> e.equals(entity)
            );

            if (result != null) {
                candidates.add((LivingEntity) entity);
            }
        }

        return candidates.stream()
                .min(Comparator.comparingDouble(Damageable::getHealth))
                .orElse(null);
    }
}
