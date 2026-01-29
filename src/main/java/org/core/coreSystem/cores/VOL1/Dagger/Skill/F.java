package org.core.coreSystem.cores.VOL1.Dagger.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Invulnerable;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Dagger.Passive.bloodStroke;
import org.core.coreSystem.cores.VOL1.Dagger.coreSystem.Dagger;

import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

public class F implements SkillBase {
    private final Dagger config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final bloodStroke bloodStroker;

    public F(Dagger config, JavaPlugin plugin, Cool cool, bloodStroke bloodStroker) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.bloodStroker = bloodStroker;
    }

    @Override
    public void Trigger(Player player) {
        if(!config.f_using.getOrDefault(player.getUniqueId(), false) && !config.f_dash.getOrDefault(player.getUniqueId(), false)){

            Title title = Title.title(
                    Component.empty(),
                    Component.text("left click to slash").color(NamedTextColor.DARK_RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(400))
            );
            player.showTitle(title);

            cool.updateCooldown(player, "F", 0L);
            X_slash_on(player);
        }else if(!config.f_using.getOrDefault(player.getUniqueId(), false) && config.f_dash.getOrDefault(player.getUniqueId(), false)){
            X_finish(player);
        }else{

            Title title = Title.title(
                    Component.empty(),
                    Component.text("dash is not set").color(NamedTextColor.DARK_RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(400))
            );
            player.showTitle(title);

            cool.updateCooldown(player, "F", 0L);
        }
    }

    public void X_slash_on(Player player){

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);

        cool.setCooldown(player, 10000L, "X-SLASH", "boss");

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if(ticks % 10 == 0){
                    player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1.0, 0), 13, 0.5, 0.1, 0.5, 0);
                }

                if(ticks >= 200 || config.f_slash.getOrDefault(player.getUniqueId(), 0) >= 2){
                    long cools = 50L;
                    cool.updateCooldown(player, "X-SLASH", cools, "boss");
                    config.f_using.remove(player.getUniqueId());
                    config.f_slash.remove(player.getUniqueId());
                    if(!config.dash_object.getOrDefault(player.getUniqueId(), new LinkedHashSet<>()).isEmpty()){
                        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1, 1);
                        config.dash_num.put(player.getUniqueId(), config.dash_object.getOrDefault(player.getUniqueId(), new LinkedHashSet<>()).size());
                        config.f_dash.put(player.getUniqueId(), true);

                        int count = config.dash_object.getOrDefault(player.getUniqueId(), new LinkedHashSet<>()).size();
                        Title title = Title.title(
                                Component.empty(),
                                Component.text(count + " set").color(NamedTextColor.DARK_RED),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(400))
                        );
                        player.showTitle(title);

                    }else{
                        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
                        config.f_dash.remove(player.getUniqueId());
                        config.dash_object.remove(player.getUniqueId());

                        Title title = Title.title(
                                Component.empty(),
                                Component.text("no slash").color(NamedTextColor.DARK_RED),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(400))
                        );
                        player.showTitle(title);

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

            int count = config.dash_object.getOrDefault(player.getUniqueId(), new LinkedHashSet<>()).size();
            Title title = Title.title(
                    Component.empty(),
                    Component.text(count).color(NamedTextColor.DARK_RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(400))
            );
            player.showTitle(title);

            cool.updateCooldown(player, "F", 50L);
            if(config.dash_object.getOrDefault(player.getUniqueId(), new LinkedHashSet<>()).isEmpty()){
                config.dash_object.remove(player.getUniqueId());
                config.f_dash.remove(player.getUniqueId());

                Title overTitle = Title.title(
                        Component.empty(),
                        Component.text("over").color(NamedTextColor.DARK_RED),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(400))
                );
                player.showTitle(overTitle);

                cool.updateCooldown(player, "F", 10000L);
            }
        }
    }

    public void detect(Player player){

        config.f_damaged_2.put(player.getUniqueId(), new HashSet<>());

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        double damage = config.f_Skill_Damage_2 * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            private double ticks = 0;

            @Override
            public void run() {

                if (ticks > 5 || player.isDead()) {
                    if(config.dash_object.getOrDefault(player.getUniqueId(), new LinkedHashSet<>()).isEmpty()){
                        config.dash_num.remove(player.getUniqueId());
                    }

                    if(config.f_damaged_2.getOrDefault(player.getUniqueId(), new HashSet<>()).isEmpty()) {
                        config.dash_object.remove(player.getUniqueId());
                        config.f_dash.remove(player.getUniqueId());

                        Title title = Title.title(
                                Component.empty(),
                                Component.text("over").color(NamedTextColor.DARK_RED),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(500))
                        );
                        player.showTitle(title);

                        cool.updateCooldown(player, "F", 10000L);
                    }

                    config.f_damaged_2.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1.0, 0), 7, 0.4, 0.4, 0.4, 0);
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1.0, 0), 20, 0.5, 0.1, 0.5, 0);

                List<Entity> nearbyEntities = player.getNearbyEntities(0.6, 0.6, 0.6);
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity target && entity != player && !config.f_damaged_2.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                        config.f_damaging.put(player.getUniqueId(), true);
                        bloodStroker.damageStroke(player, target);

                        ForceDamage forceDamage = new ForceDamage(target, damage / config.dash_num.getOrDefault(player.getUniqueId(), 1), source, true);
                        forceDamage.applyEffect(player);
                        config.f_damaging.remove(player.getUniqueId());

                        config.f_damaged_2.getOrDefault(player.getUniqueId(), new HashSet<>()).add(target);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}