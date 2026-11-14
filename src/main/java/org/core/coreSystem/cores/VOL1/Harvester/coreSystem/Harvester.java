package org.core.coreSystem.cores.VOL1.Harvester.coreSystem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class Harvester {

    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;
    public static final List<Long> REQUIRE_EXP = List.of(44L, 108L, 266L, 444L, 666L, 1414L, 2426L, 3444L, 4444L, 6164L);

    //R
    public HashMap<UUID, Boolean> rskill_using = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> damaged = new HashMap<>();
    public double r_Skill_amp = 0.2;
    public double r_Skill_damage = 6;
    public long r_Skill_Cool = 1300;

    //Q
    public long q_Skill_Cool = 16000;

    //F
    public HashMap<UUID, Boolean> fskill_using = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> f_damaged = new HashMap<>();
    public HashMap<UUID, Integer> grass = new HashMap<>();
    public HashMap<UUID, Integer> repeat = new HashMap<>();
    public long f_Skill_Cool = 26000;
    public double f_Skill_dash = 1.5;
    public double f_Skill_amp = 0.13;
    public double f_Skill_damage = 2;

    public void variableReset(Player player) {

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        rskill_using.remove(player.getUniqueId());
        damaged.remove(player.getUniqueId());

        fskill_using.remove(player.getUniqueId());
        f_damaged.remove(player.getUniqueId());
        grass.remove(player.getUniqueId());
        repeat.remove(player.getUniqueId());

    }
}