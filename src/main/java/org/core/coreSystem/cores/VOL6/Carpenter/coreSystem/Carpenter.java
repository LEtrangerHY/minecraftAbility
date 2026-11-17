package org.core.coreSystem.cores.VOL6.Carpenter.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class Carpenter {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;
    public static final List<Long> REQUIRE_EXP = List.of(77L, 120L, 240L, 480L, 960L, 1212L, 1712L, 2412L, 3612L, 4812L);
    public static final List<Long> HEALTH_BONUS = List.of(0L, 0L, 3L);

    //passive
    public long basic_Attack_Cool = 750;
    public double basic_Damage = 6;
    public double basic_Reach = 3.2;
    public double basic_Aim_Width = 0.3;
    public double basic_Anchor_Inset = 0.6;
    public double basic_Anchor_Height = 0.6;
    public double basic_Angle = 130;
    public double basic_Tilt = 55;
    public double basic_Thickness = 0.8;
    public int basic_Swing_Ticks = 4;
    public double basic_Knockback = 0.35;
    public HashSet<UUID> basic_Rising = new HashSet<>();

    //R
    public HashMap<UUID, Boolean> r_damaging = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> r_damaged = new HashMap<>();
    public double r_Skill_amp = 0.12;
    public double r_Skill_damage = 3;
    public long r_Skill_Cool = 12000;
    public double r_Radius = 4.5;
    public double r_Angle = 140;
    public double r_Height = 1.4;
    public int r_Sweep_Ticks = 4;
    public int r_Slow_Ticks = 30;
    public int r_Slow_Level = 4;

    //Q
    public HashMap<UUID, Boolean> q_damaging = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> q_damaged = new HashMap<>();
    public HashMap<UUID, Boolean> q_using = new HashMap<>();
    public HashMap<UUID, Boolean> crash = new HashMap<>();
    public HashMap<UUID, Double> normal_distribution = new HashMap<>();
    public double q_Skill_amp = 0.12;
    public double q_Skill_damage = 7;
    public double q_Skill_jump = 0.7;
    public long q_Skill_Load = 5000;
    public long q_Skill_Cool = 0;

    //F
    public HashSet<UUID> f_zone = new HashSet<>();
    public HashMap<UUID, BukkitTask> f_totem_task = new HashMap<>();
    public HashMap<UUID, ItemStack> f_stash = new HashMap<>();
    public double f_Skill_amp = 0.2;
    public double f_Skill_heal = 1;
    public long f_Skill_Cool = 90000;
    public double f_Zone_Radius = 7;
    public int f_Zone_Ticks = 60;
    public int f_Heal_Interval = 10;
    public int f_Totem_Ticks = 680;

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        r_damaging.remove(player.getUniqueId());
        r_damaged.remove(player.getUniqueId());

        q_damaging.remove(player.getUniqueId());
        q_using.remove(player.getUniqueId());
        crash.remove(player.getUniqueId());
        normal_distribution.remove(player.getUniqueId());

        f_zone.remove(player.getUniqueId());

        basic_Rising.remove(player.getUniqueId());
    }
}