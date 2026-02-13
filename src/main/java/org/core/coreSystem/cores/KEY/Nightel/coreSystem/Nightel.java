package org.core.coreSystem.cores.KEY.Nightel.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class Nightel {

    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    //passive
    public long frozenCool = 10000;
    public HashMap<UUID, Integer> chainCount = new HashMap<>();
    public HashMap<UUID, String> chainSkill = new HashMap<>();

    //R
    public HashMap<UUID, Set<Entity>> damaged = new HashMap<>();
    public HashMap<UUID, Boolean> rskill_using = new HashMap<>();
    public double r_Skill_dash = 1.6;
    public double r_Skill_amp = 0.2;
    public double r_Skill_damage = 2;
    public long r_Skill_Cool = 300;

    //Q
    public HashMap<UUID, Set<Entity>> damaged_1 = new HashMap<>();
    public double q_SKill_amp = 0.2;
    public double q_Skill_damage = 1;
    public long q_Skill_Cool = 300;

    //F
    public HashMap<UUID, Set<Entity>> damaged_2 = new HashMap<>();
    public HashMap<UUID, Boolean> fskill_using = new HashMap<>();
    public double f_Skill_amp = 0.2;
    public double f_Skill_damage = 1;
    public long f_Skill_Cool = 300;

    public void variableReset(Player player){
        UUID uuid = player.getUniqueId();

        R_COOLDOWN.remove(uuid);
        Q_COOLDOWN.remove(uuid);
        F_COOLDOWN.remove(uuid);

        damaged.remove(uuid);
        damaged_1.remove(uuid);
        damaged_2.remove(uuid);

        chainCount.remove(uuid);
        chainSkill.remove(uuid);

        rskill_using.remove(uuid);
        fskill_using.remove(uuid);
    }
}