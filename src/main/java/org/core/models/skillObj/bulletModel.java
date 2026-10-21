package org.core.models.skillObj;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.core.models.absModelSystem.absCoreModel;

public class bulletModel extends absCoreModel {

    private final BlockDisplay display;

    public bulletModel(World world, Location spawnLoc, BlockData data, float scaleX, float scaleY, float scaleZ) {
        this.display = addPart(world.spawn(spawnLoc, BlockDisplay.class, entity -> {
            entity.setBlock(data);
            entity.setTeleportDuration(1);

            Transformation transform = entity.getTransformation();
            transform.getScale().set(scaleX, scaleY, scaleZ);
            transform.getTranslation().set(0f, 0f, 0f);
            entity.setTransformation(transform);
        }));
    }

    public BlockDisplay getDisplay() {
        return display;
    }
}
