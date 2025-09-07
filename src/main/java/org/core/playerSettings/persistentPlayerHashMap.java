package org.core.playerSettings;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

public class persistentPlayerHashMap extends AbstractMap<Player, Long> {
    private final JavaPlugin plugin;
    private final NamespacedKey key;

    public persistentPlayerHashMap(JavaPlugin plugin, String keyName) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, keyName);
    }

    @Override
    public @NotNull Set<Entry<Player, Long>> entrySet() {
        return Set.of();
    }

    @Override
    public Long put(Player player, Long value) {
        player.getPersistentDataContainer().set(key, PersistentDataType.LONG, value);
        return 0L;
    }

    @Override
    public Long remove(Object o) {
        if(o instanceof Player player){
            player.getPersistentDataContainer().remove(key);
        }
        return 0L;
    }
}
