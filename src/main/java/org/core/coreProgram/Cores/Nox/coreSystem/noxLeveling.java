package org.core.coreProgram.Cores.Nox.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.core.Level.Levels;

public class noxLeveling implements Levels {

    private final Player player;
    private final long exp;

    public noxLeveling(Player player, long exp) {
        this.player = player;
        this.exp = exp;
    }

    @Override
    public void addLV(Entity entity){}

    @Override
    public void addExp(Entity entity){
        player.sendMessage("get exp : " + exp);
    }
}
