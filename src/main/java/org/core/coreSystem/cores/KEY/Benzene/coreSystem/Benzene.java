package org.core.coreSystem.cores.KEY.Benzene.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class Benzene {

    // CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    // passive
    public HashMap<UUID, Integer> crCount = new HashMap<>();
    public HashMap<UUID, LinkedHashMap<Integer, UUID>> chainRes = new HashMap<>();

    // R
    public HashMap<UUID, HashSet<Entity>> damaged_1 = new HashMap<>();
    public HashMap<UUID, Integer> atkCount = new HashMap<>();
    public HashMap<UUID, Boolean> rskill_using = new HashMap<>();
    public double r_Skill_dash = 1.6;
    public double r_Skill_amp = 0.2;
    public double r_Skill_damage = 2;
    public long r_Skill_Cool = 6000;

    // Q
    public HashMap<UUID, UUID> q_Skill_effect_1 = new HashMap<>();
    public HashMap<UUID, HashSet<UUID>> q_Skill_effect_2 = new HashMap<>();
    public long q_Skill_Cool = 3000;

    public HashMap<UUID, HashSet<Entity>> damaged_2 = new HashMap<>();
    public HashMap<UUID, Boolean> blockBreak = new HashMap<>();
    public HashMap<UUID, Boolean> canBlockBreak = new HashMap<>();
    public HashMap<UUID, Boolean> fskill_using = new HashMap<>();
    public double f_Skill_Amp = 0.2;
    public double f_Skill_Damage = 2;
    public long f_Skill_Cool = 1000;


    public void variableReset(Player player){
        UUID uuid = player.getUniqueId();

        R_COOLDOWN.remove(uuid);
        Q_COOLDOWN.remove(uuid);
        F_COOLDOWN.remove(uuid);

        atkCount.remove(uuid);

        crCount.remove(uuid);
        chainRes.remove(uuid);

        fskill_using.remove(uuid);
        blockBreak.remove(uuid);
        canBlockBreak.remove(uuid);
        damaged_2.remove(uuid);

        rskill_using.remove(uuid);
        damaged_1.remove(uuid);

        q_Skill_effect_1.remove(uuid);
        q_Skill_effect_2.remove(uuid);

    }
}