package org.core.coreSystem.cores.VOL2.Claud.coreSystem;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class Claud {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public HashMap<UUID, Boolean> collision = new HashMap<>();

    //R
    public double r_Skill_amp = 0.16;
    public double r_Skill_dash = 1.6;
    public double r_Skill_Damage = 4;
    public long r_Skill_Cool = 6000;

    //Q
    public double q_Skill_amp = 0.16;
    public double q_Skill_dash_f = 2.0;
    public double q_Skill_dash_b = 1.4;
    public double q_Skill_Damage = 4;
    public long q_Skill_Cool = 12000;

    //F
    public long f_Skill_Cool = 30000;


    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

    }
}
