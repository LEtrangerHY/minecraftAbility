package org.core.coreSystem.cores.VOL6.Jester.coreSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Jester {
    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;
    public static final List<Long> REQUIRE_EXP = List.of(66L, 202L, 363L, 666L, 1001L, 1111L, 2002L, 3003L, 4004L, 6006L);

    //passive
    public HashMap<UUID, Boolean> canUseSkill = new HashMap<>();
    public HashMap<UUID, Boolean> isOmnislashing = new HashMap<>();
    public HashMap<UUID, Long> shieldParryTime = new HashMap<>();
    public HashMap<UUID, Boolean> isExecutionTime = new HashMap<>();

    public HashMap<UUID, Material> currentQWeapon = new HashMap<>();

    //R
    public long r_Skill_Cool = 6000;

    //Q
    public double q_Skill_amp = 0.2;
    public double q_Skill_damage = 2;
    public long q_Skill_Cool = 0;

    //F
    public double f_Skill_amp = 0.2;
    public double f_Skill_damage = 4;
    public long f_Skill_Cool = 6000;

    public String getWeaponQCoolKey(Material material) {
        return switch (material) {
            case TRIDENT -> "Q_trident";
            case FIREWORK_ROCKET -> "Q_rocket";
            case SHIELD-> "Q_shield";
            case STICK -> "Q_stick";
            case CHAINMAIL_HELMET -> "Q_crown";
            default -> null;
        };
    }

    public void variableReset(Player player){
        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());
        currentQWeapon.remove(player.getUniqueId());
    }
}