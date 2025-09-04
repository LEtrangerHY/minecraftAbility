package org.core.coreProgram.Cores.Dagger.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.core.Level.Levels;

public class dagLeveling implements Levels {

    private final Player player;
    private final long exp;

    public dagLeveling(Player player, long exp) {
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
