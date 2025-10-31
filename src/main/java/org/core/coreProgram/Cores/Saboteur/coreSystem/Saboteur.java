package org.core.coreProgram.Cores.Saboteur.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class Saboteur {
    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public HashMap<UUID, Integer> trapType = new HashMap<>();
    public HashMap<UUID, Boolean> isHackAway = new HashMap<>();
    public HashMap<UUID, Boolean> collision = new HashMap<>();

    public HashMap<UUID, Boolean> skillUsing = new HashMap<>();

    //R
    public long r_Skill_Cool = 500;

    //Q
    public long q_Skill_Cool = 4000;

    //F
    public long f_Skill_Cool = 44440;
    public double f_Skill_amp = 0.2;


    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

    }
}
