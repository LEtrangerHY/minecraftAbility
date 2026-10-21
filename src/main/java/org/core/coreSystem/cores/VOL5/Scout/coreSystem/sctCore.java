package org.core.coreSystem.cores.VOL5.Scout.coreSystem;

import org.core.models.coreObj.bulletModel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL5.Scout.Skill.F;
import org.core.coreSystem.cores.VOL5.Scout.Skill.Q;
import org.core.coreSystem.cores.VOL5.Scout.Skill.R;
import org.core.effect.crowdControl.ForceDamage;
import org.core.main.Core;
import org.core.main.coreConfig;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Predicate;

public class sctCore extends absCore {
    private final Core plugin;
    private final Scout config;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public sctCore(Core plugin, coreConfig tag, Scout config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

        plugin.getLogger().info("Scout downloaded...");
    }

    private void sendSubTitle(Player player, String msg, NamedTextColor color) {
        Title title = Title.title(
                Component.empty(),
                Component.text(msg).color(color),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(300), Duration.ofMillis(200))
        );
        player.showTitle(title);
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
                new NamespacedKey(plugin, "Q"), PersistentDataType.LONG, 0L)
                + player.getPersistentDataContainer().getOrDefault(
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
    public void sneakReload(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!event.isSneaking() || !hasProperItems(player) || !contains(player)) {
            if (!event.isSneaking() && config.activeReloadTasks.containsKey(uuid)) {
                config.activeReloadTasks.get(uuid).cancel();
                config.activeReloadTasks.remove(uuid);
                if (config.activeReloadBars.containsKey(uuid)) {
                    config.activeReloadBars.get(uuid).removeAll();
                    config.activeReloadBars.remove(uuid);
                }
                sendSubTitle(player, "Reload Canceled", NamedTextColor.RED);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            }
            return;
        }

        if (config.isOverclocked.getOrDefault(uuid, false)) return;
        if (config.ammo.getOrDefault(uuid, config.MAX_AMMO) > 0) return;

        long durationTicks = 40L;

        if (config.activeReloadTasks.containsKey(uuid)) {
            config.activeReloadTasks.get(uuid).cancel();
            config.activeReloadTasks.remove(uuid);
        }
        if (config.activeReloadBars.containsKey(uuid)) {
            config.activeReloadBars.get(uuid).removeAll();
            config.activeReloadBars.remove(uuid);
        }

        BossBar bossBar = Bukkit.createBossBar("RELOADING...", BarColor.WHITE, BarStyle.SOLID);
        bossBar.setProgress(0.0);
        bossBar.addPlayer(player);
        config.activeReloadBars.put(uuid, bossBar);

        player.playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1.0f, 1.5f);

        BukkitRunnable task = new BukkitRunnable() {
            long ticks = 0;

            @Override
            public void run() {
                if (!player.isSneaking() || !hasProperItems(player)) {
                    cleanup(false);
                    return;
                }

                if (ticks < durationTicks) {
                    ticks++;
                    bossBar.setProgress((double) ticks / durationTicks);
                    if (ticks % 10 == 0) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.0f + (ticks / 40f));
                    }
                } else {
                    config.ammo.put(uuid, config.MAX_AMMO);
                    player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1.0f, 2.0f);
                    sendSubTitle(player, "Reload Complete", NamedTextColor.GREEN);
                    cleanup(true);
                }
            }

            private void cleanup(boolean success) {
                bossBar.removeAll();
                config.activeReloadBars.remove(uuid);
                config.activeReloadTasks.remove(uuid);
                if (!success) {
                    sendSubTitle(player, "Reload Canceled", NamedTextColor.RED);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                }
                cancel();
            }
        };

        task.runTaskTimer(plugin, 0L, 1L);
        config.activeReloadTasks.put(uuid, task);
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Scout.contains(player);
    }

    @Override
    protected boolean isCustomAttackUser(Player player) {
        return true;
    }

    @Override
    protected void onLSkillCooldown(PlayerInteractEvent event, Player player) {
    }

    @Override
    protected void LSkill(PlayerInteractEvent event, Player player) {
        event.setCancelled(true);
        UUID uuid = player.getUniqueId();

        if (config.activeReloadTasks.containsKey(uuid)) {
            config.activeReloadTasks.get(uuid).cancel();
            config.activeReloadTasks.remove(uuid);
            if (config.activeReloadBars.containsKey(uuid)) {
                config.activeReloadBars.get(uuid).removeAll();
                config.activeReloadBars.remove(uuid);
            }
            sendSubTitle(player, "Reload Canceled", NamedTextColor.RED);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
        }

        boolean overclocked = config.isOverclocked.getOrDefault(uuid, false);
        int currentAmmo = config.ammo.getOrDefault(uuid, config.MAX_AMMO);

        if (!overclocked) {
            if (currentAmmo <= 0) {
                player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 2.0f);
                sendSubTitle(player, "Reload Needed", NamedTextColor.RED);
                cool.setCooldown(player, 250L, "L");
                return;
            }

            currentAmmo -= 1;
            config.ammo.put(uuid, currentAmmo);

            StringBuilder ammoDisplay = new StringBuilder();
            for (int i = 0; i < config.MAX_AMMO; i++) {
                if (i < currentAmmo) ammoDisplay.append("■");
                else ammoDisplay.append("□");
            }
            sendSubTitle(player, ammoDisplay.toString(), NamedTextColor.WHITE);

        } else {
            StringBuilder ammoDisplay = new StringBuilder();
            for (int i = 0; i < config.MAX_AMMO; i++) {
                ammoDisplay.append("■");
            }
            sendSubTitle(player, ammoDisplay.toString(), NamedTextColor.AQUA);

            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 10, 0, false, false, false));
            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
        }

        cool.setCooldown(player, config.p_Skill_Cool, "L");

        World world = player.getWorld();
        Location playerLocation = player.getLocation();
        Vector direction = playerLocation.getDirection().normalize();

        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.setBaseValue(1.0);

        if (!overclocked) {
            Vector knockback = direction.clone().multiply(-0.2).setY(0.05);
            player.setVelocity(player.getVelocity().add(knockback));

            new BukkitRunnable() {
                int ticks = 0;
                float currentRecoil = 0f;
                float targetRecoil = -3.5f;

                @Override
                public void run() {
                    if (ticks > 4 || !player.isOnline() || player.isDead()) {
                        if (Math.abs(currentRecoil) > 0.01f) {
                            Location loc = player.getLocation();
                            loc.setPitch(loc.getPitch() - currentRecoil);
                            Vector velocity = player.getVelocity();
                            player.teleport(loc);
                            player.setVelocity(velocity);
                        }
                        this.cancel();
                        return;
                    }

                    float nextRecoil;
                    if (ticks < 1) nextRecoil = currentRecoil + (targetRecoil - currentRecoil) * 0.9f;
                    else {
                        targetRecoil = 0f;
                        nextRecoil = currentRecoil + (targetRecoil - currentRecoil) * 0.55f;
                    }

                    float delta = nextRecoil - currentRecoil;
                    currentRecoil = nextRecoil;

                    if (Math.abs(delta) > 0.05f) {
                        Location loc = player.getLocation();
                        loc.setPitch(loc.getPitch() + delta);
                        Vector velocity = player.getVelocity();
                        player.teleport(loc);
                        player.setVelocity(velocity);
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        Vector spawnOffset = direction.clone().multiply(0.2).add(new Vector(0, -0.3, 0));
        Location spawnLoc = player.getEyeLocation().add(spawnOffset);

        Material bulletMat = overclocked ? Material.SEA_LANTERN : Material.GOLD_BLOCK;
        BlockDisplay bulletDisplay = new bulletModel(world, spawnLoc, bulletMat.createBlockData(), 0.15f, 0.15f, 0.35f).getDisplay();

        double speed = 4.0;
        Vector velocity = direction.clone().multiply(speed);

        Sound shootSound = overclocked ? Sound.ENTITY_WARDEN_SONIC_BOOM : Sound.ENTITY_GENERIC_EXPLODE;
        float pitch = overclocked ? 1.5f : 1.8f;
        world.playSound(spawnLoc, shootSound, 1.2f, pitch);
        if (!overclocked) world.playSound(spawnLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.2f);

        world.spawnParticle(Particle.SMOKE, spawnLoc, 6, 0.1, 0.1, 0.1, 0.05);

        DamageType damageType = overclocked ? DamageType.MAGIC : DamageType.MOB_PROJECTILE;
        DamageSource source = DamageSource.builder(damageType)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build();

        Predicate<Entity> entityFilter = entity ->
                entity instanceof LivingEntity
                        && entity != player
                        && !entity.isDead()
                        && !((LivingEntity) entity).isInvulnerable()
                        && !(entity instanceof org.bukkit.entity.ArmorStand)
                        && !entity.hasMetadata("NPC");

        new BukkitRunnable() {
            int life = 60;
            Location currentLoc = spawnLoc.clone();

            @Override
            public void run() {
                if (!bulletDisplay.isValid()) {
                    this.cancel();
                    return;
                }

                if (life-- <= 0) {
                    bulletDisplay.remove();
                    this.cancel();
                    return;
                }

                Particle trail = overclocked ? Particle.SOUL_FIRE_FLAME : Particle.CRIT;
                world.spawnParticle(trail, currentLoc, 1, 0, 0, 0, 0);

                RayTraceResult hitResult = world.rayTrace(currentLoc, direction, speed, FluidCollisionMode.NEVER, true, 0.2, entityFilter);

                if (hitResult != null) {
                    Location hitLoc = hitResult.getHitPosition().toLocation(world);

                    if (hitResult.getHitEntity() != null) {
                        LivingEntity target = (LivingEntity) hitResult.getHitEntity();

                        boolean isHeadshot = hitResult.getHitPosition().getY() >= (target.getEyeLocation().getY() - 0.25);
                        double finalDamage = config.p_Skill_damage;

                        if (isHeadshot) {
                            finalDamage *= 2.0;
                            world.playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 1.0f);
                            world.spawnParticle(Particle.CRIT, hitLoc, 15, 0.2, 0.2, 0.2, 0.1);
                        }

                        ForceDamage forceDamage = new ForceDamage(target, finalDamage, source, false);
                        forceDamage.applyEffect(player);

                        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false, false));
                        world.playSound(hitLoc, Sound.ITEM_TRIDENT_HIT, 1.3f, 0.7f); // 푹 뚫고 지나가는 기본 타격음

                        Material hitMat = overclocked ? Material.DIAMOND_BLOCK : Material.GOLD_BLOCK;
                        world.spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1.0, 0), 20, 0.3, 0.3, 0.3, hitMat.createBlockData());
                    }
                    else if (hitResult.getHitBlock() != null) {
                        world.playSound(hitLoc, Sound.BLOCK_STONE_BREAK, 1.0f, 1.5f);

                        Material hitMat = overclocked ? Material.SEA_LANTERN : Material.GOLD_BLOCK;
                        world.spawnParticle(Particle.BLOCK, hitLoc, 15, 0.1, 0.1, 0.1, hitMat.createBlockData());
                    }

                    bulletDisplay.remove();
                    this.cancel();
                    return;
                }

                currentLoc.add(velocity);
                bulletDisplay.teleport(currentLoc);
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
        return main.getType() == Material.NETHERITE_HORSE_ARMOR && off.getType() == Material.AIR;
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
        return droppedItem.getType() == Material.NETHERITE_HORSE_ARMOR && off.getType() == Material.AIR;
    }

    @Override
    protected boolean isRCondition(Player player) { return canUseRSkill(player); }
    @Override
    protected boolean isQCondition(Player player) { return canUseQSkill(player); }
    @Override
    protected boolean isFCondition(Player player) { return canUseFSkill(player); }

    @Override
    protected boolean isRAnimated(Player player) { return false; }
    @Override
    protected boolean isFAnimated(Player player) { return false; }

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
                return config.p_Skill_Cool;
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