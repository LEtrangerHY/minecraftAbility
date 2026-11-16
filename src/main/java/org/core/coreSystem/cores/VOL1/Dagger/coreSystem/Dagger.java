package org.core.coreSystem.cores.VOL1.Dagger.coreSystem;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

public class Dagger {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;
    public static final List<Long> REQUIRE_EXP = List.of(44L, 108L, 231L, 444L, 666L, 1313L, 2320L, 3113L, 4444L, 6134L);
    public static final List<Long> HEALTH_BONUS = List.of(0L, 0L, 0L);

    //passive
    public long basic_Attack_Cool = 400;
    public List<Double> basic_Total_Damage = List.of(4.0, 4.8, 5.6, 6.4);
    public int basic_Max_Stage = 4;
    public List<Integer> basic_Dual_Stage = List.of(2, 4);
    public long basic_Reset = 2000;
    public double basic_Reach = 3.4;
    public double basic_Aim_Width = 0.3;
    public double basic_Anchor_Inset = 0.6;
    public double basic_Anchor_Height = 0.62;
    public double basic_Angle = 120;
    public double basic_Tilt = 70;
    public double basic_Thickness = 0.7;
    public double basic_Edge_Ratio = 0.25;
    public int basic_Animation_Ticks = 3;
    public HashMap<UUID, UUID> basic_Target = new HashMap<>();
    public HashMap<UUID, Integer> basic_Stage = new HashMap<>();
    public HashMap<UUID, Long> basic_Time = new HashMap<>();
    public HashSet<UUID> basic_Hit = new HashSet<>();

    //R
    public HashMap<UUID, Boolean> r_damaged = new HashMap<>();
    public double r_Skill_amp = 0.06;
    public double r_Skill_damage = 6;
    public long r_Skill_Cool = 4000;
    public long r_Stun = 2000;

    //Q
    public HashMap<UUID, HashSet<Entity>> q_damaged = new HashMap<>();
    public double q_Skill_amp = 0.06;
    public double q_Skill_Damage = 2;
    public long q_Skill_Cool = 8000;
    public long q_Skill_Cool_2 = 20000;
    public long q_Skill_Cool_decrease = 4000;
    public double q_Skill_dash = 1.5;
    public double q_Skill_dash_2 = 2.2;

    //F
    public double f_Skill_amp = 0.06;
    public double f_Skill_Damage = 4;
    public double f_Skill_Damage_2 = 6.5;
    public long f_Skill_Cool = 0;
    public double f_Skill_dash = 2.6;
    public int f_Dash_Ticks = 8;
    public double f_Cross_Rate = 1.6;
    public double f_Cross_Dash_Rate = 1.6;
    public double f_Double_Rate = 1.5;
    public double f_Wave_Speed = 1.6;
    public int f_Wave_Ticks = 12;
    public double f_Wave_Tilt = 33;
    public double f_Wave_Radius = 2.0;
    public double f_Wave_Angle = 180;
    public double f_Wave_Thickness = 0.7;
    public double f_Wave_Band = 0.6;

    public HashMap<UUID, HashSet<Entity>> f_damaged_2 = new HashMap<>();
    public HashMap<UUID, Integer> f_slash = new HashMap<>();
    public HashMap<UUID, Integer> f_wave_live = new HashMap<>();
    public HashMap<UUID, LivingEntity[]> f_wave_target = new HashMap<>();
    public HashMap<UUID, Location[]> f_wave_mark = new HashMap<>();
    public HashSet<UUID> f_wave_cross = new HashSet<>();
    public HashMap<UUID, Boolean> f_using = new HashMap<>();
    public HashMap<UUID, Boolean> f_dash = new HashMap<>();
    public HashMap<UUID, Boolean> f_damaging = new HashMap<>();
    public HashMap<UUID, ArrayList<LivingEntity>> dash_object = new HashMap<>();
    public HashMap<UUID, ArrayList<Location>> dash_mark = new HashMap<>();
    public HashMap<UUID, Double> dash_power = new HashMap<>();

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        basic_Target.remove(player.getUniqueId());
        basic_Stage.remove(player.getUniqueId());
        basic_Time.remove(player.getUniqueId());
        basic_Hit.remove(player.getUniqueId());

        r_damaged.remove(player.getUniqueId());

        q_damaged.remove(player.getUniqueId());

        f_damaged_2.remove(player.getUniqueId());
        f_slash.remove(player.getUniqueId());
        f_wave_live.remove(player.getUniqueId());
        f_wave_target.remove(player.getUniqueId());
        f_wave_mark.remove(player.getUniqueId());
        f_wave_cross.remove(player.getUniqueId());
        f_using.remove(player.getUniqueId());
        f_dash.remove(player.getUniqueId());
        f_damaging.remove(player.getUniqueId());
        dash_object.remove(player.getUniqueId());
        dash_mark.remove(player.getUniqueId());
        dash_power.remove(player.getUniqueId());
    }
}