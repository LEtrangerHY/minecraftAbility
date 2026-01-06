package org.core.coreSystem.cores.VOL2.Burst.coreSystem;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class Burst {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public HashMap<UUID, Boolean> collision = new HashMap<>();

    //R
    public double r_Skill_amp = 0.14;
    public double r_Skill_Damage = 4;
    public long r_Skill_Cool = 600;

    //Q
    public double q_Skill_amp = 0.14;
    public double q_Skill_Jump = 1.6;
    public double q_Skill_dash = 2.6;
    public double q_Skill_Damage = 4;
    public long q_Skill_Cool = 12000;

    //F
    public double f_Skill_amp = 0.2;
    public double f_Skill_Jump = 1.6;
    public double f_Skill_dash = 1.8;
    public double f_Skill_Damage = 4;
    public long f_Skill_Cool = 66000;


    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

    }
}
