package org.core.coreProgram.Cores.Blaze.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.coreProgram.Abs.SkillBase;
import org.core.coreProgram.Cores.Blaze.Passive.BlueFlame;
import org.core.coreProgram.Cores.Blaze.coreSystem.Blaze;

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

        if (offhandItem.getType() == Material.SOUL_LANTERN || ((offhandItem.getType() == Material.SOUL_SAND || offhandItem.getType() == Material.SOUL_SOIL) && offhandItem.getAmount() >= 12)) {

            if((offhandItem.getType() == Material.SOUL_SAND || offhandItem.getType() == Material.SOUL_SOIL) && offhandItem.getAmount() >= 12) {
                offhandItem.setAmount(offhandItem.getAmount() - 12);
            }

        }else{
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1, 1);
            player.sendActionBar(Component.text("Soul needed").color(NamedTextColor.RED));
            long cools = 100L;
            cool.updateCooldown(player, "Q", cools);
        }
    }
}
