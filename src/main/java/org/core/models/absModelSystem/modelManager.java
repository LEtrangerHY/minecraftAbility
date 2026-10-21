package org.core.models.absModelSystem;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class modelManager {

    private static final Set<Entity> entities = new HashSet<>();
    private static final Map<Location, BlockData> blocks = new LinkedHashMap<>();

    private modelManager() {}

    public static <T extends Entity> T track(T entity) {
        return track(entity, false);
    }

    public static <T extends Entity> T track(T entity, boolean keepPersistent) {
        if (entity == null) return null;
        if (!keepPersistent) entity.setPersistent(false);
        if (entities.size() > 256) entities.removeIf(e -> !e.isValid());
        entities.add(entity);
        return entity;
    }

    public static void trackBlock(Block block) {
        trackBlock(block, block.getBlockData());
    }

    public static void trackBlock(Block block, BlockData original) {
        blocks.putIfAbsent(block.getLocation(), original.clone());
    }

    public static void untrackBlock(Block block) {
        blocks.remove(block.getLocation());
    }

    public static void cleanupAll() {
        for (Entity entity : new ArrayList<>(entities)) {
            if (entity.isValid()) entity.remove();
        }
        entities.clear();

        List<Map.Entry<Location, BlockData>> entries = new ArrayList<>(blocks.entrySet());
        for (int i = entries.size() - 1; i >= 0; i--) {
            Map.Entry<Location, BlockData> entry = entries.get(i);
            Location loc = entry.getKey();
            if (loc.getWorld() == null) continue;
            loc.getBlock().setBlockData(entry.getValue(), false);
        }
        blocks.clear();
    }
}
