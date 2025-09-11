package org.core.coreProgram.Cores.Pyro.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Debuff.Burn;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.Pyro.coreSystem.Pyro;

public class F implements SkillBase {

    private final Pyro config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Pyro config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){

        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

        if(offhandItem.getType() == Material.BLAZE_POWDER && offhandItem.getAmount() >= 20) {

            player.damage(player.getHealth() / 2);
            PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS, 20 * 14, 3, false, false);
            player.addPotionEffect(slowness);
            PotionEffect fatigue = new PotionEffect(PotionEffectType.MINING_FATIGUE, 20 * 14, 2, false, false);
            player.addPotionEffect(fatigue);


            player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1, 1);

            World world = player.getWorld();
            Location playerLocation = player.getLocation();
            Vector direction = playerLocation.getDirection().normalize().multiply(1.3);

            config.F_collision.put(player.getUniqueId(), false);

            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {

                    Location particleLocation = playerLocation.clone()
                            .add(direction.clone().multiply(ticks * 1.5))
                            .add(0, 1.4, 0);

                    if (ticks >= 14) {
                        if(config.F_collision.getOrDefault(player.getUniqueId(), true)) {
                            config.F_collision.remove(player.getUniqueId());
                            this.cancel();
                            return;
                        }else{
                            config.F_collision.remove(player.getUniqueId());
                            Delay(player, particleLocation);
                            this.cancel();
                            return;
                        }
                    }

                    if(!particleLocation.getBlock().isPassable()){
                        Delay(player, particleLocation);
                        config.F_collision.put(player.getUniqueId(), true);
                    }

                    player.spawnParticle(Particle.FLAME, particleLocation, 3, 0.1, 0.1, 0.1, 0);
                    player.spawnParticle(Particle.SMOKE, particleLocation, 2, 0.1, 0.1, 0.1, 0);

                    for (Entity entity : world.getNearbyEntities(particleLocation, 0.5, 0.5, 0.5)) {
                        if (entity instanceof LivingEntity target && entity != player) {
                            Delay(player, particleLocation);
                            config.F_collision.put(player.getUniqueId(), true);
                            break;
                        }
                    }

                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);


            offhandItem.setAmount(offhandItem.getAmount() - 20);
        }else{
            player.playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
            player.sendActionBar(Component.text("powder needed").color(NamedTextColor.RED));
            long cools = 100L;
            cool.updateCooldown(player, "F", cools);
        }

    }

    public void Delay(Player player, Location burstLoction){
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (ticks >= 10) {

                    LASTBURST(player, burstLoction);

                    this.cancel();
                    return;
                }

                player.spawnParticle(Particle.FLASH, burstLoction, 30, 0.5, 0.5, 0.5, 0.9);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void LASTBURST(Player player, Location burstLoction){

        World world = player.getWorld();

        player.playSound(player.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1, 1);
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1, 1);
        player.spawnParticle(Particle.FLAME, burstLoction, 70, 0.1, 0.1, 0.1, 0.9);
        player.spawnParticle(Particle.FLASH, burstLoction, 10, 0.3, 0.3, 0.3, 0.9);
        player.spawnParticle(Particle.END_ROD, burstLoction.clone().add(0, 1.2, 0), 70, 0.7, 0.7, 0.7, 0.7);
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, burstLoction.clone().add(0, 1.2, 0), 70, 3, 3, 3, 0.7);
        player.spawnParticle(Particle.SOUL_FIRE_FLAME, burstLoction, 70, 0.1, 0.1, 0.1, 0.9);
        player.spawnParticle(Particle.SOUL_FIRE_FLAME, burstLoction.clone().add(0, 1, 0), 140, 7, 7, 7, 0);

        for (Entity entity : world.getNearbyEntities(burstLoction, 7.7, 7.7, 7.7)) {
            if (entity instanceof LivingEntity target && entity != player) {

                if (Math.random() < 0.3) {
                    Burn burn = new Burn(target, 14000L);
                    burn.applyEffect(player);
                }

                Vector direction = entity.getLocation().toVector().subtract(burstLoction.toVector()).normalize().multiply(0.5);
                direction.setY(0.5);

            }
        }

        float power = 10.0f;
        boolean setFire = true;
        boolean breakBlocks = true;

        player.getWorld().createExplosion(burstLoction, power, setFire, breakBlocks);
    }

}
