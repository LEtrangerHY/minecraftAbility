package org.core.coreProgram.AbsCoreSystem;

import org.bukkit.entity.Player;

public interface ConfigWrapper {

    void variableReset(Player player);
    void cooldownReset(Player player);
    long getRcooldown(Player player);
    long getQcooldown(Player player);
    long getFcooldown(Player player);

}