package org.core.coreProgram.Cores.Benzene.Passive;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Effect.EffectManager;
import org.core.Effect.Stun;
import org.core.coreConfig;
import org.core.coreProgram.Cores.Benzene.coreSystem.Benzene;

import java.util.*;

public class ChainCalc {
    private final coreConfig tag;
    private final Benzene config;
    private final JavaPlugin plugin;
    private final Cool cool;

    public ChainCalc(coreConfig tag, Benzene config, JavaPlugin plugin, Cool cool) {
        this.tag = tag;
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    public void increase(Player player, Entity entity) {
        if (config.Chain.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).isEmpty()) {
            config.Chain.put(player.getUniqueId(), new LinkedHashMap<>());
        }

        LinkedHashMap<Integer, Entity> playerChain = config.Chain.get(player.getUniqueId());

        if(playerChain.isEmpty()){
            updateChainList(player);
        }

        if (playerChain.size() < 6) {
            int chainCount = config.Chain_Count.getOrDefault(player.getUniqueId(), 0) + 1;
            config.Chain_Count.put(player.getUniqueId(), chainCount);

            player.getWorld().spawnParticle(Particle.BLOCK, entity.getLocation().clone().add(0, 1.2, 0), 12, 0.3, 0.3, 0.3,
                    Material.CHAIN.createBlockData());

            playerChain.put(chainCount, entity);

            if (!particleUse.containsKey(entity)) {
                chainParticle(player, entity);
            }

        } else {
            removeFirstEntryFromLinkedHashMap(config.Chain, player.getUniqueId(), player);

            int chainCount = config.Chain_Count.getOrDefault(player.getUniqueId(), 0) + 1;
            config.Chain_Count.put(player.getUniqueId(), chainCount);
            playerChain.put(chainCount, entity);

            int t = countIndivChain(player, entity);

            player.getWorld().spawnParticle(Particle.BLOCK, entity.getLocation().clone().add(0, t * 0.2, 0), 6, 0.3, 0.3, 0.3,
                    Material.CHAIN.createBlockData());

            if (!particleUse.containsKey(entity)) {
                chainParticle(player, entity);
            }
        }
    }

    public void decrease(Entity targetEntity) {

        config.Chain.forEach((uuid, entityMap) -> {
            entityMap.values().removeIf(entity -> entity.equals(targetEntity));
        });

        config.Chain.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public <K, V> void removeFirstEntryFromLinkedHashMap(Map<K, LinkedHashMap<Integer, V>> map, K key, Player player) {
        LinkedHashMap<Integer, V> chainMap = map.get(key);
        if (chainMap != null && !chainMap.isEmpty()) {
            Integer firstKey = chainMap.entrySet().iterator().next().getKey();
            Entity firstKeyEntity = config.Chain.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).get(firstKey);

            Location loc1 = player.getLocation().add(0, player.getHeight() / 2 + 0.2, 0);
            Location loc2 = firstKeyEntity.getLocation().add(0, firstKeyEntity.getHeight() / 2 + 0.2, 0);

            double distance = loc1.distance(loc2);

            int t = countIndivChain(player, firstKeyEntity);

            if(distance <= 22) {

                Stun stun = new Stun(config.Chain.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).get(firstKey), 100L * t);
                stun.applyEffect(player);

                player.getWorld().spawnParticle(Particle.BLOCK, firstKeyEntity.getLocation().clone().add(0, t * 0.2, 0), 12, 0.3, 0.3, 0.3,
                        Material.CHAIN.createBlockData());
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, firstKeyEntity.getLocation().add(0, t * 0.2, 0), 12, 0.6, 0, 0.6, 0);

            }

            chainMap.remove(firstKey);

