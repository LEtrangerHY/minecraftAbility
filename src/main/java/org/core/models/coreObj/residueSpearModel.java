package org.core.models.coreObj;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.core.models.absModelSystem.absCoreModel;

public class residueSpearModel extends absCoreModel {

    private final ArmorStand stand;

    public residueSpearModel(World world, Location spawnLoc) {
        this.stand = addPart(world.spawn(spawnLoc, ArmorStand.class));
        stand.setInvisible(true);
        stand.setGravity(false);
        stand.setArms(true);
        stand.setMarker(true);
        stand.setBasePlate(false);
        stand.setSmall(false);

        try {
            stand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SPEAR));
        } catch (NoSuchFieldError e) {
            stand.getEquipment().setItemInMainHand(new ItemStack(Material.TRIDENT));
        }
    }

    public ArmorStand getStand() {
        return stand;
    }
}
