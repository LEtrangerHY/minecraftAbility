package org.core.models.coresObj;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.util.Transformation;
import org.core.models.absModelSystem.absCoreModel;

public class undeadBombModel extends absCoreModel {

    private static final BlockData LIGHT_ON = Material.REDSTONE_BLOCK.createBlockData();
    private static final BlockData LIGHT_OFF = Material.RED_TERRACOTTA.createBlockData();

    private final BlockDisplay light;
    private boolean lit = false;

    public undeadBombModel(World world, Location spawnLoc) {
        part(world, spawnLoc, Material.BLACK_TERRACOTTA, 0.30f, 0.20f, 0.025f, 0f, 0f, -0.0525f);

        part(world, spawnLoc, Material.GREEN_TERRACOTTA, 0.125f, 0.16f, 0.08f, -0.066f, 0f, 0f);
        part(world, spawnLoc, Material.GREEN_TERRACOTTA, 0.125f, 0.16f, 0.08f, 0.066f, 0f, 0f);

        part(world, spawnLoc, Material.BLACK_CONCRETE, 0.275f, 0.022f, 0.088f, 0f, 0.05f, 0f);
        part(world, spawnLoc, Material.BLACK_CONCRETE, 0.275f, 0.022f, 0.088f, 0f, -0.05f, 0f);

        part(world, spawnLoc, Material.LIGHT_GRAY_TERRACOTTA, 0.10f, 0.075f, 0.035f, 0.035f, 0.01f, 0.0575f);
        part(world, spawnLoc, Material.CYAN_TERRACOTTA, 0.04f, 0.02f, 0.008f, 0.02f, -0.008f, 0.079f);

        part(world, spawnLoc, Material.GRAY_CONCRETE, 0.012f, 0.13f, 0.012f, -0.09f, 0.14f, 0.02f);
        part(world, spawnLoc, Material.BLACK_CONCRETE, 0.02f, 0.02f, 0.02f, -0.09f, 0.21f, 0.02f);

        part(world, spawnLoc, Material.RED_TERRACOTTA, 0.10f, 0.012f, 0.012f, -0.035f, 0.025f, 0.048f);
        part(world, spawnLoc, Material.YELLOW_TERRACOTTA, 0.012f, 0.06f, 0.012f, -0.075f, -0.02f, 0.048f);

        this.light = part(world, spawnLoc, LIGHT_OFF.getMaterial(), 0.028f, 0.028f, 0.02f, 0.06f, 0.025f, 0.08f);
    }

    private BlockDisplay part(World world, Location spawnLoc, Material material, float sx, float sy, float sz,
                              float x, float y, float z) {
        Transformation shape = centered(sx, sy, sz);
        shape.getTranslation().add(x, y, z);
        return addPart(spawnBlockDisplay(world, spawnLoc, material.createBlockData(), shape,
                d -> d.setTeleportDuration(1)));
    }

    public void teleport(Location location) {
        for (Entity part : getParts()) part.teleport(location);
    }

    public boolean toggle() {
        lit = !lit;
        light.setBlock(lit ? LIGHT_ON : LIGHT_OFF);
        light.setBrightness(lit ? new Display.Brightness(15, 15) : null);
        return lit;
    }
}