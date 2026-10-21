package org.core.models.coreObj;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.core.models.absModelSystem.absCoreModel;
import org.joml.AxisAngle4f;

public class bambSpearModel extends absCoreModel {

    private final LivingEntity hitbox;
    private final BlockDisplay display;

    public bambSpearModel(World world, Player owner, Location spawnLoc, Location hitboxLoc) {
        Chicken chicken = addPart(world.spawn(hitboxLoc, Chicken.class));
        chicken.setInvisible(true);
        chicken.setAI(false);
        chicken.setGravity(false);
        chicken.setSilent(true);
        chicken.setInvulnerable(true);
        chicken.setCollidable(false);
        chicken.setAdult();
        chicken.addScoreboardTag("bamboo_projectile");
        chicken.setLeashHolder(owner);
        this.hitbox = chicken;

        this.display = addPart(world.spawn(spawnLoc, BlockDisplay.class));
        display.setBlock(Material.BAMBOO.createBlockData());
        display.setTeleportDuration(1);

        Transformation transform = display.getTransformation();
        transform.getScale().set(1.0f, 4.0f, 1.0f);
        transform.getTranslation().set(-0.5f, 0.5f, -1.0f);
        transform.getLeftRotation().set(new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0));
        display.setTransformation(transform);
    }

    public LivingEntity getHitbox() {
        return hitbox;
    }

    public BlockDisplay getDisplay() {
        return display;
    }
}
