package org.core.coreSystem.cores.VOL1.Commander.Skill;

import org.core.models.skillObj.comBlockModel;
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

public class R implements SkillBase, Listener {

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

        FallingBlock fb = new comBlockModel(world, spawnLoc).getBlock();

        double speed = 1.2;
        fb.setVelocity(dir.multiply(speed));

        Particle.DustOptions coreDust = new Particle.DustOptions(Color.fromRGB(255, 150, 50), 1.0f);
        Particle.DustOptions swirlDust = new Particle.DustOptions(Color.fromRGB(0, 200, 255), 0.8f);
        BlockData command = Material.COMMAND_BLOCK.createBlockData();

        world.playSound(spawnLoc, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 1.5f);
        world.playSound(spawnLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.8f);

        world.spawnParticle(Particle.SOUL_FIRE_FLAME, spawnLoc, 15, 0.2, 0.2, 0.2, 0.1);

        config.damaged.put(player.getUniqueId(), new HashSet<>());

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_Damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            int life = 8;
            int ticks = 0;

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

                        world.playSound(fb.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 1.2f);
                        world.playSound(fb.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
                        world.spawnParticle(Particle.SONIC_BOOM, fb.getLocation().add(0, 0.5, 0), 1);
                        world.spawnParticle(Particle.BLOCK, fb.getLocation(), 30, 0.4, 0.4, 0.4, command);
                        world.spawnParticle(Particle.ELECTRIC_SPARK, fb.getLocation(), 15, 0.5, 0.5, 0.5, 0.1);

                        config.comBlocks.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(fb);
                    }
                    fb.setVelocity(new Vector(0, 0, 0));

                } else {
                    Location currentLoc = fb.getLocation();
                    Vector velocity = fb.getVelocity();

                    if (velocity.lengthSquared() > 0) {
                        Vector axis = velocity.clone().normalize();

                        Vector p1 = new Vector(-axis.getZ(), 0, axis.getX());
                        if (p1.lengthSquared() < 0.001) {
                            p1 = new Vector(1, 0, 0);
                        }
                        p1.normalize();

                        double radius = 0.6;
                        double angle = ticks * 0.8;

                        Vector offset1 = p1.clone().multiply(radius).rotateAroundAxis(axis, angle);
                        Vector offset2 = p1.clone().multiply(radius).rotateAroundAxis(axis, angle + Math.PI);

                        world.spawnParticle(Particle.DUST, currentLoc.clone().add(offset1), 1, 0, 0, 0, 0, swirlDust);
                        world.spawnParticle(Particle.DUST, currentLoc.clone().add(offset2), 1, 0, 0, 0, 0, swirlDust);
                    }

                    world.spawnParticle(Particle.DUST, currentLoc, 2, 0.1, 0.1, 0.1, 0, coreDust);
                    if (ticks % 2 == 0) {
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, currentLoc, 1, 0.1, 0.1, 0.1, 0);
                    }

                    for (Entity e : world.getNearbyEntities(currentLoc, 0.7, 0.7, 0.7)) {
                        if (e instanceof LivingEntity le && !le.equals(player) && !config.damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(le)) {

                            config.damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).add(le);

                            world.playSound(currentLoc, Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
                            world.playSound(currentLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 2.0f);

                            ForceDamage forceDamage = new ForceDamage(le, damage, source, false);
                            forceDamage.applyEffect(player);

                            world.spawnParticle(Particle.BLOCK, currentLoc, 20, 0.3, 0.3, 0.3, command);
                        }
                    }

                    Location nextLoc = currentLoc.clone().add(fb.getVelocity().clone().multiply(1.5));
                    Block nextBlock = nextLoc.getBlock();

                    if (!nextBlock.isPassable()) {
                        fb.setVelocity(new Vector(0, 0, 0));
                    }

                    life--;
                    ticks++;
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