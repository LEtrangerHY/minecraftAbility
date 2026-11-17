package org.core.coreSystem.cores.VOL6.Carpenter.Skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.cores.VOL6.Carpenter.coreSystem.Carpenter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class F implements SkillBase {

    private static final Particle.DustOptions ZONE = new Particle.DustOptions(Color.fromRGB(120, 226, 140), 0.9f);
    private static final Particle.DustOptions MINT = new Particle.DustOptions(Color.fromRGB(196, 255, 206), 0.6f);

    private final Carpenter config;
    private final JavaPlugin plugin;
    private final Cool cool;
    private final NamespacedKey keyF;
    private final NamespacedKey totemKey;
    private final NamespacedKey activeKey;
    private final NamespacedKey stashKey;

    public F(Carpenter config, JavaPlugin plugin, Cool cool) {
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
        this.keyF = new NamespacedKey(plugin, "F");
        this.totemKey = new NamespacedKey(plugin, "carpenter_totem");
        this.activeKey = new NamespacedKey(plugin, "carpenter_totem_active");
        this.stashKey = new NamespacedKey(plugin, "carpenter_totem_stash");
    }

    @Override
    public void Trigger(Player player) {
        UUID uuid = player.getUniqueId();
        World world = player.getWorld();

        double amp = config.f_Skill_amp * player.getPersistentDataContainer().getOrDefault(keyF, PersistentDataType.LONG, 0L);
        double heal = config.f_Skill_heal * (1 + amp);

        config.f_zone.add(uuid);
        world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
        world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !config.f_zone.contains(uuid)) {
                    config.f_zone.remove(uuid);
                    cancel();
                    return;
                }

                if (tick % 2 == 0) ring(player, tick);
                if (tick % config.f_Heal_Interval == 0) pulse(player, heal);

                if (tick >= config.f_Zone_Ticks) {
                    config.f_zone.remove(uuid);
                    grant(player);
                    cancel();
                    return;
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void pulse(Player player, double heal) {
        Location center = player.getLocation();
        double radius = config.f_Zone_Radius;

        List<Player> allies = new ArrayList<>();
        allies.add(player);
        for (Entity entity : player.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof Player ally) || ally == player || ally.isDead()) continue;
            if (ally.getGameMode() == GameMode.SPECTATOR) continue;
            if (ally.getLocation().distanceSquared(center) > radius * radius) continue;
            allies.add(ally);
        }

        for (Player ally : allies) {
            AttributeInstance maxHealth = ally.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth == null) continue;
            ally.setHealth(Math.min(maxHealth.getValue(), ally.getHealth() + heal));
            ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0, 2.1, 0), 1, 0.2, 0.1, 0.2, 0);
            ally.getWorld().spawnParticle(Particle.DUST, ally.getLocation().add(0, 1.0, 0), 8, 0.3, 0.5, 0.3, 0, MINT);
        }
        player.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.6f, 1.6f);
    }

    private void ring(Player player, int tick) {
        World world = player.getWorld();
        Location center = player.getLocation().add(0, 0.15, 0);
        double radius = config.f_Zone_Radius;
        int points = 56;
        double spin = tick * 0.03;
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points + spin;
            Location at = center.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            world.spawnParticle(Particle.DUST, at, 1, 0, 0, 0, 0, ZONE);
        }
        if (tick % 6 == 0) {
            world.spawnParticle(Particle.HAPPY_VILLAGER, center, 10, radius * 0.5, 0.2, radius * 0.5, 0);
        }
    }

    public boolean isActive(Player player) {
        return player.getPersistentDataContainer().has(activeKey, PersistentDataType.BOOLEAN);
    }

    public boolean isSkillTotem(ItemStack item) {
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(totemKey, PersistentDataType.BOOLEAN);
    }

    private void grant(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerInventory inventory = player.getInventory();
        PersistentDataContainer data = player.getPersistentDataContainer();

        ItemStack off = inventory.getItemInOffHand();
        if (!off.getType().isAir() && !isSkillTotem(off)) {
            int slot = emptySlot(inventory);
            if (slot >= 0) {
                inventory.setItem(slot, off);
            } else {
                config.f_stash.put(uuid, off.clone());
                data.set(stashKey, PersistentDataType.BYTE_ARRAY, off.serializeAsBytes());
            }
        }

        ItemStack totem = ItemStack.of(Material.TOTEM_OF_UNDYING);
        totem.editMeta(meta -> meta.getPersistentDataContainer().set(totemKey, PersistentDataType.BOOLEAN, true));
        inventory.setItemInOffHand(totem);
        data.set(activeKey, PersistentDataType.BOOLEAN, true);

        BukkitTask previous = config.f_totem_task.remove(uuid);
        if (previous != null) previous.cancel();
        config.f_totem_task.put(uuid, plugin.getServer().getScheduler().runTaskLater(plugin, () -> release(player), config.f_Totem_Ticks));

        World world = player.getWorld();
        Location at = player.getLocation().add(0, 1.2, 0);
        world.playSound(at, Sound.ITEM_ARMOR_EQUIP_GOLD, 1, 1);
        world.playSound(at, Sound.BLOCK_ANVIL_USE, 0.7f, 1.4f);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, at, 16, 0.3, 0.4, 0.3, 0.2);
        world.spawnParticle(Particle.END_ROD, at, 16, 0.3, 0.4, 0.3, 0.1);

        player.showTitle(Title.title(
                Component.empty(),
                Component.text("Totem Gained!").color(NamedTextColor.GOLD),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(600), Duration.ofMillis(300))
        ));
    }

    private int emptySlot(PlayerInventory inventory) {
        for (int i = 0; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType().isAir()) return i;
        }
        return -1;
    }

    private ItemStack takeStash(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        ItemStack stash = config.f_stash.remove(player.getUniqueId());
        byte[] bytes = data.get(stashKey, PersistentDataType.BYTE_ARRAY);
        if (stash == null && bytes != null) stash = ItemStack.deserializeBytes(bytes);
        data.remove(stashKey);
        data.remove(activeKey);
        return stash;
    }

    public void release(Player player) {
        BukkitTask task = config.f_totem_task.remove(player.getUniqueId());
        if (task != null) task.cancel();
        if (!isActive(player) && task == null) return;

        ItemStack stash = takeStash(player);

        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (isSkillTotem(inventory.getItem(i))) inventory.setItem(i, null);
        }
        if (isSkillTotem(player.getItemOnCursor())) player.setItemOnCursor(null);

        if (stash == null) return;
        if (inventory.getItemInOffHand().getType().isAir()) {
            inventory.setItemInOffHand(stash);
        } else {
            inventory.addItem(stash).values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
    }

    public void death(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isActive(player)) return;

        if (event.getKeepInventory()) {
            release(player);
            return;
        }

        BukkitTask task = config.f_totem_task.remove(player.getUniqueId());
        if (task != null) task.cancel();

        ItemStack stash = takeStash(player);
        event.getDrops().removeIf(this::isSkillTotem);
        if (stash != null) event.getDrops().add(stash);
    }
}