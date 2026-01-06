package org.core.coreSystem.cores.VOL1.Commander.Skill;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Commander.coreSystem.Commander;

import java.util.HashSet;

public class R implements SkillBase, Listener{

    private final Commander config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R(Commander config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){
        World world = player.getWorld();

        Vector dir = player.getEyeLocation().getDirection().normalize();
        Vector spawnOffset = dir.clone().multiply(0.8).add(new Vector(0, 1.2, 0));
        Location spawnLoc = player.getLocation().add(spawnOffset);

        FallingBlock fb = player.getWorld().spawn(
                spawnLoc,
                FallingBlock.class,
                entity -> {
                    entity.setBlockData(Material.COMMAND_BLOCK.createBlockData());
                    entity.setDropItem(false);
                    entity.setHurtEntities(false);
                    entity.setGravity(false);
                    entity.setPersistent(true);
                }
        );

        double speed = 1.2;
        fb.setVelocity(dir.multiply(speed));

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(220, 150, 100), 1.2f);
        Particle.DustOptions dustOptions_gra = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.7f);
        BlockData command = Material.COMMAND_BLOCK.createBlockData();

        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        world.spawnParticle(Particle.ENCHANTED_HIT, spawnLoc, 30, 0.2, 0.2, 0.2, 1);

        config.damaged.put(player.getUniqueId(), new HashSet<>());

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_Damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            int life = 8;

            @Override
            public void run() {

                if (!fb.isValid()) {

                    config.damaged.remove(player.getUniqueId());
                    config.comBlocks.getOrDefault(player.getUniqueId(), new HashSet<>()).remove(fb);
                    cancel();
                    return;

                }

                if (life <= 0) {

                    if(!config.comBlocks.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(fb)) {
                        config.damaged.remove(player.getUniqueId());
                        world.spawnParticle(Particle.BLOCK, fb.getLocation(), 20, 0.3, 0.3, 0.3,
                                command);
                        config.comBlocks.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(fb);
                    }
                    fb.setVelocity(new Vector(0, 0, 0));

                }else {

                    world.spawnParticle(Particle.ENCHANTED_HIT, fb.getLocation(), 2, 0.2, 0.2, 0.2, 0);
                    world.spawnParticle(Particle.DUST, fb.getLocation(), 4, 0.2, 0.2, 0.2, 0, dustOptions);
                    world.spawnParticle(Particle.DUST, fb.getLocation(), 1, 0, 0, 0, 0, dustOptions_gra);

                    for (Entity e : world.getNearbyEntities(fb.getLocation(), 0.7, 0.7, 0.7)) {
                        if (e instanceof LivingEntity le && !le.equals(player) && !config.damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(le)) {

                            config.damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).add(le);

                            world.playSound(fb.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                            ForceDamage forceDamage = new ForceDamage(le, damage, source);
                            forceDamage.applyEffect(player);

                            le.setVelocity(new Vector(0, 0, 0));

                            world.spawnParticle(Particle.BLOCK, fb.getLocation(), 44, 0.3, 0.3, 0.3,
                                    command);
                        }
                    }

                    Location nextLoc = fb.getLocation().clone().add(fb.getVelocity().clone().multiply(1.5));
                    Block nextBlock = nextLoc.getBlock();

                    if (!nextBlock.isPassable()) {
                        fb.setVelocity(new Vector(0, 0, 0));
                    }

                    life--;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void CollideChange(EntityChangeBlockEvent event){
        Block block = event.getBlock();

        if(block.getType() == Material.COMMAND_BLOCK){
            event.setCancelled(true);
        }
    }
}
