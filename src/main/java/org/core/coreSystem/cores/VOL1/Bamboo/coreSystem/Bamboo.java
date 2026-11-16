package org.core.coreSystem.cores.VOL1.Bamboo.coreSystem;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Bamboo {

    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;
    public static final List<Long> REQUIRE_EXP = List.of(67L, 130L, 238L, 445L, 940L, 1183L, 2380L, 3208L, 4306L, 5620L);
    public static final List<Long> HEALTH_BONUS = List.of(0L, 1L, 1L);

    //L
    public long basic_Attack_Cool = 400;
    public double thrust_Damage = 3.0;
    public double slash_Damage = 2.0;
    public double spin_Damage = 4.0;
    public double basic_Combo_Step = 0.05;
    public double basic_Combo_Max = 1.6;
    public double basic_Close_Ratio = 0.25;
    public long basic_Combo_Reset = 3000;
    public double thrust_Reach = 6.0;
    public double thrust_Width = 0.4;
    public long thrust_Interval = 3;
    public double thrust_Knockback = 0.35;
    public double slash_Radius = 5.0;
    public double slash_Angle = 140;
    public double spin_Angle = 270;
    public double slash_Tilt = 20;
    public double slash_Knockback = 0.3;
    public double spin_Knockback = 0.45;
    public HashMap<UUID, Integer> comboMode = new HashMap<>();
    public HashMap<UUID, Integer> comboCount = new HashMap<>();
    public HashMap<UUID, Long> comboTime = new HashMap<>();
    public HashSet<UUID> comboHit = new HashSet<>();

    //R
    public HashMap<UUID, Boolean> reloaded = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> damaged = new HashMap<>();
    public HashMap<UUID, Boolean> r_damaged = new HashMap<>();
    public double r_Skill_amp = 0.12;
    public double r_Skill_damage = 4;
    public long r_Skill_Cool = 0;
    public Map<UUID, Boolean> isSpearFlying = new HashMap<>();

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
    public HashSet<UUID> isDashing = new HashSet<>();

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        reloaded.remove(player.getUniqueId());
        isSpearFlying.remove(player.getUniqueId());

        stringOn.remove(player.getUniqueId());
        moveToSneaking.remove(player.getUniqueId());
        moveToThrow.remove(player.getUniqueId());
        stringCount.remove(player.getUniqueId());
        stringPoint.remove(player.getUniqueId());

        damaged.remove(player.getUniqueId());
        r_damaged.remove(player.getUniqueId());

        isDashing.remove(player.getUniqueId());

        comboMode.remove(player.getUniqueId());
        comboCount.remove(player.getUniqueId());
        comboTime.remove(player.getUniqueId());
        comboHit.remove(player.getUniqueId());
    }
}