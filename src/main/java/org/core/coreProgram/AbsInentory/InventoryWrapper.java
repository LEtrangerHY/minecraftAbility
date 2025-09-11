package org.core.coreProgram.AbsInentory;

import org.bukkit.entity.Player;

public interface InventoryWrapper {

    void InventoryCoreItem(Player player);
    int getRSkillLevel(Player player);
    int getQSkillLevel(Player player);
    int getFSkillLevel(Player player);

}
