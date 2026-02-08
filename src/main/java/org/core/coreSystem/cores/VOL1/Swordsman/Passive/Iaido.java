package org.core.coreSystem.cores.VOL1.Swordsman.Passive;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.core.cool.Cool;
import org.core.effect.debuff.Burn;
import org.core.effect.debuff.Frost;
import org.core.effect.crowdControl.Grounding;
import org.core.effect.crowdControl.Stun;
import org.core.main.coreConfig;
import org.core.coreSystem.cores.VOL1.Swordsman.coreSystem.Swordsman;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Iaido {

    private final Swordsman config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final coreConfig tag;

    public Iaido(Swordsman config, coreConfig tag, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.tag = tag;
    }

    public void Sheath(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        if (main.getType() == Material.IRON_SWORD && off.getType() == Material.BUNDLE) {
            ItemMeta meta = off.getItemMeta();
            if (meta instanceof BundleMeta bundleMeta) {
                if (bundleMeta.getItems().isEmpty()) {
                    bundleMeta.addItem(main.clone());
                    off.setItemMeta(bundleMeta);

                    player.getInventory().setItemInMainHand(off);
                    player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));

                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_BUNDLE_INSERT, 1.0f, 1.0f);
                }
            }
        }

        config.laidoSlash.put(player.getUniqueId(), true);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().clone().add(0, 1.0, 0), 14, 0.4, 0.4, 0.4, 0);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!Stun.isStunned(player) && !Grounding.isGrounded(player) && !Burn.isBurning(player)
                        && !Frost.isFrostbite(player)
                        && config.laidoSlash.getOrDefault(player.getUniqueId(), false)
                        && tag.Swordsman.contains(player)) {

                    Title title = Title.title(
                            Component.empty(),
                            Component.text("Sheath").color(NamedTextColor.YELLOW),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(200))
                    );
                    player.showTitle(title);
                }

                if (!config.laidoSlash.containsKey(player.getUniqueId()) || !tag.Swordsman.contains(player)) {
                    config.laidoSlash.remove(player.getUniqueId());

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void Draw(Player player) {
        World world = player.getWorld();

        PlayerInventory inv = player.getInventory();
        ItemStack main = inv.getItemInMainHand();

        if (main.getType() == Material.BUNDLE) {
            ItemMeta meta = main.getItemMeta();
            if (meta instanceof BundleMeta bundleMeta) {
                List<ItemStack> items = new ArrayList<>(bundleMeta.getItems());

                if (items.size() == 1 && items.get(0).getType() == Material.IRON_SWORD) {
                    ItemStack ironSword = items.get(0).clone();

                    items.clear();
                    bundleMeta.setItems(items);
                    main.setItemMeta(bundleMeta);

                    inv.setItemInMainHand(ironSword);
                    inv.setItemInOffHand(main);

                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, 1.0f, 1.0f);
                }
            }
        }

        config.laidoSlash.remove(player.getUniqueId());
        world.spawnParticle(Particle.SPIT, player.getLocation().clone().add(0, 1.0, 0), 20, 0.2, 0.3, 0.2, 0.5);
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.0f);

        Title title = Title.title(
                Component.empty(),
                Component.text("Draw").color(NamedTextColor.GREEN),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(200))
        );
        player.showTitle(title);
    }
}