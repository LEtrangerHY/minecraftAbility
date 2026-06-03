package org.core.coreSystem.cores.VOL3.Darmes.coreSystem;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL3.Darmes.Skill.F;
import org.core.coreSystem.cores.VOL3.Darmes.Skill.Q;
import org.core.coreSystem.cores.VOL3.Darmes.Skill.R;
import org.core.main.Core;
import org.core.main.coreConfig;

import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.crowdControl.Grounding;

import java.util.UUID;

public class darmesCore extends absCore {
    private final Core plugin;
    private final Darmes config;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    private static final Particle.DustOptions DUST_CHAIN = new Particle.DustOptions(Color.fromRGB(66, 66, 66), 0.6f);
    private static final BlockData CHAIN_BLOCK_DATA = Material.IRON_CHAIN.createBlockData();

    public darmesCore(Core plugin, coreConfig tag, Darmes config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

        plugin.getLogger().info("Darmes downloaded...");
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

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if(contains(player)) {
            UUID uuid = player.getUniqueId();

            player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "landingSlam"));
            player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "q_slam_active"));
            player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "q_slam_start_y"));
            player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "q_initial_y"));
            player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "noFallDamage"));

            config.q_reuse.remove(uuid);
            config.q_Skill_Jump.remove(uuid);
            config.blowParameter.remove(uuid);
        }
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

    @EventHandler
    public void onJump(PlayerJumpEvent event){
        Player player = event.getPlayer();
        World world = player.getWorld();

        if(contains(player) && config.blowParameter.getOrDefault(player.getUniqueId(), 0.0) > 0) {
            double jump = Math.min(1.6, config.blowParameter.getOrDefault(player.getUniqueId(), 0.0));
            double jumpParticle = 1 + jump * 10 / 3;

            world.spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), (int) jumpParticle, 0.3, 0.3, 0.3, 1);
            world.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.0f);
            player.setVelocity(new Vector(0, 0.42 + jump, 0));

            player.getPersistentDataContainer().set(new NamespacedKey(plugin, "landingSlam"), PersistentDataType.BOOLEAN, true);
            player.getPersistentDataContainer().set(new NamespacedKey(plugin, "noFallDamage"), PersistentDataType.BOOLEAN, true);

            config.blowParameter.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!contains(player)) return;

        UUID uuid = player.getUniqueId();
        NamespacedKey landingKey = new NamespacedKey(plugin, "landingSlam");
        NamespacedKey qSlamKey = new NamespacedKey(plugin, "q_slam_active");

        boolean hasLandingSlam = player.getPersistentDataContainer().has(landingKey, PersistentDataType.BOOLEAN);
        boolean hasQSlam = player.getPersistentDataContainer().has(qSlamKey, PersistentDataType.BOOLEAN);
        boolean hasQReuse = config.q_reuse.getOrDefault(uuid, false);

        if (!hasLandingSlam && !hasQSlam && !hasQReuse) return;

        if (event.getFrom().getY() > event.getTo().getY()) {
            Material posMat = event.getTo().getBlock().getType();
            Material belowMat = event.getTo().clone().subtract(0, 0.1, 0).getBlock().getType();

            Material hitMat = null;
            if (posMat.isSolid() || posMat == Material.WATER || posMat == Material.LAVA || posMat == Material.POWDER_SNOW || isDecorativeFloor(posMat)) {
                hitMat = posMat;
            } else if (belowMat.isSolid() || belowMat == Material.WATER || belowMat == Material.LAVA || belowMat == Material.POWDER_SNOW || isDecorativeFloor(belowMat)) {
                hitMat = belowMat;
            }

            if (hitMat != null) {
                if (hasLandingSlam) {
                    player.getPersistentDataContainer().remove(landingKey);
                    triggerLandingExplosion(player, hitMat);
                }

                if (hasQSlam) {
                    player.getPersistentDataContainer().remove(qSlamKey);
                    triggerQSlamExplosion(player, hitMat);
                }

                if (!hasQSlam && hasQReuse) {
                    config.q_reuse.remove(uuid);
                    config.q_Skill_Jump.remove(uuid);
                    player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "q_initial_y"));

                    cool.updateCooldown(player, "Q", config.q_Skill_Cool);
                }
            }
        }
    }

    private void triggerLandingExplosion(Player player, Material hitMat) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        world.spawnParticle(Particle.EXPLOSION, loc.add(0, 0.2, 0), 3, 0.6, 0.2, 0.6, 0);

        spawnEnvironmentParticles(world, loc, hitMat);

        double radius = 2.2;

        long fLevel = player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "F"), PersistentDataType.LONG, 0L);

        double fAmp = config.f_Skill_amp * fLevel;

        double damage = 2.0 * (1 + fAmp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                ForceDamage forceDamage = new ForceDamage(target, damage, source, false);
                forceDamage.applyEffect(player);
            }
        }
    }

    private void triggerQSlamExplosion(Player player, Material hitMat) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        NamespacedKey qSlamStartYKey = new NamespacedKey(plugin, "q_slam_start_y");
        double startY = player.getPersistentDataContainer().getOrDefault(qSlamStartYKey, PersistentDataType.DOUBLE, loc.getY());
        player.getPersistentDataContainer().remove(qSlamStartYKey);

        double fallHeight = Math.max(0, startY - loc.getY());

        double ratio = Math.min(fallHeight / 26.0, 1.0);
        double baseDamage = 6.0 + (16.0 * Math.pow(ratio, 2));

        double damageRatio = baseDamage / 22.0;
        int expCount = Math.max(1, (int) (6 * damageRatio));
        int chainCount = Math.max(1, (int) (40 * damageRatio));
        int smokeCount = Math.max(1, (int) (10 * damageRatio));

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
        world.spawnParticle(Particle.EXPLOSION, loc.add(0, 0.2, 0), expCount, 1.5, 0.2, 1.5, 0);
        world.spawnParticle(Particle.BLOCK, loc.clone().add(0, 0.5, 0), chainCount, 1.5, 0.5, 1.5, CHAIN_BLOCK_DATA);
        world.spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0, 0.5, 0), smokeCount, 1.5, 0.2, 1.5, 0.05);

        spawnEnvironmentParticles(world, loc, hitMat);

        new BukkitRunnable() {
            int playCount = 0;
            @Override
            public void run() {
                if (playCount >= 4) {
                    this.cancel();
                    return;
                }
                world.playSound(loc, Sound.BLOCK_CHAIN_PLACE, 1.6f, 1.0f);
                playCount++;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        double radius = 3.6;

        long qLevel = player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L);
        double amp = config.q_Skill_amp * qLevel;

        double finalDamage = baseDamage * (1 + amp);

        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        boolean isHit = false;

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                ForceDamage forceDamage = new ForceDamage(target, finalDamage, source, false);
                forceDamage.applyEffect(player);

                new Grounding(target, 2000).applyEffect(player);
                chain_qSkill_Particle_Effect(player, target, 40);

                isHit = true;
            }
        }

        if (isHit) {
            if (fallHeight >= 8.0) {
                world.playSound(loc, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.8f);
            } else {
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);
            }
        }
    }

    private void spawnEnvironmentParticles(World world, Location loc, Material mat) {
        if (mat == Material.WATER) {
            world.spawnParticle(Particle.FALLING_WATER, loc.clone().add(0, 0.5, 0), 100, 1.5, 0.5, 1.5, 0.1);
            world.playSound(loc, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.0f, 1.0f);
        } else if (mat == Material.LAVA) {
            world.spawnParticle(Particle.LAVA, loc.clone().add(0, 0.5, 0), 40, 1.5, 0.5, 1.5, 0);
            world.playSound(loc, Sound.ENTITY_HOSTILE_SPLASH, 1.0f, 1.0f);
        } else if (mat == Material.POWDER_SNOW || mat == Material.SNOW || mat == Material.SNOW_BLOCK) {
            world.spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 0.5, 0), 100, 1.5, 0.5, 1.5, 0.05);
            world.spawnParticle(Particle.BLOCK, loc.clone().add(0, 0.5, 0), 40, 1.0, 0.5, 1.0, Material.SNOW_BLOCK.createBlockData());
            world.playSound(loc, Sound.BLOCK_POWDER_SNOW_BREAK, 1.2f, 0.8f);
        } else if (mat != null) {
            Material scatterMat = mat;

            if (isPlantOrLeaf(mat)) {
                scatterMat = getUnderlyingSolidBlock(loc);
            }

            if (scatterMat.isBlock()) {
                world.spawnParticle(Particle.BLOCK, loc.clone().add(0, 0.5, 0), 50, 1.5, 0.3, 1.5, scatterMat.createBlockData());
            }
        }
    }

    private boolean isDecorativeFloor(Material mat) {
        if (mat == null || mat.isAir()) return false;
        String name = mat.name();
        if (name.endsWith("_CARPET") || name.equals("SNOW")) return true;
        return isPlantOrLeaf(mat);
    }

    private boolean isPlantOrLeaf(Material mat) {
        if (mat == null || mat.isAir()) return false;
        String name = mat.name();

        if (name.endsWith("_LEAVES")) return true;

        if (!mat.isSolid()) {
            return name.contains("GRASS") || name.contains("FERN") || name.contains("FLOWER") ||
                    name.endsWith("TULIP") || name.equals("DANDELION") || name.equals("POPPY") ||
                    name.equals("BLUE_ORCHID") || name.equals("ALLIUM") || name.equals("AZURE_BLUET") ||
                    name.equals("OXEYE_DAISY") || name.equals("CORNFLOWER") || name.equals("LILY_OF_THE_VALLEY") ||
                    name.equals("SUNFLOWER") || name.equals("LILAC") || name.equals("ROSE_BUSH") ||
                    name.equals("PEONY") || name.endsWith("_SAPLING") || name.endsWith("_MUSHROOM") ||
                    name.endsWith("_FUNGUS") || name.endsWith("_ROOTS") || name.equals("DEAD_BUSH") ||
                    name.equals("LILY_PAD") || name.endsWith("VINE") || name.equals("GLOW_LICHEN") ||
                    name.equals("MOSS_CARPET") || name.equals("HANGING_ROOTS") || name.equals("SWEET_BERRY_BUSH");
        }
        return false;
    }

    private Material getUnderlyingSolidBlock(Location loc) {
        Block b = loc.getBlock();
        for (int i = 0; i <= 3; i++) {
            Material type = b.getType();
            if (type.isSolid() && !isPlantOrLeaf(type)) {
                return type;
            }
            b = b.getRelative(BlockFace.DOWN);
        }
        return Material.DIRT;
    }

    public void chain_qSkill_Particle_Effect(Player player, Entity entity, int time) {
        World world = player.getWorld();

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick > time || entity.isDead() || !entity.isValid()) {
                    cancel();
                    return;
                }

                Location baseLoc = entity.getLocation();
                for (int i = 0; i < 33; i += 2) {
                    double yOffset = i / 10.0;
                    world.spawnParticle(Particle.DUST, baseLoc.clone().add(0, yOffset, 0), 1, 0, 0, 0, 0, DUST_CHAIN);

                    if (i % 3 == 0) {
                        double hitY = 3.3 - (i * 0.12);
                        world.spawnParticle(Particle.ENCHANTED_HIT, baseLoc.clone().add(0, hitY, 0), 1, 0, 0, 0, 0);
                    }
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if(contains(player)) {
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
        if(contains(player)) {
            if (player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "noFallDamage"), PersistentDataType.BOOLEAN, false)) {
                player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "noFallDamage"));
            }
        }
    }

    @Override
    protected boolean contains(Player player) { return tag.Darmes.contains(player); }

    @Override
    protected SkillBase getRSkill() { return Rskill; }

    @Override
    protected SkillBase getQSkill() { return Qskill; }

    @Override
    protected SkillBase getFSkill() { return Fskill; }

    private boolean hasProperItems(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return main.getType() == Material.MACE && off.getType() == Material.IRON_CHAIN;
    }

    private boolean canUseRSkill(Player player) { return true; }
    private boolean canUseQSkill(Player player) { return true; }
    private boolean canUseFSkill(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        return (main.getType() == Material.MACE);
    }

    @Override
    protected boolean isItemRequired(Player player){ return hasProperItems(player); }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem){
        ItemStack off = player.getInventory().getItemInOffHand();
        return droppedItem.getType() == Material.MACE &&
                off.getType() == Material.IRON_CHAIN;
    }

    @Override
    protected boolean isRCondition(Player player) { return canUseRSkill(player); }
    @Override
    protected boolean isQCondition(Player player) { return canUseQSkill(player); }
    @Override
    protected boolean isFCondition(Player player) { return canUseFSkill(player); }

    @Override
    protected ConfigWrapper getConfigWrapper() {
        return new ConfigWrapper() {
            @Override
            public void variableReset(Player player) {
                config.variableReset(player);
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
            public long getRcooldown(Player player) { return config.R_COOLDOWN.getOrDefault(player.getUniqueId(), config.r_Skill_Cool); }
            @Override
            public long getQcooldown(Player player) { return config.Q_COOLDOWN.getOrDefault(player.getUniqueId(), config.q_Skill_Cool); }
            @Override
            public long getFcooldown(Player player) { return config.F_COOLDOWN.getOrDefault(player.getUniqueId(), config.f_Skill_Cool); }
        };
    }
}