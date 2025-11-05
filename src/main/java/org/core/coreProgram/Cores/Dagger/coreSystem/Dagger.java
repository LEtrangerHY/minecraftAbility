package org.core.coreProgram.Cores.Dagger.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class Dagger {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive

    //R
    public HashMap<UUID, Boolean> r_damaged = new HashMap<>();
    public double r_Skill_amp = 0.06;
    public double r_Skill_damage = 6;
    public long r_Skill_Cool = 4000;
    public long r_Stun = 2000;

    //Q
    public double q_Skill_amp = 0.06;
    public double q_Skill_Damage = 3;
    public long q_Skill_Cool = 8000;
    public long q_Skill_Cool_2 = 20000;
    public long q_Skill_Cool_decrease = 4000;
    public double q_Skill_dash = 1.5;
    public double q_Skill_dash_2 = 2.2;
    public HashMap<UUID, HashSet<Entity>> q_damaged = new HashMap<>();

    //F
    public double f_Skill_amp = 0.06;
    public double f_Skill_Damage = 4;
    public double f_Skill_Damage_2 = 13;
    public long f_Skill_Cool = 0;
    public double f_Skill_dash = 1.8;

    public HashMap<UUID, HashSet<Entity>> f_damaged = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> f_damaged_2 = new HashMap<>();
    public HashMap<UUID, Integer> f_slash = new HashMap<>();
    public HashMap<UUID, Boolean> f_using = new HashMap<>();
    public HashMap<UUID, Boolean> f_dash = new HashMap<>();
    public HashMap<UUID, Boolean> f_damaging = new HashMap<>();
    public HashMap<UUID, LinkedHashSet<Entity>> dash_object = new HashMap<>();
    public HashMap<UUID, Integer> dash_num = new HashMap<>();


    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        r_damaged.remove(player.getUniqueId());

        q_damaged.remove(player.getUniqueId());

        f_damaged.remove(player.getUniqueId());
        f_damaged_2.remove(player.getUniqueId());
        f_slash.remove(player.getUniqueId());
        f_using.remove(player.getUniqueId());
        f_dash.remove(player.getUniqueId());
        f_damaging.remove(player.getUniqueId());
        dash_object.remove(player.getUniqueId());
        dash_num.remove(player.getUniqueId());
    }
}
