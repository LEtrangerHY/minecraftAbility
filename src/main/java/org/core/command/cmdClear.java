package org.core.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.core.level.LevelingManager;
import org.core.main.coreConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class cmdClear implements CommandExecutor, TabCompleter {

    private final coreConfig config;
    private final LevelingManager level;

    public cmdClear(coreConfig config, LevelingManager level) {
        this.config = config;
        this.level = level;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§c해당 플레이어를 찾을 수 없습니다.");
                return true;
            }
            this.config.clearPlayerCore(target);
            level.levelScoreBoard(target);
            sender.sendMessage("§c" + target.getName() + " 소유의 core을 모두 제거했습니다");
            return true;
        } else if (args.length == 0) {
            if (!(sender instanceof Player player)) return true;
            this.config.clearPlayerCore(player);
            level.levelScoreBoard(player);
            sender.sendMessage("§c본인 소유의 core을 모두 제거했습니다.");
            return true;
        } else {
            sender.sendMessage("§c사용법: /coreclear <플레이어 닉네임|공백>");
            return true;
        }
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