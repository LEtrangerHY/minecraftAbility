package org.core.coreSystem.cores.VOL1.Glacier.coreSystem;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class Glacier {
    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public HashMap<UUID, Boolean> collision = new HashMap<>();

    //R
    public HashMap<UUID, Boolean> Rcollision = new HashMap<>();
    public HashMap<UUID, Boolean> entityCollision = new HashMap<>();
    public long r_Skill_Cool = 600;

    //Q
    public long q_Skill_Cool = 10000;

    //F
    public long f_Skill_Cool = 100000;
    public HashMap<UUID, Boolean> F_collision = new HashMap<>();

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        collision.remove(player.getUniqueId());

        Rcollision.remove(player.getUniqueId());
        entityCollision.remove(player.getUniqueId());

        F_collision.remove(player.getUniqueId());

    }
}
