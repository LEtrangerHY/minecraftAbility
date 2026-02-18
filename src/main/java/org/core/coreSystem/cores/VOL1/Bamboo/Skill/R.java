package org.core.coreSystem.cores.VOL1.Bamboo.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Bamboo.coreSystem.Bamboo;
import org.core.effect.crowdControl.ForceDamage;
import org.joml.AxisAngle4f;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class R implements SkillBase {

    private final Bamboo config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private final Map<UUID, BambooSpearInfo> spearSessions = new HashMap<>();
    public static final String REDSTONE_KEY = "bamboo_detonator";

    public R(Bamboo config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    public boolean isSessionActive(Player player) {
        return spearSessions.containsKey(player.getUniqueId());
    }

    public boolean isLeashIntact(Player player) {
        BambooSpearInfo info = spearSessions.get(player.getUniqueId());
        return info != null && !info.isLeashBroken;
    }

    public Location retrieveSpearLocation(Player player) {
        BambooSpearInfo info = spearSessions.get(player.getUniqueId());
        if (info != null && info.hitbox != null && info.landed) {
            return info.hitbox.getLocation();
        }
        return null;
    }

    public void forceRemoveSession(Player player) {
        BambooSpearInfo info = spearSessions.remove(player.getUniqueId());
        if (info != null) {
            if (info.trackingTask != null) info.trackingTask.cancel();
            if (info.timeoutTask != null) info.timeoutTask.cancel();

            if (info.hitbox != null) {
                info.hitbox.setLeashHolder(null);
                info.hitbox.remove();
            }
            if (info.display != null) info.display.remove();

            cool.setCooldown(player, 0L, "Bamboo Hit", "boss");
        }
    }

    public void triggerKillReset(Player player) {
        forceRemoveSession(player);
        config.isSpearFlying.remove(player.getUniqueId());

        setItemToBamboo(player);

        cool.updateCooldown(player, "R", 500L);
        config.reloaded.put(player.getUniqueId(), true);

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1.0f, 1.0f);

        Title title = Title.title(
                Component.empty(),
                Component.text("Reset!").color(NamedTextColor.GREEN),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(200))
        );
        player.showTitle(title);
    }

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();
        BambooSpearInfo info = spearSessions.get(uuid);

        if (info != null) {
            if (info.landed) {
                detonateSpear(player, info);
            }
            return;
        }

        if (!config.reloaded.getOrDefault(uuid, false)) {
            reload(player);
        }
        else {
            throwBambooSpear(player);
        }
    }

    private void reload(Player player) {
        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

        if (offhandItem.getType() == Material.IRON_NUGGET && offhandItem.getAmount() > 1) {
            config.reloaded.put(player.getUniqueId(), true);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1, 1);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);

            cool.updateCooldown(player, "R", 300L);

            Title title = Title.title(
                    Component.empty(),
                    Component.text("Loaded").color(NamedTextColor.GREEN),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(500))
            );
            player.showTitle(title);

            offhandItem.setAmount(offhandItem.getAmount() - 1);
        } else {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
            Title title = Title.title(
                    Component.empty(),
                    Component.text("iron needed").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);
            cool.updateCooldown(player, "R", 500L);
        }
    }

    private void throwBambooSpear(Player player) {
        World world = player.getWorld();
        Location eyeLoc = player.getEyeLocation().clone();
        Vector velocity = eyeLoc.getDirection().clone().normalize().multiply(3.0);

        setItemToRedstone(player);

        LivingEntity hitbox = (LivingEntity) world.spawnEntity(eyeLoc.add(0, -0.5, 0), EntityType.CHICKEN);
        hitbox.setInvisible(true);
        hitbox.setAI(false);
        hitbox.setSilent(true);
        hitbox.setInvulnerable(true);
        hitbox.setCollidable(false);
        if (hitbox instanceof Ageable) ((Ageable) hitbox).setAdult();

        hitbox.addScoreboardTag("bamboo_projectile");

        hitbox.setLeashHolder(player);

        BlockDisplay bambooDisplay = (BlockDisplay) world.spawnEntity(eyeLoc, EntityType.BLOCK_DISPLAY);
        bambooDisplay.setBlock(Material.BAMBOO.createBlockData());
        bambooDisplay.setTeleportDuration(1);

        Transformation transform = bambooDisplay.getTransformation();
        transform.getScale().set(1.0f, 4.0f, 1.0f);
        transform.getTranslation().set(0, -1.25f, 0);
        transform.getLeftRotation().set(new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0));
        bambooDisplay.setTransformation(transform);

        player.playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_THROW, 1.2f, 1.2f);
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 0.8f);

        config.isSpearFlying.put(player.getUniqueId(), true);
        config.reloaded.remove(player.getUniqueId());

        BambooSpearInfo info = new BambooSpearInfo(hitbox, bambooDisplay);
        spearSessions.put(player.getUniqueId(), info);

        cool.updateCooldown(player, "R", 500L);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100;

            @Override
            public void run() {
                if (!spearSessions.containsKey(player.getUniqueId()) || !hitbox.isValid()) {
                    cleanupSession(player.getUniqueId(), 0L);
                    cancel();
                    return;
                }

                if (ticks > maxTicks) {
                    cleanupSession(player.getUniqueId(), 0L);
                    cancel();
                    return;
                }

                Location currentLoc = hitbox.getLocation();
                Location centerLoc = currentLoc.clone().add(0, 0.5, 0);

                velocity.setY(velocity.getY() - 0.035);

                if (!info.isLeashBroken && player.getLocation().distance(currentLoc) > 40.0) {
                    hitbox.setLeashHolder(null);
                    info.isLeashBroken = true;
                }

                RayTraceResult ray = world.rayTraceBlocks(centerLoc, velocity, velocity.length(), FluidCollisionMode.NEVER, true);
                Entity hitEntity = null;

                for (Entity e : world.getNearbyEntities(centerLoc.clone().add(velocity), 1.3, 1.3, 1.3)) {
                    if (e != player && e instanceof LivingEntity && e != hitbox && e != bambooDisplay) {
                        hitEntity = e;
                        break;
                    }
                }

                if (hitEntity != null) {
                    handleHit(player, info, (LivingEntity) hitEntity, null, velocity);
                    cancel();
                    return;
                } else if (ray != null && ray.getHitBlock() != null) {
                    Location hitPoint = ray.getHitPosition().toLocation(world);
                    handleHit(player, info, null, hitPoint, velocity);
                    cancel();
                    return;
                }

                hitbox.teleport(currentLoc.add(velocity));
                updateDisplayTransform(bambooDisplay, hitbox.getLocation().clone().add(0, 0.5, 0), velocity);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void handleHit(Player player, BambooSpearInfo info, LivingEntity target, Location blockHitLoc, Vector velocity) {
        World world = player.getWorld();

        config.isSpearFlying.put(player.getUniqueId(), false);
        info.landed = true;

        cool.setCooldown(player, 6000L, "Bamboo Hit", "boss");

        world.playSound(info.hitbox.getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.0f, 1.0f);
        world.playSound(info.hitbox.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 1.5f);

        if (target != null) {
            info.stuckEntity = target;
            info.offset = velocity.clone().normalize().multiply(-0.5);

            double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
            double damage = config.r_Skill_damage * (1 + amp);

            DamageSource source = DamageSource.builder(DamageType.MOB_PROJECTILE)
                    .withCausingEntity(player)
                    .withDirectEntity(info.hitbox)
                    .withDamageLocation(info.hitbox.getLocation())
                    .build();

            ForceDamage forceDamage = new ForceDamage(target, damage, source, false);
            forceDamage.applyEffect(player);

            Vector knockbackDir = target.getLocation().toVector().subtract(player.getLocation().toVector());
            if (knockbackDir.lengthSquared() < 0.01) {
                knockbackDir = player.getLocation().getDirection();
            }
            knockbackDir.normalize().multiply(0.6).setY(0.35);
            target.setVelocity(knockbackDir);

            if (target.isDead()) {
                triggerKillReset(player);
                return;
            }

            info.relativeYaw = target.getLocation().getYaw() - info.hitbox.getLocation().getYaw();
        } else if (blockHitLoc != null) {
            Location stuckLoc = blockHitLoc.clone().subtract(velocity.clone().normalize().multiply(0.8));
            stuckLoc.setDirection(velocity);
            info.hitbox.teleport(stuckLoc);
            updateDisplayTransform(info.display, stuckLoc.clone().add(0, 0.5, 0), velocity);
        }

        info.trackingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cleanupSession(player.getUniqueId(), 0L);
                    cancel();
                    return;
                }

                if (!info.isLeashBroken && player.getLocation().distance(info.hitbox.getLocation()) > 40.0) {
                    info.hitbox.setLeashHolder(null);
                    info.isLeashBroken = true;
                }

                if (info.stuckEntity != null) {
                    if (!info.stuckEntity.isValid() || info.stuckEntity.isDead()) {
                        triggerKillReset(player);
                        cancel();
                        return;
                    }
                    Location targetLoc = info.stuckEntity.getLocation().add(0, info.stuckEntity.getHeight()/2, 0).add(info.offset);
                    info.hitbox.teleport(targetLoc);
                    updateDisplayTransform(info.display, targetLoc.clone().add(0, 0.5, 0), velocity);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        info.timeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupSession(player.getUniqueId(), 500L);
            }
        }.runTaskLater(plugin, 120L);
    }

    private void detonateSpear(Player player, BambooSpearInfo info) {
        UUID uuid = player.getUniqueId();
        World world = player.getWorld();
        Location particleLoc = info.display.getLocation().clone();

        if (info.trackingTask != null) info.trackingTask.cancel();
        if (info.timeoutTask != null) info.timeoutTask.cancel();

        // ★ [수정됨] 중복 아이템 지급 방지를 위해 여기서 호출하지 않음
        // cleanupSession이 마지막에 호출되며 아이템을 복구함

        world.playSound(particleLoc, Sound.ENTITY_GENERIC_EXPLODE, 4.0f, 1.0f);
        world.playSound(particleLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 3.0f, 0.8f);

        world.spawnParticle(Particle.EXPLOSION_EMITTER, particleLoc, 1, 0.0, 0.0, 0.0, 0.0);

        world.spawnParticle(Particle.FLAME, particleLoc, 10, 0.5, 0.5, 0.5, 0.3);
        world.spawnParticle(Particle.LARGE_SMOKE, particleLoc, 10, 0.8, 0.8, 0.8, 0.1);
        world.spawnParticle(Particle.BLOCK, particleLoc, 10, 0.8, 0.8, 0.8, 0.5, Material.BAMBOO.createBlockData());

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_EXPLOSION)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        for (Entity entity : world.getNearbyEntities(particleLoc, 4, 4, 4)) {
            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

            world.spawnParticle(Particle.EXPLOSION, entity.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);

            ForceDamage forceDamage = new ForceDamage((LivingEntity) entity, damage, source, false);
            forceDamage.applyEffect(player);

            Vector direction = entity.getLocation().toVector().subtract(particleLoc.toVector()).normalize().multiply(1.4);
            direction.setY(1.0);
            entity.setVelocity(direction);
        }

        config.reloaded.remove(uuid);
        cleanupSession(uuid, 8000L);
    }

    private void cleanupSession(UUID uuid, long cooldown) {
        BambooSpearInfo info = spearSessions.remove(uuid);
        if (info != null) {
            if (info.trackingTask != null) info.trackingTask.cancel();
            if (info.timeoutTask != null) info.timeoutTask.cancel();

            if (info.hitbox != null) {
                info.hitbox.setLeashHolder(null);
                info.hitbox.remove();
            }
            if (info.display != null) info.display.remove();

            config.isSpearFlying.remove(uuid);

            cool.setCooldown(Objects.requireNonNull(Bukkit.getPlayer(uuid)), 0L, "Bamboo Hit", "boss");

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                setItemToBamboo(player);
                cool.updateCooldown(player, "R", cooldown);
            }
        }
    }

    private void setItemToRedstone(Player player) {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Detonator").color(NamedTextColor.RED));
            NamespacedKey key = new NamespacedKey(plugin, REDSTONE_KEY);
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }

        boolean found = false;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot != null && slot.getType() == Material.BAMBOO) {
                player.getInventory().setItem(i, item);
                found = true;
                break;
            }
        }

        if (!found) {
            if (player.getInventory().getItemInMainHand().getType() == Material.BAMBOO) {
                player.getInventory().setItemInMainHand(item);
            } else {
                player.getInventory().setItemInMainHand(item);
            }
        }
        player.updateInventory();
    }

    private void setItemToBamboo(Player player) {
        ItemStack bamboo = new ItemStack(Material.BAMBOO);
        NamespacedKey key = new NamespacedKey(plugin, REDSTONE_KEY);
        boolean foundDetonator = false;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.REDSTONE && item.hasItemMeta()) {
                if (item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                    player.getInventory().setItem(i, bamboo);
                    foundDetonator = true;
                    break;
                }
            }
        }

        if (!foundDetonator) {
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(bamboo);
            } else {
                player.getInventory().setItemInMainHand(bamboo);
            }
        }
        player.updateInventory();
    }

    private void updateDisplayTransform(BlockDisplay display, Location newLoc, Vector direction) {
        if (direction.lengthSquared() < 0.0001) return;

        newLoc.setDirection(direction);
        display.setRotation(newLoc.getYaw(), newLoc.getPitch());

        Transformation t = display.getTransformation();
        t.getScale().set(1.0f, 4.0f, 1.0f);
        t.getTranslation().set(0, -1.25f, 0);
        t.getLeftRotation().set(new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0));
        display.setTransformation(t);

        display.teleport(newLoc);
    }

    private static class BambooSpearInfo {
        LivingEntity hitbox;
        BlockDisplay display;
        LivingEntity stuckEntity;
        Vector offset;
        boolean landed = false;
        boolean isLeashBroken = false;
        BukkitTask trackingTask;
        BukkitTask timeoutTask;
        float relativeYaw;

        public BambooSpearInfo(LivingEntity hitbox, BlockDisplay display) {
            this.hitbox = hitbox;
            this.display = display;
        }
    }
}