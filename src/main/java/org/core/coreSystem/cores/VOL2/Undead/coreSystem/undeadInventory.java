package org.core.coreSystem.cores.VOL2.Undead.coreSystem;

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

public class undeadInventory extends absInventory {

    private final Core plugin;

    public undeadInventory(Core plugin, coreConfig config) {
        super(config);

        this.plugin = plugin;
    }

    @Override
    protected Plugin getPlugin() {
        return this.plugin;
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Undead.contains(player);
    }

    @Override
    protected Material getMainTotem(Player player) {
        return Material.IRON_AXE;
    }

    @Override
    protected Component getName(Player player, String skill) {

        return switch (skill) {
            case "main" -> Component.text("");
            case "R" -> Component.text("DeadlyWeapons");
            case "Q" -> Component.text("LethalAssure");
            case "F" -> Component.text("RESTORATION");
            default -> Component.text("???");
        };
    }

    @Override
    protected Material getTotem(Player player, String skill) {
        return switch (skill) {
            case "R" -> Material.STICK;
            case "Q" -> Material.IRON_INGOT;
            case "F" -> Material.CRIMSON_HYPHAE;
            default -> Material.BARRIER;
        };
    }

    @Override
    protected List<Component> getTotemLore(Player player, String skill) {

        List<Component> lore = new ArrayList<>();

        if (skill.equals("main")) {
            lore.add(Component.text("------------").color(NamedTextColor.WHITE));
            lore.add(Component.text("타입 : 브루저").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("장착 : 검을 제외한 모든 철제 도구류 아이템, 메인핸드, 오프핸드 제한 없음").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("------------").color(NamedTextColor.WHITE));
            lore.add(Component.text(""));
            lore.add(Component.text("메뉴북 아이템 없어도 인벤토리 화면에서 철제 도구류 아이템을 우클릭해서 메뉴 화면 진입 가능").color(NamedTextColor.AQUA));
            return lore;
        }

        long level = getSkillLevel(player, skill);
        long playerLevel = player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "level"), PersistentDataType.LONG, 0L);

        long maxLevel = switch ((int) playerLevel){
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
                lore.add(Component.text("타입 : 공격").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 복합적").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("시전하기 전 메인핸드에 장착된 아이템을 기준으로 개별적으로 쿨타임이 작동한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("1. 삽").color(NamedTextColor.GREEN));
                lore.add(Component.text("전방으로 돌진하며 적들을 밀쳐내며 피해를 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("재사용 : 전방으로 삽을 휘두른다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("피격 : 피격당한 적의 메인 핸드의 아이템을 무장해제 시킨다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("2. 곡괭이").color(NamedTextColor.GREEN));
                lore.add(Component.text("전방으로 돌진한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("재사용 : 전방을 곡괭이로 내리찍어 피격당한 대상에게 피해를 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("피격 : 대상의 방어구중 무작위 1개를 무장해재 시킨다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("3. 도끼").color(NamedTextColor.GREEN));
                lore.add(Component.text("전방으로 돌진하며 처음 충돌한 적의 이동속도를 90% 감소시키고 피해를 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("재사용 : 전방을 도끼로 내리찍어 피격당한 대상에게 피해를 가하고 2초간 기절시킨다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("4. 낫").color(NamedTextColor.GREEN));
                lore.add(Component.text("회전베기를 시전하며 후방으로 돌진한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("재사용 : 회전베기를 시전하며 전방으로 돌진한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("피격 : 피격 당한 적에게 입힌 피해의 40% 만큼 체력을 회복한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("5. 피스톨").color(NamedTextColor.GREEN));
                lore.add(Component.text("코스트 : 전체 체력의 13%").color(NamedTextColor.GREEN));
                lore.add(Component.text("전방으로 남은 체력의 25%의 피해를 가하고 2초간 기절시키는 탄환을 발사한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("탄환에 피격된 적의 남은 체력에 비례해 일정 시간동안 이동속도가 증가한다.").color(NamedTextColor.GREEN));
                break;
            case "Q":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfQ.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 공격").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : -").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("전방으로 양손에 든 무기를 통해 교차 참격을 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("양 손에 든 무기를 교체한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("시전하기 전 오프핸드에 장착된 아이템을 기준으로 개별적으로 쿨타임이 작동한다.").color(NamedTextColor.GREEN));
                break;
            case "F":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfF.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 공격").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 차징").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("차징 : 회복할 체력 수치(0~100%)를 결정한다. 차징을 할수록 수치가 상승한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("차징 : 차징하는 동안 입는 피해량이 차징 시간에 따라 16~64% 감소한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("스킬 시전 시 차징된 수치만큼 체력을 회복한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("회복 후 차징된 수치만큼 일정 시간동안 본인이 입는 피해량이 대폭 상승하지만, 이에 따라 공격력과 이동속도 또한 상승한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("입는 피해량 상승 효과는 합법칙으로 중첩된다. 최대 444% 까지 누적될 수 있으며 매초 2% 씩 감소한다.").color(NamedTextColor.GREEN));
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

    public List<Long> requireExpOfR = List.of(44L, 66L, 166L, 244L, 366L, 644L);
    public List<Long> requireExpOfQ = List.of(44L, 66L, 166L, 244L, 366L, 644L);
    public List<Long> requireExpOfF = List.of(44L, 66L, 166L, 244L, 366L, 644L);

    @Override
    protected void reinforceSkill(Player player, String skill, Long skillLevel, Inventory customInv) {
        if (skillLevel >= 6 || !contains(player)) return;

        long level = player.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "level"), PersistentDataType.LONG, 0L);

        if (skillLevel == 3 && level < 6L){
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.5f, 1);
            player.sendMessage(
                    Component.text("승급 필요 : CORE LEVEL -> 6")
                            .color(NamedTextColor.RED)
            );
            return;
        }

        if (skillLevel == 5 && level < 10L){
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
        switch (skill) {
            case "R": requireExpList = requireExpOfR; break;
            case "Q": requireExpList = requireExpOfQ; break;
            case "F": requireExpList = requireExpOfF; applyAdditionalHealth(player, 2); break;
            default: return;
        }

        int requiredExp = Math.toIntExact(requireExpList.get(Math.toIntExact(skillLevel)));
        int totalExp = player.getTotalExperience();

        if (totalExp >= requiredExp) {
            deductExp(player, requiredExp);

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
            player.setExp(remainingExp / (float)getExpToNextLevel(level));
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