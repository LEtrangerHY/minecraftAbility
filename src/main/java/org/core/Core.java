package org.core;

import com.destroystokyo.paper.event.entity.EntityJumpEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.Cool.Cool;
import org.core.Effect.*;
import org.core.Level.LevelingManager;
import org.core.coreProgram.Cores.Bambo.coreSystem.Bambo;
import org.core.coreProgram.Cores.Bambo.coreSystem.bambCore;
import org.core.coreProgram.Cores.Blaze.coreSystem.Blaze;
import org.core.coreProgram.Cores.Blaze.coreSystem.blazeCore;
import org.core.coreProgram.Cores.Commander.coreSystem.Commander;
import org.core.coreProgram.Cores.Commander.coreSystem.comCore;
import org.core.coreProgram.Cores.Nox.coreSystem.Nox;
import org.core.coreProgram.Cores.Nox.coreSystem.noxCore;
import org.core.coreProgram.Cores.Benzene.coreSystem.Benzene;
import org.core.coreProgram.Cores.Benzene.coreSystem.benzCore;
import org.core.coreProgram.Cores.Carpenter.coreSystem.Carpenter;
import org.core.coreProgram.Cores.Carpenter.coreSystem.carpCore;
import org.core.coreProgram.Cores.Dagger.coreSystem.Dagger;
import org.core.coreProgram.Cores.Dagger.coreSystem.dagCore;
import org.core.coreProgram.Cores.Glacier.coreSystem.Glacier;
import org.core.coreProgram.Cores.Glacier.coreSystem.glaCore;
import org.core.coreProgram.Cores.Knight.coreSystem.Knight;
import org.core.coreProgram.Cores.Knight.coreSystem.knightCore;
import org.core.coreProgram.Cores.Luster.coreSystem.Luster;
import org.core.coreProgram.Cores.Luster.coreSystem.lustCore;
import org.core.coreProgram.Cores.Pyro.coreSystem.Pyro;
import org.core.coreProgram.Cores.Pyro.coreSystem.pyroCore;

import javax.naming.Name;
import java.util.ArrayList;
import java.util.List;

import static org.bukkit.Bukkit.getLogger;

public final class Core extends JavaPlugin implements Listener, TabCompleter {

    private coreConfig config;
    private LevelingManager level;

    private static Core instance;
    private noxCore nox;
    private benzCore benz;
    private bambCore bamb;
    private carpCore carp;
    private dagCore dag;
    private pyroCore pyro;
    private glaCore glacier;
    private knightCore knight;
    private lustCore luster;
    private blazeCore blaze;
    private comCore commander;

    private EffectManager effectManager = new EffectManager();

