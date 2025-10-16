package org.core;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.playerSettings.persistentPlayerSet;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class coreConfig {

    private final JavaPlugin plugin;

    public Set<Player> Nox;
    public Set<Player> Benzene;
    public Set<Player> Bambo;
    public Set<Player> Carpenter;
    public Set<Player> Dagger;
    public Set<Player> Pyro;
    public Set<Player> Glacier;
    public Set<Player> Knight;
    public Set<Player> Luster;
    public Set<Player> Blaze;
    public Set<Player> Commander;
    public Set<Player> Harvester;
    public Set<Player> Bloom;
    public Set<Player> Blue;

    public coreConfig(JavaPlugin plugin) {
        this.plugin = plugin;

        this.Nox = new persistentPlayerSet(plugin, "setting_nox");
        this.Benzene = new persistentPlayerSet(plugin, "setting_benzene");
        this.Bambo = new persistentPlayerSet(plugin, "setting_bambo");
        this.Carpenter = new persistentPlayerSet(plugin, "setting_carpenter");
        this.Dagger = new persistentPlayerSet(plugin, "setting_dagger");
        this.Pyro = new persistentPlayerSet(plugin, "setting_pyro");
        this.Glacier = new persistentPlayerSet(plugin, "setting_glacier");
        this.Knight = new persistentPlayerSet(plugin, "setting_knight");
        this.Luster = new persistentPlayerSet(plugin, "setting_luster");
        this.Blaze = new persistentPlayerSet(plugin, "setting_blaze");
        this.Commander = new persistentPlayerSet(plugin, "setting_commander");
        this.Harvester = new persistentPlayerSet(plugin, "setting_harvester");
        this.Bloom = new persistentPlayerSet(plugin, "setting_bloom");
        this.Blue = new persistentPlayerSet(plugin, "setting_blue");
    }

    public String getPlayerCore(Player player) {
        if (Nox.contains(player)) return "nox";
        if (Benzene.contains(player)) return "benzene";
        if (Bambo.contains(player)) return "bambo";
        if (Carpenter.contains(player)) return "carpenter";
        if (Dagger.contains(player)) return "dagger";
        if (Pyro.contains(player)) return "pyro";
        if (Glacier.contains(player)) return "glacier";
        if (Knight.contains(player)) return "knight";
        if (Luster.contains(player)) return "luster";
        if (Blaze.contains(player)) return "blaze";
        if (Commander.contains(player)) return "commander";
        if (Harvester.contains(player)) return "harvester";
        if (Bloom.contains(player)) return "bloom";
        if (Blue.contains(player)) return "blue";
        return "none";
    }

    public void clearPlayerCore(Player player){
        player.setWalkSpeed(0.2f);
        player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(4.0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_nox"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_benzene"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_bambo"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_carpenter"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_dagger"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_pyro"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_glacier"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_knight"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_luster"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_blaze"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_commander"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_harvester"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_bloom"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_blue"), PersistentDataType.BYTE, (byte) 0);
    }

    public void setSetting(Player player, String setting, boolean value) {
        NamespacedKey key = getSettingKey(setting);
        if (key == null) return;

        PersistentDataContainer container = player.getPersistentDataContainer();
        container.set(key, PersistentDataType.BYTE, value ? (byte) 1 : (byte) 0);
    }

    private NamespacedKey getSettingKey(String setting) {
        return switch (setting.toLowerCase()) {
            case "nox" -> new NamespacedKey(plugin, "setting_nox");
            case "benzene" -> new NamespacedKey(plugin, "setting_benzene");
            case "bambo" -> new NamespacedKey(plugin, "setting_bambo");
            case "carpenter" -> new NamespacedKey(plugin, "setting_carpenter");
            case "dagger" -> new NamespacedKey(plugin, "setting_dagger");
            case "pyro" -> new NamespacedKey(plugin, "setting_pyro");
            case "glacier" -> new NamespacedKey(plugin, "setting_glacier");
            case "knight" -> new NamespacedKey(plugin, "setting_knight");
            case "luster" -> new NamespacedKey(plugin, "setting_luster");
            case "blaze" -> new NamespacedKey(plugin, "setting_blaze");
            case "commander" -> new NamespacedKey(plugin, "setting_commander");
            case "harvester" -> new NamespacedKey(plugin, "setting_harvester");
            case "bloom" -> new NamespacedKey(plugin, "setting_bloom");
            case "blue" -> new NamespacedKey(plugin, "setting_blue");
            default -> null;
        };
    }
}
