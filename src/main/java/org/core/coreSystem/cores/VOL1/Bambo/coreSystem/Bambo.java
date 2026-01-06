package org.core.coreSystem.cores.VOL1.Bambo.coreSystem;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class Bambo {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public HashMap<UUID, Boolean> reloaded = new HashMap<>();

    //R
    public HashMap<UUID, HashSet<Entity>> damaged = new HashMap<>();
    public HashMap<UUID, Boolean> r_damaged = new HashMap<>();
    public double r_Skill_amp = 0.12;
    public double r_Skill_damage = 15;
    public long r_Skill_Cool = 7000;

    //Q
    public long q_Skill_Cool = 7000;
    public double q_Skill_Jump = 2.0;

    //F
    public long f_Skill_Cool = 100;

    public HashSet<UUID> moveToSneaking = new HashSet<>();
    public HashSet<UUID> moveToThrow = new HashSet<>();
    public HashSet<UUID> stringOn = new HashSet<>();
    public HashMap<UUID, Integer> stringCount = new HashMap<>();
    public HashMap<UUID, Location> stringPoint = new HashMap<>();


    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        reloaded.remove(player.getUniqueId());
        stringOn.remove(player.getUniqueId());
        moveToSneaking.remove(player.getUniqueId());
        moveToThrow.remove(player.getUniqueId());
        stringCount.remove(player.getUniqueId());
        stringPoint.remove(player.getUniqueId());

        damaged.remove(player.getUniqueId());
        r_damaged.remove(player.getUniqueId());
    }

}
