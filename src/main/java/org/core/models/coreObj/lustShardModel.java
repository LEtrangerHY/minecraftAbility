package org.core.models.coreObj;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.core.models.absModelSystem.absCoreModel;

import java.util.ArrayList;
import java.util.List;

public class lustShardModel extends absCoreModel {

    private final List<BlockDisplay> displays = new ArrayList<>();

    public lustShardModel(World world, Location spawnLoc, int count) {
        for (int i = 0; i < count; i++) {
            displays.add(addPart(world.spawn(spawnLoc, BlockDisplay.class, entity -> {
                entity.setBlock(Math.random() > 0.5 ? Material.IRON_BLOCK.createBlockData() : Material.NETHERITE_BLOCK.createBlockData());
                entity.setTeleportDuration(1);

                Transformation t = entity.getTransformation();
                t.getScale().set(0.25f, 0.25f, 0.25f);
                entity.setTransformation(t);
            })));
        }
    }

    public List<BlockDisplay> getDisplays() {
        return new ArrayList<>(displays);
    }
}
