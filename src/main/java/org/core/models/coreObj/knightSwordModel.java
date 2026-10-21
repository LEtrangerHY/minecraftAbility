package org.core.models.coreObj;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.core.models.absModelSystem.absCoreModel;

public class knightSwordModel extends absCoreModel {

    private final ArmorStand stand;

    public knightSwordModel(World world, Location swordLoc, ItemStack sword) {
        this.stand = addPart(world.spawn(swordLoc, ArmorStand.class));
        stand.setInvisible(true);
        stand.setGravity(false);
        stand.setMarker(true);
        stand.setArms(false);
        stand.setBasePlate(false);
        stand.getEquipment().setItemInMainHand(new ItemStack(sword));
        stand.setLeftArmPose(new EulerAngle(0, 0, Math.toRadians(90)));
    }

    public ArmorStand getStand() {
        return stand;
    }
}
