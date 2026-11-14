package org.core.coreSystem.cores.VOL3.Stroke.coreSystem;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Stroke {

    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;
    public static final List<Long> REQUIRE_EXP = List.of(88L, 188L, 288L, 488L, 988L, 1088L, 2048L, 3888L, 4468L, 6088L);

    public HashMap<UUID, Long> rLastClickTime = new HashMap<>();
    public HashMap<UUID, Boolean> rIsCharging = new HashMap<>();
    public HashMap<UUID, BossBar> rChargeBars = new HashMap<>();

    public HashMap<UUID, Boolean> isFActive = new HashMap<>();
    public HashMap<UUID, BukkitTask> fTasks = new HashMap<>();

    //Q
    public HashMap<UUID, Integer> qStep = new HashMap<>();
    public HashMap<UUID, BossBar> qBars = new HashMap<>();
    public HashMap<UUID, BukkitTask> qTasks = new HashMap<>();
    public HashMap<UUID, Long> qInputDelay = new HashMap<>();
    public HashMap<UUID, Boolean> qIsSampling = new HashMap<>();

    //passive

    //R
    public double r_Skill_amp = 0.2;
    public double r_Skill_damage = 3;
    public long r_Skill_Cool = 250;

    //Q
    public double q_Skill_amp = 0.2;
    public double q_Skill_damage = 0;
    public long q_Skill_Cool = 14000;

    //F
    public double f_Skill_amp = 0.2;
    public double f_Skill_damage = 0;
    public long f_Skill_Cool = 40000;

    public void variableReset(Player player){
        UUID uuid = player.getUniqueId();
        R_COOLDOWN.remove(uuid);
        Q_COOLDOWN.remove(uuid);
        F_COOLDOWN.remove(uuid);

        rLastClickTime.remove(uuid);
        rIsCharging.remove(uuid);
        if (rChargeBars.containsKey(uuid)) {
            rChargeBars.get(uuid).removeAll();
            rChargeBars.remove(uuid);
        }

        isFActive.remove(uuid);
        if (fTasks.containsKey(uuid)) {
            fTasks.get(uuid).cancel();
            fTasks.remove(uuid);
        }

        qStep.remove(uuid);
        if (qBars.containsKey(uuid)) {
            qBars.get(uuid).removeAll();
            qBars.remove(uuid);
        }
        if (qTasks.containsKey(uuid)) {
            qTasks.get(uuid).cancel();
            qTasks.remove(uuid);
        }
        qInputDelay.remove(uuid);
        qIsSampling.remove(uuid);
    }
}