package org.core.models.coresObj;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.core.models.absModelSystem.absCoreModel;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class knightSwordModel extends absCoreModel {

    private static final float TIP = 0.665f;

    private final ItemDisplay display;
    private final Quaternionf rotation;
    private final boolean hilt;
    private final float thickness;

    public knightSwordModel(World world, Location location, Material material, float scale, boolean flat, boolean hilt) {
        this(world, location, material, scale, flat, hilt, 1.0f);
    }

    public knightSwordModel(World world, Location location, Material material, float scale, boolean flat, boolean hilt, float thickness) {
        Quaternionf base = new Quaternionf().rotationTo(new Vector3f(1f, 1f, 0f).normalize(), new Vector3f(0f, 0f, 1f));
        this.rotation = new Quaternionf().rotationZ((float) (flat ? Math.PI / 4 : -Math.PI / 4)).mul(base);
        this.hilt = hilt;
        this.thickness = thickness;

        Transformation initial = transformation(rotation, hilt, scale, thickness);
        this.display = addPart(world.spawn(location, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(material));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setTeleportDuration(1);
            d.setTransformation(initial);
        }));
    }

    private static Transformation transformation(Quaternionf rotation, boolean hilt, float scale, float thickness) {
        return new Transformation(
                new Vector3f(0f, 0f, (hilt ? TIP : -TIP) * scale),
                new Quaternionf(rotation),
                new Vector3f(scale, scale, scale * thickness),
                new Quaternionf()
        );
    }

    public void resize(float scale, int ticks) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(ticks);
        display.setTransformation(transformation(rotation, hilt, scale, thickness));
    }

    public void darken() {
        display.setBrightness(new Display.Brightness(0, 0));
    }

    public static double length(float scale) {
        return TIP * 2.0 * scale;
    }

    public ItemDisplay getDisplay() {
        return display;
    }
}