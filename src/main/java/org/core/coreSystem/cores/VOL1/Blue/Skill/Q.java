package org.core.coreSystem.cores.VOL1.Blue.Skill;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Blue.coreSystem.Blue;

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

        Particle.DustOptions dustOption_flowerDust = new Particle.DustOptions(Color.AQUA, 0.6f);
        Particle.DustOptions dustOption_flowerDust_gra = new Particle.DustOptions(Color.NAVY, 0.6f);

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

                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 13, 0.5, 0.1, 0.5, 0);

                if (Math.random() < 0.6) {
                    world.spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 6, 0.4, 0.6, 0.4, 0, dustOption_flowerDust_gra);
                }else{
                    world.spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 6, 0.4, 0.6, 0.4, 0, dustOption_flowerDust);
                }

                tick += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }
}