package org.core.Level;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.core.Core;
import org.core.coreConfig;
import org.core.coreProgram.Cores.Bambo.coreSystem.bambLeveling;
import org.core.coreProgram.Cores.Benzene.coreSystem.benzLeveling;
import org.core.coreProgram.Cores.Blaze.coreSystem.blazeLeveling;
import org.core.coreProgram.Cores.Carpenter.coreSystem.carpLeveling;
import org.core.coreProgram.Cores.Commander.coreSystem.comLeveling;
import org.core.coreProgram.Cores.Dagger.coreSystem.dagLeveling;
import org.core.coreProgram.Cores.Glacier.coreSystem.glaLeveling;
import org.core.coreProgram.Cores.Knight.coreSystem.knightLeveling;
import org.core.coreProgram.Cores.Luster.coreSystem.lustLeveling;
import org.core.coreProgram.Cores.Nox.coreSystem.noxLeveling;
import org.core.coreProgram.Cores.Pyro.coreSystem.pyroLeveling;
import org.core.playerSettings.persistentPlayerHashMap;

import javax.naming.Name;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LevelingManager implements Listener {

    private final JavaPlugin plugin;
    private final coreConfig config;

    public Map<Player, Long> Level;
    public Map<Player, Long> Exp;

    public LevelingManager(JavaPlugin plugin, coreConfig config){
        this.plugin = plugin;
        this.config = config;

        this.Level = new persistentPlayerHashMap(plugin, "level");
        this.Exp = new persistentPlayerHashMap(plugin, "exp");
    }

    @EventHandler
    public void Exp(PlayerExpChangeEvent event){

        if(event.getAmount() > 0){
            Player player = event.getPlayer();
            long exp = event.getAmount();

            switch (config.getPlayerCore(player)) {
                case "benzene" :
                    benzLeveling benzene = new benzLeveling(plugin, player, exp);
                    benzene.addExp(player);
                    break;
                case "nox" :
                    noxLeveling nox = new noxLeveling(plugin, player, exp);
                    nox.addExp(player);
                    break;
                case "bambo" :
                    bambLeveling bambo = new bambLeveling(plugin, player, exp);
                    bambo.addExp(player);
                    break;
                case "carpenter" :
                    carpLeveling carpenter = new carpLeveling(plugin, player, exp);
                    carpenter.addExp(player);
                    break;
                case "dagger" :
                    dagLeveling dagger = new dagLeveling(plugin, player, exp);
                    dagger.addExp(player);
                    break;
                case "pyro" :
                    pyroLeveling pyro = new pyroLeveling(plugin, player, exp);
                    pyro.addExp(player);
                    break;
                case "glacier" :
                    glaLeveling glacier = new glaLeveling(plugin, player, exp);
                    glacier.addExp(player);
                    break;
                case "knight" :
                    knightLeveling knight = new knightLeveling(plugin, player, exp);
                    knight.addExp(player);
                    break;
                case "luster" :
                    lustLeveling luster = new lustLeveling(plugin, player, exp);
                    luster.addExp(player);
                    break;
                case "blaze" :
                    blazeLeveling blaze = new blazeLeveling(plugin, player, exp);
                    blaze.addExp(player);
                    break;
                case "commander" :
                    comLeveling commander = new comLeveling(plugin, player, exp);
                    commander.addExp(player);
                    break;
                default :
                    break;
            }
        }
    }
}