            updateChainList(player);
        }
    }

    private final Map<Entity, BukkitRunnable> particleUse = new HashMap<>();

    public void chainParticle(Player player, Entity target) {
        BukkitRunnable particle = new BukkitRunnable() {
            @Override
            public void run() {

                int t = countIndivChain(player, target);

                Location loc1 = player.getLocation().add(0, player.getHeight() / 2 + 0.2, 0);
                Location loc2 = target.getLocation().add(0, target.getHeight() / 2 + 0.2, 0);
                double distance = loc1.distance(loc2);

                if (target.isDead() || !config.Chain.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).containsValue(target) || !player.isOnline()) {

                    particleUse.remove(target);

                    this.cancel();
                    return;
                }

                int points = 6;
                double radius = 0.6;
                double yBase = target.getY() + t * 0.2;

                double tiltAngle = Math.toRadians(16);

                Particle.DustOptions dustOption_chain = (distance <= 22) ? new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.6f) : new Particle.DustOptions(Color.fromRGB(0, 0, 0), 0.6f);

                List<Location> vertices = new ArrayList<>();

                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI / points) * i;

                    double localX = radius * Math.cos(angle);
                    double localZ = radius * Math.sin(angle);
                    double localY = 0;

                    double cosTilt = Math.cos(tiltAngle);
                    double sinTilt = Math.sin(tiltAngle);

                    double tiltedY = localY * cosTilt - localZ * sinTilt;
                    double tiltedZ = localY * sinTilt + localZ * cosTilt;

                    Location vertex = new Location(
                            target.getWorld(),
                            target.getX() + localX,
                            yBase + tiltedY,
                            target.getZ() + tiltedZ
                    );
                    vertices.add(vertex);
                }

                for (int i = 0; i < vertices.size(); i++) {
                    Location start = vertices.get(i);
                    Location end = vertices.get((i + 1) % vertices.size());

                    Vector direction = end.toVector().subtract(start.toVector());
                    double length = direction.length();
                    direction.normalize();

                    double step = 0.05;
                    int steps = (int) (length / step);

                    for (int j = 0; j < steps; j++) {
                        Location point = start.clone().add(direction.clone().multiply(j * step));
                        target.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0.08, dustOption_chain);
                    }
                }
            }
        };

        particleUse.put(target, particle);
        particle.runTaskTimer(plugin, 0L, 3L);
    }

    public int countIndivChain(Player player, Entity target){

        int t = 0;

        for (Entity chainedEntity : new ArrayList<>(config.Chain.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).values())) {
            if (chainedEntity == target) {
                t++;
            }
        }

        return t;
    }

    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();

    public void updateChainList(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (activeTasks.containsKey(playerUUID) || !tag.Benzene.contains(player)) {
            return;
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !tag.Benzene.contains(player) || player.isDead()) {

                    config.Chain.remove(player.getUniqueId());
                    config.Chain_Count.remove(player.getUniqueId());

                    activeTasks.remove(playerUUID);
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                    this.cancel();
                    return;
                }

                Scoreboard scoreboard = player.getScoreboard();
                if (scoreboard == null || scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
                    scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                    player.setScoreboard(scoreboard);
                }

                Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
                if (objective == null) {
                    objective = scoreboard.registerNewObjective("BENZENE", Criteria.DUMMY, Component.text("BENZENE"));
                    objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                }

                int startScore = 7;

                if (!config.Chain.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).isEmpty()) {

                    Score score1 = objective.getScore("⏣⌬⌬⌬⌬⌬⌬⌬⌬⌬⌬⌬⌬⌬");
                    score1.setScore(startScore--);

                    Map<UUID, LinkedHashMap<UUID, Integer>> index = new LinkedHashMap<>();
                    Map<UUID, ArrayList<String>> names = new LinkedHashMap<>();
                    Map<UUID, ArrayList<Double>> distances = new LinkedHashMap<>();

                    index.put(player.getUniqueId(), new LinkedHashMap<>());
                    names.put(player.getUniqueId(), new ArrayList<>());
                    distances.put(player.getUniqueId(), new ArrayList<>());

                    for (Entity entity : config.Chain.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).sequencedValues()) {
                        Location loc1 = player.getLocation().add(0, player.getHeight() / 2 + 0.2, 0);
                        Location loc2 = entity.getLocation().add(0, entity.getHeight() / 2 + 0.2, 0);

                        distances.get(player.getUniqueId()).add(loc1.distance(loc2));

                        UUID uuid = entity.getUniqueId();
                        String baseName = (entity instanceof Player)
                                ? entity.getName()
                                : entity.getType().name();

                        int globalCount = index.get(player.getUniqueId()).getOrDefault(uuid, 0) + 1;
                        index.get(player.getUniqueId()).put(uuid, globalCount);

                        names.get(player.getUniqueId()).add(baseName);
                    }

                    Map<UUID, ArrayList<String>> diff = new HashMap<>();
                    Map<UUID, Integer> n = new HashMap<>();
                    Map<UUID, Integer> m = new HashMap<>();

                    diff.put(player.getUniqueId(), new ArrayList<>());
                    n.put(player.getUniqueId(), 0);
                    m.put(player.getUniqueId(), 0);

                    for (UUID entityUuid : index.get(player.getUniqueId()).keySet()) {
                        for (int i = 0; i < index.get(player.getUniqueId()).get(entityUuid); i++) {
                            diff.get(player.getUniqueId()).add(names.get(player.getUniqueId()).get(m.get(player.getUniqueId())) + (n.get(player.getUniqueId()) + 1));
                            m.put(player.getUniqueId(), m.get(player.getUniqueId()) + 1);
                        }
                        n.put(player.getUniqueId(), n.get(player.getUniqueId()) + 1);
                    }

                    Map<UUID, ArrayList<String>> con = new HashMap<>();
                    Map<UUID, ArrayList<Double>> lastDist = new HashMap<>();
                    Map<UUID, Integer> k = new HashMap<>();

                    con.put(player.getUniqueId(), new ArrayList<>());
                    lastDist.put(player.getUniqueId(), new ArrayList<>());
                    k.put(player.getUniqueId(), 0);

                    for (UUID entityUuid : index.get(player.getUniqueId()).keySet()) {
                        con.get(player.getUniqueId()).add(diff.get(player.getUniqueId()).get(k.get(player.getUniqueId())) + "*".repeat(index.get(player.getUniqueId()).get(entityUuid)));
                        lastDist.get(player.getUniqueId()).add(distances.get(player.getUniqueId()).get(k.get(player.getUniqueId())));
                        for (int i = 0; i < index.get(player.getUniqueId()).get(entityUuid); i++) {
                            k.put(player.getUniqueId(), k.get(player.getUniqueId()) + 1);
                        }
                    }

                    Map<UUID, Integer> j = new HashMap<>();
                    j.put(player.getUniqueId(), 0);

                    for (String displayName : con.get(player.getUniqueId())) {
                        Score score = (lastDist.get(player.getUniqueId()).get(j.get(player.getUniqueId())) <= 22)
                                ? objective.getScore(displayName)
                                : objective.getScore("§7" + displayName);
                        score.setScore(startScore--);
                        j.put(player.getUniqueId(), j.get(player.getUniqueId()) + 1);
                    }

                    Score score2 = objective.getScore("⌬⌬⌬⌬⌬⌬⌬⌬⌬⌬⌬⌬⌬⏣");
                    score2.setScore(startScore);
                }

                player.setScoreboard(scoreboard);
            }
        };

        activeTasks.put(playerUUID, task);
        task.runTaskTimer(plugin, 0, 1L);
    }

}
