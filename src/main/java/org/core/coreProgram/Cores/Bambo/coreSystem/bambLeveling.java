package org.core.coreProgram.Cores.Bambo.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.core.Level.Levels;

public class bambLeveling implements Levels {

    private final Player player;
    private final long exp;

    public bambLeveling(Player player, long exp) {
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
