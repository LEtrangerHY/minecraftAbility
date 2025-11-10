package org.core.coreProgram.Cores.Blue.Skill;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.core.Cool.Cool;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.Blue.coreSystem.Blue;

public class Q implements SkillBase {
    private final Blue config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public Q(Blue config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){

        World world = player.getWorld();

        cool.setCooldown(player, 10000L, "Absorb");

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {

                config.qSoulAbsorb.put(player.getUniqueId(), true);

                if (!player.isOnline() || player.isDead() || tick > 20 * 10 || !config.qSoulAbsorb.getOrDefault(player.getUniqueId(), false)) {
                    config.qSoulAbsorb.remove(player.getUniqueId());
                    cool.updateCooldown(player , "Absorb", 0L);
                    cancel();
                    return;
                }

                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1.3, 0), 13, 0.5, 0.1, 0.5, 0);

                tick += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
}