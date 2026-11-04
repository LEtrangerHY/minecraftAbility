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

    // Q
    public long q_Skill_Cool = 12000;
    public double q_Skill_amp = 0.2;

    // F
    public long f_Skill_Cool = 1000;
    public double f_Skill_amp = 0.2;
    public double f_Skill_Damage = 2.0;

    public void variableReset(Player player) {
        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());
    }
}