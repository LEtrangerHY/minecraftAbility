package org.core.models.skillObj;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.core.models.absModelSystem.absCoreModel;

public class comBlockModel extends absCoreModel {

    private final FallingBlock block;

    public comBlockModel(World world, Location spawnLoc) {
        this.block = addPersistentPart(world.spawn(spawnLoc, FallingBlock.class, entity -> {
            entity.setBlockData(Material.COMMAND_BLOCK.createBlockData());
            entity.setDropItem(false);
            entity.setHurtEntities(false);
            entity.setGravity(false);
            entity.setPersistent(true);
        }));
    }

    public FallingBlock getBlock() {
        return block;
    }
}
