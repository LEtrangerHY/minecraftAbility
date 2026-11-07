package org.core.coreSystem.cores.VOL3.Cheshire.coreSystem;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class Cheshire {

    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public long passive_Cool = 30000;
    public HashSet<UUID> passiveLoaded = new HashSet<>();
    public HashSet<UUID> voidState = new HashSet<>();
    public HashSet<UUID> invisState = new HashSet<>();
    public HashMap<UUID, Location> smileLocations = new HashMap<>();
    public HashMap<UUID, Vector> smileDirections = new HashMap<>();
    public HashMap<UUID, Integer> smileTasks = new HashMap<>();

    //R
    public double r_Skill_amp = 0.2;
    public double r_Skill_damage = 4;
    public long r_Skill_Cool = 600;
    public HashMap<UUID, Boolean> rSwingLeftToRight = new HashMap<>();

    //Q
    public double q_Skill_amp = 0.2;
    public double q_Skill_damage = 3;
    public long q_Skill_Cool = 8000;

    //F
    public double f_Skill_amp = 0.2;
    public double f_Skill_damage = 0;
    public long f_Skill_Cool = 6000;
    public HashMap<UUID, Boolean> fUltBuffActive = new HashMap<>();
    public HashMap<UUID, Integer> fUltSmileCounts = new HashMap<>();
    public HashMap<UUID, List<Location>> activeSmiles = new HashMap<>();
    public HashMap<UUID, List<Integer>> activeSmileTasks = new HashMap<>();

    public void variableReset(Player player){
        UUID uuid = player.getUniqueId();
        R_COOLDOWN.remove(uuid);
        Q_COOLDOWN.remove(uuid);
        F_COOLDOWN.remove(uuid);

        passiveLoaded.remove(uuid);
        voidState.remove(uuid);
        invisState.remove(uuid);
        smileLocations.remove(uuid);
        smileDirections.remove(uuid);
        smileTasks.remove(uuid);

        rSwingLeftToRight.remove(uuid);

        fUltBuffActive.remove(uuid);
        fUltSmileCounts.remove(uuid);
        activeSmiles.remove(uuid);
        activeSmileTasks.remove(uuid);
    }
}