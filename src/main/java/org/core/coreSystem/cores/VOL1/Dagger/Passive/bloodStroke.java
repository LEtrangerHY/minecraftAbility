package org.core.coreSystem.cores.VOL1.Dagger.Passive;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.cool.Cool;
import org.core.effect.crowdControl.EffectManager;
import org.core.effect.crowdControl.ForceDamage;
import org.core.main.coreConfig;
import org.core.coreSystem.cores.VOL1.Dagger.coreSystem.Dagger;

public class bloodStroke {

    private final coreConfig tag;
    private final Dagger config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private EffectManager effectManager = new EffectManager();

    public bloodStroke(coreConfig tag, Dagger config, JavaPlugin plugin, Cool cool) {
        this.tag = tag;
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    public void damageStroke(Player player, LivingEntity entity){

        BlockData blood = Material.REDSTONE_BLOCK.createBlockData();

        player.getWorld().spawnParticle(Particle.BLOCK, entity.getLocation().clone().add(0, 1.2, 0), 13, 0.3, 0.3, 0.3,
                blood);

        DamageSource source = DamageSource.builder(DamageType.GENERIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        ForceDamage forceDamage = new ForceDamage(entity, entity.getHealth() * 0.13, source, true);
        forceDamage.applyEffect(player);

    }
}
