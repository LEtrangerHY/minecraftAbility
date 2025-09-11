package org.core.coreProgram.Cores.Dagger.coreSystem;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.core.Cool.Cool;
import org.core.Core;
import org.core.Effect.ForceDamage;
import org.core.coreConfig;
import org.core.coreProgram.AbsCoreSystem.ConfigWrapper;
import org.core.coreProgram.AbsCoreSystem.SkillBase;
import org.core.coreProgram.AbsCoreSystem.absCore;
import org.core.coreProgram.Cores.Dagger.Passive.DamageStroke;
import org.core.coreProgram.Cores.Dagger.Skill.F;
import org.core.coreProgram.Cores.Dagger.Skill.Q;
import org.core.coreProgram.Cores.Dagger.Skill.R;

import java.util.HashSet;
import java.util.LinkedHashSet;

import static org.bukkit.Bukkit.getLogger;


public class dagCore extends absCore {
    private final Core plugin;
    private final Dagger config;

    private final DamageStroke damagestroker;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;

    public dagCore(Core plugin, coreConfig tag, Dagger config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.damagestroker = new DamageStroke(tag, config, plugin, cool);

        this.Rskill = new R(config, plugin, cool, damagestroker);
        this.Qskill = new Q(config, plugin, cool, damagestroker);
        this.Fskill = new F(config, plugin, cool, damagestroker);

        getLogger().info("Dagger downloaded...");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void passiveAttackEffect(PlayerInteractEvent event) {
        if(tag.Dagger.contains(event.getPlayer())){
            if (skillUsing.contains(event.getPlayer().getUniqueId())) {
                skillUsing.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler
    public void passiveDamage(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        ItemStack itemInOffHand = player.getInventory().getItemInOffHand();

        if(tag.Dagger.contains(player) && itemInMainHand.getType() == Material.ECHO_SHARD && itemInOffHand.getType() == Material.AIR){
            if(!config.r_damaged.getOrDefault(player.getUniqueId(), false) && !config.f_using.getOrDefault(player.getUniqueId(), false) && !config.f_damaging.getOrDefault(player.getUniqueId(), false)) {

                Vector direction = player.getEyeLocation().add(0, -0.5, 0).getDirection().normalize();
                Location particleLocation = player.getEyeLocation().clone()
                        .add(direction.clone().multiply(2.6));

                player.spawnParticle(Particle.SWEEP_ATTACK, particleLocation, 1, 0, 0, 0, 0);

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
                event.setDamage(4.0);

            }
        }
    }

    @EventHandler
    public void fskillEffect(PlayerInteractEvent event){

        Player player = event.getPlayer();
        Action action = event.getAction();

        if(!cool.isReloading(player, "X_slash") && config.f_using.getOrDefault(player.getUniqueId(), false) && config.f_slash.getOrDefault(player.getUniqueId(), 0) < 2) {

            World world = player.getWorld();

            Location playerLocation = player.getLocation();
            Vector direction = playerLocation.getDirection().normalize().multiply(1.3);


            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {

                if(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK){
                    return;
                }

                if(!hasProperItems(player)){
                    return;
                }

                config.f_damaged.put(player.getUniqueId(), new HashSet<>());

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
                event.setCancelled(true);

                new BukkitRunnable() {
                    int ticks = 0;

                    @Override
                    public void run() {
                        if (ticks >= 10 || !config.f_using.getOrDefault(player.getUniqueId(), false)) {
                            config.f_slash.put(player.getUniqueId(), config.f_slash.getOrDefault(player.getUniqueId(), 0) + 1);
                            this.cancel();
                            return;
                        }

                        Location particleLocation = playerLocation.clone()
                                .add(direction.clone().multiply(ticks * 1.8))
                                .add(0, 1.5, 0);

                        world.spawnParticle(Particle.SWEEP_ATTACK, particleLocation, 1, 0, 0, 0, 0);
                        player.spawnParticle(Particle.SMOKE, particleLocation, 1, 0, 0, 0, 0);

                        for (Entity entity : world.getNearbyEntities(particleLocation, 0.6, 0.6, 0.6)) {
                            if (entity instanceof LivingEntity target
                                    && entity != player
                                    && !config.f_damaged.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(entity)) {

                                config.f_damaging.put(player.getUniqueId(), true);
                                ForceDamage forceDamage = new ForceDamage(target, config.f_Skill_Damage);
                                target.setVelocity(new Vector(0, 0, 0));
                                forceDamage.applyEffect(player);
                                config.f_damaging.remove(player.getUniqueId());

                                config.f_damaged.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(target);

                                config.dash_object.computeIfAbsent(player.getUniqueId(), k -> new LinkedHashSet<>()).add(target);
                            }
                        }

                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }

            cool.setCooldown(player, 400L, "X_slash");
        }
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Dagger.contains(player);
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
        return main.getType() == Material.ECHO_SHARD && off.getType() == Material.AIR;
    }

    private boolean canUseRSkill(Player player) { return true; }

    private boolean canUseQSkill(Player player) { return true; }

    private boolean canUseFSkill(Player player) { return true; }

    @Override
    protected boolean isItemRequired(Player player){
        return hasProperItems(player);
    }

    @Override
    protected boolean isRCondition(Player player) {
        return canUseRSkill(player);
    }

    @Override
    protected boolean isQCondition(Player player, ItemStack droppedItem) {
        ItemStack off = player.getInventory().getItemInOffHand();
        return droppedItem.getType() == Material.ECHO_SHARD &&
                off.getType() == Material.AIR &&
                canUseQSkill(player);
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