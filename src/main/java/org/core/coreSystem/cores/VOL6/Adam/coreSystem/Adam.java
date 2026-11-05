package org.core.coreSystem.cores.VOL6.Adam.coreSystem;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class Adam {

    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    // R
    public double r_Skill_amp = 0;
    public long r_Skill_Cool = 6000;

    public int stack_Max = 12;
    public double stack_Speed = 0.03;
    public double charge_Absorption = 0.5;
    public long charge_Min = 300;
    public long charge_Max = 700;
    public long charge_Free_Level = 3;
    public double charge_Level_Cut = 0.2;

    // Q
    public long q_Skill_Cool = 3000;
    public double q_Skill_amp = 0.2;
    public double q_Range = 12.0;
    public double q_Ray = 0.6;
    public long q_Grounding = 2000;
    public int q_Stack = 3;
    public long q_Reuse_Window = 1000;
    public long q_Reuse_Delay = 150;
    public long q_Miss_Cool = 300;
    public double q_Face_Range = 6.0;

    // F
    public long f_Skill_Cool = 1000;
    public double f_Skill_amp = 0.2;
    public double f_Skill_Damage = 2.0;
    public int f_Stack_Per_Swing = 3;
    public double f_Strong_Damage_Per_Stack = 1.0;
    public double f_Strong_Radius_Min = 3.5;
    public double f_Strong_Radius_Max = 8.0;
    public double f_Strong_Inner = 0.4;
    public double f_Strong_Width_Min = 0.6;
    public double f_Strong_Width_Max = 1.2;
    public double f_Strong_Ring_Min = 1.5;
    public double f_Strong_Ring_Max = 4.0;
    public int f_Auto_Stack = 6;
    public long f_Strong_Stun = 2000;
    public int f_Strong_Windup = 4;
    public int f_Strong_Ticks = 5;
    public int f_Strong_Linger = 6;

    public void variableReset(Player player) {
        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());
    }
}