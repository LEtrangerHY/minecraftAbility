package org.core.coreSystem.cores.VOL6.Luster.coreSystem;

import org.core.models.skillObj.lustShardModel;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
import org.core.coreSystem.cores.VOL6.Luster.Skill.F;
import org.core.coreSystem.cores.VOL6.Luster.Skill.Q;
import org.core.coreSystem.cores.VOL6.Luster.Skill.R;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
        long addHP = player.getPersistentDataContainer().getOrDefault(
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
        if (event.getEntity() instanceof IronGolem deadGolem) {
            Player owner = null;
            UUID deadUUID = deadGolem.getUniqueId();

            for (Map.Entry<Player, Set<IronGolem>> entry : config.golems.entrySet()) {
                Set<IronGolem> golemSet = entry.getValue();
                Iterator<IronGolem> iterator = golemSet.iterator();

                while (iterator.hasNext()) {
                    IronGolem golem = iterator.next();
                    if (golem.getUniqueId().equals(deadUUID)) {
                        iterator.remove();
                        owner = entry.getKey();
                        break;
                    }
                }

                if (owner != null) {
                    break;
                }
            }

            if (owner != null) {
                event.getDrops().clear();
                event.setDroppedExp(0);

                boolean allDead = true;
                for (IronGolem golem : config.golems.get(owner)) {
                    if (golem != null && !golem.isDead()) {
                        allDead = false;
                        break;
                    }
                }

                if (allDead && tag.Luster.contains(owner)) {
                    cool.updateCooldown(owner, "Golem Duration", 0L, "boss");
                    long cools = 66000L;
                    cool.updateCooldown(owner, "F", cools);
                    config.golems.remove(owner);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onGolemDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof IronGolem attackingGolem) {
            boolean isPlayerOwned = false;
            UUID attackingUUID = attackingGolem.getUniqueId();

            for (Set<IronGolem> ownedGolems : config.golems.values()) {
                for (IronGolem golem : ownedGolems) {
                    if (golem != null && golem.getUniqueId().equals(attackingUUID)) {
                        isPlayerOwned = true;
                        break;
                    }
                }
                if (isPlayerOwned) break;
            }

            if (isPlayerOwned) {
                double originalDamage = event.getDamage();
                event.setDamage(originalDamage * 0.66);
            }
        }
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Luster.contains(player);
    }

    @Override
    protected boolean isCustomAttackUser(Player player) {
        return true;
    }

    @Override
    protected void onLSkillCooldown(PlayerInteractEvent event, Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_IRON_PLACE, 1, 1);
    }

    @Override
    protected void LSkill(PlayerInteractEvent event, Player player) {
        event.setCancelled(true);

        World world = player.getWorld();
        Location playerLocation = player.getLocation();
        Vector direction = playerLocation.getDirection().normalize();

        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.setBaseValue((double) 1 / 1.3);

        config.collision.put(player.getUniqueId(), false);

        boolean isCrit = Math.random() < 0.26;
        double finalDamage = isCrit ? 12.0 : 6.0;

        int blockCount = isCrit ? 7 : 3;

        world.playSound(playerLocation, Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0f, isCrit ? 0.8f : 1.2f);
        if (isCrit) {
            world.playSound(playerLocation, Sound.BLOCK_ANVIL_PLACE, 0.6f, 1.5f);
        }

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Location spawnLoc = playerLocation.clone().add(0, 1.4, 0).add(direction.clone().multiply(0.5));
        List<BlockDisplay> blockDisplays = new lustShardModel(world, spawnLoc, blockCount).getDisplays();
        Vector[] offsets = new Vector[blockCount];

        for (int i = 0; i < blockCount; i++) {
            offsets[i] = new Vector(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5)
                    .normalize()
                    .multiply(isCrit ? 0.35 : 0.15);
        }

        new BukkitRunnable() {
            int ticks = 0;
            double speed = 1.4;

            @Override
            public void run() {
                if (ticks >= 13 || config.collision.getOrDefault(player.getUniqueId(), true) || !player.isOnline()) {
                    blockDisplays.forEach(Entity::remove);
                    config.collision.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                Location center = playerLocation.clone()
                        .add(0, 1.4, 0)
                        .add(direction.clone().multiply(ticks * speed));

                float rotationStep = ticks * 35f;
                for (int i = 0; i < blockDisplays.size(); i++) {
                    BlockDisplay bd = blockDisplays.get(i);
                    Location tpLoc = center.clone().add(offsets[i]);
                    tpLoc.setYaw(rotationStep + i * 45f);
                    tpLoc.setPitch(rotationStep + i * 60f);
                    bd.teleport(tpLoc);
                }

                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(200, 200, 200), isCrit ? 1.5f : 1.0f);
                world.spawnParticle(Particle.DUST, center, isCrit ? 4 : 2, 0.2, 0.2, 0.2, 0, dust);

                for (Entity entity : world.getNearbyEntities(center, 0.7, 0.7, 0.7)) {
                    if (entity instanceof LivingEntity target && entity != player) {

                        player.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);
                        world.playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.2f);

                        world.spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, Material.IRON_BLOCK.createBlockData());

                        ForceDamage forceDamage = new ForceDamage(target, finalDamage, source, false);
                        forceDamage.applyEffect(player);

                        blockDisplays.forEach(Entity::remove);
                        config.collision.put(player.getUniqueId(), true);
                        break;
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
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
    protected boolean isRAnimated(Player player) {
        return false;
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

                cool.updateCooldown(player, "R", config.frozenCool);
                cool.updateCooldown(player, "Q", config.frozenCool);
                cool.updateCooldown(player, "F", config.frozenCool);
            }

            @Override
            public long getLcooldown(Player player) {
                return 1300L;
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