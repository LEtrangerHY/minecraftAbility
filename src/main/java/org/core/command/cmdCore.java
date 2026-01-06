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

public class cmdCore implements CommandExecutor, TabCompleter {

    private final coreConfig config;
    private final LevelingManager level;

    public cmdCore(coreConfig config, LevelingManager level) {
        this.config = config;
        this.level = level;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 3) {
            sender.sendMessage("§c사용법: /core <플레이어 닉네임> <설정 이름> <true|false>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§c해당 플레이어를 찾을 수 없습니다.");
            return true;
        }

        String setting = args[1].toLowerCase();
        boolean value;
        if (args[2].equalsIgnoreCase("true")) {
            value = true;
        } else if (args[2].equalsIgnoreCase("false")) {
            value = false;
        } else {
            sender.sendMessage("§ctrue 또는 false를 입력하세요.");
            return true;
        }

        this.config.setSetting(target, setting, value);
        level.levelScoreBoard(target);
        sender.sendMessage("§a" + target.getName() + "의 " + setting + " 값을 " + value + "로 설정했습니다.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                suggestions.add(p.getName());
            }
        } else if (args.length == 2) {
            suggestions.add("nightel");
            suggestions.add("knight");
            suggestions.add("pyro");
            suggestions.add("glacier");
            suggestions.add("dagger");
            suggestions.add("carpenter");
            suggestions.add("bambo");
            suggestions.add("luster");
            suggestions.add("blaze");
            suggestions.add("commander");
            suggestions.add("harvester");
            suggestions.add("bloom");
            suggestions.add("blue");
            suggestions.add("swordsman");
            suggestions.add("saboteur");
            suggestions.add("burst");
            suggestions.add("benzene");
        } else if (args.length == 3) {
            suggestions.add("true");
            suggestions.add("false");
        }
        return suggestions;
    }
}