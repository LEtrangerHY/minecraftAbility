package org.core.coreSystem.cores.VOL1.Blossom.coreSystem;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class Blossom {
    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public HashMap<UUID, Boolean> collision = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> damaged = new HashMap<>();

    //R
    public long r_Skill_Cool = 3700;
    public double r_Skill_damage = 0.43;
    public double r_Skill_amp = 0.17;

    //Q
    public HashMap<UUID, HashSet<Entity>> q_damaged = new HashMap<>();
    public HashMap<UUID, Integer> repeatCount = new HashMap<>();
    public long q_Skill_Cool = 14000;
    public double q_Skill_damage = 3;
    public double q_Skill_amp = 0.17;
    
    public HashMap<UUID, Location> treeLoc = new HashMap<>();
    public long f_Skill_Cool = 70000;
    public double f_Skill_damage = 0.7;
    public double f_Skill_amp = 0.17;

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        collision.remove(player.getUniqueId());
        damaged.remove(player.getUniqueId());

        q_damaged.remove(player.getUniqueId());
        repeatCount.remove(player.getUniqueId());

    }
}
