package org.core.coreSystem.cores.VOL3.Residue.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class Residue {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public HashMap<UUID, Boolean> collision = new HashMap<>();

    // R
    public double r_Skill_amp = 0;
    public long r_Skill_Cool = 6000;

    // Q
    public HashMap<UUID, Boolean> isSpearFlying = new HashMap<>();
    public long q_Skill_Cool = 500;
    public double q_Skill_amp = 0.2;
    public double q_Skill_Damage = 12.0;

    // F
    public HashMap<UUID, Set<Entity>> damaged = new HashMap<>();
    public HashMap<UUID, Boolean> fskill_using = new HashMap<>();
    public HashMap<UUID, Integer> f_Skill_dash_f = new HashMap<>();
    public double f_Skill_dash_b = -0.6;
    public double f_Skill_amp = 0.2;
    public double f_Skill_Damage = 2;
    public double f_Skill_Heal = 1;
    public long f_Skill_Cool = 1000;

    public void variableReset(Player player){
        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        isSpearFlying.remove(player.getUniqueId());
        fskill_using.remove(player.getUniqueId());
    }
}