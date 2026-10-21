package org.core.coreSystem.cores.VOL5.Scout.coreSystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.core.coreSystem.absInventorySystem.InventoryWrapper;
import org.core.coreSystem.absInventorySystem.absInventory;
import org.core.main.Core;
import org.core.main.coreConfig;

import java.util.ArrayList;
import java.util.List;

public class sctInventory extends absInventory {

    private final Core plugin;

    public sctInventory(Core plugin, coreConfig config) {
        super(config);
        this.plugin = plugin;
    }

    @Override
    protected Plugin getPlugin() {
        return this.plugin;
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Scout.contains(player);
    }

    @Override
    protected Material getMainTotem(Player player) {
        return Material.NETHERITE_HORSE_ARMOR;
    }

    @Override
    protected Component getName(Player player, String skill) {
        return switch (skill) {
            case "main" -> Component.text("Scout");
            case "R" -> Component.text("RemoteBomb");
            case "Q" -> Component.text("PhaseDash");
            case "F" -> Component.text("OVERCLOCK");
            default -> Component.text("???");
        };
    }

    @Override
    protected Material getTotem(Player player, String skill) {
        return switch (skill) {
            case "R" -> Material.OBSERVER;
            case "Q" -> Material.ENDER_PEARL;
            case "F" -> Material.BEACON;
            default -> Material.BARRIER;
        };
    }

