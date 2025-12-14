package org.core.coreProgram.Cores.VOL1.Bloom.coreSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Main.Core;
import org.core.Effect.ForceDamage;
import org.core.Main.coreConfig;
import org.core.coreProgram.AbsCoreSystem.ConfigWrapper;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.AbsCoreSystem.absCore;
import org.core.coreProgram.Cores.VOL1.Bloom.Skill.F;
import org.core.coreProgram.Cores.VOL1.Bloom.Skill.Q;
import org.core.coreProgram.Cores.VOL1.Bloom.Skill.R;

public class bloomCore extends absCore {

    private final Core plugin;
    private final Bloom config;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public bloomCore(Core plugin, coreConfig tag, Bloom config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.Rskill = new R(config, plugin, cool);
        this.Qskill = new Q(config, plugin, cool);
        this.Fskill = new F(config, plugin, cool);

        plugin.getLogger().info("Bloom downloaded...");
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void passiveAttackEffect(PlayerInteractEvent event) {

        if(tag.Bloom.contains(event.getPlayer()) && hasProperItems(event.getPlayer())) {
            if (!pAttackUsing.contains(event.getPlayer().getUniqueId())) {

                Player player = event.getPlayer();

                if (hasProperItems(player)) {
                    if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {

                        if (cool.isReloading(player, "blossom")) {
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SPORE_BLOSSOM_BREAK, 1.7f, 1.0f);
                            return;
                        }

                        cool.setCooldown(player, 2700L, "blossom");

                        World world = player.getWorld();

                        player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(1 / 2.7);
                        world.playSound(player.getLocation(), Sound.BLOCK_GRASS_PLACE, 1.7f, 1.0f);
                        world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.7f, 1.0f);

                        DamageSource source = DamageSource.builder(DamageType.MAGIC)
                                .withCausingEntity(player)
                                .withDirectEntity(player)
                                .build();

                        player.heal(2.7);

                        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(77, 255, 77), 0.7f);

                        new BukkitRunnable() {
                            int ticks = 0;

                            @Override
                            public void run() {

                                if (ticks >= 27) {
                                    config.collision.remove(player.getUniqueId());
                                    this.cancel();
                                    return;
                                }

                                Location playerLocation = player.getLocation();
                                Vector direction = playerLocation.getDirection().normalize().multiply(1.2);

                                Location particleLocation = playerLocation.clone()
                                        .add(direction.clone().multiply(ticks * 1.1))
                                        .add(0, 1.4, 0);

                                world.spawnParticle(Particle.CHERRY_LEAVES, particleLocation, 7, 0.7, 0.7, 0.7, 0);
                                world.spawnParticle(Particle.DUST, particleLocation, 5, 0.5, 0.5, 0.5, 0, dustOptions);

                                for (Entity entity : world.getNearbyEntities(particleLocation, 1.7, 1.7, 1.7)) {
                                    if (entity instanceof LivingEntity target && entity != player) {

                                        ForceDamage forceDamage = new ForceDamage(target, 1.7, source);
                                        forceDamage.applyEffect(player);
                                    }
                                }

                                ticks++;
                            }
                        }.runTaskTimer(plugin, 0L, 2L);

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
        return tag.Bloom.contains(player);
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
        return main.getType() == Material.PINK_PETALS && off.getType() == Material.AIR;
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
        return droppedItem.getType() == Material.PINK_PETALS &&
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
