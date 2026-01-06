package org.core.coreSystem.cores.VOL1.Saboteur.coreSystem;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

public class Saboteur {
    //CoolHashmap
    public HashMap<UUID, Long> R_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> Q_COOLDOWN = new HashMap<>();
    public HashMap<UUID, Long> F_COOLDOWN = new HashMap<>();

    public long frozenCool = 10000;

    //passive
    public HashMap<UUID, Integer> trapType = new HashMap<>();
    public HashMap<UUID, Boolean> collision = new HashMap<>();

    //R
    public HashMap<UUID, List<Location>> trapSpikePos = new HashMap<>();
    public HashMap<UUID, Boolean> trapSpikeDamage = new HashMap<>();
    public HashMap<UUID, List<Location>> trapThrowPos = new HashMap<>();
    public HashMap<UUID, LivingEntity> trapTarget = new HashMap<>();
    public long r_Skill_Cool = 1800;
    public double r_Skill_amp = 0.06;

    public HashMap<UUID, Boolean> r_skillUsing_Hack = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> r_damaged_Sweep_Hack = new HashMap<>();
    public HashMap<UUID, Boolean> r_skillUsing_Sweep_Hack = new HashMap<>();
    public long r_Skill_Cool_HACK = 300;
    public long r_Skill_Damage_Spike_HACK = 5;
    public long r_Skill_Damage_Throw_HACK = 4;

    //Q
    public HashMap<UUID, Location> trapPedalPos = new HashMap<>();
    public HashMap<UUID, Boolean> trapActive = new HashMap<>();
    public long q_Skill_Cool = 12000;

    public HashMap<UUID, Integer> q_skillCount_Hack = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> q_damaged_Hack = new HashMap<>();
    public HashMap<UUID, HashSet<Entity>> q_damaged_Sweep_Hack = new HashMap<>();
    public HashMap<UUID, Boolean> q_skillUsing_Hack = new HashMap<>();
    public HashMap<UUID, Boolean> q_skillUsing_Sweep_Hack = new HashMap<>();
    public long q_Skill_Cool_HACK = 2000;
    public double q_Skill_Dash_HACK = 1.5;
    public long q_Skill_Damage_HACK = 3;

    //F
    public HashMap<UUID, Boolean> isHackAway = new HashMap<>();
    public long f_Skill_Cool = 44440;
    public double f_Skill_amp = 0.13;


    public void variableReset(Player player){

        R_COOLDOWN.remove(player.getUniqueId());
        Q_COOLDOWN.remove(player.getUniqueId());
        F_COOLDOWN.remove(player.getUniqueId());

        trapType.remove(player.getUniqueId());
        collision.remove(player.getUniqueId());

        trapSpikePos.remove(player.getUniqueId());
        trapSpikeDamage.remove(player.getUniqueId());
        trapThrowPos.remove(player.getUniqueId());
        trapTarget.remove(player.getUniqueId());

        r_skillUsing_Hack.remove(player.getUniqueId());
        r_damaged_Sweep_Hack.remove(player.getUniqueId());
        r_skillUsing_Sweep_Hack.remove(player.getUniqueId());

        q_skillCount_Hack.remove(player.getUniqueId());
        q_damaged_Hack.remove(player.getUniqueId());
        q_damaged_Sweep_Hack.remove(player.getUniqueId());
        q_skillUsing_Hack.remove(player.getUniqueId());
        q_skillUsing_Sweep_Hack.remove(player.getUniqueId());

        isHackAway.remove(player.getUniqueId());

    }
}
