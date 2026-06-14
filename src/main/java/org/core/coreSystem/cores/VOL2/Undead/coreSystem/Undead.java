package org.core.coreSystem.cores.VOL2.Undead.coreSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask; // BukkitTask 임포트 추가

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
    public HashMap<UUID, Boolean> collision = new HashMap<>();
    public HashMap<UUID, Double> f_charge_defend = new HashMap<>();
    public HashMap<UUID, Integer> f_charge_hit_count = new HashMap<>();
    public HashMap<UUID, Double> f_charge_progress = new HashMap<>();
    public HashMap<UUID, Boolean> f_charge_consumed = new HashMap<>();
    public HashMap<UUID, Double> f_buff_rate = new HashMap<>();
    public HashMap<UUID, Integer> f_buff_ticks = new HashMap<>();
    public HashMap<UUID, Double> f_debuff_rate = new HashMap<>();

    public HashSet<UUID> is_swapping = new HashSet<>();

    //R
    public double r_Skill_amp = 0.2;
    public double r_Skill_Damage = 0;
    public long r_Skill_Cool = 13000;
    public long r_Skill_Cool_re = 4000;

    //Q
    public double q_Skill_amp = 0.2;
    public double q_Skill_Damage = 0;
    public long q_Skill_Cool = 13000;

    //F
    public long f_Skill_Cool = 0;

    public String getWeaponCoolKey(Material material) {
        return switch (material) {
            case IRON_SHOVEL -> "R_shovel";
            case IRON_PICKAXE -> "R_pickaxe";
            case IRON_AXE -> "R_axe";
            case IRON_HOE -> "R_scythe";
            case IRON_HORSE_ARMOR, STICK -> "R_pistol";
            default -> null;
        };
    }

    public String getWeaponRCoolKey(Material material) {
        return switch (material) {
            case IRON_SHOVEL -> "R_shovel_re";
            case IRON_PICKAXE -> "R_pickaxe_re";
            case IRON_AXE -> "R_axe_re";
            case IRON_HOE -> "R_scythe_re";
            case IRON_HORSE_ARMOR, STICK -> "R_pistol_re";
            default -> null;
        };
    }

    public String getWeaponQCoolKey(Material material) {
        return switch (material) {
            case IRON_SHOVEL -> "Q_shovel";
            case IRON_PICKAXE -> "Q_pickaxe";
            case IRON_AXE -> "Q_axe";
            case IRON_HOE -> "Q_scythe";
            case IRON_HORSE_ARMOR, STICK -> "Q_pistol";
            default -> null;
        };
    }

    public void variableReset(Player player){
        UUID uuid = player.getUniqueId();

        R_COOLDOWN.remove(uuid);
        Q_COOLDOWN.remove(uuid);
        F_COOLDOWN.remove(uuid);

        f_charge_progress.remove(uuid);
        f_charge_consumed.remove(uuid);
        f_charge_defend.remove(uuid);
        f_charge_hit_count.remove(uuid);

        f_buff_rate.remove(uuid);
        f_buff_ticks.remove(uuid);
        f_debuff_rate.remove(uuid);

        is_swapping.remove(uuid);

        if (player.isOnline()) {
            player.setWalkSpeed(0.2f);
        }
    }
}