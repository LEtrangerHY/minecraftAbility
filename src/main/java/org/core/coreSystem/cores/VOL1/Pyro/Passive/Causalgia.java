package org.core.coreSystem.cores.VOL1.Pyro.Passive;

import org.bukkit.Particle;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.core.cool.Cool;
import org.core.effect.crowdControl.EffectManager;
import org.core.main.coreConfig;
import org.core.coreSystem.cores.VOL1.Pyro.coreSystem.Pyro;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class Causalgia {

    private final coreConfig tag;
    private final Pyro config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private EffectManager effectManager = new EffectManager();

    public Causalgia(coreConfig tag, Pyro config, JavaPlugin plugin, Cool cool) {
        this.tag = tag;
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    public void addCausalgia(Player player, Entity entity){
        config.causalgia.put(entity.getUniqueId(), 1);
    }

    public void handleCausalgia(){

    }

    private final Map<Entity, BukkitRunnable> particleUse = new HashMap<>();

    public void causalgiaParticle(Player player, Entity target) {
        BukkitRunnable particle = new BukkitRunnable() {
            @Override
            public void run() {

                int t = config.causalgia.getOrDefault(target.getUniqueId(), 0);

                if (target.isDead() || !player.isOnline()) {

                    particleUse.remove(target);

                    this.cancel();
                    return;
                }

                target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().clone().add(0, 1.4, 0), t, 0, 0, 0, 0.08);
            }
        };

        particleUse.put(target, particle);
        particle.runTaskTimer(plugin, 0L, 10L);
    }

}
