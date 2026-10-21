package org.core.models.coresObj;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.core.models.absModelSystem.absCoreModel;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class charShieldModel extends absCoreModel {

    private static final BlockData GLASS = Material.WHITE_STAINED_GLASS.createBlockData();
    private static final Transformation PANE = new Transformation(
            new Vector3f(-0.5f, -0.5f, -0.0625f),
            new Quaternionf(),
            new Vector3f(1f, 1f, 0.125f),
            new Quaternionf()
    );

    private final BlockDisplay[] panes = new BlockDisplay[6];

    public charShieldModel(World world, Location spawnLoc) {
        for (int i = 0; i < panes.length; i++) {
            panes[i] = addPart(spawnBlockDisplay(world, spawnLoc, GLASS, PANE, display -> {
                display.setTeleportDuration(1);
                display.setBrightness(new Display.Brightness(15, 15));
            }));
        }
    }

    public void update(Location center, Vector right, Vector up, float yaw, float pitch) {
        int index = 0;
        for (int row = 0; row < 2; row++) {
            for (int col = -1; col <= 1; col++) {
                double upOffset = (row == 0) ? -0.5 : 0.5;
                Vector offset = right.clone().multiply(col).add(up.clone().multiply(upOffset));

                Location displayLoc = center.clone().add(offset);
                displayLoc.setYaw(yaw);
                displayLoc.setPitch(pitch);

                BlockDisplay display = panes[index];
                if (display != null && display.isValid()) {
                    display.teleport(displayLoc);
                }
                index++;
            }
        }
    }
}