    public static Core getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, this);

        Nox noxConfig = new Nox();
        Benzene benzConfig = new Benzene();
        Bambo bambConfig = new Bambo();
        Carpenter carpConfig = new Carpenter();
        Dagger dagConfig = new Dagger();
        Pyro pyroConfig = new Pyro();
        Glacier glaConfig = new Glacier();
        Knight knightConfig = new Knight();
        Luster lustConfig = new Luster();
        Blaze blazeConfig = new Blaze();
        Commander comConfig = new Commander();

        Cool cool = new Cool(this);

        this.config = new coreConfig(this);

        this.level = new LevelingManager(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.level, this);

        this.nox = new noxCore(this, this.config, noxConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.nox, this);

        this.benz = new benzCore(this, this.config, benzConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.benz, this);

        this.bamb = new bambCore(this, this.config, bambConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.bamb, this);

        this.carp = new carpCore(this, this.config, carpConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.carp, this);

        this.dag = new dagCore(this, this.config, dagConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.dag, this);

        this.pyro = new pyroCore(this, config, pyroConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.pyro, this);

        this.glacier = new glaCore(this, config, glaConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.glacier, this);

        this.knight = new knightCore(this, config, knightConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.knight, this);

        this.luster = new lustCore(this, config, lustConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.luster, this);

        this.blaze = new blazeCore(this, config, blazeConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.blaze, this);

        this.commander = new comCore(this, config, comConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.commander, this);

        getCommand("core").setExecutor(this);
        getCommand("corecheck").setExecutor(this);
        getCommand("coreclear").setExecutor(this);
        getCommand("gc").setExecutor(this);

        getCommand("core").setTabCompleter(this);
        getCommand("corecheck").setTabCompleter(this);
        getCommand("coreclear").setTabCompleter(this);
        getCommand("gc").setTabCompleter(this);

        getLogger().info("Cores downloaded!");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        event.getPlayer().setInvulnerable(false);
    }

    @Override
    public void onDisable() {
        getLogger().info("Cores disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("core")) {
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
            sender.sendMessage("§a" + target.getName() + "의 " + setting + " 값을 " + value + "로 설정했습니다.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("corecheck")) {
            if(args.length == 1){
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage("§c해당 플레이어를 찾을 수 없습니다.");
                    return true;
                }
                sender.sendMessage( "§a" + target.getName() + " 소유의 core : " + this.config.getPlayerCore(target));
                return true;
            }else if(args.length == 0){
                if (!(sender instanceof Player player)) return true;
                sender.sendMessage( "§a본인 소유의 core : " + this.config.getPlayerCore(player));
                return true;
            }else{
                sender.sendMessage("§c사용법: /corecheck <플레이어 닉네임|공백>");
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("coreclear")) {
            if(args.length == 1){
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage("§c해당 플레이어를 찾을 수 없습니다.");
                    return true;
                }
                this.config.clearPlayerCore(target);
                sender.sendMessage( "§c" + target.getName() + " 소유의 core을 모두 제거했습니다");
                return true;
            }else if(args.length == 0){
                if (!(sender instanceof Player player)) return true;
                this.config.clearPlayerCore(player);
                sender.sendMessage( "§c본인 소유의 core을 모두 제거했습니다.");
                return true;
            }else{
                sender.sendMessage("§c사용법: /coreclear <플레이어 닉네임|공백>");
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("corelevelreset")) {
            if(args.length == 1){
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage("§c해당 플레이어를 찾을 수 없습니다.");
                    return true;
                }

                target.getPersistentDataContainer().set(new NamespacedKey(this, "exp"), PersistentDataType.LONG, 0L);
                target.getPersistentDataContainer().set(new NamespacedKey(this, "level"), PersistentDataType.LONG, 0L);
                sender.sendMessage( "§a" + target.getName() + " 경험치, 레벨 리셋 : " + this.level.Exp.getOrDefault(target, 0L) + ", " + this.level.Level.getOrDefault(target, 0L));
                return true;
            }else if(args.length == 0){
                if (!(sender instanceof Player player)) return true;
                player.getPersistentDataContainer().set(new NamespacedKey(this, "exp"), PersistentDataType.LONG, 0L);
                player.getPersistentDataContainer().set(new NamespacedKey(this, "level"), PersistentDataType.LONG, 0L);
                sender.sendMessage( "§a본인의 경험치, 레벨 리셋 " + this.level.Exp.getOrDefault(player, 0L) + ", " + this.level.Level.getOrDefault(player, 0L));
                return true;
            }else{
                sender.sendMessage("§c사용법: /corelevelreset <플레이어 닉네임|공백>");
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("gc")) {
            if (args.length != 0) {
                sender.sendMessage("§c사용법: /gc");
                return true;
            }
            System.gc();
            sender.sendMessage("가비지 컬렉션 작동");
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("core")) {
            if (args.length == 1) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    suggestions.add(p.getName());
                }
            } else if (args.length == 2) {
                suggestions.add("benzene");
                suggestions.add("nox");
                suggestions.add("knight");
                suggestions.add("pyro");
                suggestions.add("glacier");
                suggestions.add("dagger");
                suggestions.add("carpenter");
                suggestions.add("bambo");
                suggestions.add("luster");
                suggestions.add("blaze");
                suggestions.add("commander");
            } else if (args.length == 3) {
                suggestions.add("true");
                suggestions.add("false");
            }
        }

        if (command.getName().equalsIgnoreCase("corecheck") || command.getName().equalsIgnoreCase("coreclear")) {
            if (args.length == 1) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    suggestions.add(p.getName());
                }
            }
        }

        return suggestions;
    }
}