    @Override
    protected List<Component> getTotemLore(Player player, String skill) {
        List<Component> lore = new ArrayList<>();

        if (skill.equals("main")) {
            lore.add(Component.text("------------").color(NamedTextColor.WHITE));
            lore.add(Component.text("타입 : 원거리 딜러").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("장착 : 메인핸드에 네더라이트 말갑옷 장착, 오프핸드는 장착 금지.").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("------------").color(NamedTextColor.WHITE));
            lore.add(Component.text(""));
            lore.add(Component.text("Shift로 재장전을 수행할 수 있다.").color(NamedTextColor.AQUA));
            lore.add(Component.text("메뉴북 아이템 없어도 인벤토리 화면에서 무기 우클릭해서 메뉴 화면 진입 가능").color(NamedTextColor.AQUA));
            return lore;
        }

        long level = getSkillLevel(player, skill);
        long playerLevel = player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "level"), PersistentDataType.LONG, 0L);

        long maxLevel = switch ((int) playerLevel) {
            case 6, 7, 8, 9 -> 5;
            case 10 -> 6;
            default -> 3;
        };

        lore.add(Component.text("Lv." + level + "/" + maxLevel).color(NamedTextColor.YELLOW));

        Component requireXp;

        switch (skill) {
            case "R":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfR.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 공격 및 유틸").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 투척 및 원격 기폭").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 자신 및 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("투척 : 전방으로 지형에 달라붙는 전술 폭약을 던진다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("기폭 : 폭탄이 필드에 있을 때 재사용하면 폭탄을 폭발시킨다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("폭발에 휩쓸린 적에게는 피해와 발광을 부여하고 밀쳐낸다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("자신이 폭발에 휩쓸릴 경우 피해 없이 넉백을 한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("6초 내에 기폭하지 않으면 자동 기폭되며, 이때 휩쓸리면 절반의 피해를 입는다.").color(NamedTextColor.GREEN));
                break;
            case "Q":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfQ.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 효과").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 잔류").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 자신 및 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("전방으로 3초간 유지되는 잔상을 남기며 돌진한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("적군이 잔상에 닿을 시 지속적으로 구속과 발광 효과를 받는다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("본인이 잔상 위에 머무를 경우 지속적으로 체력을 회복한다.").color(NamedTextColor.GREEN));
                break;
            case "F":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfF.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 자가 버프").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 강화").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 자신").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("기본 : 10초 동안 무기의 성능을 극대화하는 오버클럭 상태에 돌입한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("유지 시간 동안 재장전이 필요 없고 무한 탄창이 적용된다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("평타가 마법 피해 판정의 무반동 플라즈마 탄환으로 변경된다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("평타를 사격할 때마다 자신에게 0.5초의 짧은 투명화가 발동하여 타겟팅을 어지럽힌다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("지속 시간 동안 위상 도약 스킬의 쿨타임이 2초로 고정된다.").color(NamedTextColor.GREEN));
                break;
            default:
                break;
        }

        lore.add(Component.text("------------").color(NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(Component.text("우클릭을 통해 강화").color(NamedTextColor.AQUA));

        return lore;
    }

    @Override
    protected Long getSkillLevel(Player player, String skill) {
        if (skill.equals("main")) return 0L;
        return player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, skill), PersistentDataType.LONG, 0L);
    }

    public List<Long> requireExpOfR = List.of(22L, 77L, 170L, 257L, 307L, 617L);
    public List<Long> requireExpOfQ = List.of(22L, 77L, 170L, 257L, 307L, 617L);
    public List<Long> requireExpOfF = List.of(22L, 77L, 170L, 257L, 307L, 617L);

    @Override
    protected void reinforceSkill(Player player, String skill, Long skillLevel, Inventory customInv) {
        if (skillLevel >= 6 || !contains(player)) return;

        long level = player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "level"), PersistentDataType.LONG, 0L);

        if (skillLevel == 3 && level < 6L) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.5f, 1);
            player.sendMessage(
                    Component.text("승급 필요 : CORE LEVEL -> 6")
                            .color(NamedTextColor.RED)
            );
            return;
        }

        if (skillLevel == 5 && level < 10L) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.5f, 1);
            player.sendMessage(
                    Component.text("승급 필요 : CORE LEVEL -> 10")
                            .color(NamedTextColor.RED)
            );
            return;
        }

        long current = player.getPersistentDataContainer()
                .getOrDefault(new NamespacedKey(plugin, skill), PersistentDataType.LONG, 0L);

        List<Long> requireExpList;
        long hpBonus = 0;
        switch (skill) {
            case "R":
                requireExpList = requireExpOfR;
                // R은 체력 보너스 없음
                break;
            case "Q":
                requireExpList = requireExpOfQ;
                hpBonus = 1; // Q 업그레이드 시 체력 +1
                break;
            case "F":
                requireExpList = requireExpOfF;
                hpBonus = 2; // F 업그레이드 시 체력 +2
                break;
            default: return;
        }

        int requiredExp = Math.toIntExact(requireExpList.get(Math.toIntExact(skillLevel)));
        int totalExp = player.getTotalExperience();

        if (totalExp >= requiredExp) {
            deductExp(player, requiredExp);
            if (hpBonus > 0) applyAdditionalHealth(player, hpBonus);

            player.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, skill),
                    PersistentDataType.LONG,
                    current + 1
            );

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 1);
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.5f, 1);
            customInvReroll(player, customInv);
            player.sendMessage(
                    Component.text("스킬 레벨업 성공!")
                            .color(NamedTextColor.GREEN)
            );
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.5f, 1);
            player.sendMessage(
                    Component.text("경험치(Minecraft EXP) 부족 " + requiredExp + "Exp 필요")
                            .color(NamedTextColor.RED)
            );
        }
    }

    private void applyAdditionalHealth(Player player, long addHP) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double current = maxHealth.getBaseValue();
            double newMax = current + addHP;
            maxHealth.setBaseValue(newMax);
        }
    }

    private void deductExp(Player player, int expToDeduct) {
        int newTotalExp = player.getTotalExperience() - expToDeduct;
        if (newTotalExp < 0) newTotalExp = 0;
        player.setTotalExperience(newTotalExp);

        int level = 0;
        int remainingExp = newTotalExp;
        while (remainingExp >= getExpToNextLevel(level)) {
            remainingExp -= getExpToNextLevel(level);
            level++;
        }

        player.setLevel(level);
        if (level < 1000) {
            player.setExp(remainingExp / (float) getExpToNextLevel(level));
        } else {
            player.setExp(0);
        }
    }

    private int getExpToNextLevel(int level) {
        if (level >= 0 && level <= 15) return 2 * level + 7;
        else if (level >= 16 && level <= 30) return 5 * level - 38;
        else return 9 * level - 158;
    }

    @Override
    protected InventoryWrapper getInventoryWrapper() {
        return new InventoryWrapper() {
        };
    }
}