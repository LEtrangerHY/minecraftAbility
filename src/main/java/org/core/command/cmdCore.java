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
import java.util.Arrays;
import java.util.List;

public class cmdCore implements CommandExecutor, TabCompleter {

    private final coreConfig config;
    private final LevelingManager level;

    private static final List<String> VALID_SETTINGS = Arrays.asList(
            "player", "nightel", "knight", "pyro", "glacier", "dagger", "carpenter",
            "bamboo", "luster", "blaze", "commander", "harvester", "blossom", "blue",
            "swordsman", "saboteur", "burst", "lavender", "rose", "residue", "darmes",
            "charlotte", "benzene"
    );

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

        if (!VALID_SETTINGS.contains(setting)) {
            sender.sendMessage("§c해당 data 는 존재하지 않습니다.");
            return true;
        }

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
        sender.sendMessage("§b" + "DataMenu 를 들고 우클릭하여 설정한 data의 상세정보를 확인할 수 있습니다.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    suggestions.add(p.getName());
                }
            }
        } else if (args.length == 2) {
            for (String s : VALID_SETTINGS) {
                if (s.toLowerCase().startsWith(args[1].toLowerCase())) {
                    suggestions.add(s);
                }
            }
        } else if (args.length == 3) {
            if ("true".startsWith(args[2].toLowerCase())) suggestions.add("true");
            if ("false".startsWith(args[2].toLowerCase())) suggestions.add("false");
        }
        return suggestions;
    }
}