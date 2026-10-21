package org.core.models.skillObj;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.core.models.absModelSystem.absCoreModel;

public class charWallModel extends absCoreModel {

    public static final int COLUMNS = 5;
    public static final int ROWS = 3;

    private static final Transformation CUBE = centered(1f, 1f, 1f);

    private final BlockDisplay[] blocks = new BlockDisplay[COLUMNS * ROWS];

    public charWallModel(World world, Location spawnLoc, BlockData data) {
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = addPart(spawnBlockDisplay(world, spawnLoc, data, CUBE,
                    display -> display.setTeleportDuration(1)));
        }
    }

    public void update(Location center, Vector forward, Vector right, Vector up) {
        float yaw = center.getYaw();
        if (yaw == 0) {
            yaw = (float) Math.toDegrees(Math.atan2(-forward.getX(), forward.getZ()));
        }

        int index = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = -(COLUMNS / 2); col <= COLUMNS / 2; col++) {
                Vector offset = right.clone().multiply(col).add(up.clone().multiply(row));
                Location targetLoc = center.clone().add(offset);

                targetLoc.setYaw(yaw);
                targetLoc.setPitch(0f);

                BlockDisplay display = blocks[index];
                if (display != null && display.isValid()) {
                    display.teleport(targetLoc);
                }
                index++;
            }
        }
    }
}
