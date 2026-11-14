package org.core.coreSystem.cores.VOL1.Pyro.coreSystem;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class Pyro {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;
    public static final List<Long> REQUIRE_EXP = List.of(77L, 154L, 277L, 477L, 977L, 1177L, 2277L, 3377L, 4447L, 6007L);

    //passive
    public HashMap<UUID, Boolean> collision = new HashMap<>();

    public LinkedHashMap<UUID, Integer> causalgia = new LinkedHashMap<>();
    public long coolCausalgia = 7000;
    public long burnCoolExtends = 3000;
    public double burnDownStats = 0.7;

    //R
    public double r_Skill_amp = 0.2;
    public double r_Skill_Damage = 4;
    public long r_Skill_Cool = 7000;
    public long r_Skill_stun = 1700;

    //Q
    public double q_Skill_Damage_Percent = 50;
    public double q_Skill_Jump = 1.4;
    public long q_Skill_Cool = 10000;

    //F
    public long f_Skill_Cool = 130000;

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

    }
}