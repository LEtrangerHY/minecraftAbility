package org.core.coreSystem.cores.VOL2.Rose.coreSystem;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class Rose {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //R
    public double r_Skill_amp = 0.2;
    public double r_Skill_damage = 2;
    public long r_Skill_Cool = 600;

    //Q
    public double q_Skill_dash = 1.5;
    public double q_SKill_amp = 0.2;
    public double q_Skill_damage = 3;
    public long q_Skill_Cool = 500;

    //F
    public double f_Skill_amp = 0.2;
    public double f_Skill_damage = 1;
    public long f_Skill_Cool = 600;

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

    }
}
