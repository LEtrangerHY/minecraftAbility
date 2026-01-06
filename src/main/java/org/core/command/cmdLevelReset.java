package org.core.command;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.core.level.LevelingManager;
import org.core.main.Core;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class cmdLevelReset implements CommandExecutor, TabCompleter {

    private final Core plugin;
    private final LevelingManager level;

    public cmdLevelReset(Core plugin, LevelingManager level) {
        this.plugin = plugin;
        this.level = level;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player target;

        if (args.length == 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§c해당 플레이어를 찾을 수 없습니다.");
                return true;
            }
        } else if (args.length == 0) {
            if (!(sender instanceof Player player)) return true;
            target = player;
        } else {
            sender.sendMessage("§c사용법: /corelevelreset <플레이어 닉네임|공백>");
            return true;
        }

        AttributeInstance maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(20.0);
            target.setHealth(20.0);
        }
        target.getPersistentDataContainer().set(new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L);
        target.getPersistentDataContainer().set(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
        target.getPersistentDataContainer().set(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);
        target.getPersistentDataContainer().set(new NamespacedKey(plugin, "exp"), PersistentDataType.LONG, 0L);
        target.getPersistentDataContainer().set(new NamespacedKey(plugin, "level"), PersistentDataType.LONG, 0L);

        sender.sendMessage("§a" + target.getName() + " 경험치, 레벨 리셋 : " + level.Exp.getOrDefault(target, 0L) + ", " + level.Level.getOrDefault(target, 0L));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                suggestions.add(p.getName());
            }
        }
        return suggestions;
    }
}