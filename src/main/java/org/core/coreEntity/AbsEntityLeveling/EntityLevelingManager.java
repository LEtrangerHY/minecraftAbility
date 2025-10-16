package org.core.coreEntity.AbsEntityLeveling;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class EntityLevelingManager implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey levelKey;

    private final Queue<LivingEntity> spawnQueue = new ConcurrentLinkedQueue<>();
    private final Map<Integer, NamedTextColor> levelColorCache = new HashMap<>();

    public EntityLevelingManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.levelKey = new NamespacedKey(plugin, "entity_level");

        Bukkit.getScheduler().runTaskTimer(plugin, this::processSpawnQueue, 1L, 2L);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity entity = (LivingEntity) event.getEntity();
        EntityType type = entity.getType();

        if (type == EntityType.FALLING_BLOCK || type == EntityType.ITEM
                || type == EntityType.ARMOR_STAND || type == EntityType.VILLAGER
                || type == EntityType.BAT || type == EntityType.SQUID || type == EntityType.GLOW_SQUID)
            return;

        spawnQueue.add(entity);
    }

    private void processSpawnQueue() {
        LivingEntity entity;
        while ((entity = spawnQueue.poll()) != null) {
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

            String readableName = Arrays.stream(entity.getType().name().split("_"))
                    .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                    .reduce((a, b) -> a + " " + b)
                    .orElse(entity.getType().name());

            AttributeInstance healthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
            if (healthAttr != null) {
                double baseHealth = healthAttr.getBaseValue();
                double p = (0.005 * level * level + 0.055 * level) * 1.33;
                double newHealth = baseHealth * (1 + p);
                healthAttr.setBaseValue(newHealth);
                entity.setHealth(newHealth);
            }

            updateHealthName(entity, readableName, level, color);
        }
    }

    private void updateHealthName(LivingEntity entity, String name, int level, NamedTextColor color) {
        if (!entity.isValid()) return;

        double health = Math.max(0, Math.round(entity.getHealth() * 10.0) / 10.0);
        double maxHealth = Math.round(entity.getAttribute(Attribute.MAX_HEALTH).getValue() * 10.0) / 10.0;

        Component newName = Component.text("[Lv." + level + "] " + name + " " + health + "/" + maxHealth + "❤", color);
        entity.customName(newName);
        entity.setCustomNameVisible(true);
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

        PersistentDataContainer data = attacker.getPersistentDataContainer();
        if (!data.has(levelKey, PersistentDataType.INTEGER)) return;

        int level = data.get(levelKey, PersistentDataType.INTEGER);
        double p = (0.005 * level * level + 0.055 * level);
        double originalDamage = event.getDamage();
        double amplifiedDamage = (originalDamage * (1 + p) >= 20)
                ? originalDamage * (1 + p)
                : originalDamage * (1 + p) * 1.66;

        event.setDamage(amplifiedDamage);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        PersistentDataContainer data = deadEntity.getPersistentDataContainer();
        if (!data.has(levelKey, PersistentDataType.INTEGER)) return;

        int level = data.get(levelKey, PersistentDataType.INTEGER);
        int baseExp = event.getDroppedExp();
        event.setDroppedExp(baseExp * (level + 1));
    }
}
