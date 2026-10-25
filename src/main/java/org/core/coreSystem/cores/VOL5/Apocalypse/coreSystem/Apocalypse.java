package org.core.coreSystem.cores.VOL5.Apocalypse.coreSystem;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class Apocalypse {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    public static final Material SWORD = Material.WOODEN_SWORD;
    public static final Material BAT = Material.STICK;

    public HashSet<UUID> skillDamaging = new HashSet<>();

    //passive
    public double bat_attack_speed = 2.0;
    public double bat_damage_max = 4.4;
    public double bat_damage_min = 1.1;
    public double bat_damage_sigma = 14;
    public double bat_center_angle = 18;
    public double bat_vanilla_knockback = 0.4;
    public double bat_reach = 3.8;
    public double bat_band = 1.1;

    public long combo_reset = 4000;
    public HashMap<UUID, Integer> combo_stage = new HashMap<>();
    public HashMap<UUID, Long> combo_lastHit = new HashMap<>();
    public HashMap<UUID, Long> combo_lastSwing = new HashMap<>();
    public HashMap<UUID, Long> combo_busyUntil = new HashMap<>();

    public double empower_multiplier = 1.5;
    public double empower_length = 4.2;
    public double empower_knockback = 1.0;
    public long empower_invulnerable = 350;
    public double empower_heal_ratio = 0.25;
    public double empower_heal_cap = 1.0 / 6.0;

    public double q_empower_multiplier = 3.0;
    public double q_empower_length = 4.6;
    public double q_empower_knockback = 1.7;
    public long q_empower_wallStun = 1000;
    public double q_empower_shield_ratio = 0.2;
    public double q_empower_shield_cap = 0.2;
    public long q_empower_shield_duration = 12000;

    public long q_empower_duration = 4000;
    public HashMap<UUID, Integer> q_empower = new HashMap<>();
    public HashMap<UUID, Long> q_empower_until = new HashMap<>();

    //R
    public HashMap<UUID, Boolean> r_damaged = new HashMap<>();
    public double r_Skill_amp = 0.2;
    public double r_Skill_damage = 6;
    public long r_Skill_Cool = 8000;
    public long r_reuse_Delay = 250;
    public long r_Skill_duration = 1000;
    public double r_Skill_teleport = 6.0;
    public double r_Skill_radius = 16.0;
    public double r_Skill_angle = 40.0;
    public double r_Skill_heightBase = 0.9;
    public double r_Skill_heightGrow = 1.0;
    public long r_Skill_stun = 2000;

    //Q
    public HashMap<UUID, HashSet<Entity>> q_damaged = new HashMap<>();
    public double q_Skill_amp = 0.2;
    public double q_Skill_damage = 2;
    public double q_Skill_designatedDamage = 6;
    public long q_Skill_Cool = 4000;
    public double q_Skill_range = 4.0;
    public double q_Skill_dash = 0.9;
    public long q_Skill_invulnerable = 350;
    public double q_Skill_length = 2.7;
    public double q_Skill_tilt = -38;
    public double q_Skill_sweep = 100;
    public double q_Skill_offset = 1.2;
    public double q_Skill_reach = 3.0;
    public double q_Skill_band = 1.3;

    //F
    public HashMap<UUID, HashSet<Entity>> f_damaged = new HashMap<>();
    public double f_Skill_amp = 0.13;
    public double f_Skill_damage = 8;
    public double f_Skill_minDamage = 2.2;
    public double f_Skill_sigma = 20;
    public long f_Skill_Cool = 44000;
    public double f_Skill_reach = 6.6;
    public double f_Skill_inner = 2.2;
    public double f_Skill_angle = 160;
    public double f_Skill_band = 1.0;
    public double f_Skill_tilt = 5;
    public double f_Skill_tiltJitter = 1.5;
    public int f_Skill_windup = 7;
    public double f_drawn_attack_speed = 4.0;
    public double f_sound_range = 1.8;
    public int f_sound_layers = 2;
    public int f_sound_slashLayers = 3;
    public int f_Skill_releaseDelay = 2;
    public int[] f_Skill_frames = {4, 3, 2, 1};

    public double f_counter_damage = 12;
    public double f_counter_reach = 7.0;
    public double f_counter_strike = 5.6;
    public double f_counter_inner = 2.2;
    public double f_counter_drop = 0.6;
    public double f_counter_angle = 170;
    public double f_counter_tilt = 8;
    public double f_counter_knockback = 1.2;
    public long f_counter_window = 3000;
    public double f_dash_range = 15;
    public double f_dash_speed = 4.5;
    public int f_dash_charge = 3;
    public double f_dash_ringEvery = 2.5;

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        combo_stage.remove(player.getUniqueId());
        combo_lastHit.remove(player.getUniqueId());
        combo_busyUntil.remove(player.getUniqueId());
        combo_lastSwing.remove(player.getUniqueId());
        q_empower.remove(player.getUniqueId());
        q_empower_until.remove(player.getUniqueId());

        r_damaged.remove(player.getUniqueId());

        q_damaged.remove(player.getUniqueId());

        f_damaged.remove(player.getUniqueId());
    }
}