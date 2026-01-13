package org.core.main;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.playerSettings.persistentPlayerSet;

import java.util.Set;

public class coreConfig {

    private final JavaPlugin plugin;

    public Set<Player> Nightel;
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
    public Set<Player> Swordsman;
    public Set<Player> Saboteur;
    public Set<Player> Burst;
    public Set<Player> Lavender;
    public Set<Player> Rose;

    public coreConfig(JavaPlugin plugin) {
        this.plugin = plugin;

        this.Nightel = new persistentPlayerSet(plugin, "setting_nightel");
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
        this.Swordsman = new persistentPlayerSet(plugin, "setting_swordsman");
        this.Saboteur = new persistentPlayerSet(plugin, "setting_saboteur");
        this.Burst = new persistentPlayerSet(plugin, "setting_burst");
        this.Lavender = new persistentPlayerSet(plugin, "setting_lavender");
        this.Rose = new persistentPlayerSet(plugin, "setting_rose");
    }

    public String getPlayerCore(Player player) {
        if (Nightel.contains(player)) return "NIGHTEL";
        if (Benzene.contains(player)) return "BENZENE";
        if (Bambo.contains(player)) return "BAMBO";
        if (Carpenter.contains(player)) return "CARPENTER";
        if (Dagger.contains(player)) return "DAGGER";
        if (Pyro.contains(player)) return "PYRO";
        if (Glacier.contains(player)) return "GLACIER";
        if (Knight.contains(player)) return "KNIGHT";
        if (Luster.contains(player)) return "LUSTER";
        if (Blaze.contains(player)) return "BLAZE";
        if (Commander.contains(player)) return "COMMANDER";
        if (Harvester.contains(player)) return "HARVESTER";
        if (Bloom.contains(player)) return "BLOOM";
        if (Blue.contains(player)) return "BLUE";
        if (Swordsman.contains(player)) return "SWORDSMAN";
        if (Saboteur.contains(player)) return "SABOTEUR";
        if (Burst.contains(player)) return "BURST";
        if (Lavender.contains(player)) return "LAVENDER";
        if (Rose.contains(player)) return "ROSE";
        return "NONE";
    }

    public void clearPlayerCore(Player player){
        player.setWalkSpeed(0.2f);
        player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(4.0);
        player.setInvisible(false);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_nightel"), PersistentDataType.BYTE, (byte) 0);
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
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_swordsman"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_saboteur"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_burst"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_lavender"), PersistentDataType.BYTE, (byte) 0);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "setting_rose"), PersistentDataType.BYTE, (byte) 0);
    }

    public void setSetting(Player player, String setting, boolean value) {
        NamespacedKey key = getSettingKey(setting);
        if (key == null) return;

        PersistentDataContainer container = player.getPersistentDataContainer();
        container.set(key, PersistentDataType.BYTE, value ? (byte) 1 : (byte) 0);
    }

    private NamespacedKey getSettingKey(String setting) {
        return switch (setting.toLowerCase()) {
            case "nightel" -> new NamespacedKey(plugin, "setting_nightel");
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
            case "swordsman" -> new NamespacedKey(plugin, "setting_swordsman");
            case "saboteur" -> new NamespacedKey(plugin, "setting_saboteur");
            case "burst" -> new NamespacedKey(plugin, "setting_burst");
            case "lavender" -> new NamespacedKey(plugin, "setting_lavender");
            case "rose" -> new NamespacedKey(plugin, "setting_rose");
            default -> null;
        };
    }
}
