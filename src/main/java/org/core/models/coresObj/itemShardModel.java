package org.core.models.coresObj;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.core.models.absModelSystem.absCoreModel;

public class itemShardModel extends absCoreModel {

    private final Item item;

    public itemShardModel(World world, Location location, Material material, Vector velocity, boolean gravity) {
        this.item = addPart(world.dropItem(location, new ItemStack(material)));
        item.setVelocity(velocity);
        item.setPickupDelay(1000);
        item.setGravity(gravity);
    }

    public Item getItem() {
        return item;
    }
}
