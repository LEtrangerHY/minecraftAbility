package org.core.models.coresObj;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.core.models.absModelSystem.absCoreModel;

public class sctBombModel extends absCoreModel {

    private final ItemDisplay display;

    public sctBombModel(World world, Location spawnLoc) {
        this.display = addPart(world.spawn(spawnLoc, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.OBSERVER));
            d.setTeleportDuration(1);

            Transformation transform = d.getTransformation();
            transform.getScale().set(0.5f, 0.5f, 0.5f);
            d.setTransformation(transform);
        }));
    }

    public ItemDisplay getDisplay() {
        return display;
    }
}
