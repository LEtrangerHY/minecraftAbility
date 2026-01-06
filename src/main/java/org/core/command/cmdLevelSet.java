package org.core.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
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

public class cmdLevelSet implements CommandExecutor, TabCompleter {

    private final Core plugin;
    private final LevelingManager level;

    public cmdLevelSet(Core plugin, LevelingManager level) {
        this.plugin = plugin;
        this.level = level;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 3) {
            sender.sendMessage("§c사용법: /corelevelset <플레이어 닉네임> <레벨(0~10)> <경험치(음이 아닌 정수)>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§c해당 플레이어를 찾을 수 없습니다.");
            return true;
        }

        long xp;
        long lv;

        try {
            lv = Long.parseLong(args[1].toLowerCase());
            xp = Long.parseLong(args[2].toLowerCase());
        } catch (Exception e) {
            target.sendMessage(Component.text("유효한 숫자가 아닙니다").color(NamedTextColor.RED));
            return true;
        }

        target.getPersistentDataContainer().set(new NamespacedKey(plugin, "exp"), PersistentDataType.LONG, xp);
        target.getPersistentDataContainer().set(new NamespacedKey(plugin, "level"), PersistentDataType.LONG, lv);

        level.levelScoreBoard(target);
        level.applyLevelHealth(target, true);
        sender.sendMessage("§a" + target.getName() + "의 경험치, 레벨 수정 " + xp + ", " + lv);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                suggestions.add(p.getName());
            }
            return suggestions;
        }
        return null;
    }
}