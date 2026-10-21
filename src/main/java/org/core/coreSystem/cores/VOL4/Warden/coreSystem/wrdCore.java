package org.core.coreSystem.cores.VOL4.Warden.coreSystem;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.core.cool.Cool;
import org.core.coreSystem.absCoreSystem.ConfigWrapper;
import org.core.coreSystem.absCoreSystem.SkillBase;
import org.core.coreSystem.absCoreSystem.absCore;
import org.core.main.coreConfig;

public class wrdCore extends absCore {
    public wrdCore(coreConfig tag, Cool cool) {
        super(tag, cool);
    }

    @Override
    protected boolean contains(Player player) {
        return false;
    }

    @Override
    protected boolean isCustomAttackUser(Player player) {
        return false;
    }

    @Override
    protected void LSkill(PlayerInteractEvent event, Player player) {

    }

    @Override
    protected SkillBase getRSkill() {
        return null;
    }

    @Override
    protected SkillBase getQSkill() {
        return null;
    }

    @Override
    protected SkillBase getFSkill() {
        return null;
    }

    @Override
    protected boolean isItemRequired(Player player) {
        return false;
    }

    @Override
    protected boolean isDropRequired(Player player, ItemStack droppedItem) {
        return false;
    }

    @Override
    protected boolean isRCondition(Player player) {
        return false;
    }

    @Override
    protected boolean isQCondition(Player player) {
        return false;
    }

    @Override
    protected boolean isFCondition(Player player) {
        return false;
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
        return null;
    }
}
