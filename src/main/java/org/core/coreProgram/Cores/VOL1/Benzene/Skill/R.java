package org.core.coreProgram.Cores.VOL1.Benzene.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.Effect.Invulnerable;
import org.core.coreProgram.Cores.VOL1.Benzene.Passive.ChainCalc;
import org.core.coreProgram.Cores.VOL1.Benzene.coreSystem.Benzene;
import org.core.coreProgram.AbsCoreSystem.SkillBase;

import java.util.HashSet;
import java.util.List;

public class R implements SkillBase {

    private final Benzene config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final ChainCalc chainCalc;

    public R(Benzene config, JavaPlugin plugin, Cool cool, ChainCalc chainCalc) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.chainCalc = chainCalc;
    }

    @Override
    public void Trigger(Player player) {

        player.swingMainHand();

        Location startLocation = player.getLocation();

        Vector direction = startLocation.getDirection().normalize().multiply(config.r_Skill_dash);

        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        Invulnerable invulnerable = new Invulnerable(player, 600);
        invulnerable.applyEffect(player);

        detect(player);

    }

    public void detect(Player player){

        config.rskill_using.put(player.getUniqueId(), true);
        config.damaged_2.put(player.getUniqueId(), new HashSet<>());

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.GENERIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        config.atkCount.put(player.getUniqueId(), 0);
        player.sendActionBar(Component.text("⌬").color(NamedTextColor.GRAY));

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(111, 111, 111), 0.6f);
        Particle.DustOptions dustOptions_small = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.6f);
        BlockData chain = Material.OXIDIZED_COPPER_CHAIN.createBlockData();

        new BukkitRunnable() {
            private double ticks = 0;

            @Override
            public void run() {

                if(ticks < 4){
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.6f, 1.0f);
                    player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().clone().add(0, 1.2, 0), 6, 0.3, 0.3, 0.3,
                            chain);
                }

                if (ticks > 6 || player.isDead()) {
                    config.rskill_using.remove(player.getUniqueId());
                    config.damaged_2.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 66, 0.3, 0, 0.3, 0.08, dustOptions);
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 66, 0.3, 0, 0.3, 0.08, dustOptions_small);

                List<Entity> nearbyEntities = player.getNearbyEntities(1.2, 1.2, 1.2);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player && !config.damaged_2.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                        ForceDamage forceDamage = new ForceDamage(target, damage, source);
                        forceDamage.applyEffect(player);

                        config.damaged_2.getOrDefault(player.getUniqueId(), new HashSet<>()).add(target);
                        target.setVelocity(new Vector(0, 0, 0));

                        if(!target.isDead()){
                            chainCalc.increase(player, target);
                            if(target.isDead()){
                                chainCalc.decrease(target);
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
