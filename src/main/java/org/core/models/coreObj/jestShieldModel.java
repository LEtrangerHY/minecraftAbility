package org.core.models.coreObj;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.core.models.absModelSystem.absCoreModel;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class jestShieldModel extends absCoreModel {

    private final ItemDisplay display;

    public jestShieldModel(World world, Location spawnLoc, double scale) {
        this.display = addPart(world.spawn(spawnLoc, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.SHIELD));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setTeleportDuration(1);
            d.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf().rotationX((float) (-Math.PI / 2)),
                    new Vector3f((float) scale, (float) scale, (float) scale),
                    new Quaternionf()
            ));
        }));
    }

    public ItemDisplay getDisplay() {
        return display;
    }
}
