package org.core.coreSystem.cores.VOL2.Undead.coreSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class Undead {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public HashMap<UUID, String> swap_Command = new HashMap<>();
    public HashSet<UUID> is_swapping = new HashSet<>();
    public HashSet<UUID> is_acting = new HashSet<>();
    public HashSet<UUID> is_damaging = new HashSet<>();
    public HashMap<UUID, Boolean> slash_Flip = new HashMap<>();

    public HashMap<UUID, Double> restore_buff_rate = new HashMap<>();
    public HashMap<UUID, Double> f_Empower = new HashMap<>();
    public HashMap<UUID, Integer> restore_buff_ticks = new HashMap<>();
    public HashMap<UUID, Double> restore_debuff_rate = new HashMap<>();
    public HashMap<UUID, Double> restore_charge_defend = new HashMap<>();
    public HashMap<UUID, Integer> restore_charge_hit_count = new HashMap<>();
    public int restore_Charge_Ticks = 60;
    public int restore_Min_Ticks = 6;
    public double restore_Debuff_Max = 4.0;
    public double restore_Dash_Min = 0.6;
    public double restore_Dash_Max = 1.8;

    //L
    public double attack_Center_Sigma = 14.0;
    public double attack_Edge_Ratio = 0.25;
    public double attack_Heal_Ratio = 0.04;
    public double attack_Heal_Cap = 0.16;

    public long sword_Attack_Cool = 500;
    public double sword_Attack_Damage = 4.0;
    public double sword_Attack_Reach = 3.6;

    public long axe_Attack_Cool = 800;
    public double axe_Attack_Damage = 6.0;
    public double axe_Attack_Reach = 2.2;
    public double axe_Attack_Knockback = 0.0;
    public double axe_Attack_Lift = 0.0;

    public long pickaxe_Attack_Cool = 700;
    public double pickaxe_Attack_Damage = 5.5;
    public double pickaxe_Attack_Reach = 4.0;
    public double pickaxe_Attack_Knockback = 0.0;
    public double pickaxe_Attack_Lift = 0.0;

    public long shovel_Attack_Cool = 550;
    public double shovel_Attack_Damage = 4.0;
    public double shovel_Attack_Reach = 2.8;
    public double shovel_Attack_Knockback = 0.55;
    public double shovel_Attack_Lift = 0.65;

    public long hoe_Attack_Cool = 300;
    public double hoe_Attack_Damage = 2.0;
    public double hoe_Attack_Reach = 4.2;
    public double hoe_Attack_Knockback = 0.25;
    public double hoe_Attack_Lift = 0.25;

    public long gun_Attack_Cool = 800;
    public long gun_Attack_Empty_Cool = 250;
    public double gun_Attack_Damage = 4.0;
    public double gun_Attack_Health_Ratio = 0.13;
    public double gun_Attack_Lost_Ratio = 0.06;
    public double gun_Attack_Cost_Ratio = 0.06;
    public int gun_Attack_Pierce = 4;
    public double gun_Attack_Knockback = 0.3;
    public double gun_Attack_Speed = 4.0;
    public int gun_Attack_Life = 15;
    public float gun_Attack_Recoil = 2.0f;

    //R
    public double r_Skill_amp = 0.2;
    public long melee_r_Skill_Cool = 6000;
    public double melee_r_Skill_Rate = 0.66;
    public int melee_r_Skill_Delay = 4;
    public long gun_r_Skill_Cool = 6000;
    public int gun_r_Skill_Interval = 4;
    public int gun_r_Skill_Finish_Delay = 8;
    public double gun_r_Skill_Rate = 0.44;
    public double gun_r_Skill_Finish_Rate = 2.0;
    public double gun_r_Skill_Cost_Ratio = 0.04;
    public double gun_r_Skill_Finish_Cost_Ratio = 0.12;
    public double gun_r_Skill_Knockback = 0.9;
    public double gun_r_Skill_Push = 0.7;
    public float gun_r_Skill_Recoil = 6.0f;

    //Q
    public double q_Skill_amp = 0.2;
    public double q_Skill_Dash = 1.6;
    public int q_Skill_Dash_Ticks = 8;
    public long melee_q_Skill_Cool = 12000;
    public double melee_q_Skill_Range = 4.0;
    public double melee_q_Skill_Damage = 8.0;
    public int melee_q_Skill_Duration = 80;
    public int melee_q_Skill_Interval = 10;
    public double melee_q_Skill_Burst = 1.3;
    public int melee_q_Skill_Slow = 1;
    public long gun_q_Skill_Cool = 12000;
    public double gun_q_Skill_Range = 6.0;
    public int gun_q_Skill_Fuse = 80;
    public int gun_q_Skill_Throw_Ticks = 5;
    public double gun_q_Skill_Throw_Arc = 0.5;
    public double gun_q_Skill_Damage = 6.0;
    public double gun_q_Skill_Radius = 3.0;
    public double gun_q_Skill_Knockback = 1.6;
    public double gun_q_Skill_Lift = 0.4;
    public int gun_q_Skill_Trigger_Delay = 2;
    public long gun_q_Skill_Stun = 1200;

    //F
    public double f_Skill_amp = 0.2;
    public long melee_f_Skill_Cool = 9000;
    public long gun_f_Skill_Cool = 9000;
    public double f_Skill_Cost_Ratio = 0.10;
    public double f_Skill_Cost_Max = 0.40;
    public int f_Skill_Window = 120;
    public double f_Skill_Rate = 1.6;
    public double f_Skill_Rate_Step = 0.2;
    public double f_Skill_Scale = 1.3;
    public double f_Skill_Scale_Step = 0.075;

    public boolean isTool(Material material) {
        return material == Material.IRON_SWORD ||
                material == Material.IRON_AXE ||
                material == Material.IRON_PICKAXE ||
                material == Material.IRON_SHOVEL ||
                material == Material.IRON_HOE;
    }

    public boolean isGun(Material material) {
        return material == Material.IRON_HORSE_ARMOR;
    }

    public void variableReset(Player player){
        UUID uuid = player.getUniqueId();

        R_COOLDOWN.remove(uuid);
        Q_COOLDOWN.remove(uuid);
        F_COOLDOWN.remove(uuid);

        swap_Command.remove(uuid);
        is_swapping.remove(uuid);
        is_acting.remove(uuid);
        is_damaging.remove(uuid);
        slash_Flip.remove(uuid);

        restore_buff_rate.remove(uuid);
        f_Empower.remove(uuid);
        restore_buff_ticks.remove(uuid);
        restore_debuff_rate.remove(uuid);
        restore_charge_defend.remove(uuid);
        restore_charge_hit_count.remove(uuid);

        if (player.isOnline()) {
            player.setWalkSpeed(0.2f);
        }
    }
}