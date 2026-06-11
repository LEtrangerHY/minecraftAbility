package org.core.coreSystem.cores.VOL3.Charlotte.coreSystem;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL3.Charlotte.Skill.F;
import org.core.coreSystem.cores.VOL3.Charlotte.Skill.Q;
import org.core.coreSystem.cores.VOL3.Charlotte.Skill.R;
import org.core.effect.crowdControl.ForceDamage;
import org.core.effect.debuff.Burn;
import org.core.effect.debuff.Frost;
import org.core.main.Core;
import org.core.main.coreConfig;

public class charCore extends absCore {
    private final Core plugin;
    private final Charlotte config;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    private static final BlockData GLASS = Material.GLASS.createBlockData();
    private static final BlockData CHAIN = Material.IRON_CHAIN.createBlockData();

    public charCore(Core plugin, coreConfig tag, Charlotte config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

        plugin.getLogger().info("Charlotte downloaded...");
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

    @Override
    protected boolean contains(Player player) {
        return tag.Charlotte.contains(player);
    }

    @Override
    protected boolean isCustomAttackUser(Player player) {
        return true;
    }

    @Override
    protected void onLSkillCooldown(PlayerInteractEvent event, Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_HIT, 1, 1);
    }

    @Override
    protected void LSkill(PlayerInteractEvent event, Player player) {
        event.setCancelled(true);

        World world = player.getWorld();
        Location playerLocation = player.getLocation();
        Vector direction = playerLocation.getDirection().normalize().multiply(1.3);

        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.setBaseValue(1 / 3.0);

        world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.2f);
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 0.5f, 1.5f);

        config.collision.put(player.getUniqueId(), false);

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (ticks >= 16 || config.collision.getOrDefault(player.getUniqueId(), true)) {
                    config.collision.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                Location particleLocation = playerLocation.clone()
                        .add(direction.clone().multiply(ticks * 1.5))
                        .add(0, 1.2, 0);

                world.spawnParticle(Particle.BLOCK, particleLocation, 6, 0.3, 0.3, 0.3, GLASS);
                world.spawnParticle(Particle.ENCHANT, particleLocation, 12, 0.3, 0.3, 0.3, 0);
                world.spawnParticle(Particle.END_ROD, particleLocation, 2, 0.1, 0.1, 0.1, 0.02);

                Block block = particleLocation.getBlock();

                if(!block.isPassable()){
                    Burst(player, particleLocation);
                    config.collision.put(player.getUniqueId(), true);
                }

                for (Entity entity : world.getNearbyEntities(particleLocation, 0.6, 0.6, 0.6)) {
                    if (entity instanceof LivingEntity target && entity != player) {

                        ForceDamage forceDamage = new ForceDamage(target, 6.0, source, false);
                        forceDamage.applyEffect(player);
                        Burst(player, particleLocation);
                        config.collision.put(player.getUniqueId(), true);
                        break;
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void Burst(Player player, Location loc){
        World world = player.getWorld();

        int prism = config.f_prism.getOrDefault(player.getUniqueId(), 1);

        world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.8f);
        world.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.2f, 1.4f);
        world.playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.5f);

        world.spawnParticle(Particle.ENCHANTED_HIT, loc, 25 * prism, 0.1, 0.1, 0.1, 0.5);

        world.spawnParticle(Particle.BLOCK, loc, 15 * prism, 0.2, 0.2, 0.2, 0.35, GLASS);

        world.spawnParticle(Particle.END_ROD, loc, 12 * prism, 0.1, 0.1, 0.1, 0.6);

        if (prism >= 6) {
            new BukkitRunnable() {
                int playCount = 0;
                @Override
                public void run() {
                    if (playCount >= 4) {
                        this.cancel();
                        return;
                    }
                    world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, 1.6f, 1.0f + (playCount * 0.15f));
                    world.spawnParticle(Particle.ENCHANTED_HIT, loc, 5 * prism, prism * 0.3, prism * 0.3, prism * 0.3, 0.1);
                    playCount++;
                }
            }.runTaskTimer(plugin, 0L, 2L);

            world.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 0.5f);
            world.spawnParticle(Particle.BLOCK, loc, 12 * prism, 0.2, 0.2, 0.2, 0.5, CHAIN);
        }

        world.spawnParticle(Particle.ENCHANT, loc, 10 * prism, prism * 0.4, prism * 0.4, prism * 0.4, 0.5);

        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        double damage = 2.0 * prism;

        for (Entity entity : world.getNearbyEntities(loc, prism * 0.5, prism * 0.5, prism * 0.5)) {
            if (entity instanceof LivingEntity target && entity != player) {
                ForceDamage forceDamage = new ForceDamage(target, damage, source, false);
                forceDamage.applyEffect(player);
            }
        }

        config.f_prism.remove(player.getUniqueId());
    }

    @EventHandler
    public void sneakPassive(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (!event.isSneaking() || !hasProperItems(player) || !tag.Charlotte.contains(player)) return;
        World world = player.getWorld();

        org.bukkit.entity.BlockDisplay[] shieldBlocks = new org.bukkit.entity.BlockDisplay[6];

        BlockData glassData = Material.GLASS_PANE.createBlockData();
        if (glassData instanceof org.bukkit.block.data.MultipleFacing) {
            MultipleFacing pane = (org.bukkit.block.data.MultipleFacing) glassData;
            pane.setFace(BlockFace.EAST, true);
            pane.setFace(BlockFace.WEST, true);
            glassData = pane;
        }

        Location spawnLoc = player.getLocation();
        for (int i = 0; i < 6; i++) {
            org.bukkit.entity.BlockDisplay display = world.spawn(spawnLoc, org.bukkit.entity.BlockDisplay.class);
            display.setBlock(glassData);

            display.setTeleportDuration(1);

            display.setTransformation(new org.bukkit.util.Transformation(
                    new org.joml.Vector3f(-0.5f, -0.5f, -0.5f),
                    new org.joml.Quaternionf(),
                    new org.joml.Vector3f(1f, 1f, 1f),
                    new org.joml.Quaternionf()
            ));

            shieldBlocks[i] = display;
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.isSneaking() || player.isDead()) {
                    for (org.bukkit.entity.BlockDisplay display : shieldBlocks) {
                        if (display != null && display.isValid()) {
                            display.remove();
                        }
                    }
                    this.cancel();
                    return;
                }

                Location eyeLoc = player.getEyeLocation();
                Vector forward = eyeLoc.getDirection().normalize();

                float playerYaw = eyeLoc.getYaw();
                float playerPitch = eyeLoc.getPitch();

                Vector up = new Vector(0, 1, 0);
                Vector right;

                if (Math.abs(forward.getY()) > 0.99) {
                    right = new Vector(1, 0, 0);
                } else {
                    right = forward.clone().crossProduct(up).normalize();
                }
                Vector actualUp = right.clone().crossProduct(forward).normalize();

                Location center = eyeLoc.clone().add(forward.clone().multiply(3));

                int index = 0;
                for (int row = 0; row < 2; row++) {
                    for (int col = -1; col <= 1; col++) {
                        double rightOffset = col * 1.0;
                        double upOffset = (row == 0) ? -0.5 : 0.5;

                        Vector offset = right.clone().multiply(rightOffset).add(actualUp.clone().multiply(upOffset));
                        Location targetLoc = center.clone().add(offset);

                        Location displayLoc = targetLoc.clone();
                        displayLoc.setYaw(playerYaw);
                        displayLoc.setPitch(playerPitch);

                        org.bukkit.entity.BlockDisplay display = shieldBlocks[index];
                        if (display != null && display.isValid()) {
                            display.teleport(displayLoc);
                        }
                        index++;
                    }
                }

                double widthLimit = 1.8;
                double heightLimit = 1.5;
                double depthLimit = 0.6;

                for (Entity entity : world.getNearbyEntities(center, 3.0, 3.0, 3.0)) {
                    if (entity.equals(player)) continue;

                    Vector entityCenter = entity.getLocation().toVector();
                    if (entity instanceof LivingEntity) {
                        entityCenter.add(new Vector(0, entity.getHeight() / 2, 0));
                    }

                    Vector toEntity = entityCenter.subtract(center.toVector());

                    double dotRight = toEntity.dot(right);
                    double dotUp = toEntity.dot(actualUp);
                    double dotForward = toEntity.dot(forward);

                    if (Math.abs(dotRight) <= widthLimit && Math.abs(dotUp) <= heightLimit && Math.abs(dotForward) <= depthLimit) {

                        if (entity instanceof Projectile proj) {
                            if (proj.getShooter() == player) continue;

                            world.playSound(entity.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                            world.spawnParticle(Particle.BLOCK, entity.getLocation(), 15, 0.2, 0.2, 0.2, GLASS);
                            proj.remove();
                        }
                        else if (entity instanceof LivingEntity) {
                            Vector pushVector = forward.clone().multiply(0.7);
                            pushVector.setY(0.15);
                            entity.setVelocity(pushVector);
                        }
                    }
                }
            }
        };

        task.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onShieldBlockDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (!player.isSneaking() || !hasProperItems(player) || !tag.Charlotte.contains(player)) return;

        Location eyeLoc = player.getEyeLocation();
        Vector forward = eyeLoc.getDirection().normalize();

        Vector up = new Vector(0, 1, 0);
        Vector right;
        if (Math.abs(forward.getY()) > 0.99) {
            right = new Vector(1, 0, 0);
        } else {
            right = forward.clone().crossProduct(up).normalize();
        }
        Vector actualUp = right.clone().crossProduct(forward).normalize();

        Location center = eyeLoc.clone().add(forward.clone().multiply(3));

        Entity damager = event.getDamager();
        Location damagerLoc;
        if (damager instanceof Projectile) {
            damagerLoc = damager.getLocation();
        } else if (damager instanceof LivingEntity) {
            damagerLoc = ((LivingEntity) damager).getEyeLocation();
        } else {
            damagerLoc = damager.getLocation();
        }

        Vector rayDir = damagerLoc.toVector().subtract(eyeLoc.toVector());
        double rayDotForward = rayDir.dot(forward);

        if (rayDotForward > 0.0001) {

            double t = 3.0 / rayDotForward;

            if (t > 0 && t <= 1.5) {
                Vector intersection = eyeLoc.toVector().add(rayDir.clone().multiply(t));
                Vector toIntersection = intersection.subtract(center.toVector());

                double dotRight = toIntersection.dot(right);
                double dotUp = toIntersection.dot(actualUp);

                double widthLimit = 1.8;
                double heightLimit = 1.5;

                if (Math.abs(dotRight) <= widthLimit && Math.abs(dotUp) <= heightLimit) {
                    event.setCancelled(true);

                    World world = player.getWorld();
                    world.playSound(intersection.toLocation(world), Sound.BLOCK_GLASS_HIT, 1.0f, 1.0f);
                    world.spawnParticle(Particle.BLOCK, intersection.toLocation(world), 15, 0.2, 0.2, 0.2, GLASS);

                    if (damager instanceof Projectile) {
                        damager.remove();
                    }
                }
            }
        }
    }

    @Override
    protected SkillBase getRSkill() { return Rskill; }

    @Override
    protected SkillBase getQSkill() { return Qskill; }

    @Override
    protected SkillBase getFSkill() { return Fskill; }

    private boolean hasProperItems(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return main.getType() == Material.PRISMARINE_CRYSTALS && off.getType() == Material.IRON_CHAIN;
    }

    private boolean canUseRSkill(Player player) { return true; }
    private boolean canUseQSkill(Player player) { return true; }
    private boolean canUseFSkill(Player player) { return true; }

    @Override
    protected boolean isItemRequired(Player player){ return hasProperItems(player); }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem){
        ItemStack off = player.getInventory().getItemInOffHand();
        return droppedItem.getType() == Material.PRISMARINE_CRYSTALS &&
                off.getType() == Material.IRON_CHAIN;
    }

    @Override
    protected boolean isRCondition(Player player) { return canUseRSkill(player); }

    @Override
    protected boolean isQCondition(Player player) {
        return canUseQSkill(player) && !Q.isMoving(player);
    }

    @Override
    protected boolean isFCondition(Player player) { return canUseFSkill(player); }

    @Override
    protected boolean isRAnimated(Player player) {
        return false;
    }

    @Override
    protected boolean isFAnimated(Player player) {
        return true;
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
                return 3000L;
            }

            @Override
            public long getRcooldown(Player player) { return config.R_COOLDOWN.getOrDefault(player.getUniqueId(), config.r_Skill_Cool); }

            @Override
            public long getQcooldown(Player player) {
                return Q.isRecastPhase(player) ? config.q_Skill_Cool : 0L;
            }

            @Override
            public long getFcooldown(Player player) { return config.F_COOLDOWN.getOrDefault(player.getUniqueId(), config.f_Skill_Cool); }
        };
    }
}