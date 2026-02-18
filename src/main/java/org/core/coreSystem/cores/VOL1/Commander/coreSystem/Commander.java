package org.core.coreSystem.cores.VOL1.Commander.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class Commander {
    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public HashMap<UUID, Boolean> collision = new HashMap<>();

    public HashMap<UUID, HashSet<FallingBlock>> comBlocks = new HashMap<>();

    //R
    public HashMap<UUID, HashSet<Entity>> damaged = new HashMap<>();
    public double r_Skill_amp = 0.2;
    public double r_Skill_Damage = 3;
    public long r_Skill_Cool = 4000;

    //Q
    public double q_Skill_amp = 0.1;
    public double q_Skill_Damage = 2;
    public double q_Skill_Heal = 2;
    public long q_Skill_Cool = 9000;

    //F
    public HashMap<UUID, HashSet<FallingBlock>> received = new HashMap<>();
    public double f_Skill_amp = 0.2;
    public double f_Skill_Damage = 3;
    public long f_Skill_Cool = 7000;

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        for(FallingBlock fb : comBlocks.getOrDefault(player.getUniqueId(), new HashSet<>())){
            fb.remove();;
        }

        comBlocks.remove(player.getUniqueId());
        damaged.remove(player.getUniqueId());
        received.remove(player.getUniqueId());
    }
}
