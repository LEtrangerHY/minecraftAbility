package org.core.coreSystem.cores.VOL1.Commander.coreSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.main.Core;
import org.core.effect.crowdControl.ForceDamage;
import org.core.main.coreConfig;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL1.Commander.Skill.F;
import org.core.coreSystem.cores.VOL1.Commander.Skill.Q;
import org.core.coreSystem.cores.VOL1.Commander.Skill.R;

import java.util.HashSet;

public class comCore extends absCore {
    private final Core plugin;
    private final Commander config;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public comCore(Core plugin, coreConfig tag, Commander config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

        plugin.getLogger().info("Commander downloaded...");
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

    @Override
    protected boolean contains(Player player) {
        return tag.Commander.contains(player);
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
        Vector direction = playerLocation.getDirection().normalize().multiply(1.0);

        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.setBaseValue(1.0);

        world.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2.0f, 1);

        config.collision.put(player.getUniqueId(), false);

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(220, 150, 100), 1.2f);
        Particle.DustOptions dustOptions_gra = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.7f);

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 12 || config.collision.getOrDefault(player.getUniqueId(), true)) {
                    config.collision.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                Location particleLocation = playerLocation.clone()
                        .add(direction.clone().multiply(ticks * 1.6))
                        .add(0, 1.4, 0);

                world.spawnParticle(Particle.DUST, particleLocation, 1, 0.1, 0.1, 0.1, 0, dustOptions);
                world.spawnParticle(Particle.DUST, particleLocation, 2, 0.1, 0.1, 0.1, 0, dustOptions_gra);

                for (Entity entity : world.getNearbyEntities(particleLocation, 0.5, 0.5, 0.5)) {
                    if (entity instanceof LivingEntity target && entity != player) {

                        ForceDamage forceDamage = new ForceDamage(target, 3.0, source, false);
                        forceDamage.applyEffect(player);

                        for (FallingBlock fb : config.comBlocks.getOrDefault(player.getUniqueId(), new HashSet<>())) {
                            commandReceiver(player, fb);
                        }

                        config.collision.put(player.getUniqueId(), true);
                        break;
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void commandReceiver(Player player, FallingBlock fb) {
        World world = player.getWorld();
        Location center = fb.getLocation();

        for (Entity entity : world.getNearbyEntities(center, 5, 5, 5)) {
            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2.0f, 1);

            DamageSource source = DamageSource.builder(DamageType.MAGIC)
                    .withCausingEntity(player)
                    .withDirectEntity(player)
                    .build();

            ForceDamage forceDamage = new ForceDamage((LivingEntity) entity, 1.0, source, false);
            forceDamage.applyEffect(player);

            Location start = fb.getLocation().clone().add(0, 0.5, 0);
            Vector dir = entity.getLocation().clone().add(0, 1.2, 0).toVector().subtract(start.toVector()).normalize();
            double maxDistance = start.distance(entity.getLocation().clone().add(0, 1, 0));

            attackLine(player, maxDistance, start, dir);
        }
    }

    public void attackLine(Player player, double maxDistance, Location start, Vector direction){
        double step = 0.2;
        Particle.DustOptions dustOptions_gra = new Particle.DustOptions(Color.fromRGB(0, 255, 0), 0.7f);

        for (double i = 0; i <= maxDistance; i += step) {
            Location point = start.clone().add(direction.clone().multiply(i));
            player.spawnParticle(Particle.DUST, point, 2, 0.05, 0.05, 0.05, 0, dustOptions_gra);
        }
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
        return main.getType() == Material.CLOCK && off.getType() == Material.AIR;
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
        return droppedItem.getType() == Material.CLOCK && off.getType() == Material.AIR;
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
                return 1000L;
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