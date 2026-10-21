package org.core.coreSystem.cores.VOL2.Cheshire.coreSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL2.Cheshire.Skill.F;
import org.core.coreSystem.cores.VOL2.Cheshire.Skill.Q;
import org.core.coreSystem.cores.VOL2.Cheshire.Skill.R;
import org.core.main.Core;
import org.core.main.coreConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class chesCore extends absCore {
    private final Core plugin;
    private final Cheshire config;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    private final Map<UUID, BossBar> activeChargeBars = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeChargeTasks = new HashMap<>();

    public chesCore(Core plugin, coreConfig tag, Cheshire config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool, this::breakInvisibility);

        plugin.getLogger().info("Cheshire downloaded...");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        if(!contains(event.getPlayer())) return;
        applyAdditionalHealth(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRespawn(PlayerRespawnEvent event) {
        if(!contains(event.getPlayer())) return;
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            applyAdditionalHealth(player, true);
        }, 1L);
    }

    private void applyAdditionalHealth(Player player, boolean healFull) {
        long addHP = player.getPersistentDataContainer().getOrDefault(
                new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L) * 3;

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double current = maxHealth.getBaseValue();
            double newMax = current + addHP;

            maxHealth.setBaseValue(newMax);

            if (healFull) {
                player.setHealth(newMax);
            } else if (player.getHealth() > newMax) {
                player.setHealth(newMax);
            }
        }
    }

    @EventHandler
    public void sneakCharge(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (!event.isSneaking() || cool.isReloading(player, "PASSIVE") || !hasProperItems(player) || !contains(player)) {
            if (!event.isSneaking() && activeChargeTasks.containsKey(player.getUniqueId())) {
                activeChargeTasks.get(player.getUniqueId()).cancel();
                activeChargeTasks.remove(player.getUniqueId());
                if (activeChargeBars.containsKey(player.getUniqueId())) {
                    activeChargeBars.get(player.getUniqueId()).removeAll();
                    activeChargeBars.remove(player.getUniqueId());
                }
            }
            return;
        }

        if (config.passiveLoaded.contains(player.getUniqueId())) return;

        long durationTicks = 20L;

        if (activeChargeTasks.containsKey(player.getUniqueId())) {
            activeChargeTasks.get(player.getUniqueId()).cancel();
            activeChargeTasks.remove(player.getUniqueId());
        }
        if (activeChargeBars.containsKey(player.getUniqueId())) {
            activeChargeBars.get(player.getUniqueId()).removeAll();
            activeChargeBars.remove(player.getUniqueId());
        }

        BossBar bossBar = Bukkit.createBossBar("Wasitacatisaw Charge", BarColor.PURPLE, BarStyle.SOLID);
        bossBar.setProgress(0.0);
        bossBar.addPlayer(player);
        activeChargeBars.put(player.getUniqueId(), bossBar);

        BukkitRunnable task = new BukkitRunnable() {
            long ticks = 0;

            @Override
            public void run() {
                if (!player.isSneaking() || !hasProperItems(player) || cool.isReloading(player, "PASSIVE")) {
                    cleanup();
                    return;
                }

                if (ticks < durationTicks) {
                    ticks++;
                    double progress = (double) ticks / durationTicks;
                    bossBar.setProgress(progress);
                } else {
                    bossBar.setProgress(1.0);
                    config.passiveLoaded.add(player.getUniqueId());
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
                    cleanup();
                }
            }

            private void cleanup() {
                bossBar.removeAll();
                activeChargeBars.remove(player.getUniqueId());
                activeChargeTasks.remove(player.getUniqueId());
                cancel();
            }
        };

        task.runTaskTimer(plugin, 0L, 1L);
        activeChargeTasks.put(player.getUniqueId(), task);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTakeDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!contains(player)) return;

        UUID uuid = player.getUniqueId();

        if (config.voidState.contains(uuid)) {
            event.setCancelled(true);
            return;
        }

        if (config.passiveLoaded.contains(uuid) && !cool.isReloading(player, "PASSIVE")) {
            event.setCancelled(true);

            config.passiveLoaded.remove(uuid);
            cool.setCooldown(player, config.passive_Cool, "PASSIVE", "boss");

            activateWasitacatisaw(player);
        }
    }

    private void activateWasitacatisaw(Player player) {
        UUID uuid = player.getUniqueId();
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().setY(0.0).normalize();

        config.smileLocations.put(uuid, eyeLoc);
        config.smileDirections.put(uuid, direction);

        int particleTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                Location loc = config.smileLocations.get(uuid);
                Vector dir = config.smileDirections.get(uuid);
                if(loc == null || dir == null) {
                    this.cancel();
                    return;
                }

                loc.getWorld().spawnParticle(Particle.WITCH, loc, 5, 0.3, 0.3, 0.3, 0.02);
                loc.getWorld().spawnParticle(Particle.PORTAL, loc, 10, 0.5, 0.5, 0.5, 0.1);

                drawCheshireSmile(loc, dir, tick);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L).getTaskId();

        config.smileTasks.put(uuid, particleTask);

        Vector backstep = direction.clone().multiply(-1.3).setY(0.3);
        player.setVelocity(backstep);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.1);

        config.voidState.add(uuid);
        config.invisState.add(uuid);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 120, 0, false, false));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hidePlayer(plugin, player);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            config.voidState.remove(uuid);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showPlayer(plugin, player);
            }
        }, 24L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            breakInvisibility(player);
            if(config.smileTasks.containsKey(uuid)) {
                Bukkit.getScheduler().cancelTask(config.smileTasks.get(uuid));
                config.smileTasks.remove(uuid);
            }
            config.smileLocations.remove(uuid);
            config.smileDirections.remove(uuid);
        }, 120L);
    }

    private void drawCheshireSmile(Location loc, Vector dir, int tick) {
        Vector right = new Vector(-dir.getZ(), 0.0, dir.getX()).normalize();
        Vector up = new Vector(0.0, 1.0, 0.0);

        float scale = 1.1f + (float) Math.sin(tick * 0.3) * 0.2f;

        Particle.DustOptions smileDust = new Particle.DustOptions(Color.PURPLE, scale);
        Particle.DustOptions eyeDust = new Particle.DustOptions(Color.FUCHSIA, scale * 1.2f);

        for(double t = -1.0; t <= 1.0; t += 0.05) {
            double x = t * (0.6 * scale);
            double y = (t * t) * (0.4 * scale) - (0.25 * scale);

            Vector point = right.clone().multiply(x).add(up.clone().multiply(y));
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(point), 1, 0.0, 0.0, 0.0, 0.0, smileDust);
        }

        Location leftEye = loc.clone().add(right.clone().multiply(-0.35 * scale)).add(up.clone().multiply(0.3 * scale));
        Location rightEye = loc.clone().add(right.clone().multiply(0.35 * scale)).add(up.clone().multiply(0.3 * scale));

        loc.getWorld().spawnParticle(Particle.DUST, leftEye, 2, 0.0, 0.0, 0.0, 0.0, eyeDust);
        loc.getWorld().spawnParticle(Particle.DUST, rightEye, 2, 0.0, 0.0, 0.0, 0.0, eyeDust);
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!contains(player)) return;

        UUID uuid = player.getUniqueId();

        if (config.voidState.contains(uuid)) {
            event.setCancelled(true);
            return;
        }

        if (config.invisState.contains(uuid)) {
            breakInvisibility(player);
        }
    }

    public void breakInvisibility(Player player) {
        UUID uuid = player.getUniqueId();
        if (config.invisState.contains(uuid)) {
            config.invisState.remove(uuid);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Cheshire.contains(player);
    }

    @Override
    protected boolean isCustomAttackUser(Player player) {
        return false;
    }

    @Override
    protected void onLSkillCooldown(PlayerInteractEvent event, Player player) {}

    @Override
    protected void LSkill(PlayerInteractEvent event, Player player) {}

    @Override
    protected SkillBase getRSkill() { return Rskill; }

    @Override
    protected SkillBase getQSkill() { return Qskill; }

    @Override
    protected SkillBase getFSkill() { return Fskill; }

    private boolean hasProperItems(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return main.getType() == Material.NETHERITE_HOE && off.getType() == Material.AIR;
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem){
        ItemStack off = player.getInventory().getItemInOffHand();
        return droppedItem.getType() == Material.NETHERITE_HOE && off.getType() == Material.AIR;
    }

    private boolean canUseRSkill(Player player) {
        return !config.voidState.contains(player.getUniqueId());
    }

    private boolean canUseQSkill(Player player) {
        return !config.voidState.contains(player.getUniqueId());
    }

    private boolean canUseFSkill(Player player) {
        return !config.voidState.contains(player.getUniqueId());
    }

    @Override
    protected boolean isItemRequired(Player player){
        return hasProperItems(player);
    }

    @Override
    protected boolean isRCondition(Player player) {
        return canUseRSkill(player);
    }

    @Override
    protected boolean isQCondition(Player player) {
        return canUseQSkill(player);
    }

    @Override
    protected boolean isFCondition(Player player) {
        return canUseFSkill(player);
    }

    @Override
    protected boolean isRAnimated(Player player) {
        return true;
    }

    @Override
    protected boolean isFAnimated(Player player) {
        return false;
    }

    @Override
    protected ConfigWrapper getConfigWrapper() {
        return new ConfigWrapper() {
            @Override
            public void variableReset(Player player) {
                config.variableReset(player);
            }

            @Override
            public void cooldownReset(Player player) {
                cool.setCooldown(player, config.frozenCool, "R");
                cool.setCooldown(player, config.frozenCool, "Q");
                cool.setCooldown(player, config.frozenCool, "F");
                cool.setCooldown(player, config.frozenCool, "GRIN WITHOUT A CAT");
                cool.setCooldown(player, config.frozenCool, "PASSIVE");
            }

            @Override
            public long getLcooldown(Player player) { return 0; }

            @Override
            public long getRcooldown(Player player) {
                if(config.invisState.contains(player.getUniqueId())) return 130;
                return config.R_COOLDOWN.getOrDefault(player.getUniqueId(), config.r_Skill_Cool);
            }

            @Override
            public long getQcooldown(Player player) {
                return config.Q_COOLDOWN.getOrDefault(player.getUniqueId(), config.q_Skill_Cool);
            }

            @Override
            public long getFcooldown(Player player) {
                return config.F_COOLDOWN.getOrDefault(player.getUniqueId(), config.f_Skill_Cool);
            }
        };
    }
}