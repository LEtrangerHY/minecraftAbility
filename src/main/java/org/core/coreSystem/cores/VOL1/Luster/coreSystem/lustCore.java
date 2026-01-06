package org.core.coreSystem.cores.VOL1.Luster.coreSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.main.Core;
import org.core.effect.crowdControl.ForceDamage;
import org.core.main.coreConfig;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL1.Luster.Skill.F;
import org.core.coreSystem.cores.VOL1.Luster.Skill.Q;
import org.core.coreSystem.cores.VOL1.Luster.Skill.R;

import java.util.Map;
import java.util.Set;

public class lustCore extends absCore {
    private final Core plugin;
    private final Luster config;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;


    public lustCore(Core plugin, coreConfig tag, Luster config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

        plugin.getLogger().info("Luster downloaded...");
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
        long addHP =
                player.getPersistentDataContainer().getOrDefault(
                        new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L) * 2;

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
    public void onGolemDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof IronGolem golem) {
            Player owner = null;

            for (Map.Entry<Player, Set<IronGolem>> entry : config.golems.entrySet()) {
                if (entry.getValue().remove(golem)) {
                    owner = entry.getKey();
                    break;
                }
            }

            if (owner != null && config.golems.get(owner).isEmpty() && tag.Luster.contains(owner)) {
                long cools = 60000L;
                cool.updateCooldown(owner, "F", cools);
                config.golems.remove(owner);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void passiveAttackEffect(PlayerInteractEvent event) {

        if(tag.Luster.contains(event.getPlayer())) {
            if (!pAttackUsing.contains(event.getPlayer().getUniqueId())) {

                Player player = event.getPlayer();

                if (hasProperItems(player)) {
                    if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {

                        if (cool.isReloading(player, "iron")) {
                            player.playSound(player.getLocation(), Sound.BLOCK_IRON_PLACE, 1, 1);
                            return;
                        }

                        cool.setCooldown(player, 2000L, "iron");

                        World world = player.getWorld();
                        Location playerLocation = player.getLocation();
                        Vector direction = playerLocation.getDirection().normalize().multiply(1.4);

                        player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue((double) 0.5);
                        world.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 1, 1);

                        config.collision.put(player.getUniqueId(), false);

                        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(200, 200, 200), 1.7f);
                        Particle.DustOptions dustOptions_gra = new Particle.DustOptions(Color.fromRGB(244, 244, 244), 1.4f);

                        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                                .withCausingEntity(player)
                                .withDirectEntity(player)
                                .build();

                        new BukkitRunnable() {
                            int ticks = 0;

                            @Override
                            public void run() {
                                if (ticks >= 14 || config.collision.getOrDefault(player.getUniqueId(), true)) {
                                    config.collision.remove(player.getUniqueId());
                                    this.cancel();
                                    return;
                                }

                                Location particleLocation = playerLocation.clone()
                                        .add(direction.clone().multiply(ticks * 1.4))
                                        .add(0, 1.4, 0);

                                world.spawnParticle(Particle.DUST, particleLocation, 1, 0.1, 0.1, 0.1, 0, dustOptions);
                                world.spawnParticle(Particle.DUST, particleLocation, 2, 0.1, 0.1, 0.1, 0, dustOptions_gra);

                                for (Entity entity : world.getNearbyEntities(particleLocation, 0.5, 0.5, 0.5)) {
                                    if (entity instanceof LivingEntity target && entity != player) {

                                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1, 1);

                                        ForceDamage forceDamage = new ForceDamage(target, 4.0, source);
                                        forceDamage.applyEffect(player);

                                        if (Math.random() < 0.26) {
                                            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);

                                            ForceDamage addForceDamage = new ForceDamage(target, 13.0, source);
                                            addForceDamage.applyEffect(player);
                                        }

                                        config.collision.put(player.getUniqueId(), true);
                                        break;
                                    }
                                }

                                ticks++;
                            }
                        }.runTaskTimer(plugin, 0L, 1L);

                        event.setCancelled(true);
                    }
                } else {
                    player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(4.0);
                }
            } else {
                pAttackUsing.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Luster.contains(player);
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
        return main.getType() == Material.HEAVY_CORE && (off.getType() == Material.LODESTONE || off.getType() == Material.IRON_INGOT);
    }

    private boolean canUseRSkill(Player player) { return true; }

    private boolean canUseQSkill(Player player) { return true; }

    private boolean canUseFSkill(Player player) { return true; }

    @Override
    protected boolean isItemRequired(Player player){
        return hasProperItems(player);
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem){
        ItemStack off = player.getInventory().getItemInOffHand();
        return droppedItem.getType() == Material.HEAVY_CORE && (off.getType() == Material.LODESTONE || off.getType() == Material.IRON_INGOT);
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
