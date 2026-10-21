package org.core.models.skillObj;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.core.models.absModelSystem.absCoreModel;

import java.util.ArrayList;
import java.util.List;

public class lustSphereModel extends absCoreModel {

    private static final BlockData IRON = Material.IRON_BLOCK.createBlockData();

    private final List<BlockDisplay> displays = new ArrayList<>();

    public lustSphereModel(World world, Location spawnLoc, int count) {
        for (int i = 0; i < count; i++) {
            displays.add(addPart(world.spawn(spawnLoc, BlockDisplay.class, entity -> {
                entity.setBlock(IRON);
                entity.setTeleportDuration(1);
                Transformation transform = entity.getTransformation();
                transform.getScale().set(0.35f, 0.35f, 0.35f);
                transform.getTranslation().set(-0.175f, -0.175f, -0.175f);
                entity.setTransformation(transform);
            })));
        }
    }

    public List<BlockDisplay> getDisplays() {
        return new ArrayList<>(displays);
    }
}
