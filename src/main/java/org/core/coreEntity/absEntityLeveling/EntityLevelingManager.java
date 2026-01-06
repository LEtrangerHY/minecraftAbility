package org.core.coreEntity.absEntityLeveling;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class EntityLevelingManager implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey levelKey;

    private final Queue<LivingEntity> spawnQueue = new ConcurrentLinkedQueue<>();
    private final Map<Integer, NamedTextColor> levelColorCache = new HashMap<>();
    private final Map<UUID, Set<UUID>> visibleEntities = new HashMap<>();
    private int playerIndex = 0;

    private static final double NAME_TAG_RADIUS = 6.0;
    private static final double TARGET_RANGE = 22.0;
    private static final double TARGET_RAY_SIZE = 0.5;

    public EntityLevelingManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.levelKey = new NamespacedKey(plugin, "entity_level");

        Bukkit.getScheduler().runTaskTimer(plugin, this::processSpawnQueue, 1L, 2L);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        startNameTagUpdater();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                for (LivingEntity entity : world.getLivingEntities()) {
                    if (isExcludedEntity(entity)) continue;
                    PersistentDataContainer data = entity.getPersistentDataContainer();
                    if (!data.has(levelKey, PersistentDataType.INTEGER)) {
                        spawnQueue.add(entity);
                    }
                }
            }
        }, 40L);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (isExcludedEntity(entity)) return; // 플레이어 제외 포함
        spawnQueue.add(entity);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity e : event.getChunk().getEntities()) {
            if (!(e instanceof LivingEntity entity)) continue;
            if (isExcludedEntity(entity)) continue; // 플레이어 제외 포함
            PersistentDataContainer data = entity.getPersistentDataContainer();
            if (!data.has(levelKey, PersistentDataType.INTEGER)) {
                spawnQueue.add(entity);
            }
        }
    }

    private boolean isExcludedEntity(Entity entity) {
        if (entity instanceof Player) return true;
        EntityType type = entity.getType();
        return type == EntityType.FALLING_BLOCK || type == EntityType.ITEM
                || type == EntityType.ARMOR_STAND || type == EntityType.VILLAGER
                || type == EntityType.BAT || type == EntityType.SQUID
                || type == EntityType.GLOW_SQUID || type == EntityType.BEE;
    }

    private void processSpawnQueue() {
        LivingEntity entity;
        while ((entity = spawnQueue.poll()) != null) {
            if (!entity.isValid()) continue;
            if (entity instanceof Player) continue; // 안전하게 플레이어 제외

            PersistentDataContainer data = entity.getPersistentDataContainer();
            if (data.has(levelKey, PersistentDataType.INTEGER)) continue;

            double y = entity.getLocation().getY();
            double centerY = 64.0;
            double sigma = 64.0;
            double normalized = Math.exp(-Math.pow((y - centerY), 2) / (2 * sigma * sigma));
            int baseLevel = (int) Math.round((1.0 - normalized) * 10.0);
            int jitter = ThreadLocalRandom.current().nextInt(-2, 3);
            int level = Math.max(0, Math.min(10, baseLevel + jitter));

            data.set(levelKey, PersistentDataType.INTEGER, level);

            NamedTextColor color = levelColorCache.computeIfAbsent(level, l -> {
                if (l <= 2) return NamedTextColor.WHITE;
                else if (l <= 5) return NamedTextColor.GREEN;
                else if (l <= 8) return NamedTextColor.GOLD;
                else return NamedTextColor.RED;
            });

            String readableName = getReadableName(entity);

            AttributeInstance healthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
            if (healthAttr != null) {
                double baseHealth = healthAttr.getBaseValue();
                double p = (0.005 * level * level + 0.055 * level) * 1.44;
                double newHealth = baseHealth * (1 + p);
                healthAttr.setBaseValue(newHealth);
                entity.setHealth(newHealth);
            }

            entity.setCustomNameVisible(false);
            updateHealthName(entity, readableName, level, color);
        }
    }

    private void updateHealthName(LivingEntity entity, String name, int level, NamedTextColor color) {
        if (!entity.isValid()) return;
        double health = Math.max(0, Math.round(entity.getHealth() * 10.0) / 10.0);
        double maxHealth = Math.round(Objects.requireNonNull(entity.getAttribute(Attribute.MAX_HEALTH)).getValue() * 10.0) / 10.0;
        Component newName = Component.text("[Lv." + level + "] " + name + " " + health + "/" + maxHealth + "❤", color);
        entity.customName(newName);
    }

    private String getReadableName(LivingEntity entity) {
        return Arrays.stream(entity.getType().name().split("_"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(entity.getType().name());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity entity)) return;
        if (entity instanceof Player) return; // 플레이어 제외
        PersistentDataContainer data = entity.getPersistentDataContainer();
        if (!data.has(levelKey, PersistentDataType.INTEGER)) return;

        int level = data.get(levelKey, PersistentDataType.INTEGER);
        String name = getReadableName(entity);
        NamedTextColor color = levelColorCache.getOrDefault(level, NamedTextColor.WHITE);

        Bukkit.getScheduler().runTaskLater(plugin, () -> updateHealthName(entity, name, level, color), 1L);
    }

    @EventHandler
    public void onHeal(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof LivingEntity entity)) return;
        if (entity instanceof Player) return; // 플레이어 제외
        PersistentDataContainer data = entity.getPersistentDataContainer();
        if (!data.has(levelKey, PersistentDataType.INTEGER)) return;

        int level = data.get(levelKey, PersistentDataType.INTEGER);
        String name = getReadableName(entity);
        NamedTextColor color = levelColorCache.getOrDefault(level, NamedTextColor.WHITE);

        Bukkit.getScheduler().runTaskLater(plugin, () -> updateHealthName(entity, name, level, color), 1L);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;
        if (attacker instanceof Player) return; // 플레이어 제외
        PersistentDataContainer data = attacker.getPersistentDataContainer();
        if (!data.has(levelKey, PersistentDataType.INTEGER)) return;

        int level = data.get(levelKey, PersistentDataType.INTEGER);
        double p = (0.005 * level * level + 0.055 * level);
        double originalDamage = event.getDamage();
        double amplifiedDamage = (originalDamage * (1 + p) >= 20)
                ? originalDamage * (1 + p)
                : originalDamage * (1 + p) * 1.33;

        event.setDamage(amplifiedDamage);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        if (deadEntity instanceof Player) return; // 플레이어 제외
        PersistentDataContainer data = deadEntity.getPersistentDataContainer();
        if (!data.has(levelKey, PersistentDataType.INTEGER)) return;

        int level = data.get(levelKey, PersistentDataType.INTEGER);
        int baseExp = event.getDroppedExp();
        event.setDroppedExp((int) (baseExp * (1 + (Math.pow(level, 1.5f) * 1/3))));
    }

    private void startNameTagUpdater() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
            if (players.isEmpty()) return;

            Player player = players.get(playerIndex % players.size());
            playerIndex++;
            updatePlayerNearbyNameTags(player);
        }, 0L, 2L);
    }

    private void updatePlayerNearbyNameTags(Player player) {
        UUID playerUUID = player.getUniqueId();
        Set<UUID> currentlyVisible = visibleEntities.computeIfAbsent(playerUUID, k -> new HashSet<>());
        Set<UUID> visibleNow = new HashSet<>();

        for (Entity e : player.getNearbyEntities(NAME_TAG_RADIUS, NAME_TAG_RADIUS, NAME_TAG_RADIUS)) {
            if (!(e instanceof LivingEntity entity)) continue;
            if (entity instanceof Player) continue;
            if (!entity.getPersistentDataContainer().has(levelKey, PersistentDataType.INTEGER)) continue;

            visibleNow.add(entity.getUniqueId());
            if (!currentlyVisible.contains(entity.getUniqueId())) {
                entity.setCustomNameVisible(true);
                currentlyVisible.add(entity.getUniqueId());
            }
        }

        LivingEntity targeted = getTargetedEntity(player, TARGET_RANGE, TARGET_RAY_SIZE);
        if (targeted != null) {
            if (!currentlyVisible.contains(targeted.getUniqueId())) {
                targeted.setCustomNameVisible(true);
                currentlyVisible.add(targeted.getUniqueId());
            }
            visibleNow.add(targeted.getUniqueId());
        }

        Iterator<UUID> iter = currentlyVisible.iterator();
        while (iter.hasNext()) {
            UUID entityUUID = iter.next();
            if (!visibleNow.contains(entityUUID)) {
                Entity e = player.getWorld().getEntity(entityUUID);
                if (e instanceof LivingEntity entity) {
                    entity.setCustomNameVisible(false);
                }
                iter.remove();
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
            EntityType type = entity.getType();
            if (type == EntityType.FALLING_BLOCK || type == EntityType.ITEM
                    || type == EntityType.ARMOR_STAND || type == EntityType.VILLAGER
                    || type == EntityType.BAT || type == EntityType.SQUID
                    || type == EntityType.GLOW_SQUID || type == EntityType.BEE)
                continue;

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
