package org.core.coreProgram.Cores.Glacier.coreSystem;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.Level.Levels;

import java.util.Collections;
import java.util.Set;

public class glaLeveling implements Levels {

    private final JavaPlugin plugin;
    private final Player player;
    private final long exp;

    public glaLeveling(JavaPlugin plugin, Player player, long exp) {
        this.plugin = plugin;
        this.player = player;
        this.exp = exp;
    }

    public Set<Long> requireExp = Set.of(77L, 144L, 288L, 488L, 788L, 1288L, 2388L, 3988L, 4844L, 5888L);

    @Override
    public void addLV(Entity entity){

        long current = player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "level"), PersistentDataType.LONG, 0L);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "level"), PersistentDataType.LONG, current + 1);

        long updatedLevel = player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "level"), PersistentDataType.LONG, 0L);

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getBaseValue() + 2.0);

            player.setHealth(maxHealth.getBaseValue());
        }

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);
        player.sendMessage("§a" + "level " + updatedLevel + " 로 상승!");

    }

    @Override
    public void addExp(Entity entity) {
        NamespacedKey expKey = new NamespacedKey(plugin, "exp");
        NamespacedKey levelKey = new NamespacedKey(plugin, "level");

        long currentLevel = player.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.LONG, 0L);
        long currentExp = player.getPersistentDataContainer().getOrDefault(expKey, PersistentDataType.LONG, 0L);

        long maxExp = Collections.max(requireExp);

        if (currentLevel < requireExp.size()) {
            player.sendMessage("§e" + "exp : " + exp + " 획득");
        }

        for (int i = 0; i < exp; i++) {
            if (currentLevel >= requireExp.size()) {
                break;
            }
            if (currentExp >= maxExp) {
                break;
            }

            currentExp++;

            player.getPersistentDataContainer().set(expKey, PersistentDataType.LONG, currentExp);

            if (requireExp.contains(currentExp)) {
                addLV(player);
                currentLevel++;
            }
        }
    }
}
