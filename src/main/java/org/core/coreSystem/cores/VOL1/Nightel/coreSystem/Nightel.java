package org.core.coreSystem.cores.VOL1.Nightel.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class Nightel {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public HashMap<UUID, Integer> chainCount = new HashMap<>();
    public HashMap<UUID, String> chainSkill = new HashMap<>();

    //R
    public HashMap<UUID, HashSet<Entity>> damaged = new HashMap<>();
    public HashMap<UUID, Boolean> rskill_using = new HashMap<>();
    public double r_Skill_dash = 1.6;
    public double r_Skill_amp = 0.2;
    public double r_Skill_damage = 2;
    public long r_Skill_Cool = 300;

    //Q
    public HashMap<UUID, HashSet<Entity>> damaged_1 = new HashMap<>();
    public double q_SKill_amp = 0.2;
    public double q_Skill_damage = 1;
    public long q_Skill_Cool = 300;

    //F
    public HashMap<UUID, HashSet<Entity>> damaged_2 = new HashMap<>();
    public HashMap<UUID, Boolean> fskill_using = new HashMap<>();
    public double f_Skill_amp = 0.2;
    public double f_Skill_damage = 1;
    public long f_Skill_Cool = 300;

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        damaged.remove(player.getUniqueId());
        damaged_1.remove(player.getUniqueId());
        damaged_2.remove(player.getUniqueId());

        chainCount.remove(player.getUniqueId());
        chainSkill.remove(player.getUniqueId());

        rskill_using.remove(player.getUniqueId());
        fskill_using.remove(player.getUniqueId());

    }
}
