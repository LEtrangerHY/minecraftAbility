package org.core.coreProgram.Cores.Knight.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.Knight.coreSystem.Knight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class R implements SkillBase {
    private final Knight config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R(Knight config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        if(config.swordCount.getOrDefault(player.getUniqueId(), 0) < 3) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);

            Entity target = getTargetedEntity(player, 17, 0.3);
            if (target == null) return;

            config.swordCount.put(player.getUniqueId(), config.swordCount.getOrDefault(player.getUniqueId(), 0)+1);
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().clone().add(0, 1, 0), 49, 0.4, 0.4, 0.4, 1);
            player.sendActionBar(Component.text(config.swordCount.getOrDefault(player.getUniqueId(), 0)).color(NamedTextColor.BLACK));
            TripleSword(player, target);

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
            ItemStack offHand = player.getInventory().getItemInOffHand();
            ItemMeta meta = offHand.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.Damageable && offHand.getType().getMaxDurability() > 0) {
                org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;
                int newDamage = damageable.getDamage() + 1;
                damageable.setDamage(newDamage);
                offHand.setItemMeta(meta);

                if (newDamage >= offHand.getType().getMaxDurability()) {
                    player.getInventory().setItemInOffHand(null);
                }
            }

        }else{
            player.sendActionBar(Component.text("Can use 3 times").color(NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
            long cools = 100L;
            cool.updateCooldown(player, "R", cools);
        }
    }

    public void TripleSword(Player player, Entity target) {
        if (target == null || target.isDead()) return;

        final World world = target.getWorld();
        final int swordCount = 3;
        final double radius = 7;
        final double yOffset = 0.3;
        final double projectileSpeed = 1.0;
        final double damage = config.R_Skill_Damage;

        final List<ArmorStand> swords = new ArrayList<>();
        final List<Double> baseAngles = new ArrayList<>();
        final List<Location> prevHiltLocations = new ArrayList<>();
        Particle.DustOptions dustOption = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 0.7f);
        Particle.DustOptions dustOption_gra = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.7f);

        Location center = target.getLocation();

        world.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1, 1);

        for (int i = 0; i < swordCount; i++) {
            double angle = 2 * Math.PI / swordCount * i;
            baseAngles.add(angle);

            double x = center.getX() + radius * Math.cos(angle);
            double y = center.getY() + yOffset;
            double z = center.getZ() + radius * Math.sin(angle);

            Location swordLoc = new Location(world, x, y, z);

            ArmorStand stand = (ArmorStand) world.spawnEntity(swordLoc, EntityType.ARMOR_STAND);
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, swordLoc, 21, 0.4, 0.4, 0.4, 0);

            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setArms(false);
            stand.setBasePlate(false);

            ItemStack swordItem = new ItemStack(Material.DIAMOND_SWORD);
            stand.getEquipment().setItemInMainHand(swordItem);

            stand.setLeftArmPose(new EulerAngle(0, 0, Math.toRadians(90)));

            swords.add(stand);
            prevHiltLocations.add(swordLoc.clone());
        }

        new BukkitRunnable() {
            double orbitAngle = 0;
            int ticks = 0;

            @Override
            public void run() {
                if (target.isDead()) {
                    swords.forEach(ArmorStand::remove);
                    if(config.swordCount.getOrDefault(player.getUniqueId(), 0) > 0) {
                        config.swordCount.put(player.getUniqueId(), config.swordCount.getOrDefault(player.getUniqueId(), 0) - 1);
                        player.sendActionBar(Component.text((3 - config.swordCount.getOrDefault(player.getUniqueId(), 0)) + " set").color(NamedTextColor.GRAY));
                        if (config.swordCount.getOrDefault(player.getUniqueId(), 0) == 0) {
                            config.swordCount.remove(player.getUniqueId());
                        }
                    }
                    cancel();
                    return;
                }

                Location center = target.getLocation();
                orbitAngle += Math.toRadians(1);

                for (int i = 0; i < swords.size(); i++) {
                    double angle = baseAngles.get(i) + orbitAngle;
                    double x = center.getX() + radius * Math.cos(angle);
                    double y = center.getY() + yOffset;
                    double z = center.getZ() + radius * Math.sin(angle);

                    Location newLoc = new Location(world, x, y, z);

                    double dx = center.getX() - x;
                    double dy = (center.getY() + yOffset) - y;
                    double dz = center.getZ() - z;
                    double distXZ = Math.sqrt(dx * dx + dz * dz);

                    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    float pitch = (float) -Math.toDegrees(Math.atan2(dy, distXZ));
                    newLoc.setYaw(yaw);
                    newLoc.setPitch(pitch);

                    Location prevHilt = prevHiltLocations.get(i);
                    if(Math.random() < 0.17) {
                        spawnParticleTrail(world, prevHilt, newLoc, Particle.DUST, dustOption_gra);
                    }else{
                        spawnParticleTrail(world, prevHilt, newLoc, Particle.DUST, dustOption);
                    }

                    swords.get(i).teleport(newLoc);
                    prevHiltLocations.set(i, newLoc.clone());
                }

                ticks++;
                if (ticks >= 20 * 7) {
                    this.cancel();

                    if(config.swordCount.getOrDefault(player.getUniqueId(), 0) > 0) {
                        config.swordCount.put(player.getUniqueId(), config.swordCount.getOrDefault(player.getUniqueId(), 0) - 1);
                        player.sendActionBar(Component.text((3 - config.swordCount.getOrDefault(player.getUniqueId(), 0)) + " set").color(NamedTextColor.GRAY));
                        if (config.swordCount.getOrDefault(player.getUniqueId(), 0) == 0) {
                            config.swordCount.remove(player.getUniqueId());
                        }
                    }
                    for (ArmorStand sword : swords) {
                        launchSwordProjectile(player, sword, target, projectileSpeed, damage, plugin);
                        player.playSound(target.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 1);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnParticleTrail(World world, Location from, Location to, Particle particle, Particle.DustOptions dust) {
        int points = 7;
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            double x = from.getX() + (to.getX() - from.getX()) * t;
            double y = from.getY() + (to.getY() - from.getY()) * t + 1;
            double z = from.getZ() + (to.getZ() - from.getZ()) * t;
            world.spawnParticle(particle, x, y, z, 1, 0, 0, 0, 0, dust);
        }
    }

    private void launchSwordProjectile(Player player, ArmorStand sword, Entity target, double speed, double damage, JavaPlugin plugin) {

        Particle.DustOptions dustOption = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f);
        Particle.DustOptions dustOption_gra = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);

        final Location fixedTargetPos = target.getLocation().clone().add(0, 0.3, 0);

        double amp = config.r_Skill_amplify * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        damage = damage * (1 + amp);

        double finalDamage = damage;
        new BukkitRunnable() {
            Vector dir = null;

            @Override
            public void run() {
                if (!sword.isValid()) {
                    sword.remove();
                    cancel();
                    return;
                }

                if (dir == null) {
                    Vector diff = fixedTargetPos.toVector().subtract(sword.getLocation().toVector());
                    if (diff.lengthSquared() < 0.0001) {
                        sword.remove();
                        cancel();
                        return;
                    }
                    dir = diff.normalize();
                }

                sword.teleport(sword.getLocation().add(dir.clone().multiply(speed)));

                Location swordLoc = sword.getLocation().clone();
                Location hiltLoc = swordLoc.clone().add(0, 0.7, 0);
                if(Math.random() < 0.17) {
                    sword.getWorld().spawnParticle(Particle.DUST, hiltLoc, 1, 0, 0, 0, 0, dustOption_gra);
                }else{
                    sword.getWorld().spawnParticle(Particle.DUST, hiltLoc, 1, 0, 0, 0, 0, dustOption);
                }

                for (Entity e : sword.getNearbyEntities(0.7, 0.7, 0.7)) {
                    if (e != player && e instanceof LivingEntity) {
                        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, e.getLocation().clone().add(0, 1, 0), 21, 0.4, 0.4, 0.4, 1);
                        player.playSound(e.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
                        ForceDamage forceDamage = new ForceDamage((LivingEntity) e, finalDamage);
                        forceDamage.applyEffect(player);
                        PotionEffect darkness = new PotionEffect(PotionEffectType.DARKNESS, 20 * 3, 1, false, false);
                        ((LivingEntity) e).addPotionEffect(darkness);
                        sword.remove();
                        cancel();
                        break;
                    }
                }

                if (sword.getLocation().distanceSquared(fixedTargetPos) > 777) {
                    sword.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
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