package org.core.coreSystem.cores.VOL1.Swordsman.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class Swordsman {
    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public HashMap<UUID, Boolean> laidoSlash = new HashMap<>();

    //R
    public HashMap<UUID, Boolean> r_skillUsing = new HashMap<>();
    public HashMap<UUID, Integer> r_Skill_count = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> r_damaged = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> r_damaged_2 = new HashMap<>();
    public long r_Skill_Cool = 550;
    public double r_Skill_damage = 3;
    public double r_Skill_amp = 0.1;
    public double r_Skill_dash = 1.6;
    public double r_Skill_add_damage = 9;
    public long r_Skill_stun = 2000;

    //Q
    public HashMap<UUID, Boolean> q_skillUsing = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> q_damaged = new HashMap<>();
    public long q_Skill_Cool = 5500;
    public double q_Skill_damage = 5;
    public double q_Skill_amp = 0.1;

    //F
    public HashMap<UUID, Boolean> f_skillUsing = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> f_damaged = new HashMap<>();
    public long f_Skill_Cool = 16200;
    public double f_Skill_damage = 5;
    public double f_Skill_amp = 0.1;
    public double f_Skill_dash = 1.4;
    public long f_Skill_stun = 2000;

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        laidoSlash.remove(player.getUniqueId());

        r_skillUsing.remove(player.getUniqueId());
        r_Skill_count.remove(player.getUniqueId());
        r_damaged.remove(player.getUniqueId());
        r_damaged_2.remove(player.getUniqueId());

        q_skillUsing.remove(player.getUniqueId());
        q_damaged.remove(player.getUniqueId());

        f_skillUsing.remove(player.getUniqueId());
        f_damaged.remove(player.getUniqueId());
    }
}