package org.core.Main;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.Cool.Cool;
import org.core.Database.dbConnect;
import org.core.Level.LevelingManager;
import org.core.coreEntity.AbsEntityLeveling.EntityLevelingManager;
import org.core.coreProgram.Cores.Bambo.coreSystem.Bambo;
import org.core.coreProgram.Cores.Bambo.coreSystem.bambCore;
import org.core.coreProgram.Cores.Bambo.coreSystem.bambInventory;
import org.core.coreProgram.Cores.Benzene.coreSystem.benzInventory;
import org.core.coreProgram.Cores.Blaze.coreSystem.Blaze;
import org.core.coreProgram.Cores.Blaze.coreSystem.blazeCore;
import org.core.coreProgram.Cores.Blaze.coreSystem.blazeInventory;
import org.core.coreProgram.Cores.Bloom.coreSystem.Bloom;
import org.core.coreProgram.Cores.Bloom.coreSystem.bloomCore;
import org.core.coreProgram.Cores.Bloom.coreSystem.bloomInventory;
import org.core.coreProgram.Cores.Blue.coreSystem.Blue;
import org.core.coreProgram.Cores.Blue.coreSystem.blueCore;
import org.core.coreProgram.Cores.Blue.coreSystem.blueInventory;
import org.core.coreProgram.Cores.Carpenter.coreSystem.carpInventory;
import org.core.coreProgram.Cores.Commander.coreSystem.Commander;
import org.core.coreProgram.Cores.Commander.coreSystem.comCore;
import org.core.coreProgram.Cores.Commander.coreSystem.comInventory;
import org.core.coreProgram.Cores.Dagger.coreSystem.dagInventory;
import org.core.coreProgram.Cores.Glacier.coreSystem.glaInventory;
import org.core.coreProgram.Cores.Harvester.coreSystem.Harvester;
import org.core.coreProgram.Cores.Harvester.coreSystem.harvCore;
import org.core.coreProgram.Cores.Harvester.coreSystem.harvInventory;
import org.core.coreProgram.Cores.Knight.coreSystem.knightInventory;
import org.core.coreProgram.Cores.Luster.coreSystem.lustInventory;
import org.core.coreProgram.Cores.Nightel.coreSystem.Nightel;
import org.core.coreProgram.Cores.Nightel.coreSystem.nightCore;
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
import org.core.coreProgram.Cores.Nightel.coreSystem.nightInventory;
import org.core.coreProgram.Cores.Pyro.coreSystem.Pyro;
import org.core.coreProgram.Cores.Pyro.coreSystem.pyroCore;
import org.core.coreProgram.Cores.Pyro.coreSystem.pyroInventory;
import org.core.coreProgram.Cores.Saboteur.coreSystem.Saboteur;
import org.core.coreProgram.Cores.Saboteur.coreSystem.sabCore;
import org.core.coreProgram.Cores.Saboteur.coreSystem.sabInventory;
import org.core.coreProgram.Cores.Swordsman.coreSystem.Swordsman;
import org.core.coreProgram.Cores.Swordsman.coreSystem.swordCore;
import org.core.coreProgram.Cores.Swordsman.coreSystem.swordInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class Core extends JavaPlugin implements Listener, TabCompleter {

    private static Core instance;

    private dbConnect dbConn;

    private coreConfig config;

    private LevelingManager level;

    private nightCore nightel;
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
    private harvCore harvester;
    private bloomCore bloom;
    private blueCore blue;
    private swordCore swordsman;
    private sabCore saboteur;

    private nightInventory nightInv;
    private benzInventory benzInv;
    private bambInventory bambInv;
    private carpInventory carpInv;
    private dagInventory dagInv;
    private pyroInventory pyroInv;
    private glaInventory glaInv;
    private knightInventory knightInv;
    private lustInventory lustInv;
    private blazeInventory blazeInv;
    private comInventory comInv;
    private harvInventory harvInv;
    private bloomInventory bloomInv;
    private blueInventory blueInv;
    private swordInventory swordInv;
    private sabInventory sabInv;

    private EntityLevelingManager Elevel;

    public static Core getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        getLogger().info("CORE downloading...");

        instance = this;
        Bukkit.getPluginManager().registerEvents(this, this);

        saveDefaultConfig();

        Nightel nightConfig = new Nightel();
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
        Harvester harvConfig = new Harvester();
        Bloom bloomConfig = new Bloom();
        Blue blueConfig = new Blue();
        Swordsman swordConfig = new Swordsman();
        Saboteur sabConfig = new Saboteur();

        Cool cool = new Cool(this);

        this.config = new coreConfig(this);

        this.dbConn = new dbConnect(config, this);

        this.level = new LevelingManager(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.level, this);

        this.nightel = new nightCore(this, this.config, nightConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.nightel, this);
        this.nightInv = new nightInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.nightInv, this);

        this.bamb = new bambCore(this, this.config, bambConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.bamb, this);
        this.bambInv = new bambInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.bambInv, this);

        this.carp = new carpCore(this, this.config, carpConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.carp, this);
        this.carpInv = new carpInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.carpInv, this);

        this.dag = new dagCore(this, this.config, dagConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.dag, this);
        this.dagInv = new dagInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.dagInv, this);

        this.pyro = new pyroCore(this, config, pyroConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.pyro, this);
        this.pyroInv = new pyroInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.pyroInv, this);

        this.glacier = new glaCore(this, config, glaConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.glacier, this);
        this.glaInv = new glaInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.glaInv, this);

        this.knight = new knightCore(this, config, knightConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.knight, this);
        this.knightInv = new knightInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.knightInv, this);

        this.luster = new lustCore(this, config, lustConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.luster, this);
        this.lustInv = new lustInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.lustInv, this);

        this.blaze = new blazeCore(this, config, blazeConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.blaze, this);
        this.blazeInv = new blazeInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.blazeInv, this);

        this.commander = new comCore(this, config, comConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.commander, this);
        this.comInv = new comInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.comInv, this);

        this.harvester = new harvCore(this, config, harvConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.harvester, this);
        this.harvInv = new harvInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.harvInv, this);

        this.bloom = new bloomCore(this, config, bloomConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.bloom, this);
        this.bloomInv = new bloomInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.bloomInv, this);

        this.blue = new blueCore(this, config, blueConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.blue, this);
        this.blueInv = new blueInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.blueInv, this);

        this.swordsman = new swordCore(this, config, swordConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.swordsman, this);
        this.swordInv = new swordInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.swordInv, this);

        this.saboteur = new sabCore(this, config, sabConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.saboteur, this);
        this.sabInv = new sabInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.sabInv, this);

        this.benz = new benzCore(this, this.config, benzConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.benz, this);
        this.benzInv = new benzInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.benzInv, this);

        this.Elevel = new EntityLevelingManager(this);
        Bukkit.getPluginManager().registerEvents(this.Elevel, this);

        getCommand("core").setExecutor(this);
        getCommand("corecheck").setExecutor(this);
        getCommand("coreclear").setExecutor(this);
        getCommand("corelevelreset").setExecutor(this);
        getCommand("corelevelset").setExecutor(this);
        getCommand("gc").setExecutor(this);

        getCommand("core").setTabCompleter(this);
        getCommand("corecheck").setTabCompleter(this);
        getCommand("coreclear").setTabCompleter(this);
        getCommand("corelevelreset").setTabCompleter(this);
        getCommand("corelevelset").setTabCompleter(this);
        getCommand("gc").setTabCompleter(this);

        getLogger().info("CORE downloaded!");
    }

    @Override
    public void onDisable() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        int playerCount = players.size();

        getLogger().info("Server shutting down : Starting DB save for " + playerCount + " players...");

        CountDownLatch latch = new CountDownLatch(playerCount);

        Thread dbThread = new Thread(() -> {
            for (Player player : players) {
                try {
                    dbConn.insertMember(player);
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE,
                            "Error occurred while saving player '" + player.getName() + "'", e);
                } finally {
                    latch.countDown();
                }
            }
        });

        dbThread.start();

        try {
            boolean finished = latch.await(30, TimeUnit.SECONDS);

            if (!finished) {
                getLogger().warning("Not all player DB saves completed within 30 seconds!");
            }
        } catch (InterruptedException e) {
            getLogger().log(Level.SEVERE, "Interrupted while waiting for DB save completion", e);
        }

        getLogger().info("CORE disabled!");
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
            level.levelScoreBoard(target);
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
                level.levelScoreBoard(target);
                sender.sendMessage( "§c" + target.getName() + " 소유의 core을 모두 제거했습니다");
                return true;
            }else if(args.length == 0){
                if (!(sender instanceof Player player)) return true;
                this.config.clearPlayerCore(player);
                level.levelScoreBoard(player);
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

                AttributeInstance maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.setBaseValue(20.0);
                    target.setHealth(20.0);
                }
                target.getPersistentDataContainer().set(new NamespacedKey(this, "R"), PersistentDataType.LONG, 0L);
                target.getPersistentDataContainer().set(new NamespacedKey(this, "Q"), PersistentDataType.LONG, 0L);
                target.getPersistentDataContainer().set(new NamespacedKey(this, "F"), PersistentDataType.LONG, 0L);
                target.getPersistentDataContainer().set(new NamespacedKey(this, "exp"), PersistentDataType.LONG, 0L);
                target.getPersistentDataContainer().set(new NamespacedKey(this, "level"), PersistentDataType.LONG, 0L);
                sender.sendMessage( "§a" + target.getName() + " 경험치, 레벨 리셋 : " + this.level.Exp.getOrDefault(target, 0L) + ", " + this.level.Level.getOrDefault(target, 0L));
                return true;
            }else if(args.length == 0){
                if (!(sender instanceof Player player)) return true;

                AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.setBaseValue(20.0);
                    player.setHealth(20.0);
                }

                player.getPersistentDataContainer().set(new NamespacedKey(this, "R"), PersistentDataType.LONG, 0L);
                player.getPersistentDataContainer().set(new NamespacedKey(this, "Q"), PersistentDataType.LONG, 0L);
                player.getPersistentDataContainer().set(new NamespacedKey(this, "F"), PersistentDataType.LONG, 0L);
                player.getPersistentDataContainer().set(new NamespacedKey(this, "exp"), PersistentDataType.LONG, 0L);
                player.getPersistentDataContainer().set(new NamespacedKey(this, "level"), PersistentDataType.LONG, 0L);
                sender.sendMessage( "§a본인의 레벨, 경험치 리셋 " + this.level.Exp.getOrDefault(player, 0L) + ", " + this.level.Level.getOrDefault(player, 0L));
                return true;
            }else{
                sender.sendMessage("§c사용법: /corelevelreset <플레이어 닉네임|공백>");
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("corelevelset")) {
            if (args.length != 3) {
                sender.sendMessage("§c사용법: /corelevelset <플레이어 닉네임> <레벨(0~10)> <경험치(음이 아닌 정수)>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§c해당 플레이어를 찾을 수 없습니다.");
                return true;
            }

            long xp = 0;
            long lv = 0;

            try {
                xp = Long.parseLong(args[1].toLowerCase());
            } catch (Exception e) {
                target.sendMessage(Component.text("유효한 숫자가 아닙니다").color(NamedTextColor.RED));
                return true;
            }

            try {
                lv = Long.parseLong(args[2].toLowerCase());
            } catch (Exception e) {
                target.sendMessage(Component.text("유효한 숫자가 아닙니다").color(NamedTextColor.RED));
                return true;
            }

            target.getPersistentDataContainer().set(new NamespacedKey(this, "exp"), PersistentDataType.LONG, lv);
            target.getPersistentDataContainer().set(new NamespacedKey(this, "level"), PersistentDataType.LONG, xp);
            level.levelScoreBoard(target);
            level.applyLevelHealth(target, true);
            sender.sendMessage( "§a" + target.getName() +"의 경험치, 레벨 수정 " + xp + ", " + lv);
            return true;
        }

        if (command.getName().equalsIgnoreCase("coredbpaste")) {
            if(args.length == 1){
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage("§c해당 플레이어를 찾을 수 없습니다.");
                    return true;
                }
                dbConn.db_PastePlayerInfo(target);
                level.applyLevelHealth(target, true);
                sender.sendMessage( "§a" + target.getName() + " 소유의 core를 데이터베이스 수치로 붙여넣었습니다.");
                return true;
            }else if(args.length == 0){
                if (!(sender instanceof Player player)) return true;
                dbConn.db_PastePlayerInfo(player);
                level.applyLevelHealth(player, true);
                sender.sendMessage( "§a본인 소유의 core를 데이터베이스 수치로 붙여넣었습니다.");
                return true;
            }else{
                sender.sendMessage("§c사용법: /coredbpaste <플레이어 닉네임|공백>");
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("coredbupdate")) {
            if(args.length == 1){
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage("§c해당 플레이어를 찾을 수 없습니다.");
                    return true;
                }
                dbConn.insertMember(target);
                sender.sendMessage( "§a" + "현재 " + target.getName() + " 소유의 core를 데이터베이스에 업데이트 하였습니다.");
                return true;
            }else if(args.length == 0){
                if (!(sender instanceof Player player)) return true;
                dbConn.insertMember(player);
                sender.sendMessage( "§a" + "현재 본인 소유의 core를 데이터베이스에 업데이트 하였습니다.");
                return true;
            }else{
                sender.sendMessage("§c사용법: /coredbupdate <플레이어 닉네임|공백>");
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
                suggestions.add("benzene");
            } else if (args.length == 3) {
                suggestions.add("true");
                suggestions.add("false");
            }
        }

        if (command.getName().equalsIgnoreCase("corecheck") || command.getName().equalsIgnoreCase("coreclear")
                || command.getName().equalsIgnoreCase("corelevelset") || command.getName().equalsIgnoreCase("coredbpaste") || command.getName().equalsIgnoreCase("coredbupdate")) {
            if (args.length == 1) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    suggestions.add(p.getName());
                }
            }
        }

        return suggestions;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){
        Player player = e.getPlayer();
        player.setInvulnerable(false);

        dbConn.insertMember(player);
    }
}
