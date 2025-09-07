package org.core.playerSettings;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class persistentPlayerSet extends AbstractSet<Player> {
    private final JavaPlugin plugin;
    private final NamespacedKey key;

    public persistentPlayerSet(JavaPlugin plugin, String keyName) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, keyName);
    }

    @Override
    public boolean add(Player player) {
        player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof Player player) {
            player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 0);
            return true;
        }
        return false;
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof Player player) {
            Byte result = player.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
            return result != null && result == (byte) 1;
        }
        return false;
    }


    @Override
    public @NotNull Iterator<Player> iterator() {
        Set<Player> result = new HashSet<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (contains(player)) {
                result.add(player);
            }
        }
        return result.iterator();
    }

    @Override
    public int size() {
        return (int) plugin.getServer().getOnlinePlayers().stream()
                .filter(this::contains).count();
    }
}