package org.core.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.core.database.dbConnect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class cmdDBUpdate implements CommandExecutor, TabCompleter {

    private final dbConnect dbConn;

    public cmdDBUpdate(dbConnect dbConn) {
        this.dbConn = dbConn;
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
            sender.sendMessage("§c사용법: /coredbupdate <플레이어 닉네임|공백>");
            return true;
        }

        dbConn.insertMember(target);
        sender.sendMessage("§a" + "현재 " + target.getName() + " 에게 할당된 data를 데이터베이스에 업데이트 하였습니다.");
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