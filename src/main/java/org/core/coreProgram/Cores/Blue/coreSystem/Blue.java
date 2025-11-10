package org.core.coreProgram.Cores.Blue.coreSystem;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class Blue {
    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public HashMap<UUID, Boolean> collision = new HashMap<>();

    //R
    public HashMap<UUID, HashSet<Entity>> r_damaged = new HashMap<>();
    public HashMap<UUID, List<Block>> Flower = new HashMap<>();
    public long r_Skill_Cool = 13000;
    public double r_Skill_damage = 4;
    public double r_Skill_amp = 0.13;

    //Q
    public HashMap<UUID, Boolean> qSoulAbsorb = new HashMap<>();
    public long q_Skill_Cool = 26000;
    public double q_Skill_amp = 0.13;

    //F
    public HashMap<UUID, Boolean> fskill_using = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> f_damaged = new HashMap<>();
    public HashMap<UUID, Integer> repeatCount = new HashMap<>();
    public long f_Skill_Cool = 66000;
    public double f_Skill_amp = 0.13;
    public double f_Skill_damage = 1.3;

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

    }
}
