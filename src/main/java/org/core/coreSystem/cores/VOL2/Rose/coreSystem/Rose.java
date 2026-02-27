package org.core.coreSystem.cores.VOL2.Rose.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class Rose {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    //passive
    public long frozenCool = 10000;
    public HashMap<UUID, String> atk = new HashMap<>();
    public HashMap<UUID, String> atkType = new HashMap<>();
    public HashMap<UUID, Integer> petals = new HashMap<>();

    //R
    public double r_Skill_amp = 0.1;
    public long r_Skill_Cool = 0;

    //Q
    public HashMap<UUID, Set<Entity>> damaged = new HashMap<>();
    public HashMap<UUID, Boolean> qskill_using = new HashMap<>();
    public double q_Skill_dash = 1.7;
    public double q_Skill_amp = 0.2;
    public double q_Skill_damage = 2;
    public long q_Skill_Cool = 6000;

    //F
    public double f_Skill_amp = 0.1;
    public double f_Skill_damage = 2;
    public long f_Skill_Cool = 28000;

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

    }
}
