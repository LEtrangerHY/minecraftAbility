package org.core.coreSystem.cores.VOL3.Residue.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL3.Residue.coreSystem.Residue;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Grounding;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Q implements SkillBase {
    private final Residue config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private static final Map<UUID, SpearInfo> spearSessions = new HashMap<>();
    private static final Particle.DustOptions DUST_CHAIN = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.6f);

    public static final String PEARL_KEY = "residue_pearl";

    public Q(Residue config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    public static boolean isSessionActive(Player player) {
        return spearSessions.containsKey(player.getUniqueId());
    }

    public static boolean isLanded(Player player) {
        SpearInfo info = spearSessions.get(player.getUniqueId());
        return info != null && info.landed;
    }

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();
        SpearInfo info = spearSessions.get(uuid);

        if (info != null) {
            if (info.landed) {
                if (cool.isReloading(player, "Q Reuse")) {
                    teleportToSpear(player);
                } else {
                    cleanupSession(uuid, 500L);
                }
            }
        } else {
            throwSpear(player);
        }
    }

    private void throwSpear(Player player) {
        World world = player.getWorld();
        Location eyeLoc = player.getEyeLocation().clone();

        Vector velocity = eyeLoc.getDirection().clone().normalize().multiply(3.3);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            setItemToPearl(player);
        }, 1L);

        ArmorStand spearStand = (ArmorStand) world.spawnEntity(eyeLoc.add(0, -1.2, 0), EntityType.ARMOR_STAND);
        spearStand.setInvisible(true);
        spearStand.setGravity(false);
        spearStand.setArms(true);
        spearStand.setMarker(true);
        spearStand.setBasePlate(false);
        spearStand.setSmall(false);

        try {
            spearStand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SPEAR));
        } catch (NoSuchFieldError e) {
            spearStand.getEquipment().setItemInMainHand(new ItemStack(Material.TRIDENT));
        }

        updateSpearRotation(spearStand, velocity);

        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.2f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_THROW, 0.5f, 1.3f);

        config.isSpearFlying.put(player.getUniqueId(), true);

        SpearInfo info = new SpearInfo(spearStand);
        spearSessions.put(player.getUniqueId(), info);

        cool.updateCooldown(player, "Q", 500L);

        new BukkitRunnable() {
            int ticks = 0;
            final double gravity = 0.055;
            final int maxTicks = 100;

            @Override
            public void run() {
                if (!spearSessions.containsKey(player.getUniqueId()) || !spearStand.isValid()) {
                    removeSpearAndCooldown(player, spearStand, this, 6000L);
                    return;
                }

                if (ticks > maxTicks) {
                    removeSpearAndCooldown(player, spearStand, this, 6000L);
                    return;
                }

                Location currentLoc = spearStand.getLocation();
                Location headLoc = currentLoc.clone().add(0, 1.2, 0);

                velocity.setY(velocity.getY() - gravity);
                velocity.multiply(0.99);

                RayTraceResult ray = world.rayTraceBlocks(headLoc, velocity, velocity.length(), FluidCollisionMode.NEVER, true);

                Entity hitEntity = null;
                Location searchLoc = headLoc.clone().add(velocity.clone().multiply(0.5));
                for (Entity e : world.getNearbyEntities(searchLoc, 1.2, 1.2, 1.2)) {
                    if (e != player && e instanceof LivingEntity && e != spearStand) {
                        hitEntity = e;
                        break;
                    }
                }

                if (hitEntity != null) {
                    handleHitEntity(player, info, (LivingEntity) hitEntity, velocity);
                    cancel();
                    return;
                } else if (ray != null && ray.getHitBlock() != null) {
                    Location hitPoint = ray.getHitPosition().toLocation(world);
                    handleHitBlock(player, info, hitPoint, velocity);
                    cancel();
                    return;
                }

                updateSpearRotation(spearStand, velocity);
                spearStand.teleport(currentLoc.add(velocity));

                world.spawnParticle(Particle.CRIT, headLoc, 1, 0, 0, 0, 0);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void handleHitEntity(Player player, SpearInfo info, LivingEntity target, Vector velocity) {
        long level = player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_Damage * (1 + config.q_Skill_amp * level);

        DamageSource source = DamageSource.builder(DamageType.MOB_PROJECTILE)
                .withCausingEntity(player)
                .withDirectEntity(info.spear)
                .withDamageLocation(info.spear.getLocation())
                .build();
        new ForceDamage(target, damage, source, false).applyEffect(player);
        new Grounding(target, 2000).applyEffect(player);

        target.setVelocity(new Vector(0, 0, 0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false));

        player.playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1, 1);
        player.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.6f, 1.0f);
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.2f, 1.0f);

        info.stuckEntity = target;
        info.landed = true;

        Location targetLoc = target.getLocation();
        Location targetCenter = targetLoc.clone().add(0, target.getHeight() / 2.0, 0);

        Location idealHeadLoc = targetCenter.clone().subtract(velocity.clone().normalize().multiply(0.7));
        Location idealOrigin = idealHeadLoc.clone().subtract(0, 1.2, 0);
        idealOrigin.setDirection(velocity);

        info.stuckOffset = idealOrigin.toVector().subtract(targetLoc.toVector());
        info.initialEntityYaw = targetLoc.getYaw();
        info.initialSpearYaw = idealOrigin.getYaw();
        info.originalArmPose = info.spear.getRightArmPose();

        updateSpearRotation(info.spear, velocity);
        info.spear.teleport(idealOrigin);

        chain_qSkill_Particle_Effect(player, info.spear, 40);
        cool.setCooldown(player, 6000L, "Q Reuse", "boss");

        if (target.isDead()) {
            triggerKillReset(player);
            return;
        }

        info.trackingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cleanupSession(player.getUniqueId(), 500L);
                    cancel();
                    return;
                }

                if (!info.stuckEntity.isValid() || info.stuckEntity.isDead()) {
                    triggerKillReset(player);
                    cancel();
                    return;
                }

                Location currentTargetLoc = info.stuckEntity.getLocation();
                float yawDelta = currentTargetLoc.getYaw() - info.initialEntityYaw;

                Vector rotatedOffset = info.stuckOffset.clone();
                rotatedOffset.rotateAroundY(Math.toRadians(-yawDelta));

                Location newSpearLoc = currentTargetLoc.clone().add(rotatedOffset);
                newSpearLoc.setYaw(info.initialSpearYaw + yawDelta);

                info.spear.teleport(newSpearLoc);
                info.spear.setRightArmPose(info.originalArmPose);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        info.timeoutTask = new BukkitRunnable() {
            @Override
            public void run() { cleanupSession(player.getUniqueId(), 500L); }
        }.runTaskLater(plugin, 120L);
    }

    private void handleHitBlock(Player player, SpearInfo info, Location hitPoint, Vector velocity) {
        player.playSound(hitPoint.clone().add(0, 1.5, 0), Sound.ITEM_TRIDENT_HIT_GROUND, 1.2f, 0.8f);
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.2f, 0.8f);

        Location idealHeadLoc = hitPoint.clone().subtract(velocity.clone().normalize().multiply(0.7));
        Location idealOrigin = idealHeadLoc.clone().subtract(0, 1.2, 0);
        idealOrigin.setDirection(velocity);

        updateSpearRotation(info.spear, velocity);
        info.spear.teleport(idealOrigin);

        info.landed = true;
        chain_qSkill_Particle_Effect(player, info.spear, 120);
        cool.setCooldown(player, 6000L, "Q Reuse", "boss");

        info.timeoutTask = new BukkitRunnable() {
            @Override
            public void run() { cleanupSession(player.getUniqueId(), 500L); }
        }.runTaskLater(plugin, 120L);
    }

    private void updateSpearRotation(ArmorStand stand, Vector velocity) {
        if (velocity.lengthSquared() < 0.001) return;
        Location dirLoc = stand.getLocation().clone();
        dirLoc.setDirection(velocity);
        stand.setRotation(dirLoc.getYaw(), 0);
        stand.setRightArmPose(new EulerAngle(Math.toRadians(dirLoc.getPitch()), 0, 0));
    }

    private void teleportToSpear(Player player) {
        UUID uuid = player.getUniqueId();
        SpearInfo info = spearSessions.get(uuid);
        if (info == null || !info.spear.isValid()) return;

        Location spearLoc = info.spear.getLocation().add(0, 1.5, 0);

        double distance = player.getLocation().distance(spearLoc);

        long calculatedCooldown = 12000L + (long)(32.1 * distance * distance);
        long finalCooldown = Math.min(333000L, calculatedCooldown);

        player.teleport(spearLoc);
        player.getWorld().playSound(spearLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);

        cleanupSession(uuid, finalCooldown);
    }

    private void triggerKillReset(Player player) {
        cleanupSession(player.getUniqueId(), 0L);

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1.0f, 1.0f);

        Title title = Title.title(
                Component.empty(),
                Component.text("Reset!").color(NamedTextColor.GREEN),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(200))
        );
        player.showTitle(title);
    }

    private void cleanupSession(UUID uuid, long cooldownTime) {
        SpearInfo info = spearSessions.remove(uuid);

        if (info != null) {
            if (info.timeoutTask != null && !info.timeoutTask.isCancelled()) info.timeoutTask.cancel();
            if (info.trackingTask != null && !info.trackingTask.isCancelled()) info.trackingTask.cancel();
            if (info.spear != null) info.spear.remove();

            config.isSpearFlying.remove(uuid);

            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    setItemToSpear(player);
                }, 1L);
                cool.updateCooldown(player, "Q Reuse", 0L, "boss");
                cool.updateCooldown(player, "Q", cooldownTime);
            }
        }
    }

    private void removeSpearAndCooldown(Player player, ArmorStand spear, BukkitRunnable task, long cooldownTime) {
        if(spear != null) spear.remove();
        if(task != null) task.cancel();
        cleanupSession(player.getUniqueId(), cooldownTime);
    }

    private void setItemToPearl(Player player) {
        ItemStack pearl = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = pearl.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Teleport").color(NamedTextColor.LIGHT_PURPLE));
            NamespacedKey key = new NamespacedKey(plugin, PEARL_KEY);
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            pearl.setItemMeta(meta);
        }
        player.getInventory().setItemInMainHand(pearl);
        player.updateInventory();
    }

    private void setItemToSpear(Player player) {
        ItemStack spear;
        try { spear = new ItemStack(Material.valueOf("IRON_SPEAR")); }
        catch (IllegalArgumentException | NoSuchFieldError e) { spear = new ItemStack(Material.TRIDENT); }

        NamespacedKey key = new NamespacedKey(plugin, PEARL_KEY);
        boolean found = false;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.ENDER_PEARL && item.hasItemMeta()) {
                if (item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                    player.getInventory().setItem(i, spear);
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand.getType() == Material.ENDER_PEARL && offhand.hasItemMeta()) {
                if (offhand.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                    player.getInventory().setItemInOffHand(spear);
                    found = true;
                }
            }
        }

        if (!found) player.getInventory().setItemInMainHand(spear);
        player.updateInventory();
    }

    public void chain_qSkill_Particle_Effect(Player player, Entity entity, int time) {
        World world = player.getWorld();

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > time || !entity.isValid()) { cancel(); return; }

                Location baseLoc;
                if (entity instanceof ArmorStand) {
                    baseLoc = entity.getLocation().add(0, 0.5, 0);
                } else {
                    baseLoc = entity.getLocation();
                }

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

    private static class SpearInfo {
        ArmorStand spear;
        LivingEntity stuckEntity;

        Vector stuckOffset;
        float initialEntityYaw;
        float initialSpearYaw;
        EulerAngle originalArmPose;

        boolean landed = false;
        BukkitTask trackingTask;
        BukkitTask timeoutTask;

        public SpearInfo(ArmorStand spear) {
            this.spear = spear;
        }
    }
}