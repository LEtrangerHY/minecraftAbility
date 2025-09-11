package org.core.coreProgram.Cores.Dagger.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.ForceDamage;
import org.core.Effect.Invulnerable;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.Dagger.Passive.DamageStroke;
import org.core.coreProgram.Cores.Dagger.coreSystem.Dagger;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

public class F implements SkillBase {
    private final Dagger config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final DamageStroke damagestroker;

    public F(Dagger config, JavaPlugin plugin, Cool cool, DamageStroke damagestroker) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.damagestroker = damagestroker;
    }

    @Override
    public void Trigger(Player player) {
        if(!config.f_using.getOrDefault(player.getUniqueId(), false) && !config.f_dash.getOrDefault(player.getUniqueId(), false)){
            player.sendActionBar(Component.text("left click to slash").color(NamedTextColor.DARK_RED));
            cool.updateCooldown(player, "F", 0L);
            X_slash_on(player);
        }else if(!config.f_using.getOrDefault(player.getUniqueId(), false) && config.f_dash.getOrDefault(player.getUniqueId(), false)){
            X_finish(player);
        }else{
            player.sendActionBar(Component.text("dash is not set").color(NamedTextColor.DARK_RED));
            cool.updateCooldown(player, "F", 0L);
        }
    }

    public void X_slash_on(Player player){

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);

        cool.setCooldown(player, 10000L, "X");

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if(ticks % 10 == 0){
                    player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1.0, 0), 13, 0.5, 0.1, 0.5, 0);
                }

                if(ticks >= 200 || config.f_slash.getOrDefault(player.getUniqueId(), 0) >= 2){
                    long cools = 50L;
                    cool.updateCooldown(player, "X", cools);
                    config.f_using.remove(player.getUniqueId());
                    config.f_slash.remove(player.getUniqueId());
                    if(!config.dash_object.getOrDefault(player.getUniqueId(), new LinkedHashSet<>()).isEmpty()){
                        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1, 1);
                        config.dash_num.put(player.getUniqueId(), config.dash_object.getOrDefault(player.getUniqueId(), new LinkedHashSet<>()).size());
                        config.f_dash.put(player.getUniqueId(), true);

                        player.sendActionBar(Component.text(config.dash_object.getOrDefault(player.getUniqueId(), new LinkedHashSet<>()).size() + " set").color(NamedTextColor.DARK_RED));
                    }else{
                        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
                        config.f_dash.remove(player.getUniqueId());
                        config.dash_object.remove(player.getUniqueId());
                        player.sendActionBar(Component.text("no slash").color(NamedTextColor.DARK_RED));
                        cool.setCooldown(player, 10000L, "F");
                    }
                    this.cancel();
                    return;
                }

                config.f_using.put(player.getUniqueId(), true);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void X_finish(Player player){
        player.swingMainHand();

        Location startLocation = player.getLocation();

        Vector direction = startLocation.getDirection().normalize().multiply(config.f_Skill_dash);

        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);
        player.spawnParticle(Particle.SPIT, player.getLocation().add(0, 1.0, 0), 7, 0.1, 0.2, 0.1, 0.5);

        Invulnerable invulnerable = new Invulnerable(player, 500);
        invulnerable.applyEffect(player);

        if(!config.dash_object.getOrDefault(player.getUniqueId(), new LinkedHashSet<>()).isEmpty()){
            detect(player);
            config.dash_object.getOrDefault(player.getUniqueId(), new LinkedHashSet<>()).removeFirst();
            player.sendActionBar(Component.text(config.dash_object.getOrDefault(player.getUniqueId(), new LinkedHashSet<>()).size()).color(NamedTextColor.DARK_RED));
            cool.updateCooldown(player, "F", 50L);
            if(config.dash_object.getOrDefault(player.getUniqueId(), new LinkedHashSet<>()).isEmpty()){
                config.dash_object.remove(player.getUniqueId());
                config.f_dash.remove(player.getUniqueId());
                player.sendActionBar(Component.text("over").color(NamedTextColor.DARK_RED));
                cool.updateCooldown(player, "F", 10000L);
            }
        }
    }

    public void detect(Player player){

        config.f_damaged_2.put(player.getUniqueId(), new HashSet<>());

        new BukkitRunnable() {
            private double ticks = 0;

            @Override
            public void run() {

                if (ticks > 5 || player.isDead()) {
                    if(config.dash_object.getOrDefault(player.getUniqueId(), new LinkedHashSet<>()).isEmpty()){
                        config.dash_num.remove(player.getUniqueId());
                    }

                    config.f_damaged_2.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                player.spawnParticle(Particle.CRIT, player.getLocation().add(0, 1.0, 0), 7, 0.4, 0.4, 0.4, 0);
                player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1.0, 0), 20, 0.5, 0.1, 0.5, 0);

                List<Entity> nearbyEntities = player.getNearbyEntities(0.6, 0.6, 0.6);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player && !config.f_damaged_2.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                        config.f_damaging.put(player.getUniqueId(), true);
                        damagestroker.damageStroke(player, target);
                        ForceDamage forceDamage = new ForceDamage(target, config.f_Skill_Damage_2 / config.dash_num.getOrDefault(player.getUniqueId(), 1));
                        forceDamage.applyEffect(player);
                        target.setVelocity(new Vector(0, 0, 0));
                        config.f_damaging.remove(player.getUniqueId());

                        config.f_damaged_2.getOrDefault(player.getUniqueId(), new HashSet<>()).add(target);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}