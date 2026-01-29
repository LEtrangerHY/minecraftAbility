package org.core.coreSystem.cores.VOL1.Bambo.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.coreSystem.cores.VOL1.Bambo.coreSystem.Bambo;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

public class R implements SkillBase {

    private final Bambo config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public R(Bambo config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        if(!config.reloaded.getOrDefault(player.getUniqueId(), false)){

            ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

            if (offhandItem.getType() == Material.IRON_NUGGET && offhandItem.getAmount() >= 1) {
                config.reloaded.put(player.getUniqueId(), true);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1, 1);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);

                long cools = 300L;

                cool.updateCooldown(player, "R", cools);

                Title title = Title.title(
                        Component.empty(),
                        Component.text("Loaded").color(NamedTextColor.GREEN),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(500))
                );
                player.showTitle(title);

                offhandItem.setAmount(offhandItem.getAmount() - 1);
            }else{
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);

                Title title = Title.title(
                        Component.empty(),
                        Component.text("iron needed").color(NamedTextColor.RED),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
                );
                player.showTitle(title);

                long cools = 500L;
                cool.updateCooldown(player, "R", cools);
            }

        }else{

            config.reloaded.remove(player.getUniqueId());
            player.swingMainHand();
            reUse(player);
            player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 1);

        }
    }

    public void reUse(Player player){
        config.damaged.put(player.getUniqueId(), new HashSet<>());

        World world = player.getWorld();

        Location playerLocation = player.getLocation();
        Vector direction = playerLocation.getDirection().normalize().multiply(1.2);

        double amp = config.r_Skill_amp * player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        double damage = config.r_Skill_damage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.MOB_PROJECTILE)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        config.r_damaged.put(player.getUniqueId(), true);
        for (int ticks = 0; ticks < 40; ticks++) {
            Location particleLocation = playerLocation.clone()
                    .add(direction.clone().multiply(ticks * 0.4))
                    .add(0, 1.5, 0);

            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255),  0.8f);
            world.spawnParticle(Particle.DUST, particleLocation, 6, 0, 0, 0, 0, dustOptions);
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, particleLocation, 6, 0, 0, 0, 0);

            for (Entity entity : world.getNearbyEntities(particleLocation, 0.3, 0.3, 0.3)) {
                if (entity instanceof LivingEntity target
                        && entity != player
                        && !config.damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                    ForceDamage forceDamage = new ForceDamage(target, damage, source, true);
                    forceDamage.applyEffect(player);

                    config.damaged.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(target);
                }
            }
        }
        config.r_damaged.put(player.getUniqueId(), false);

        config.damaged.remove(player.getUniqueId());

        Title title = Title.title(
                Component.empty(),
                Component.text("Fired!").color(NamedTextColor.DARK_GREEN),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(200))
        );
        player.showTitle(title);

    }
}