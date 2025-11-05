package org.core.coreProgram.Cores.Blue.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class Blue {
    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public HashMap<UUID, Boolean> collision = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> damaged = new HashMap<>();

    //R
    public long r_Skill_Cool = 10000;

    //Q
    public long q_Skill_Cool = 10000;
    public double q_Skill_amp = 0.13;

    //F
    public long f_Skill_Cool = 4000;
    public double f_Skill_amp = 0.2;


    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

    }
}
