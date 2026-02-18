package org.core.coreSystem.cores.VOL3.Residue.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

        SpearInfo initialInfo = new SpearInfo(spearStand, null, null);
        initialInfo.landed = false;
        spearSessions.put(player.getUniqueId(), initialInfo);

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
                    handleHitEntity(player, spearStand, (LivingEntity) hitEntity, velocity.clone().normalize());
                    cancel();
                    return;
                } else if (ray != null && ray.getHitBlock() != null) {
                    Location hitPoint = ray.getHitPosition().toLocation(world);
                    Vector pullback = velocity.clone().normalize().multiply(0.7);
                    Location hitLoc = hitPoint.subtract(pullback);

                    hitLoc.subtract(0, 1.2, 0);

                    handleHitBlock(player, spearStand, hitLoc, velocity);
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
        try { spear = new ItemStack(Material.IRON_SPEAR); }
        catch (NoSuchFieldError e) { spear = new ItemStack(Material.TRIDENT); }

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

    private void updateSpearRotation(ArmorStand stand, Vector velocity) {
        if (velocity.lengthSquared() < 0.001) return;
        Location dirLoc = stand.getLocation().clone();
        dirLoc.setDirection(velocity);
        stand.setRotation(dirLoc.getYaw(), 0);
        stand.setRightArmPose(new EulerAngle(Math.toRadians(dirLoc.getPitch()), 0, 0));
    }

    private void handleHitEntity(Player player, ArmorStand spear, LivingEntity target, Vector direction) {
        long level = player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
        double damage = config.q_Skill_Damage * (1 + config.q_Skill_amp * level);

        DamageSource source = DamageSource.builder(DamageType.MOB_PROJECTILE)
                .withCausingEntity(player)
                .withDirectEntity(spear)
                .withDamageLocation(spear.getLocation())
                .build();
        new ForceDamage(target, damage, source, false).applyEffect(player);
        new Grounding(target, 2000).applyEffect(player);

        target.setVelocity(new Vector(0, 0, 0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false));

        chain_qSkill_Particle_Effect(player, target, 40);

        player.playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1, 1);
        player.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.6f, 1.0f);

        updateSessionToLanded(player, spear, target, direction.clone().multiply(-0.25));
    }

    private void handleHitBlock(Player player, ArmorStand spear, Location hitLoc, Vector velocity) {
        player.playSound(hitLoc.clone().add(0, 1.5, 0), Sound.ITEM_TRIDENT_HIT_GROUND, 1.2f, 0.8f);
        updateSpearRotation(spear, velocity);
        hitLoc.setYaw(spear.getLocation().getYaw());
        hitLoc.setPitch(0);
        spear.teleport(hitLoc);

        chain_qSkill_Particle_Effect(player, spear, 120);

        updateSessionToLanded(player, spear, null, null);
    }

    private void updateSessionToLanded(Player player, ArmorStand spear, LivingEntity stuckEntity, Vector offset) {
        UUID uuid = player.getUniqueId();
        config.isSpearFlying.put(uuid, false);

        SpearInfo info = spearSessions.get(uuid);
        if (info == null) return;

        info.landed = true;
        info.stuckEntity = stuckEntity;
        info.offset = offset;
        info.finalYaw = spear.getLocation().getYaw();
        info.finalArmPose = spear.getRightArmPose();

        cool.setCooldown(player, 6000L, "Q Reuse", "boss");

        info.timeoutTask = new BukkitRunnable() {
            @Override
            public void run() { cleanupSession(uuid, 500L); }
        }.runTaskLater(plugin, 20L * 6);

        if (stuckEntity != null) {
            info.trackingTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!stuckEntity.isValid() || stuckEntity.isDead()) {
                        cleanupSession(uuid, 500L);
                        cancel();
                        return;
                    }
                    Location targetLoc = stuckEntity.getLocation().add(0, stuckEntity.getHeight() / 1.8 - 1.5, 0).add(offset);
                    targetLoc.setYaw(info.finalYaw);
                    spear.setRightArmPose(info.finalArmPose);
                    spear.teleport(targetLoc);
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    private void teleportToSpear(Player player) {
        UUID uuid = player.getUniqueId();
        SpearInfo info = spearSessions.get(uuid);
        if (info == null || !info.spear.isValid()) return;

        Location spearLoc = info.spear.getLocation().add(0, 1.5, 0);

        double distance = player.getLocation().distance(spearLoc);
        long calculatedCooldown = 16000L + (long)(distance * 1000L);
        long finalCooldown = Math.min(66000L, calculatedCooldown);

        player.teleport(spearLoc);
        player.getWorld().playSound(spearLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);

        cleanupSession(uuid, finalCooldown);
    }

    private void cleanupSession(UUID uuid, long cooldownTime) {
        SpearInfo info = spearSessions.remove(uuid);

        if (info != null) {
            if (info.timeoutTask != null && !info.timeoutTask.isCancelled()) info.timeoutTask.cancel();
            if (info.trackingTask != null && !info.trackingTask.isCancelled()) info.trackingTask.cancel();
            if (info.spear != null) info.spear.remove();

            config.isSpearFlying.remove(uuid);

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
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
        Vector offset;
        boolean landed;
        float finalYaw;
        EulerAngle finalArmPose;
        @NotNull BukkitTask timeoutTask;
        BukkitTask trackingTask;

        public SpearInfo(ArmorStand spear, LivingEntity stuckEntity, Vector offset) {
            this.spear = spear;
            this.stuckEntity = stuckEntity;
            this.offset = offset;
            this.landed = false;
        }
    }
}