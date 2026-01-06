package org.core.coreSystem.cores.VOL1.Saboteur.coreSystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.effect.crowdControl.ForceDamage;
import org.core.main.Core;
import org.core.main.coreConfig;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL1.Saboteur.Passive.TrapType;
import org.core.coreSystem.cores.VOL1.Saboteur.Skill.F;
import org.core.coreSystem.cores.VOL1.Saboteur.Skill.Q;
import org.core.coreSystem.cores.VOL1.Saboteur.Skill.R;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class sabCore extends absCore {
    private final Core plugin;
    private final Saboteur config;

    private final TrapType trapType;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public sabCore(Core plugin, coreConfig tag, Saboteur config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.trapType = new TrapType(tag, config, plugin, cool);

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

        plugin.getLogger().info("Saboteur downloaded...");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        if(!contains(event.getPlayer())) return;

        Player player = event.getPlayer();
        applyAdditionalHealth(player, false);

        if(tag.Saboteur.contains(player)){
            trapType.trapTypeBoard(player);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRespawn(PlayerRespawnEvent event) {
        if(!contains(event.getPlayer())) return;

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            applyAdditionalHealth(player, true);
        }, 1L);

        if(tag.Saboteur.contains(player)){
            trapType.trapTypeBoard(player);
        }
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
        if(tag.Saboteur.contains(event.getPlayer()) && config.trapType.getOrDefault(event.getPlayer().getUniqueId(), 1) == 1){
            if (pAttackUsing.contains(event.getPlayer().getUniqueId())) {
                pAttackUsing.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler
    public void passiveDamage(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        if(tag.Saboteur.contains(player) && hasProperItems(player)){
            if(!skillUsing(player) && config.trapType.getOrDefault(player.getUniqueId(), 1) == 1) {

                player.spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().clone().add(0, 1.2, 0), 1, 0, 0, 0, 0);

                player.playSound(target.getLocation().clone().add(0, 1.2, 0), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

                Location loc1 = player.getLocation().add(0, player.getHeight() / 2 + 0.2, 0);
                Location loc2 = target.getLocation().add(0, target.getHeight() / 2 + 0.2, 0);

                double distance = loc1.distance(loc2);

                if(distance > 3) {
                    event.setDamage(event.getDamage() * 3);
                }else {
                    event.setDamage(event.getDamage() * 5);
                }

            }
            if(config.isHackAway.getOrDefault(player.getUniqueId(), false)) player.heal(1);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void passiveThrow(PlayerInteractEvent event) {

        if (tag.Saboteur.contains(event.getPlayer())) {
            if (!pAttackUsing.contains(event.getPlayer().getUniqueId()) &&
                    !skillUsing(event.getPlayer())) {

                Player player = event.getPlayer();

                if (hasProperItems(player) &&
                        config.trapType.getOrDefault(event.getPlayer().getUniqueId(), 1) == 2) {

                    if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {

                        if (cool.isReloading(player, "throw")) {
                            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
                            return;
                        }

                        cool.setCooldown(player, 1000L, "throw");

                        BlockData blood = Material.REDSTONE_BLOCK.createBlockData();
                        BlockData iron = Material.IRON_BLOCK.createBlockData();

                        World world = player.getWorld();
                        Location playerLocation = player.getLocation().add(0, 1.5, 0);
                        Vector direction = playerLocation.getDirection().normalize().multiply(2.3);

                        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 1);

                        Item item = world.dropItem(playerLocation, new ItemStack(Material.IRON_NUGGET));
                        item.setVelocity(direction);
                        item.setPickupDelay(1000);
                        item.setGravity(true);

                        DamageSource source = DamageSource.builder(DamageType.MOB_PROJECTILE)
                                .withCausingEntity(player)
                                .withDirectEntity(item)
                                .withDamageLocation(player.getLocation())
                                .build();

                        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                            int life = 80;

                            @Override
                            public void run() {
                                if (item.isDead() || !item.isValid()) {
                                    Bukkit.getScheduler().cancelTask(this.hashCode());
                                    return;
                                }

                                Location loc = item.getLocation();

                                for (Entity nearby : item.getNearbyEntities(0.5, 0.5, 0.5)) {
                                    if (nearby instanceof LivingEntity target && nearby != player) {

                                        ForceDamage forceDamage = new ForceDamage(target, 5.0, source);
                                        forceDamage.applyEffect(player);
                                        world.playSound(target.getLocation().clone(), Sound.ITEM_TRIDENT_HIT, 1.0f, 1.0f);
                                        world.spawnParticle(Particle.BLOCK, target.getLocation().clone().add(0, 1.2, 0), 14, 0.3, 0.3, 0.3,
                                                blood);

                                        item.remove();
                                        Bukkit.getScheduler().cancelTask(this.hashCode());
                                        return;
                                    }
                                }

                                if (!loc.clone().add(direction).getBlock().isPassable()) {
                                    world.playSound(loc.clone(), Sound.ITEM_TRIDENT_HIT_GROUND, 1.0f, 1.0f);
                                    world.spawnParticle(Particle.BLOCK,loc.clone(), 14, 0.3, 0.3, 0.3,
                                            iron);
                                    item.remove();
                                    Bukkit.getScheduler().cancelTask(this.hashCode());
                                    return;
                                }

                                if (life-- <= 0) {
                                    item.remove();
                                    Bukkit.getScheduler().cancelTask(this.hashCode());
                                }
                            }
                        }, 1L, 1L);

                        event.setCancelled(true);
                    }
                }
            } else {
                pAttackUsing.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    private final Map<UUID, BossBar> activeChargeBars = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeChargeTasks = new HashMap<>();

    @EventHandler
    public void sneakCharge(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (!event.isSneaking() || !hasProperItems(player) || !tag.Saboteur.contains(player) || skillUsing(player)) return;

        long durationTicks =(config.isHackAway.getOrDefault(player.getUniqueId(), false)) ? 4L : 10L;

        if (activeChargeTasks.containsKey(player.getUniqueId())) {
            activeChargeTasks.get(player.getUniqueId()).cancel();
            activeChargeTasks.remove(player.getUniqueId());
        }
        if (activeChargeBars.containsKey(player.getUniqueId())) {
            activeChargeBars.get(player.getUniqueId()).removeAll();
            activeChargeBars.remove(player.getUniqueId());
        }

        BossBar bossBar = Bukkit.createBossBar("trap change", BarColor.PURPLE, BarStyle.SOLID);
        bossBar.setProgress(0.0);
        bossBar.addPlayer(player);
        activeChargeBars.put(player.getUniqueId(), bossBar);

        BukkitRunnable task = new BukkitRunnable() {
            long ticks = 0;

            @Override
            public void run() {
                if (!player.isSneaking() || !hasProperItems(player)
                        || skillUsing(player)) {
                    cleanup();
                    return;
                }

                if (ticks < durationTicks) {
                    ticks++;
                    double progress = (double) ticks / durationTicks;
                    bossBar.setProgress(progress);
                } else {
                    bossBar.setProgress(1.0);
                    int trap = (config.trapType.getOrDefault(player.getUniqueId(), 1) == 1) ? 2 : 1;
                    config.trapType.put(player.getUniqueId(), trap);
                    if(config.trapType.getOrDefault(player.getUniqueId(), 1) == 1) {
                        player.sendActionBar(Component.text("Spike").color(NamedTextColor.GREEN));
                    }else{
                        player.sendActionBar(Component.text("Throw").color(NamedTextColor.GREEN));
                    }

                    trapType.trapTypeBoard(player);
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

    @Override
    protected boolean contains(Player player) {
        return tag.Saboteur.contains(player);
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
        return main.getType() == Material.SHEARS && off.getType() == Material.AIR;
    }

    private boolean skillUsing(Player player){
        return (config.r_skillUsing_Hack.getOrDefault(player.getUniqueId(), false) || config.q_skillUsing_Hack.getOrDefault(player.getUniqueId(), false)
                || config.r_skillUsing_Hack.getOrDefault(player.getUniqueId(), false) || config.r_skillUsing_Sweep_Hack.getOrDefault(player.getUniqueId(), false)
                || config.q_skillUsing_Hack.getOrDefault(player.getUniqueId(), false) || config.q_skillUsing_Sweep_Hack.getOrDefault(player.getUniqueId(), false));
    }

    private boolean canUseRSkill(Player player) {
        return !skillUsing(player);
    }

    private boolean canUseQSkill(Player player) {
        return !skillUsing(player);
    }

    private boolean canUseFSkill(Player player) {
        return !skillUsing(player);
    }

    @Override
    protected boolean isItemRequired(Player player){
        return hasProperItems(player);
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem){
        ItemStack off = player.getInventory().getItemInOffHand();
        return droppedItem.getType() == Material.SHEARS &&
                off.getType() == Material.AIR;
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
                cool.setCooldown(player, config.frozenCool, "R");
                cool.setCooldown(player, config.frozenCool, "Q");
                cool.setCooldown(player, config.frozenCool, "F");

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
