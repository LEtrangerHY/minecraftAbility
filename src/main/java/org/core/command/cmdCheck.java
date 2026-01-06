package org.core.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.core.main.coreConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class cmdCheck implements CommandExecutor, TabCompleter {

    private final coreConfig config;

    public cmdCheck(coreConfig config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§c해당 플레이어를 찾을 수 없습니다.");
                return true;
            }
            sender.sendMessage("§a" + target.getName() + " 소유의 core : " + this.config.getPlayerCore(target));
            return true;
        } else if (args.length == 0) {
            if (!(sender instanceof Player player)) return true;
            sender.sendMessage("§a본인 소유의 core : " + this.config.getPlayerCore(player));
            return true;
        } else {
            sender.sendMessage("§c사용법: /corecheck <플레이어 닉네임|공백>");
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