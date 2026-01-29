package org.core.coreSystem.cores.KEY.Benzene.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class Benzene {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public LinkedHashMap<Entity, LinkedHashMap<Entity, Long>> damageTimes = new LinkedHashMap<>();
    public HashMap<UUID, Integer> crCount = new HashMap<>();
    public HashMap<UUID, LinkedHashMap<Integer, Entity>> chainRes = new HashMap<>();

    //R
    public HashMap<UUID, HashSet<Entity>> damaged_1 = new HashMap<>();
    public HashMap<UUID, Integer> atkCount = new HashMap<>();
    public HashMap<UUID, Boolean> rskill_using = new HashMap<>();
    public double r_Skill_dash = 1.6;
    public double r_Skill_amp = 0.2;
    public double r_Skill_damage = 2;
    public long r_Skill_Cool = 6000;

    //Q
    public HashMap<UUID, Entity> q_Skill_effect_1 = new HashMap<>();
    public HashMap<UUID, Entity> q_Skill_effect_2 = new HashMap<>();
    public long q_Skill_Cool = 3000;

    //F
    public HashMap<UUID, HashSet<Entity>> damaged_2 = new HashMap<>();
    public HashMap<UUID, Boolean> blockBreak = new HashMap<>();
    public HashMap<UUID, Boolean> canBlockBreak = new HashMap<>();
    public HashMap<UUID, Boolean> fskill_using = new HashMap<>();
    public double f_Skill_Amp = 0.2;
    public double f_Skill_Damage = 2;
    public long f_Skill_Cool = 1000;


    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        atkCount.remove(player.getUniqueId());

        crCount.remove(player.getUniqueId());
        chainRes.remove(player.getUniqueId());

        fskill_using.remove(player.getUniqueId());
        blockBreak.remove(player.getUniqueId());
        canBlockBreak.remove(player.getUniqueId());
        damaged_2.remove(player.getUniqueId());

        rskill_using.remove(player.getUniqueId());
        damaged_1.remove(player.getUniqueId());

        q_Skill_effect_1.remove(player.getUniqueId());
        q_Skill_effect_2.remove(player.getUniqueId());

    }
}
