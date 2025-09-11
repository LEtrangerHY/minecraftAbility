package org.core.coreProgram.Cores.Bambo.Skill;

import org.bukkit.*;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.coreProgram.Cores.Bambo.coreSystem.Bambo;
import org.core.coreProgram.AbsCoreSystem.SkillBase;

public class Q implements SkillBase {

    private final Bambo config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public Q(Bambo config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {

        World world = player.getWorld();

        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), 4, 0.3, 0.3, 0.3, 1);

        Vector upward = new Vector(0, config.q_Skill_Jump, 0);
        Vector upward2 = new Vector(0, config.q_Skill_Jump * ((double) 3 /4), 0);

        for (Entity entity : world.getNearbyEntities(player.getLocation(), 4, 4, 4)) {
            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

            entity.setVelocity(upward2);

        }

        player.setVelocity(upward);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.0f);

        player.setMetadata("noFallDamage", new FixedMetadataValue(plugin, true));

    }
}
