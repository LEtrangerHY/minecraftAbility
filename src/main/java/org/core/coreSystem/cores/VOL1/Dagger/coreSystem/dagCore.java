package org.core.coreSystem.cores.VOL1.Dagger.coreSystem;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.core.cool.Cool;
import org.core.main.Core;
import org.core.main.coreConfig;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.coreSystem.cores.VOL1.Dagger.Passive.basicAttack;
import org.core.coreSystem.cores.VOL1.Dagger.Passive.bloodStroke;
import org.core.coreSystem.cores.VOL1.Dagger.Skill.F;
import org.core.coreSystem.cores.VOL1.Dagger.Skill.Q;
import org.core.coreSystem.cores.VOL1.Dagger.Skill.R;
import org.core.effect.crowdControl.Stun;
import org.core.effect.effectPlugin.swingEchoGuard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class dagCore extends absCore {
    private final Core plugin;
    private final Dagger config;

    private final bloodStroke bloodStroker;

    private final R Rskill;
    private final Q Qskill;
    private final F Fskill;
    private final basicAttack attack;
    private final Map<UUID, Integer> lastUseTick = new HashMap<>();

    public dagCore(Core plugin, coreConfig tag, Dagger config, Cool cool) {
        super(tag, cool);

        this.plugin = plugin;
        this.config = config;

        this.bloodStroker = new bloodStroke(tag, config, plugin, cool);

        this.Rskill = new R(config, plugin, cool, bloodStroker);
        this.Qskill = new Q(config, plugin, cool, bloodStroker);
        this.attack = new basicAttack(config, plugin);
        this.Fskill = new F(config, plugin, cool, bloodStroker, attack);

        plugin.getLogger().info("Dagger downloaded...");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onVanillaAttack(PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        if (!contains(player) || !hasProperItems(player)) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUseInput(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!contains(player)) return;

        lastUseTick.put(player.getUniqueId(), Bukkit.getCurrentTick());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAttackSwing(PlayerArmSwingEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!contains(player) || !hasProperItems(player) || Stun.isStunned(player)) return;
        if (config.f_using.getOrDefault(player.getUniqueId(), false)) return;
        if (swingEchoGuard.isEchoPending(player)) return;

        Integer used = lastUseTick.get(player.getUniqueId());
        if (used != null && Bukkit.getCurrentTick() - used <= 1) return;

        if (cool.isReloading(player, "L")) return;

        cool.setCooldown(player, config.basic_Attack_Cool, "L");
        attack.attack(player);
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Dagger.contains(player);
    }

    @Override
    protected boolean isCustomAttackUser(Player player) {
        return config.f_using.getOrDefault(player.getUniqueId(), false) && config.f_slash.getOrDefault(player.getUniqueId(), 0) < 2;
    }

    @Override
    protected void onLSkillCooldown(PlayerInteractEvent event, Player player) {}

    @Override
    protected void LSkill(PlayerInteractEvent event, Player player) {
        event.setCancelled(true);
        Fskill.wave(player);
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
        return main.getType() == Material.ECHO_SHARD && (off.getType() == Material.AIR || off.getType() == Material.ECHO_SHARD);
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
        return droppedItem.getType() == Material.ECHO_SHARD &&
                (off.getType() == Material.AIR || off.getType() == Material.ECHO_SHARD);
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
            public long getLcooldown(Player player) {
                return 400L;
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