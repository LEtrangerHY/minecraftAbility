package org.core.coreSystem.cores.VOL6.Jester.coreSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.effect.crowdControl.ForceDamage;
import org.core.main.Core;
import org.core.main.coreConfig;
import org.core.coreSystem.cores.VOL6.Jester.Skill.R;
import org.core.coreSystem.cores.VOL6.Jester.Skill.JackInTheBox.Q;
import org.core.coreSystem.cores.VOL6.Jester.Skill.F;
import org.core.coreSystem.cores.VOL6.Jester.Skill.JackInTheBox.Q_stick;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class jestCore extends absCore {
    private final Core plugin;
    private final Jester config;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    private final Map<UUID, TrickShowTask> trickShowTasks = new HashMap<>();
    private final Map<UUID, BossBar> stickChargeBars = new HashMap<>();
    private final Map<UUID, BukkitRunnable> stickChargeTasks = new HashMap<>();

    public jestCore(Core plugin, coreConfig tag, Jester config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool, this::skipTrickShowStage);
        this.Fskill = new F(config, plugin, cool, this::trySpecialSkipTrickShowStage, this::forceEndTrickShow);

        plugin.getLogger().info("Jester downloaded...");
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
        Bukkit.getScheduler().runTaskLater(plugin, () -> applyAdditionalHealth(player, true), 1L);
    }

    private void applyAdditionalHealth(Player player, boolean healFull) {
        long addHP = player.getPersistentDataContainer().getOrDefault(
                new NamespacedKey(plugin, "R"), PersistentDataType.LONG, 0L) * 3;

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

    public void syncCooldowns(Player player) {
        if (!contains(player)) return;

        Material handMat = player.getInventory().getItemInMainHand().getType();

        String qKey = config.getWeaponQCoolKey(handMat);
        if (qKey != null) {
            cool.updateCooldown(player, "Q", cool.getRemainCooldown(player, qKey));
        } else {
            cool.updateCooldown(player, "Q", 0L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSwap(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (contains(player)) {
            Bukkit.getScheduler().runTask(plugin, () -> syncCooldowns(player));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRocketConsume(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!contains(player)) return;

        if (event.getAction().name().contains("RIGHT_CLICK")) {
            if (player.getInventory().getItemInMainHand().getType() == Material.FIREWORK_ROCKET) {
                event.setCancelled(true);
                player.updateInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onTridentHitTrickShow(ProjectileHitEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Trident trident) {
            if (trident.getShooter() instanceof Player player) {
                if (!contains(player)) return;

                if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                    player.getInventory().setItemInMainHand(new ItemStack(Material.TRIDENT));
                }

                trident.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSlotChangeDuringTrickShow(PlayerItemHeldEvent event) {
        TrickShowTask task = trickShowTasks.get(event.getPlayer().getUniqueId());
        if (task != null && event.getNewSlot() != task.getSavedSlot()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClickDuringTrickShow(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        TrickShowTask task = trickShowTasks.get(player.getUniqueId());
        if (task == null) return;

        int lockedSlot = task.getSavedSlot();

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
            if (event.getSlot() == lockedSlot) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
            if (event.getHotbarButton() == lockedSlot) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDragDuringTrickShow(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        TrickShowTask task = trickShowTasks.get(player.getUniqueId());
        if (task == null) return;

        int lockedSlot = task.getSavedSlot();

        for (int rawSlot : event.getRawSlots()) {
            if (event.getView().getInventory(rawSlot) != null && event.getView().getInventory(rawSlot).equals(player.getInventory())) {
                if (event.getView().convertSlot(rawSlot) == lockedSlot) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHandDuringTrickShow(PlayerSwapHandItemsEvent event) {
        TrickShowTask task = trickShowTasks.get(event.getPlayer().getUniqueId());
        if (task != null && event.getPlayer().getInventory().getHeldItemSlot() == task.getSavedSlot()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJesterDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        if (trickShowTasks.containsKey(uuid)) {
            TrickShowTask task = trickShowTasks.get(uuid);

            if (!event.getKeepInventory()) {
                event.getDrops().removeIf(item -> item != null && isJesterWeapon(item.getType()));

                if (task.originalItem != null && task.originalItem.getType() != Material.AIR) {
                    event.getDrops().add(task.originalItem.clone());
                }

                task.originalItem = new ItemStack(Material.AIR);
            }

            task.endShow(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJesterQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (trickShowTasks.containsKey(uuid)) {
            trickShowTasks.get(uuid).endShow(false);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEggHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Egg egg) {
            if (egg.getShooter() instanceof Player shooter) {

                Player targetJester = null;

                if (contains(shooter)) {
                    targetJester = shooter;
                } else {
                    if (Math.random() <= 0.0066) {
                        double minDistanceSq = Double.MAX_VALUE;

                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (contains(p) && p.getWorld().equals(shooter.getWorld())) {
                                double distSq = p.getLocation().distanceSquared(shooter.getLocation());
                                if (distSq < minDistanceSq) {
                                    minDistanceSq = distSq;
                                    targetJester = p;
                                }
                            }
                        }
                    }
                }

                if (targetJester == null) return;

                UUID uuid = targetJester.getUniqueId();

                if (config.canUseSkill.getOrDefault(uuid, false)) {
                    return;
                }

                if (cool.isReloading(targetJester, "TRICK SHOW Cooldown")) {
                    return;
                }

                config.canUseSkill.put(uuid, true);

                Location hitLocation = egg.getLocation().clone();
                hitLocation.setYaw(targetJester.getLocation().getYaw());
                hitLocation.setPitch(targetJester.getLocation().getPitch());
                hitLocation.add(0, 0.2, 0);

                targetJester.getWorld().spawnParticle(Particle.PORTAL, targetJester.getLocation(), 50, 0.5, 1.0, 0.5, 0.1);
                targetJester.teleport(hitLocation);
                targetJester.getWorld().spawnParticle(Particle.PORTAL, hitLocation, 50, 0.5, 1.0, 0.5, 0.1);
                targetJester.getWorld().playSound(hitLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                targetJester.playSound(targetJester.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

                if (!shooter.equals(targetJester)) {
                    shooter.playSound(shooter.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);
                }

                TrickShowTask task = new TrickShowTask(targetJester);
                task.runTaskTimer(plugin, 0L, 1L);
                trickShowTasks.put(uuid, task);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onParryDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player && contains(player)) {
            long parryEnd = config.shieldParryTime.getOrDefault(player.getUniqueId(), 0L);

            if (System.currentTimeMillis() < parryEnd) {
                if (player.getInventory().getItemInMainHand().getType() == Material.SHIELD) {
                    event.setCancelled(true);

                    config.shieldParryTime.remove(player.getUniqueId());
                    cool.updateCooldown(player, "Shield - Parry", 0L);

                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.5f, 0.8f);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.5f);
                    player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.1);

                    Entity damager = event.getDamager();
                    LivingEntity attacker = null;

                    if (damager instanceof LivingEntity le) {
                        attacker = le;
                    } else if (damager instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof LivingEntity le) {
                        attacker = le;
                    }

                    if (attacker != null && attacker != player) {
                        AttributeInstance maxHp = player.getAttribute(Attribute.MAX_HEALTH);
                        double playerMaxHp = maxHp != null ? maxHp.getBaseValue() : 20.0;
                        double damage = playerMaxHp * 0.16;

                        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                                .withCausingEntity(player)
                                .withDirectEntity(player)
                                .build();

                        ForceDamage fd = new ForceDamage(attacker, damage, source, true);
                        fd.applyEffect(player);

                        Vector knockback = attacker.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.8).setY(0.4);
                        attacker.setVelocity(knockback);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if(tag.Jester.contains(player)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL &&
                    player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "noFallDamage"), PersistentDataType.BOOLEAN, false)) {
                event.setCancelled(true);
                player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "noFallDamage"));
            }
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if(tag.Jester.contains(player)) {
            if (player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "noFallDamage"), PersistentDataType.BOOLEAN, false)) {
                player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "noFallDamage"));
            }
        }
    }

    @EventHandler
    public void onStickCharge(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!contains(player)) return;
        if (!config.canUseSkill.getOrDefault(uuid, false)) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.STICK) return;
        if (config.isOmnislashing.getOrDefault(uuid, false)) return;

        if (event.isSneaking()) {
            if (cool.isReloading(player, "Q_stick_internal")) return;

            if (stickChargeTasks.containsKey(uuid)) {
                stickChargeTasks.remove(uuid).cancel();
            }
            if (stickChargeBars.containsKey(uuid)) {
                stickChargeBars.remove(uuid).removeAll();
            }

            BossBar bar = Bukkit.createBossBar("Stick Charge", BarColor.PINK, BarStyle.SEGMENTED_10);
            bar.addPlayer(player);
            stickChargeBars.put(uuid, bar);

            BukkitRunnable task = new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (!player.isOnline() || !player.isSneaking() || player.getInventory().getItemInMainHand().getType() != Material.STICK) {
                        cleanupStickCharge(uuid);
                        return;
                    }

                    ticks++;

                    if (ticks <= 12) {
                        bar.setProgress(ticks / 12.0);
                    } else {
                        bar.setProgress(1.0);
                        bar.setColor(BarColor.PURPLE);
                        bar.setTitle("MAX CHARGE!");
                    }

                    Q_stick.stickChargeTicks.put(uuid, ticks);
                }
            };
            task.runTaskTimer(plugin, 0L, 1L);
            stickChargeTasks.put(uuid, task);

        } else {
            if (stickChargeTasks.containsKey(uuid)) {
                cleanupStickCharge(uuid);
                Qskill.Trigger(player);
            }
        }
    }

    public void cleanupStickCharge(UUID uuid) {
        if (stickChargeTasks.containsKey(uuid)) {
            stickChargeTasks.remove(uuid).cancel();
        }
        if (stickChargeBars.containsKey(uuid)) {
            stickChargeBars.remove(uuid).removeAll();
        }
        Q_stick.stickChargeTicks.remove(uuid);
    }

    public class TrickShowTask extends BukkitRunnable {
        private final Player player;
        private final UUID uuid;
        private int stage = 0;
        private int ticks = 0;

        private final int savedSlot;
        private ItemStack originalItem;

        private Material[] sequence = {
                Material.TRIDENT,
                Material.FIREWORK_ROCKET,
                Material.SHIELD,
                Material.STICK,
                Material.CHAINMAIL_HELMET
        };

        private String[] stageNames = {
                "Trick - Trident",
                "Trick - Rocket",
                "Trick - Shield",
                "Trick - Stick",
                "Trick - Crown"
        };

        public TrickShowTask(Player player) {
            this.player = player;
            this.uuid = player.getUniqueId();

            this.savedSlot = player.getInventory().getHeldItemSlot();
            ItemStack currentItem = player.getInventory().getItem(savedSlot);
            this.originalItem = (currentItem != null && currentItem.getType() != Material.AIR)
                    ? currentItem.clone() : new ItemStack(Material.AIR);

            setItemForStage();
        }

        public int getSavedSlot() {
            return savedSlot;
        }

        private void setItemForStage() {
            if (stage < sequence.length) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.getInventory().setItem(savedSlot, new ItemStack(sequence[stage]));
                    player.getInventory().setHeldItemSlot(savedSlot);

                    player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1.0f, 1.0f);
                    syncCooldowns(player);

                    cool.setCooldown(player, 6000L, stageNames[stage], "boss");

                    if (sequence[stage] == Material.SHIELD) {
                        config.shieldParryTime.put(uuid, System.currentTimeMillis() + 3000L);
                        cool.setCooldown(player, 3000L, "Shield - Parry", "boss");
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, 1.0f, 1.2f);

                        new BukkitRunnable() {
                            int parryTicks = 0;
                            @Override
                            public void run() {
                                if (parryTicks >= 60 || !player.isOnline() || player.getInventory().getItemInMainHand().getType() != Material.SHIELD) {
                                    this.cancel();
                                    return;
                                }
                                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 2, 0.4, 0.4, 0.4, 0);
                                parryTicks++;
                            }
                        }.runTaskTimer(plugin, 0L, 1L);
                    }
                });
            }
        }

        public void skipStage() {
            if (stage < stageNames.length) {
                cool.updateCooldown(player, stageNames[stage], 0L);
            }
            cool.updateCooldown(player, "Shield - Parry", 0L);

            stage++;
            ticks = 0;
            if (stage >= sequence.length) {
                endShow(true);
            } else {
                setItemForStage();
            }
        }

        public boolean trySpecialSkipStage() {
            if (stage + 1 >= sequence.length || sequence[stage + 1] == Material.CHAINMAIL_HELMET) {
                return false;
            }

            long remainTime = 6000L - (ticks * 50L);

            if (stage < stageNames.length) {
                cool.updateCooldown(player, stageNames[stage], 0L);
            }
            cool.updateCooldown(player, "Shield - Parry", 0L);

            Material tempMat = sequence[stage];
            String tempName = stageNames[stage];

            sequence[stage] = sequence[stage + 1];
            stageNames[stage] = stageNames[stage + 1];

            sequence[stage + 1] = tempMat;
            stageNames[stage + 1] = tempName;

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.getInventory().setItem(savedSlot, new ItemStack(sequence[stage]));
                player.getInventory().setHeldItemSlot(savedSlot);
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1.0f, 1.0f);
                syncCooldowns(player);

                if (remainTime > 0) {
                    cool.setCooldown(player, remainTime, stageNames[stage], "boss");
                }

                if (sequence[stage] == Material.SHIELD) {
                    config.shieldParryTime.put(uuid, System.currentTimeMillis() + 3000L);
                    cool.setCooldown(player, 3000L, "Shield - Parry", "boss");
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, 1.0f, 1.2f);

                    new BukkitRunnable() {
                        int parryTicks = 0;
                        @Override
                        public void run() {
                            if (parryTicks >= 60 || !player.isOnline() || player.getInventory().getItemInMainHand().getType() != Material.SHIELD) {
                                this.cancel();
                                return;
                            }
                            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 2, 0.4, 0.4, 0.4, 0);
                            parryTicks++;
                        }
                    }.runTaskTimer(plugin, 0L, 1L);
                }
            });

            return true;
        }

        @Override
        public void run() {
            if (!player.isOnline() || player.isDead() || !config.canUseSkill.getOrDefault(uuid, false)) {
                endShow(false);
                return;
            }

            ticks++;
            if (ticks >= 120) {
                skipStage();
            }
        }

        public void endShow(boolean natural) {
            config.canUseSkill.put(uuid, false);

            Runnable restoreLogic = () -> {
                if (originalItem != null) {
                    player.getInventory().setItem(savedSlot, originalItem);
                }
                if (natural && player.isOnline() && !player.isDead()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                }
            };

            if (Bukkit.isPrimaryThread()) {
                restoreLogic.run();
            } else {
                Bukkit.getScheduler().runTask(plugin, restoreLogic);
            }

            if (stage < stageNames.length) {
                cool.updateCooldown(player, stageNames[stage], 0L);
            }
            cool.updateCooldown(player, "Shield - Parry", 0L);

            trickShowTasks.remove(uuid);
            try {
                this.cancel();
            } catch (IllegalStateException ignored) {}
        }
    }

    public void skipTrickShowStage(Player player) {
        TrickShowTask task = trickShowTasks.get(player.getUniqueId());
        if (task != null) {
            task.skipStage();
        }
    }

    public boolean trySpecialSkipTrickShowStage(Player player) {
        TrickShowTask task = trickShowTasks.get(player.getUniqueId());
        if (task != null) {
            return task.trySpecialSkipStage();
        }
        return false;
    }

    public void forceEndTrickShow(Player player) {
        TrickShowTask task = trickShowTasks.get(player.getUniqueId());
        if (task != null) {
            task.endShow(false);
        }
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Jester.contains(player);
    }

    @Override
    protected boolean isCustomAttackUser(Player player) {
        return false;
    }

    @Override
    protected void onLSkillCooldown(PlayerInteractEvent event, Player player) {
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

    @Override
    protected void LSkill(PlayerInteractEvent event, Player player) {
    }

    private boolean isJesterWeapon(Material material) {
        return material == Material.TRIDENT ||
                material == Material.FIREWORK_ROCKET ||
                material == Material.SHIELD ||
                material == Material.STICK ||
                material == Material.CHAINMAIL_HELMET ||
                material == Material.EGG;
    }

    private boolean hasProperItems(Player player) {
        Material mainHand = player.getInventory().getItemInMainHand().getType();
        Material offHand = player.getInventory().getItemInOffHand().getType();

        return isJesterWeapon(mainHand) && offHand == Material.AIR;
    }

    @Override
    protected boolean isItemRequired(Player player) {
        if (hasProperItems(player)) {
            return true;
        } else {
            AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
            if (attackSpeed != null) attackSpeed.setBaseValue(4.0);
            return false;
        }
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem) {
        Material offHand = player.getInventory().getItemInOffHand().getType();
        boolean isRequired = isJesterWeapon(droppedItem.getType()) && offHand == Material.AIR;

        if (isRequired) {
            config.currentQWeapon.put(player.getUniqueId(), droppedItem.getType());
        }

        return isRequired;
    }

    @Override
    protected boolean isRCondition(Player player) {
        return config.canUseSkill.getOrDefault(player.getUniqueId(), false);
    }

    @Override
    protected boolean isQCondition(Player player) {
        if (config.isOmnislashing.getOrDefault(player.getUniqueId(), false) || config.isExecutionTime.getOrDefault(player.getUniqueId(), false)) {
            return false;
        }
        return config.canUseSkill.getOrDefault(player.getUniqueId(), false);
    }

    @Override
    protected boolean isFCondition(Player player) {
        if (config.isOmnislashing.getOrDefault(player.getUniqueId(), false) || config.isExecutionTime.getOrDefault(player.getUniqueId(), false)) {
            return false;
        }
        return true;
    }

    @Override
    protected ConfigWrapper getConfigWrapper() {
        return new ConfigWrapper() {
            @Override
            public void variableReset(Player player) {
                config.variableReset(player);

                UUID uuid = player.getUniqueId();
                if (trickShowTasks.containsKey(uuid)) {
                    trickShowTasks.get(uuid).endShow(false);
                }

                cleanupStickCharge(uuid);

                cool.updateCooldown(player, "Trick - Trident", 0L);
                cool.updateCooldown(player, "Trick - Rocket", 0L);
                cool.updateCooldown(player, "Trick - Shield", 0L);
                cool.updateCooldown(player, "Trick - Stick", 0L);
                cool.updateCooldown(player, "Trick - Crown", 0L);
                cool.updateCooldown(player, "Shield - Parry", 0L);
                cool.updateCooldown(player, "TRICK SHOW Cooldown", 0L);
            }

            @Override
            public void cooldownReset(Player player) {
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
            public long getLcooldown(Player player) {
                return 0;
            }

            @Override
            public long getRcooldown(Player player) {
                return config.R_COOLDOWN.getOrDefault(player.getUniqueId(), config.r_Skill_Cool);
            }

            @Override
            public long getQcooldown(Player player) {
                return 0L;
            }

            @Override
            public long getFcooldown(Player player) {
                if (config.canUseSkill.getOrDefault(player.getUniqueId(), false)) {
                    return 600L;
                }
                return config.F_COOLDOWN.getOrDefault(player.getUniqueId(), config.f_Skill_Cool);
            }
        };
    }
}