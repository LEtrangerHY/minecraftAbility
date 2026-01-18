package org.core.coreSystem.cores.VOL2.Lavender.coreSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL2.Lavender.Skill.F;
import org.core.coreSystem.cores.VOL2.Lavender.Skill.Q;
import org.core.coreSystem.cores.VOL2.Lavender.Skill.R;
import org.core.effect.crowdControl.ForceDamage;
import org.core.main.Core;
import org.core.main.coreConfig;

public class lavCore extends absCore {
    private final Core plugin;
    private final Lavender config;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public lavCore(Core plugin, coreConfig tag, Lavender config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool, Rskill);
        this.Fskill = new F(config, plugin, cool, Rskill);

        plugin.getLogger().info("Lavender downloaded...");
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

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (config.activeWalls.containsKey(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void passiveAttackRange(PlayerInteractEvent event) {

        if(tag.Lavender.contains(event.getPlayer())) {
            if (!pAttackUsing.contains(event.getPlayer().getUniqueId())) {

                Player player = event.getPlayer();

                if (hasProperItems(player) && !config.r_Skill_using.containsKey(player.getUniqueId())) {
                    if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {

                        World world = player.getWorld();

                        boolean coolSet = player.getAttackCooldown() >= 1.0;
                        double extendedRange = coolSet ? 9.0 : 6.6;
                        double damage = coolSet ? 4.0 : 2.0;

                        RayTraceResult result =
                                player.getWorld().rayTrace(player.getEyeLocation(), player.getEyeLocation().getDirection(),
                                        extendedRange, FluidCollisionMode.NEVER, true, 0.6,
                                        entity -> entity != player && entity instanceof LivingEntity);

                        if (result != null && result.getHitEntity() != null) {
                            LivingEntity target = (LivingEntity) result.getHitEntity();

                            BlockData amethyst = Material.AMETHYST_BLOCK.createBlockData();

                            double distance = player.getLocation().distance(target.getLocation());

                            if (distance <= 3.5) return;

                            DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                                    .withCausingEntity(player)
                                    .withDirectEntity(player)
                                    .build();

                            target.damage(damage, source);

                            ItemStack mainHand = player.getInventory().getItemInMainHand();
                            ItemMeta meta = mainHand.getItemMeta();
                            if (meta instanceof Damageable && mainHand.getType().getMaxDurability() > 0) {
                                Damageable damageable = (Damageable) meta;
                                int newDamage = damageable.getDamage() + 1;
                                damageable.setDamage(newDamage);
                                mainHand.setItemMeta(meta);

                                if (newDamage >= mainHand.getType().getMaxDurability()) {
                                    player.getInventory().setItemInMainHand(null);
                                }
                            }

                            if(coolSet) {
                                world.spawnParticle(Particle.BLOCK, target.getLocation().clone().add(0, 1.2, 0), 12, 0.6, 0.6, 0.6, amethyst);
                                world.spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().clone().add(0, 1.2, 0), 1, 0, 0, 0, 1);
                                world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
                                world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.0f);
                            }else{
                                world.spawnParticle(Particle.BLOCK, target.getLocation().clone().add(0, 1.2, 0), 6, 0.6, 0.6, 0.6, amethyst);
                                world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 1.0f, 1.0f);
                                world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.6f, 1.0f);
                            }
                        }
                    }
                }
            } else {
                pAttackUsing.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Lavender.contains(player);
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
        return main.getType() == Material.IRON_SWORD && off.getType() == Material.AMETHYST_SHARD;
    }

    private boolean canUseRSkill(Player player) {
        return !config.bladeShoot.getOrDefault(player.getUniqueId(), false);
    }

    private boolean canUseQSkill(Player player) {
        return !config.bladeShoot.getOrDefault(player.getUniqueId(), false);
    }

    private boolean canUseFSkill(Player player) {
        return !config.bladeShoot.getOrDefault(player.getUniqueId(), false);
    }

    @Override
    protected boolean isItemRequired(Player player){
        return hasProperItems(player);
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem){
        ItemStack off = player.getInventory().getItemInOffHand();
        return droppedItem.getType() == Material.IRON_SWORD &&
                off.getType() == Material.AMETHYST_SHARD;
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
