package org.core.coreSystem.cores.VOL2.Undead.Skill.DeadlyWeapons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Undead.coreSystem.Undead;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class R_pickaxe implements SkillBase {
    private final Undead config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final NamespacedKey keyR;

    private final Random random = new Random();

    public R_pickaxe(Undead config, JavaPlugin plugin, Cool cool) {
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
            Effect(player);
            cool.setCooldown(player, config.r_Skill_Cool_re, coolKey_re, "boss");
            cool.setCooldown(player, 0L, coolKey);
            cool.setCooldown(player, 0L, "R");
        }
    }

    public void Effect(Player player) {
        World world = player.getWorld();

        Invulnerable invulnerable = new Invulnerable(player, 400);
        invulnerable.applyEffect(player);

        Vector dashDir = player.getLocation().getDirection().normalize().multiply(1.8).setY(0.2);
        player.setVelocity(dashDir);

        world.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.2f, 0.8f);
        world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.2f);

        new BukkitRunnable() {
            int ticks = 0;
            boolean hit = false;

            @Override
            public void run() {
                if (ticks > 8 || player.isDead() || hit) {
                    this.cancel();
                    return;
                }
                world.spawnParticle(Particle.CRIT, player.getLocation().add(0, 1.0, 0), 5, 0.2, 0.2, 0.2, 0.1);
                world.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 0.2, 0), 3, 0.2, 0.1, 0.2, 0.05);

                for (Entity entity : player.getNearbyEntities(1.1, 1.1, 1.1)) {
                    if (entity != player) {
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

        LivingEntity target = getTargetedEntity(player, 4.0, 0.4);

        if (target != null) {
            double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyR, PersistentDataType.LONG, 0L);
            double damage = 4.0 * (1 + amp);

            DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                    .withCausingEntity(player)
                    .withDirectEntity(player)
                    .build();

            world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);

            Location impactLoc = target.getLocation();

            world.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 0.8f, 1.5f);
            world.playSound(impactLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 0.7f);
            world.spawnParticle(Particle.EXPLOSION, impactLoc.add(0, 0.5, 0), 1, 0, 0, 0, 0);

            Material groundMat = impactLoc.clone().subtract(0, 0.5, 0).getBlock().getType();
            if (groundMat.isSolid()) {
                world.spawnParticle(Particle.BLOCK, impactLoc, 30, 0.5, 0.2, 0.5, groundMat.createBlockData());
            }

            ItemStack pickaxeItem = new ItemStack(Material.IRON_PICKAXE);
            world.spawnParticle(Particle.ITEM, impactLoc, 10, 0.3, 0.3, 0.3, 0.1, pickaxeItem);

            ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
            forceDamage.applyEffect(player);

            world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.9f);
            world.playSound(impactLoc, Sound.ITEM_TRIDENT_HIT, 1.0f, 0.8f);

            disarmRandomArmor(target);

        } else {
            world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 1);

            Vector direction = player.getEyeLocation().getDirection().normalize();
            Location impactLoc = player.getEyeLocation().clone().add(direction.multiply(2.0));

            world.playSound(impactLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.8f, 0.7f);

            Material groundMat = impactLoc.clone().subtract(0, 0.5, 0).getBlock().getType();

            ItemStack pickaxeItem = new ItemStack(Material.IRON_PICKAXE);

            if (groundMat.isSolid() && groundMat != Material.AIR) {
                world.spawnParticle(Particle.BLOCK, impactLoc, 30, 0.5, 0.2, 0.5, groundMat.createBlockData());
                world.spawnParticle(Particle.ITEM, impactLoc, 10, 0.3, 0.3, 0.3, 0.1, pickaxeItem);
            } else {
                world.spawnParticle(Particle.ITEM, impactLoc, 15, 0.3, 0.3, 0.3, 0.1, pickaxeItem);
            }

            Title title = Title.title(
                    Component.empty(),
                    Component.text("not designated").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);
        }
    }

    public static LivingEntity getTargetedEntity(Player player, double range, double raySize) {
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        List<LivingEntity> candidates = new ArrayList<>();

        for (Entity entity : world.getNearbyEntities(eyeLocation, range, range, range)) {
            if (!(entity instanceof LivingEntity) || entity.equals(player) || entity.isInvulnerable()) continue;

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

    private void disarmRandomArmor(LivingEntity target) {
        EntityEquipment equipment = target.getEquipment();
        if (equipment == null) return;

        ItemStack[] armorContents = equipment.getArmorContents();
        List<Integer> validSlots = new ArrayList<>();

        for (int i = 0; i < armorContents.length; i++) {
            if (armorContents[i] != null && armorContents[i].getType() != Material.AIR) {
                validSlots.add(i);
            }
        }

        if (!validSlots.isEmpty()) {
            int randomSlotIndex = validSlots.get(random.nextInt(validSlots.size()));

            ItemStack droppedArmor = armorContents[randomSlotIndex].clone();

            armorContents[randomSlotIndex] = null;
            equipment.setArmorContents(armorContents);

            Location dropLoc = target.getLocation().add(0, 1.0, 0);
            Item itemEntity = target.getWorld().dropItem(dropLoc, droppedArmor);

            itemEntity.setPickupDelay(40);

            itemEntity.setVelocity(new Vector(
                    (random.nextDouble() - 0.5) * 0.3,
                    0.4,
                    (random.nextDouble() - 0.5) * 0.3
            ));

            target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 0.8f);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
            target.getWorld().spawnParticle(Particle.ITEM, target.getLocation().add(0, 1.2, 0), 20, 0.3, 0.4, 0.3, 0.1, droppedArmor);
        }
    }
}