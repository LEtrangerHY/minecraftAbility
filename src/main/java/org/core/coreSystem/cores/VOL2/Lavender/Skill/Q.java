package org.core.coreSystem.cores.VOL2.Lavender.Skill;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL2.Lavender.coreSystem.Lavender;
import org.core.effect.crowdControl.Invulnerable;
import org.core.effect.crowdControl.Stiff;

public class Q implements SkillBase {

    private final Lavender config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private final R r;

    public Q(Lavender config, JavaPlugin plugin, Cool cool, R r) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.r = r;
    }

    @Override
    public void Trigger(Player player){
        int balIndex = config.bladeBallistic.getOrDefault(player.getUniqueId(), 0);

        r.Retract(player);
        cool.updateCooldown(player, "Q", config.q_Skill_Cool - 1000L * balIndex);
        cool.updateCooldown(player, "R", 0L);
        Stiff.breakStiff(player);

        Location startLocation = player.getLocation();

        Vector direction = startLocation.getDirection().normalize().multiply(config.q_Skill_dash + balIndex * 0.1);

        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        Invulnerable invulnerable = new Invulnerable(player, 500);
        invulnerable.applyEffect(player);
    }
}