package org.core.coreSystem.cores.VOL1.Bambo.Skill;

import org.bukkit.*;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.cores.VOL1.Bambo.coreSystem.Bambo;
import org.core.coreSystem.absCoreSystem.SkillBase;

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

        world.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.0f);

        player.setVelocity(upward);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.getPersistentDataContainer().set(new NamespacedKey(plugin, "noFallDamage"), PersistentDataType.BOOLEAN, true);
        }, 1L);

    }
}
