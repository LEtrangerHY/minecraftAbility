package org.core.coreSystem.cores.KEY.Benzene.coreSystem;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.core.cool.Cool;
import org.core.main.Core;
import org.core.main.coreConfig;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.KEY.Benzene.Passive.chainResonance;
import org.core.coreSystem.cores.KEY.Benzene.Passive.damageAmplify;
import org.core.coreSystem.cores.KEY.Benzene.Passive.damageShare;
import org.core.coreSystem.cores.KEY.Benzene.Skill.F;
import org.core.coreSystem.cores.KEY.Benzene.Skill.Q;
import org.core.coreSystem.cores.KEY.Benzene.Skill.R;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class benzCore extends absCore {

    private final Core plugin;
    private final Benzene config;

    private final chainResonance chainResonance;
    private final org.core.coreSystem.cores.KEY.Benzene.Passive.damageAmplify damageAmplify;
    private final org.core.coreSystem.cores.KEY.Benzene.Passive.damageShare damageShare;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public benzCore(Core plugin, coreConfig tag, Benzene config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.chainResonance = new chainResonance(tag, config, plugin, cool);
        this.damageAmplify = new damageAmplify(config);
        this.damageShare = new damageShare(config, plugin, cool);

        this.Rskill = new R(config, plugin, cool, chainResonance);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool, chainResonance);

        plugin.getLogger().info("Benzene downloaded...");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        if(!contains(event.getPlayer())) return;

        Player player = event.getPlayer();
        applyAdditionalHealth(player, false);

        chainResonance.updateChainResList(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRespawn(PlayerRespawnEvent event) {
        if(!contains(event.getPlayer())) return;

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            applyAdditionalHealth(player, true);
        }, 1L);

        chainResonance.updateChainResList(player);
    }

    private void applyAdditionalHealth(Player player, boolean healFull) {
        long addHP =
                player.getPersistentDataContainer().getOrDefault(
                        new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double current = maxHealth.getBaseValue();
            double newMax = current + addHP;

            maxHealth.setBaseValue(newMax);

            if (healFull) {
                player.setHealth(newMax);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void passiveAttackEffect(PlayerInteractEvent event) {
        if(tag.Benzene.contains(event.getPlayer())){
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

        if (!event.isSneaking() || cool.isReloading(player, "F") || !hasProperItems(player) || !tag.Benzene.contains(player)) return;

        long lv = player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);

        if(lv < 3) return;

        long durationTicks = 60L - lv * 12L + 36L;

        if (activeChargeTasks.containsKey(player.getUniqueId())) {
            activeChargeTasks.get(player.getUniqueId()).cancel();
            activeChargeTasks.remove(player.getUniqueId());
        }
        if (activeChargeBars.containsKey(player.getUniqueId())) {
            activeChargeBars.get(player.getUniqueId()).removeAll();
            activeChargeBars.remove(player.getUniqueId());
        }

        BossBar bossBar = Bukkit.createBossBar("F skill Charge", BarColor.PURPLE, BarStyle.SOLID);
        bossBar.setProgress(0.0);
        bossBar.addPlayer(player);
        activeChargeBars.put(player.getUniqueId(), bossBar);

        BukkitRunnable task = new BukkitRunnable() {
            long ticks = 0;

            @Override
            public void run() {
                if (!player.isSneaking() || !hasProperItems(player)) {
                    config.canBlockBreak.remove(player.getUniqueId());
                    cleanup();
                    return;
                }

                if (cool.isReloading(player, "F")) {
                    cleanup();
                    return;
                }

                if (ticks < durationTicks) {
                    ticks++;
                    double progress = (double) ticks / durationTicks;
                    bossBar.setProgress(progress);
                } else {
                    bossBar.setProgress(1.0);
                    if (!config.canBlockBreak.getOrDefault(player.getUniqueId(), false)) {
                        config.canBlockBreak.put(player.getUniqueId(), true);
                    }
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

    @EventHandler
    public void rSkillPassive(PlayerMoveEvent event){

        Player player = event.getPlayer();

        if (tag.Benzene.contains(player)) {
            if (Math.abs(player.getWalkSpeed() - 0.2f * (4f / 3f)) > 0.0001f) {
                player.setWalkSpeed(0.2f * (4f / 3f));
            }
        }
    }

    @EventHandler
    public void onEnvironmentalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        if (event instanceof EntityDamageByEntityEvent) return;

        if (!tag.Benzene.contains(player) && !player.isDead()) return;

        LinkedHashMap<Integer, Entity> playerChain = config.chainRes.getOrDefault(player.getUniqueId(), new LinkedHashMap<>());
        int count = playerChain.size();

        double reductionPercentage = 0.66 - 0.11 * count;

        double originalDamage = event.getDamage();
        double reductionAmount = originalDamage * reductionPercentage;
        double newDamage = originalDamage - reductionAmount;

        if (newDamage < 0) newDamage = 0;

        event.setDamage(newDamage);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void passiveEffect(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        if (!tag.Benzene.contains(player)) return;

        Location loc1 = player.getLocation().add(0, player.getHeight() / 2 + 0.2, 0);
        Location loc2 = target.getLocation().add(0, target.getHeight() / 2 + 0.2, 0);
        double distance = loc1.distance(loc2);

        boolean isRUsing = config.rskill_using.getOrDefault(player.getUniqueId(), false);
        int currentAtkCount = config.atkCount.getOrDefault(player.getUniqueId(), 0);

        if (!isRUsing && currentAtkCount < 3) {
            currentAtkCount++;
            config.atkCount.put(player.getUniqueId(), currentAtkCount);

            if (currentAtkCount == 3) {
                cool.updateCooldown(player, "R", 0L);
                cool.resumeCooldown(player, "R");
            } else if (currentAtkCount == 2){
                cool.updateCooldown(player, "R", 2000L);
                cool.pauseCooldown(player, "R");
            } else if (currentAtkCount == 1){
                cool.updateCooldown(player, "R", 4000L);
                cool.pauseCooldown(player, "R");
            } else {
                cool.updateCooldown(player, "R", 6000L);
                cool.pauseCooldown(player, "R");
            }
        }

        if(config.chainRes.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).containsValue(target) && distance <= 22){
            double originalDamage = event.getDamage();
            double amplifiedDamage = damageAmplify.Amplify(player, target, originalDamage);

            event.setDamage(amplifiedDamage);

            damageShare.damageShareTrigger(player, target, originalDamage);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void chainDelete(EntityDeathEvent event) {
        Entity death = event.getEntity();

        chainResonance.decrease(death);

        if(event.getEntity() instanceof Player player && tag.Benzene.contains(player)){
            config.variableReset(player);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void chainedCreeperExplode(EntityExplodeEvent event) {
        Entity ex = event.getEntity();

        chainResonance.decrease(ex);
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Benzene.contains(player);
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

    private boolean hasProperItems(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return main.getType() == Material.IRON_SWORD && off.getType() == Material.IRON_CHAIN;
    }

    private boolean canUseRSkill(Player player) {
        int count = config.atkCount.getOrDefault(player.getUniqueId(), 0);
        return count >= 3 && !config.rskill_using.getOrDefault(player.getUniqueId(), false) && !config.fskill_using.getOrDefault(player.getUniqueId(), false);
    }

    private boolean canUseQSkill(Player player) {
        return true;
    }

    private boolean canUseFSkill(Player player) {
        return !config.rskill_using.getOrDefault(player.getUniqueId(), false) && !config.fskill_using.getOrDefault(player.getUniqueId(), false);
    }

    @Override
    protected boolean isItemRequired(Player player){
        return hasProperItems(player);
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem){
        ItemStack off = player.getInventory().getItemInOffHand();
        return droppedItem.getType() == Material.IRON_SWORD &&
                off.getType() == Material.IRON_CHAIN;
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
    protected ConfigWrapper getConfigWrapper() {
        return new ConfigWrapper() {
            @Override
            public void variableReset(Player player) {
                config.variableReset(player);
            }

            @Override
            public void cooldownReset(Player player) {
                chainResonance.updateChainResList(player);

                cool.setCooldown(player, 6000L, "R");
                cool.pauseCooldown(player, "R");
                cool.setCooldown(player, config.frozenCool, "Q");
                cool.setCooldown(player, config.frozenCool, "F");

                cool.updateCooldown(player, "R", 6000L);
                cool.pauseCooldown(player, "R");
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