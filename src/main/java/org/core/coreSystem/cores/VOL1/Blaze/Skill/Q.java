package org.core.coreSystem.cores.VOL1.Blaze.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Blaze.Passive.BlueFlame;
import org.core.coreSystem.cores.VOL1.Blaze.coreSystem.Blaze;

import java.time.Duration;

public class Q implements SkillBase {
    private final Blaze config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final BlueFlame blueFlame;

    public Q(Blaze config, JavaPlugin plugin, Cool cool, BlueFlame blueFlame) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.blueFlame = blueFlame;
    }

    @Override
    public void Trigger(Player player) {
        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);
        Material type = offhandItem.getType();

        if (type == Material.SOUL_LANTERN || ((type == Material.SOUL_SAND || type == Material.SOUL_SOIL) && offhandItem.getAmount() > 20)) {

            player.spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().clone().add(0, 0.6, 0), 130, 0.1, 0.1, 0.1, 0.8);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PARROT_IMITATE_BLAZE, 1, 1);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_BURN, 1, 1);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1, 1);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1, 1);

            config.BurstBlaze.put(player.getUniqueId(), true);

            cool.setCooldown(player, 13000L, "BurstBlaze", "boss");

            PotionEffect wither = new PotionEffect(PotionEffectType.WITHER, 120, 1, false, false);
            player.addPotionEffect(wither);

            PotionEffect fireResistance = new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 260, 4, false, false);
            player.addPotionEffect(fireResistance);

            new BukkitRunnable(){
                int tick = 0;

                @Override
                public void run(){

                    if(tick > 65 || player.isDead()){
                        cool.updateCooldown(player, "BurstBlaze", 0L, "boss");
                        config.BurstBlaze.remove(player.getUniqueId());
                        cancel();
                        return;
                    }

                    player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().clone().add(0, 0.6, 0), 4, 0.3, 0.3, 0.3, 0);
                    player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().clone().add(0, 0.6, 0), 1, 0.3, 0.3, 0.3, 0);
                    player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().clone().add(0, 0.6, 0), 2, 0.6, 0.6, 0.6, 0.04);

                    tick++;

                }
            }.runTaskTimer(plugin, 0L, 4L);

            if ((type == Material.SOUL_SAND || type == Material.SOUL_SOIL) && offhandItem.getAmount() >= 20) {
                offhandItem.setAmount(offhandItem.getAmount() - 20);
            }
        } else {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1f, 1f);

            Title title = Title.title(
                    Component.empty(),
                    Component.text("soul sand needed").color(NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
            );
            player.showTitle(title);

            cool.updateCooldown(player, "Q", 500L);
        }
    }

}