package org.core.models.coresObj;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.core.models.absModelSystem.absCoreModel;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class adamSwordModel extends absCoreModel {

    private static final float TIP = 0.665f;

    private final ItemDisplay display;
    private final float breadth;
    private final float thickness;

    public adamSwordModel(World world, Location location, Material material, float scale, float breadth, float thickness) {
        this.breadth = breadth;
        this.thickness = thickness;

        Transformation initial = transformation(scale);
        this.display = addPart(world.spawn(location, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(material));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setTeleportDuration(1);
            d.setTransformation(initial);
        }));
    }

    private Transformation transformation(float scale) {
        return new Transformation(
                new Vector3f(0f, 0f, TIP * scale),
                new Quaternionf().rotationZ((float) (Math.PI / 2)).rotateX((float) (Math.PI / 2)),
                new Vector3f(scale * breadth, scale, scale * thickness),
                new Quaternionf().rotationZ((float) (Math.PI / 4))
        );
    }

    public void resize(float scale, int ticks) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(ticks);
        display.setTransformation(transformation(scale));
    }

    public static double length(float scale) {
        return TIP * 2.0 * scale;
    }

    public ItemDisplay getDisplay() {
        return display;
    }
}