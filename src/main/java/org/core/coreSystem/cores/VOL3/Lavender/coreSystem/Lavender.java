package org.core.coreSystem.cores.VOL3.Lavender.coreSystem;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class Lavender {

    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //R
    public HashMap<UUID, Boolean> r_Skill_using = new HashMap<>();
    public HashMap<UUID, Integer> bladeBallistic = new HashMap<>();
    public HashMap<UUID, Boolean> bladeShoot = new HashMap<>();
    public double r_Skill_amp = 0.1;
    public double r_Skill_damage = 1;
    public long r_Skill_Cool = 600;
    public long r_re_Skill_Cool = 6000;

    //Q
    public double q_Skill_dash = 1.4;
    public long q_Skill_Cool = 6000;

    //F
    public HashMap<Location, UUID> activeWalls = new HashMap<>();
    public HashMap<UUID, Location> transportPos = new HashMap<>();
    public long f_Skill_ground = 3000;
    public double f_Skill_dash = 1.6;
    public long f_Skill_Cool = 33000;

    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

    }
}
