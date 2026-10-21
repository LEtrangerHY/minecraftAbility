package org.core.models.absModelSystem;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public abstract class absCoreModel {

    private final List<Entity> parts = new ArrayList<>();

    protected <T extends Entity> T addPart(T entity) {
        modelManager.track(entity);
        parts.add(entity);
        return entity;
    }

    protected <T extends Entity> T addPersistentPart(T entity) {
        modelManager.track(entity, true);
        parts.add(entity);
        return entity;
    }

    public List<Entity> getParts() {
        return Collections.unmodifiableList(parts);
    }

    public boolean isValid() {
        if (parts.isEmpty()) return false;
        for (Entity part : parts) {
            if (!part.isValid()) return false;
        }
        return true;
    }

    public void remove() {
        for (Entity part : parts) {
            if (part.isValid()) part.remove();
        }
        parts.clear();
    }

    protected static BlockDisplay spawnBlockDisplay(World world, Location location, BlockData data,
                                                    Transformation transformation, Consumer<BlockDisplay> extra) {
        return world.spawn(location, BlockDisplay.class, display -> {
            display.setBlock(data);
            display.setTransformation(transformation);
            if (extra != null) extra.accept(display);
        });
    }

    protected static Transformation centered(float sx, float sy, float sz) {
        return new Transformation(
                new Vector3f(-sx / 2f, -sy / 2f, -sz / 2f),
                new Quaternionf(),
                new Vector3f(sx, sy, sz),
                new Quaternionf()
        );
    }
}
