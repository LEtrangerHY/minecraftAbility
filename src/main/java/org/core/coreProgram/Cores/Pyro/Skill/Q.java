package org.core.coreProgram.Cores.Pyro.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.Cool.Cool;
import org.core.Debuff.Burn;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.Cores.Pyro.coreSystem.Pyro;
import java.util.Random;

public class Q implements SkillBase {

    private final Pyro config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public Q(Pyro config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    @Override
    public void Trigger(Player player){

        ItemStack offhandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

        if(offhandItem.getType() == Material.BLAZE_POWDER && offhandItem.getAmount() >= 7) {
            World world = player.getWorld();
            Location center = player.getLocation();

            player.getWorld().playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
            player.getWorld().playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);

            player.spawnParticle(Particle.FLASH, center, 10, 0.3, 0.3, 0.3, 0.9);
            player.spawnParticle(Particle.END_ROD, center.clone().add(0, 1.2, 0), 70, 0.7, 0.7, 0.7, 0.7);
            player.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 21, 0.1, 0.1, 0.1, 0.9);
            player.spawnParticle(Particle.FLAME, center, 21, 0.1, 0.1, 0.1, 0.9);
            player.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 1, 0), 140, 7, 7, 7, 0);

            Random random = new Random();
            int radius = 7;
            int fireCount = 50;

            for (int i = 0; i < fireCount; i++) {
                int x = random.nextInt(radius * 2 + 1) - radius;
                int z = random.nextInt(radius * 2 + 1) - radius;

                Location fireLoc = center.clone().add(x, 0, z);
                fireLoc.setY(world.getHighestBlockYAt(fireLoc) + 1); // 가장 위 공기층 (지표면 위)

                Block block = fireLoc.getBlock();

                if (block.getType() == Material.AIR) {
                    block.setType(Material.FIRE);
                }
            }

            for (Entity rangeTarget : world.getNearbyEntities(center, 7.7, 7.7, 7.7)) {
                if (rangeTarget instanceof LivingEntity target) {

                    if (rangeTarget == player) {
                        ((Player) rangeTarget).heal(player.getHealth() / 2);
                    }

                    Burn burn = new Burn(target, 3000L);
                    burn.applyEffect(player);
                }
            }

            offhandItem.setAmount(offhandItem.getAmount() - 7);
        }else{
            player.playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
            player.sendActionBar(Component.text("powder needed").color(NamedTextColor.RED));
            long cools = 100L;
            cool.updateCooldown(player, "Q", cools);
        }
    }
}
