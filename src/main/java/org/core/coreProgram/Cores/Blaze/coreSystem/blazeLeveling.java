package org.core.coreProgram.Cores.Blaze.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.core.Level.Levels;

public class blazeLeveling implements Levels {

    private final Player player;
    private final long exp;

    public blazeLeveling(Player player, long exp) {
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
