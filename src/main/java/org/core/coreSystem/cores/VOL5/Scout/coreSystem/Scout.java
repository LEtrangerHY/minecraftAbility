package org.core.coreSystem.cores.VOL5.Scout.coreSystem;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scout {

    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;
    public static final List<Long> REQUIRE_EXP = List.of(77L, 177L, 347L, 527L, 777L, 1114L, 1427L, 2104L, 3371L, 4137L);

    //passive
    public final int MAX_AMMO = 10;
    public HashMap<UUID, Integer> ammo = new HashMap<>();
    public Map<UUID, BossBar> activeReloadBars = new HashMap<>();
    public Map<UUID, BukkitRunnable> activeReloadTasks = new HashMap<>();

    public HashMap<UUID, Boolean> collision = new HashMap<>();
    public double p_Skill_damage = 5.0;
    public long p_Skill_Cool = 700;

    public HashMap<UUID, Boolean> isOverclocked = new HashMap<>();

    public HashMap<UUID, ItemDisplay> activeBombs = new HashMap<>();
    public HashMap<UUID, BukkitRunnable> bombTasks = new HashMap<>();

    //R
    public double r_Skill_amp = 0.2;
    public double r_Skill_damage = 7.0;
    public long r_Skill_Cool = 10000;

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

        ammo.put(uuid, MAX_AMMO);
        collision.remove(uuid);
        isOverclocked.put(uuid, false);

        if (activeReloadTasks.containsKey(uuid)) {
            activeReloadTasks.get(uuid).cancel();
            activeReloadTasks.remove(uuid);
        }
        if (activeReloadBars.containsKey(uuid)) {
            activeReloadBars.get(uuid).removeAll();
            activeReloadBars.remove(uuid);
        }

        if (bombTasks.containsKey(uuid)) {
            bombTasks.get(uuid).cancel();
            bombTasks.remove(uuid);
        }
        if (activeBombs.containsKey(uuid)) {
            activeBombs.get(uuid).remove();
            activeBombs.remove(uuid);
        }
    }
}