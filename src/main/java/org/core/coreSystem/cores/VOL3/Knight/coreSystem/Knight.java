package org.core.coreSystem.cores.VOL3.Knight.coreSystem;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class Knight {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public HashMap<UUID, Boolean> collision = new HashMap<>();
    public double l_Skill_Damage = 3.0;
    public long l_Skill_Cool = 625;
    public double l_Touch_Radius = 1.0;

    public int blade_Max = 12;
    public long blade_Duration = 12000;
    public double blade_Heal = 2.33;
    public double blade_Sink_Entity = 0.6;
    public double blade_Sink_Block = 0.3;
    public double blade_Burst_Speed = 0.22;
    public double blade_Burst_Lift = 0.38;
    public double blade_Gravity = 0.06;

    public double bond_Range = 12.0;
    public double bond_Share = 0.6;
    public double bond_Width = 0.4;
    public double bond_Speed = 1.82;

    //R
    public double r_Skill_amp = 0.2;
    public double r_Throw_Speed = 2.8;
    public double r_Throw_Range = 24.0;
    public double r_Reuse_Radius = 7.0;
    public double r_Reuse_Divide = 3.0;
    public long r_Reuse_Stagger = 100;
    public long r_Windup_Time = 400;
    public double r_Windup_Turn = 60.0;
    public double r_Straight_Speed = 2.2;
    public double r_Straight_Distance = 21.0;
    public long r_Spiral_Ticks = 12;
    public double r_Spiral_Turn = 90.0;
    public double r_Spiral_Exit = 2.0;
    public long r_Skill_Cool = 400;

    //Q
    public double q_Skill_amp = 0.14;
    public long q_Parry_Duration = 3000;
    public double q_Parry_Slow = 0.7;
    public long q_Reuse_Delay = 250;
    public double q_Fan_Angle = 40.0;
    public int q_Fan_Count = 3;
    public int q_Fan_Full_Count = 4;
    public double q_Fan_Speed = 2.8;
    public double q_Fan_Range = 24.0;
    public long q_Fan_Cool = 12000;
    public double q_Homing_Speed = 2.2;
    public long q_Homing_Duration = 5000;
    public long q_Parry_Cool = 3000;
    public long q_Skill_Cool = 3000;

    //F
    public double f_Skill_amp = 0.2;
    public double f_Damage_Divide = 3.0;
    public double f_Pull_Divide = 6.0;
    public long f_Pull_Duration = 500;
    public double f_Length_Min = 6.0;
    public double f_Length_Max = 12.0;
    public double f_Width_Min = 2.0;
    public double f_Width_Max = 3.0;
    public float f_Scale_Min = 3.0f;
    public float f_Scale_Max = 6.0f;
    public long f_Windup = 600;
    public long f_Swing = 150;
    public int f_Stun_From = 6;
    public long f_Stun_Per_Blade = 250;
    public long f_Stun_Max = 3000;
    public int f_Break_From = 9;
    public int f_Wear = 77;
    public long f_Cool_Per_Blade = 5000;
    public long f_Cool_Max = 60000;

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        collision.remove(player.getUniqueId());

    }
}