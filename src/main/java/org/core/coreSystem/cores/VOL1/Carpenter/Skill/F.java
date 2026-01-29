package org.core.coreSystem.cores.VOL1.Carpenter.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL1.Carpenter.coreSystem.Carpenter;

import java.time.Duration;

public class F implements SkillBase {

    private final Carpenter config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public F(Carpenter config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player) {
        int currentCount = config.f_count.getOrDefault(player.getUniqueId(), 0);

        if(currentCount < 3){
            config.f_count.put(player.getUniqueId(), currentCount + 1);

            int remaining = 3 - (currentCount + 1);
            String msg = (remaining == 0) ? "Ready" : remaining + " left";

            Title title = Title.title(
                    Component.empty(),
                    Component.text(msg).color(NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(230), Duration.ofMillis(200))
            );
            player.showTitle(title);

            heal(player);
            cool.updateCooldown(player, "F", 430L);
        }else{
            config.f_count.remove(player.getUniqueId());

            Title title = Title.title(
                    Component.empty(),
                    Component.text("Totem Gained!").color(NamedTextColor.GOLD),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(400), Duration.ofMillis(300))
            );
            player.showTitle(title);

            totem(player);
            cool.updateCooldown(player, "F", 120000L);
        }
    }

    public void heal(Player player){

        World world = player.getWorld();

        world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);
        world.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1, 1);

        world.spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 3, 0.3, 0.4, 0.3, 0);
        world.spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 43, 0.5, 0.5, 0.5, 0);
        world.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 43, 0.7, 0.7, 0.7, 0);

        player.heal(config.f_Skill_heal);

    }

    public void totem(Player player){

        World world = player.getWorld();

        world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);
        world.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1, 1);
        world.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1, 1);

        world.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1.2, 0), 12, 0.3, 0.4, 0.3, 0);
        world.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1.2, 0), 24, 0.3, 0.4, 0.3, 0.7);
        world.spawnParticle(Particle.WITCH, player.getLocation().add(0, 1.2, 0), 43, 0.5, 0.5, 0.5, 0);
        world.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1.2, 0), 43, 0.7, 0.7, 0.7, 0);

        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);
        ItemStack setItem = ItemStack.of(Material.TOTEM_OF_UNDYING);

        if (offhandItem.getType() == Material.AIR) {
            player.getInventory().setItemInOffHand(setItem);
        }else{
            player.give(setItem);
        }

    }

}