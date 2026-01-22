package org.core.coreSystem.cores.VOL1.Benzene.Passive;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.Stun;
import org.core.main.coreConfig;
import org.core.coreSystem.cores.VOL1.Benzene.coreSystem.Benzene;

import java.util.*;

public class chainResonance {
    private final coreConfig tag;
    private final Benzene config;
    private final JavaPlugin plugin;
    private final Cool cool;

    private static final BlockData CHAIN_DATA = Material.IRON_CHAIN.createBlockData();
    private static final Particle.DustOptions DUST_NEAR = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.5f);
    private static final Particle.DustOptions DUST_FAR = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 0.5f);
    private static final Component ICON_FULL = Component.text("⌬").color(NamedTextColor.GRAY);
    private static final String[] BENZENE_ICONS = new String[7];

    static {
        for (int i = 0; i <= 6; i++) {
            BENZENE_ICONS[i] = "⌬ ".repeat(Math.max(0, 6 - i)).trim();
        }
    }

    public chainResonance(coreConfig tag, Benzene config, JavaPlugin plugin, Cool cool) {
        this.tag = tag;
        this.config = config;
        this.plugin = plugin;
        this.cool = cool;
    }

    public void increase(Player player, Entity entity) {
        LinkedHashMap<Integer, Entity> playerChain = config.chainRes.computeIfAbsent(player.getUniqueId(), k -> new LinkedHashMap<>());
        int count = playerChain.size();

        if (!activeTasks.containsKey(player.getUniqueId())) {
            updateChainResList(player);
        }

        if (count < 6) {
            int chainCount = config.crCount.getOrDefault(player.getUniqueId(), 0) + 1;
            config.crCount.put(player.getUniqueId(), chainCount);

            player.getWorld().spawnParticle(Particle.BLOCK, entity.getLocation().add(0, 1.2, 0), 12, 0.3, 0.3, 0.3, CHAIN_DATA);

            playerChain.put(chainCount, entity);

        } else {
            removeFirstEntryFromLinkedHashMap(config.chainRes, player.getUniqueId(), player);

            int chainCount = config.crCount.getOrDefault(player.getUniqueId(), 0) + 1;
            config.crCount.put(player.getUniqueId(), chainCount);
            playerChain.put(chainCount, entity);

            int t = countIndivChain(player, entity);

            player.getWorld().spawnParticle(Particle.BLOCK, entity.getLocation().add(0, t * 0.2, 0), 6, 0.3, 0.3, 0.3, CHAIN_DATA);
        }

        sendActionBar(player, config.chainRes.get(player.getUniqueId()).size());

        if (!particleUse.containsKey(entity)) {
            chainParticle(player, entity);
        }
    }

    public void decrease(Entity targetEntity) {
        config.chainRes.forEach((uuid, entityMap) -> {
            boolean isRemoved = entityMap.values().removeIf(entity -> entity.equals(targetEntity));

            if (isRemoved) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    sendActionBar(player, entityMap.size());
                }
            }
        });
        config.chainRes.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private void sendActionBar(Player player, int currentSize) {
        if (currentSize < 6) {
            String hex = BENZENE_ICONS[currentSize];
            player.sendActionBar(Component.text(hex).color(NamedTextColor.DARK_GRAY));
        } else {
            player.sendActionBar(ICON_FULL);
        }
    }

    public <K, V> void removeFirstEntryFromLinkedHashMap(Map<K, LinkedHashMap<Integer, V>> map, K key, Player player) {
        LinkedHashMap<Integer, V> chainMap = map.get(key);

        if (chainMap != null && !chainMap.isEmpty()) {
            Integer firstKey = chainMap.keySet().iterator().next();
            Entity firstKeyEntity = config.chainRes.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).get(firstKey);

            Location pLoc = player.getLocation();
            Location eLoc = firstKeyEntity.getLocation();
            double distSq = distanceSquared(pLoc, eLoc, player.getHeight(), firstKeyEntity.getHeight());

            int t = countIndivChain(player, firstKeyEntity);

            if (distSq <= 484) {
                Stun stun = new Stun(firstKeyEntity, 100L * t);
                stun.applyEffect(player);

                player.getWorld().spawnParticle(Particle.BLOCK, eLoc.clone().add(0, t * 0.2, 0), 12, 0.3, 0.3, 0.3, CHAIN_DATA);
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, eLoc.clone().add(0, t * 0.2, 0), 12, 0.6, 0, 0.6, 0);
            }

            chainMap.remove(firstKey);
            updateChainResList(player);
        }
    }

    private double distanceSquared(Location l1, Location l2, double h1, double h2) {
        double x = l1.getX() - l2.getX();
        double y = (l1.getY() + h1 / 2 + 0.2) - (l2.getY() + h2 / 2 + 0.2);
        double z = l1.getZ() - l2.getZ();
        return x * x + y * y + z * z;
    }

    private final Map<Entity, BukkitRunnable> particleUse = new HashMap<>();

    public void chainParticle(Player player, Entity target) {
        BukkitRunnable particle = new BukkitRunnable() {
            final double tiltAngle = Math.toRadians(16);
            final double cosTilt = Math.cos(tiltAngle);
            final double sinTilt = Math.sin(tiltAngle);

            @Override
            public void run() {
                LinkedHashMap<Integer, Entity> chainMap = config.chainRes.get(player.getUniqueId());
                if (target.isDead() || !player.isOnline() || chainMap == null || !chainMap.containsValue(target)) {
                    particleUse.remove(target);
                    this.cancel();
                    return;
                }

                int t = countIndivChain(player, target);

                Location pLoc = player.getLocation();
                Location tLoc = target.getLocation();
                double distSq = distanceSquared(pLoc, tLoc, player.getHeight(), target.getHeight());

                Particle.DustOptions currentDust = (distSq <= 484) ? DUST_NEAR : DUST_FAR;

                int points = 6;
                double radius = 0.6;
                double yBase = tLoc.getY() + t * 0.2;
                double tX = tLoc.getX();
                double tZ = tLoc.getZ();
                World world = tLoc.getWorld();

                double startLocalX = radius;
                double startLocalZ = 0;
                double startYOffset = -startLocalZ * sinTilt;
                double startZOffset = startLocalZ * cosTilt;

                Location start = new Location(world, tX + startLocalX, yBase + startYOffset, tZ + startZOffset);

                for (int i = 1; i <= points; i++) {
                    double angle = (2 * Math.PI / points) * (i % points);

                    double localX = radius * Math.cos(angle);
                    double localZ = radius * Math.sin(angle);

                    double tiltedY = -localZ * sinTilt;
                    double tiltedZ = localZ * cosTilt;

                    Location end = new Location(world, tX + localX, yBase + tiltedY, tZ + tiltedZ);

                    Vector direction = end.toVector().subtract(start.toVector());
                    double length = direction.length();
                    direction.normalize().multiply(0.05);

                    int steps = (int) (length / 0.05);

                    Location drawLoc = start.clone();
                    for (int j = 0; j < steps; j++) {
                        drawLoc.add(direction);
                        world.spawnParticle(Particle.DUST, drawLoc, 1, 0, 0, 0, 0.08, currentDust);
                    }

                    start = end;
                }
            }
        };

        particleUse.put(target, particle);
        particle.runTaskTimer(plugin, 0L, 3L);
    }

    public int countIndivChain(Player player, Entity target) {
        int t = 0;
        LinkedHashMap<Integer, Entity> map = config.chainRes.get(player.getUniqueId());
        if (map == null) return 0;

        for (Entity chainedEntity : map.values()) {
            if (chainedEntity == target) {
                t++;
            }
        }
        return t;
    }

    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();

    public void updateChainResList(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (!tag.Benzene.contains(player)) {
            return;
        }

        if (activeTasks.containsKey(playerUUID)) {
            activeTasks.get(playerUUID).cancel();
            activeTasks.remove(playerUUID);
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !tag.Benzene.contains(player)) {
                    config.variableReset(player);
                    activeTasks.remove(playerUUID);
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                    this.cancel();
                    return;
                }

                Scoreboard scoreboard = player.getScoreboard();
                if (scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
                    scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                    player.setScoreboard(scoreboard);
                }

                Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
                if (objective == null) {
                    objective = scoreboard.registerNewObjective("BENZENE", Criteria.DUMMY, Component.text("BENZENE"));
                    objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                }

                int startScore = 7;

                LinkedHashMap<Integer, Entity> playerChain = config.chainRes.computeIfAbsent(player.getUniqueId(), k -> new LinkedHashMap<>());
                int count = playerChain.size();
                String benzene = (count >= 6) ? "§7⌬" : BENZENE_ICONS[count];

                Score score1 = objective.getScore(benzene);
                score1.setScore(startScore--);

                if (!playerChain.isEmpty()) {
                    List<Entity> rawEntities = new ArrayList<>(playerChain.values());

                    Map<UUID, Integer> groupFrequencies = new LinkedHashMap<>();
                    for (Entity e : rawEntities) {
                        groupFrequencies.put(e.getUniqueId(), groupFrequencies.getOrDefault(e.getUniqueId(), 0) + 1);
                    }

                    int globalIndex = 0;
                    int groupCounter = 1;

                    for (Integer freq : groupFrequencies.values()) {
                        String rings = "⏣".repeat(freq);

                        for (int i = 0; i < freq; i++) {
                            Entity currentEntity = rawEntities.get(globalIndex);

                            Location pLoc = player.getLocation();
                            Location eLoc = currentEntity.getLocation();
                            double distSq = distanceSquared(pLoc, eLoc, player.getHeight(), currentEntity.getHeight());

                            String baseName = (currentEntity instanceof Player) ? currentEntity.getName() : currentEntity.getType().name();
                            String finalDisplay = baseName + groupCounter + rings;

                            Score score = (distSq <= 484)
                                    ? objective.getScore(finalDisplay)
                                    : objective.getScore("§7§m" + finalDisplay);
                            score.setScore(startScore--);

                            globalIndex++;
                        }
                        groupCounter++;
                    }
                }
                player.setScoreboard(scoreboard);
            }
        };

        activeTasks.put(playerUUID, task);
        task.runTaskTimer(plugin, 0, 1L);
    }
}