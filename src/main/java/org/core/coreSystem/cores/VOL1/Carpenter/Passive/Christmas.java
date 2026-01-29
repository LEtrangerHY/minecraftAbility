package org.core.coreSystem.cores.VOL1.Carpenter.Passive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.EffectManager;
import org.core.main.coreConfig;
import org.core.coreSystem.cores.VOL1.Carpenter.coreSystem.Carpenter;

public class Christmas {

    private final coreConfig tag;
    private final Carpenter config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private EffectManager effectManager = new EffectManager();

    public Christmas(coreConfig tag, Carpenter config, JavaPlugin plugin, Cool cool) {
        this.tag = tag;
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    public void recurrent(Player player){

        World world = player.getWorld();

        world.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1);
        world.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1.2, 0), 120, 1.2, 1.2, 1.2, 0);
        world.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1.2, 0), 70, 1.2, 1.2, 1.2, 0.7);
        int duration = 430;

        PotionEffect resistance = new PotionEffect(PotionEffectType.RESISTANCE, duration, 3);

        player.addPotionEffect(resistance);

        player.heal(12);

        Location center = player.getLocation();

        for (Entity entity : world.getNearbyEntities(center, 5, 5, 5)) {
            if (entity.equals(player) || !(entity instanceof LivingEntity) || entity.isInvulnerable()) continue;

            Vector direction = entity.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.2);
            direction.setY(0.7);

            entity.setVelocity(direction);
        }

    }
}
