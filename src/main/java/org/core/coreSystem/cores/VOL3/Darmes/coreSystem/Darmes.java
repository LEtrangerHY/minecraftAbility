package org.core.coreSystem.cores.VOL3.Darmes.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class Darmes {

    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    public HashMap<UUID, Boolean> collision = new HashMap<>();

    // R
    public double r_Skill_amp = 0;
    public long r_Skill_Cool = 6000;

    // Q
    public HashMap<UUID, HashSet<Entity>> q_damaged = new HashMap<>();
    public HashMap<UUID, Boolean> q_reuse = new HashMap<>();
    public HashMap<UUID, Double> q_Skill_Jump = new HashMap<>();
    public long q_Skill_Cool = 12000;
    public double q_Skill_amp = 0.2;
    public double q_Skill_Jump_Int = 0.3;
    public double q_Skill_Damage = 3.0;

    // F
    public HashMap<UUID, Double> blowParameter = new HashMap<>();
    public double blowParapeterInc = 0.2;
    public long f_Skill_Cool = 1000;
    public double f_Skill_amp = 0.2;
    public double f_Skill_Heal= 1.0;
    public double f_Skill_Damage = 2.0;

    public void variableReset(Player player) {
        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());
    }
}
