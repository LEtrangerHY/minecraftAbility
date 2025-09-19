package org.core.coreProgram.Cores.Luster.coreSystem;

import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Luster {
    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public HashMap<UUID, Boolean> collision = new HashMap<>();

    //R
    public double r_Skill_amp = 0.13;
    public double r_Skill_Damage = 20;
    public long r_Skill_Cool = 13000;

    //Q
    public double q_Skill_amp = 0.13;
    public double q_Skill_Damage = 3;
    public long q_Skill_Cool = 13000;

    //F
    public HashMap<Player, Set<IronGolem>> golems = new HashMap<>();
    public long f_Skill_Cool = 4000;

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

    }
}
