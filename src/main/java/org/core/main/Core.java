package org.core.main;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.command.*;
import org.core.cool.Cool;
import org.core.coreSystem.cores.KEY.PLAYER.coreSystem.PLAYER;
import org.core.coreSystem.cores.KEY.PLAYER.coreSystem.playerCore;
import org.core.coreSystem.cores.KEY.PLAYER.coreSystem.playerInventory;
import org.core.coreSystem.cores.VOL2.Lavender.coreSystem.Lavender;
import org.core.coreSystem.cores.VOL2.Lavender.coreSystem.lavCore;
import org.core.coreSystem.cores.VOL2.Lavender.coreSystem.lavInventory;
import org.core.coreSystem.cores.VOL2.Rose.coreSystem.Rose;
import org.core.coreSystem.cores.VOL2.Rose.coreSystem.roseCore;
import org.core.coreSystem.cores.VOL2.Rose.coreSystem.roseInventory;
import org.core.database.dbConnect;
import org.core.effect.crowdControl.Stiff;
import org.core.effect.effectPlugin.moveCancelManager;
import org.core.level.LevelingManager;
import org.core.coreEntity.absEntityLeveling.EntityLevelingManager;
import org.core.coreSystem.cores.VOL1.Bambo.coreSystem.Bambo;
import org.core.coreSystem.cores.VOL1.Bambo.coreSystem.bambCore;
import org.core.coreSystem.cores.VOL1.Bambo.coreSystem.bambInventory;
import org.core.coreSystem.cores.KEY.Benzene.coreSystem.Benzene;
import org.core.coreSystem.cores.KEY.Benzene.coreSystem.benzCore;
import org.core.coreSystem.cores.KEY.Benzene.coreSystem.benzInventory;
import org.core.coreSystem.cores.VOL1.Blaze.coreSystem.Blaze;
import org.core.coreSystem.cores.VOL1.Blaze.coreSystem.blazeCore;
import org.core.coreSystem.cores.VOL1.Blaze.coreSystem.blazeInventory;
import org.core.coreSystem.cores.VOL1.Bloom.coreSystem.Bloom;
import org.core.coreSystem.cores.VOL1.Bloom.coreSystem.bloomCore;
import org.core.coreSystem.cores.VOL1.Bloom.coreSystem.bloomInventory;
import org.core.coreSystem.cores.VOL2.Blue.coreSystem.Blue;
import org.core.coreSystem.cores.VOL2.Blue.coreSystem.blueCore;
import org.core.coreSystem.cores.VOL2.Blue.coreSystem.blueInventory;
import org.core.coreSystem.cores.VOL1.Carpenter.coreSystem.Carpenter;
import org.core.coreSystem.cores.VOL1.Carpenter.coreSystem.carpCore;
import org.core.coreSystem.cores.VOL1.Carpenter.coreSystem.carpInventory;
import org.core.coreSystem.cores.VOL1.Commander.coreSystem.Commander;
import org.core.coreSystem.cores.VOL1.Commander.coreSystem.comCore;
import org.core.coreSystem.cores.VOL1.Commander.coreSystem.comInventory;
import org.core.coreSystem.cores.VOL1.Dagger.coreSystem.Dagger;
import org.core.coreSystem.cores.VOL1.Dagger.coreSystem.dagCore;
import org.core.coreSystem.cores.VOL1.Dagger.coreSystem.dagInventory;
import org.core.coreSystem.cores.VOL1.Glacier.coreSystem.Glacier;
import org.core.coreSystem.cores.VOL1.Glacier.coreSystem.glaCore;
import org.core.coreSystem.cores.VOL1.Glacier.coreSystem.glaInventory;
import org.core.coreSystem.cores.VOL1.Harvester.coreSystem.Harvester;
import org.core.coreSystem.cores.VOL1.Harvester.coreSystem.harvCore;
import org.core.coreSystem.cores.VOL1.Harvester.coreSystem.harvInventory;
import org.core.coreSystem.cores.VOL2.Knight.coreSystem.Knight;
import org.core.coreSystem.cores.VOL2.Knight.coreSystem.knightCore;
import org.core.coreSystem.cores.VOL2.Knight.coreSystem.knightInventory;
import org.core.coreSystem.cores.VOL2.Luster.coreSystem.Luster;
import org.core.coreSystem.cores.VOL2.Luster.coreSystem.lustCore;
import org.core.coreSystem.cores.VOL2.Luster.coreSystem.lustInventory;
import org.core.coreSystem.cores.VOL1.Nightel.coreSystem.Nightel;
import org.core.coreSystem.cores.VOL1.Nightel.coreSystem.nightCore;
import org.core.coreSystem.cores.VOL1.Nightel.coreSystem.nightInventory;
import org.core.coreSystem.cores.VOL1.Pyro.coreSystem.Pyro;
import org.core.coreSystem.cores.VOL1.Pyro.coreSystem.pyroCore;
import org.core.coreSystem.cores.VOL1.Pyro.coreSystem.pyroInventory;
import org.core.coreSystem.cores.VOL1.Saboteur.coreSystem.Saboteur;
import org.core.coreSystem.cores.VOL1.Saboteur.coreSystem.sabCore;
import org.core.coreSystem.cores.VOL1.Saboteur.coreSystem.sabInventory;
import org.core.coreSystem.cores.VOL1.Swordsman.coreSystem.Swordsman;
import org.core.coreSystem.cores.VOL1.Swordsman.coreSystem.swordCore;
import org.core.coreSystem.cores.VOL1.Swordsman.coreSystem.swordInventory;
import org.core.coreSystem.cores.VOL2.Burst.coreSystem.Burst;
import org.core.coreSystem.cores.VOL2.Burst.coreSystem.burstCore;
import org.core.coreSystem.cores.VOL2.Burst.coreSystem.burstInventory;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class Core extends JavaPlugin implements Listener {

    private static Core instance;

    private dbConnect dbConn;

    private coreConfig config;

    private LevelingManager level;

    private Cool cool;

    private playerCore player;
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
    private burstCore burst;
    private lavCore lavender;
    private roseCore rose;

    private playerInventory playerInv;
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
    private burstInventory burstInv;
    private lavInventory lavInv;
    private roseInventory roseInv;

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

        PLAYER playerConfig = new PLAYER();
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
        Burst burstConfig = new Burst();
        Lavender lavConfig = new Lavender();
        Rose roseConfig = new Rose();

        this.cool = new Cool(this);

        this.config = new coreConfig(this, cool);

        this.dbConn = new dbConnect(config, this);

        this.level = new LevelingManager(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.level, this);

        this.player = new playerCore(this, this.config, playerConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.player, this);
        this.playerInv = new playerInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.playerInv, this);

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

        this.burst = new burstCore(this, this.config, burstConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.burst, this);
        this.burstInv = new burstInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.burstInv, this);

        this.lavender = new lavCore(this, this.config, lavConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.lavender, this);
        this.lavInv = new lavInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.lavInv, this);

        this.rose = new roseCore(this, this.config, roseConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.rose, this);
        this.roseInv = new roseInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.roseInv, this);

        this.benz = new benzCore(this, this.config, benzConfig, cool);
        Bukkit.getPluginManager().registerEvents(this.benz, this);
        this.benzInv = new benzInventory(this, this.config);
        Bukkit.getPluginManager().registerEvents(this.benzInv, this);

        this.Elevel = new EntityLevelingManager(this);
        Bukkit.getPluginManager().registerEvents(this.Elevel, this);

        getServer().getPluginManager().registerEvents(new moveCancelManager(), this);

        if (getCommand("core") != null) {
            cmdCore cmd = new cmdCore(this.config, this.level);
            getCommand("core").setExecutor(cmd);
            getCommand("core").setTabCompleter(cmd);
        }

        if (getCommand("corecheck") != null) {
            cmdCheck cmd = new cmdCheck(this.config);
            getCommand("corecheck").setExecutor(cmd);
            getCommand("corecheck").setTabCompleter(cmd);
        }

        if (getCommand("coreclear") != null) {
            cmdClear cmd = new cmdClear(this.config, this.level);
            getCommand("coreclear").setExecutor(cmd);
            getCommand("coreclear").setTabCompleter(cmd);
        }

        if (getCommand("corelevelreset") != null) {
            cmdLevelReset cmd = new cmdLevelReset(this, this.level);
            getCommand("corelevelreset").setExecutor(cmd);
            getCommand("corelevelreset").setTabCompleter(cmd);
        }

        if (getCommand("corelevelset") != null) {
            cmdLevelSet cmd = new cmdLevelSet(this, this.level);
            getCommand("corelevelset").setExecutor(cmd);
            getCommand("corelevelset").setTabCompleter(cmd);
        }

        if (getCommand("gc") != null) {
            cmdGC cmd = new cmdGC();
            getCommand("gc").setExecutor(cmd);
            getCommand("gc").setTabCompleter(cmd);
        }

        if (getCommand("coredbpaste") != null) {
            cmdDBPaste cmd = new cmdDBPaste(this.dbConn, this.level);
            getCommand("coredbpaste").setExecutor(cmd);
            getCommand("coredbpaste").setTabCompleter(cmd);
        }

        if (getCommand("coredbupdate") != null) {
            cmdDBUpdate cmd = new cmdDBUpdate(this.dbConn);
            getCommand("coredbupdate").setExecutor(cmd);
            getCommand("coredbupdate").setTabCompleter(cmd);
        }

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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){
        Player player = e.getPlayer();
        player.setInvulnerable(false);

        dbConn.insertMember(player);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();

        if (Stiff.isStiff(player)) {
            Location from = e.getFrom();
            Location to = e.getTo();

            if (to == null) return;

            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {

                Location fixedLocation = from.clone();
                fixedLocation.setYaw(to.getYaw());
                fixedLocation.setPitch(to.getPitch());

                e.setTo(fixedLocation);
            }
        }
    }
}