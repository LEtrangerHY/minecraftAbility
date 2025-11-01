package org.core.coreProgram.Cores.Swordsman.coreSystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.core.Cool.Cool;
import org.core.Core;
import org.core.coreConfig;
import org.core.coreProgram.AbsCoreSystem.ConfigWrapper;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.AbsCoreSystem.absCore;
import org.core.coreProgram.Cores.Swordsman.Skill.F;
import org.core.coreProgram.Cores.Swordsman.Skill.Q;
import org.core.coreProgram.Cores.Swordsman.Skill.R;
import org.core.coreProgram.Cores.Swordsman.Passive.Laido;

import java.util.*;

import static org.bukkit.Bukkit.getLogger;

public class swordCore extends absCore {
    private final Core plugin;
    private final Swordsman config;

    private final Laido laido;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public swordCore(Core plugin, coreConfig tag, Swordsman config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.laido = new Laido(config, tag, plugin, cool);

        this.Rskill = new R(config, plugin, cool, laido);
        this.Qskill = new Q(config, plugin, cool, laido);
        this.Fskill = new F(config, plugin, cool, laido);

        getLogger().info("Swordsman downloaded...");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        if(!contains(event.getPlayer())) return;

        Player player = event.getPlayer();
        applyAdditionalHealth(player, false);
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
        long addHP = 0;

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

    @EventHandler(priority = EventPriority.NORMAL)
    public void passiveAttackEffect(PlayerInteractEvent event) {
        if(tag.Swordsman.contains(event.getPlayer())){
            if (pAttackUsing.contains(event.getPlayer().getUniqueId())) {
                pAttackUsing.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    private final Map<UUID, BossBar> activeChargeBars = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeChargeTasks = new HashMap<>();

    @EventHandler
    public void sneakCharge(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (!event.isSneaking() || !hasProperItems_Sheath(player) || !tag.Swordsman.contains(player)
                || config.skillUsing.getOrDefault(player.getUniqueId(), false) || config.laidoSlash.getOrDefault(player.getUniqueId(), false)) return;

        long durationTicks = 20L;

        if (activeChargeTasks.containsKey(player.getUniqueId())) {
            activeChargeTasks.get(player.getUniqueId()).cancel();
            activeChargeTasks.remove(player.getUniqueId());
        }
        if (activeChargeBars.containsKey(player.getUniqueId())) {
            activeChargeBars.get(player.getUniqueId()).removeAll();
            activeChargeBars.remove(player.getUniqueId());
        }

        BossBar bossBar = Bukkit.createBossBar("change", BarColor.PURPLE, BarStyle.SOLID);
        bossBar.setProgress(0.0);
        bossBar.addPlayer(player);
        activeChargeBars.put(player.getUniqueId(), bossBar);

        BukkitRunnable task = new BukkitRunnable() {
            long ticks = 0;

            @Override
            public void run() {
                if (!player.isSneaking() || !hasProperItems_Sheath(player)
                        || config.skillUsing.getOrDefault(player.getUniqueId(), false) || config.laidoSlash.getOrDefault(player.getUniqueId(), false)) {
                    cleanup();
                    return;
                }

                if (ticks < durationTicks) {
                    ticks++;
                    double progress = (double) ticks / durationTicks;
                    bossBar.setProgress(progress);
                } else {
                    bossBar.setProgress(1.0);
                    laido.Sheath(player);
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

    @EventHandler(priority = EventPriority.LOW)
    public void passiveEffect(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        World world = player.getWorld();

        BlockData blood = Material.REDSTONE_BLOCK.createBlockData();

        if(tag.Swordsman.contains(player) && !config.skillUsing.getOrDefault(player.getUniqueId(), false) && hasProperItems_Draw(player) && config.laidoSlash.getOrDefault(player.getUniqueId(), false)) {
            double originalDamage = event.getDamage();
            event.setDamage(originalDamage * 10);

            world.spawnParticle(Particle.CRIT, target.getLocation().clone().add(0, 1.2, 0), 20, 0.4, 0.4, 0.4, 1);
            world.spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().clone().add(0, 1.2, 0), 1, 0, 0, 0, 1);
            world.spawnParticle(Particle.BLOCK, target.getLocation().clone().add(0, 1.2, 0), 10, 0.3, 0.3, 0.3,
                    blood);

            laido.Draw(player);
        }
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Swordsman.contains(player);
    }

    @Override
    protected SkillBase getRSkill() {
        return Rskill;
    }

    @Override
    protected SkillBase getQSkill() {
        return Qskill;
    }

    @Override
    protected SkillBase getFSkill() {
        return Fskill;
    }

    private boolean hasProperItems_Draw(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        boolean ironSwordInBundle = false;

        if (main.getType() == Material.BUNDLE) {
            ItemMeta meta = main.getItemMeta();
            if (meta instanceof BundleMeta bundleMeta) {
                List<ItemStack> items = bundleMeta.getItems();
                if (items.size() == 1 && items.get(0).getType() == Material.IRON_SWORD) {
                    ironSwordInBundle = true;
                }
            }
        }

        return main.getType() == Material.BUNDLE && off.getType() == Material.AIR && ironSwordInBundle && config.laidoSlash.getOrDefault(player.getUniqueId(), false);
    }

    private boolean hasProperItems_Sheath(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        boolean bundleIsEmpty = false;

        if (off.getType() == Material.BUNDLE) {
            ItemMeta meta = off.getItemMeta();
            if (meta instanceof BundleMeta bundleMeta) {
                if (bundleMeta.getItems().isEmpty()) {
                    bundleIsEmpty = true;
                }
            }
        }

        return main.getType() == Material.IRON_SWORD && off.getType() == Material.BUNDLE && bundleIsEmpty && !config.laidoSlash.getOrDefault(player.getUniqueId(), false);
    }

    private boolean canUseRSkill(Player player) {
        return !config.skillUsing.getOrDefault(player.getUniqueId(), false);
    }

    private boolean canUseQSkill(Player player) {
        return !config.skillUsing.getOrDefault(player.getUniqueId(), false);
    }

    private boolean canUseFSkill(Player player) {
        return !config.skillUsing.getOrDefault(player.getUniqueId(), false);
    }

    @Override
    protected boolean isItemRequired(Player player){
        return hasProperItems_Draw(player) || hasProperItems_Sheath(player);
    }

    @Override
    protected boolean isRCondition(Player player) {
        return canUseRSkill(player);
    }

    @Override
    protected boolean isQCondition(Player player, ItemStack droppedItem) {
        ItemStack off = player.getInventory().getItemInOffHand();
        boolean ironSwordInBundle = false;

        if (droppedItem.getType() == Material.BUNDLE) {
            ItemMeta meta = droppedItem.getItemMeta();
            if (meta instanceof BundleMeta bundleMeta) {
                List<ItemStack> items = bundleMeta.getItems();
                if (items.size() == 1 && items.get(0).getType() == Material.IRON_SWORD) {
                    ironSwordInBundle = true;
                }
            }
        }

        boolean bundleIsEmpty = false;

        if (off.getType() == Material.BUNDLE) {
            ItemMeta meta = off.getItemMeta();
            if (meta instanceof BundleMeta bundleMeta) {
                if (bundleMeta.getItems().isEmpty()) {
                    bundleIsEmpty = true;
                }
            }
        }
        return ((droppedItem.getType() == Material.IRON_SWORD &&
                off.getType() == Material.BUNDLE && bundleIsEmpty && !config.laidoSlash.getOrDefault(player.getUniqueId(), false)) || (droppedItem.getType() == Material.BUNDLE &&
                off.getType() == Material.AIR) && ironSwordInBundle && config.laidoSlash.getOrDefault(player.getUniqueId(), false)) &&
                canUseQSkill(player);
    }

    @Override
    protected boolean isFCondition(Player player) {
        return canUseFSkill(player);
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
                cool.updateCooldown(player, "R", config.frozenCool);
                cool.updateCooldown(player, "Q", config.frozenCool);
                cool.updateCooldown(player, "F", config.frozenCool);
            }

            @Override
            public long getRcooldown(Player player) {
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
