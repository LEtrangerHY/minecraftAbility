package org.core.coreSystem.cores.VOL3.Carpenter.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class Carpenter {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive

    //R
    public HashMap<UUID, Boolean> r_damaging = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> r_damaged = new HashMap<>();
    public double r_Skill_amp = 0.12;
    public double r_Skill_damage = 7;
    public long r_Stun = 2400;
    public long r_Skill_Cool = 13000;
    public double r_Skill_dash = 1.5;

    //Q
    public HashMap<UUID, Boolean> q_damaging = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> q_damaged = new HashMap<>();
    public HashMap<UUID, Boolean> q_using = new HashMap<>();
    public HashMap<UUID, Boolean> crash = new HashMap<>();
    public HashMap<UUID, Double> normal_distribution = new HashMap<>();
    public double q_Skill_amp = 0.12;
    public double q_Skill_damage = 7;
    public double q_Skill_jump = 0.7;
    public long q_Skill_Load = 5000;
    public long q_Skill_Cool = 0;

    //F
    public HashMap<UUID, Integer> f_count = new HashMap<>();
    public double f_Skill_amp = 0.2;
    public double f_Skill_damage = 0;
    public double f_Skill_heal = 3;
    public long f_Skill_Cool = 0;

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        r_damaging.remove(player.getUniqueId());
        r_damaged.remove(player.getUniqueId());

        q_damaging.remove(player.getUniqueId());
        q_using.remove(player.getUniqueId());
        crash.remove(player.getUniqueId());
        normal_distribution.remove(player.getUniqueId());

        f_count.remove(player.getUniqueId());
    }
}
