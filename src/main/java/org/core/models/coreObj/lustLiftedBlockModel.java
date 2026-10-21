package org.core.models.coreObj;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.core.models.absModelSystem.absCoreModel;

public class lustLiftedBlockModel extends absCoreModel {

    private final BlockDisplay display;

    public lustLiftedBlockModel(World world, Location spawnLoc, BlockData data, NamespacedKey summonedKey) {
        this.display = addPart(world.spawn(spawnLoc, BlockDisplay.class, entity -> {
            entity.setBlock(data);
            entity.setTeleportDuration(1);
            Transformation t = entity.getTransformation();
            t.getTranslation().set(-0.5f, -0.5f, -0.5f); // 회전 중심 맞춤
            entity.setTransformation(t);
            if (summonedKey != null) {
                entity.getPersistentDataContainer().set(summonedKey, PersistentDataType.BYTE, (byte) 1);
            }
        }));
    }

    public BlockDisplay getDisplay() {
        return display;
    }
}
