package org.core.coreSystem.cores.VOL6.Jester.coreSystem;

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

public class jestInventory extends absInventory {

    private final Core plugin;

    public jestInventory(Core plugin, coreConfig config) {
        super(config);

        this.plugin = plugin;
    }

    @Override
    protected Plugin getPlugin() {
        return this.plugin;
    }

    @Override
    protected boolean contains(Player player) {
        return tag.Jester.contains(player);
    }

    @Override
    protected Material getMainTotem(Player player) {
        return Material.EGG;
    }

    @Override
    protected Component getName(Player player, String skill) {

        return switch (skill) {
            case "main" -> Component.text("JESTER");
            case "R" -> Component.text("Surprise");
            case "Q" -> Component.text("JackInTheBox");
            case "F" -> Component.text("FAINT");
            default -> Component.text("???");
        };
    }

    @Override
    protected Material getTotem(Player player, String skill) {
        return switch (skill) {
            case "R" -> Material.FIREWORK_ROCKET;
            case "Q" -> Material.TRAPPED_CHEST;
            case "F" -> Material.WIND_CHARGE;
            default -> Material.BARRIER;
        };
    }

    @Override
    protected List<Component> getTotemLore(Player player, String skill) {

        List<Component> lore = new ArrayList<>();

        if (skill.equals("main")) {
            lore.add(Component.text("------------").color(NamedTextColor.WHITE));
            lore.add(Component.text("타입 : 브루저").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("장착 : ???").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("------------").color(NamedTextColor.WHITE));
            lore.add(Component.text(""));
            lore.add(Component.text("메뉴북 아이템 없어도 인벤토리 화면에서 egg을 우클릭해서 메뉴 화면 진입 가능").color(NamedTextColor.AQUA));
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
                lore.add(Component.text("시스템 : -").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("-").color(NamedTextColor.GREEN));
                break;
            case "Q":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfQ.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 공격").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 복합적").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("달걀을 투척하여 달걀이 오브젝트에 피격 시 최대 30초(아이템마다 6초 할당) 동안 트릭쇼 상태가 된다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("효과 발동 종료 시 해당 아이템에 할당된 6초 간격이 강제 스킵된 후 다음 아이템으로 교체된다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("메인 핸드에 할당된 아이템에 따라 다음과 같은 효과를 활용할 수 있다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("1. 삼지창").color(NamedTextColor.GREEN));
                lore.add(Component.text("전방으로 돌진한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("돌진 후 자기 주변 반경 4칸 내에 적이 없을 시 실패하고 해당 아이템 간격이 강제 스킵된다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("돌진 후 자신과 가장 가까운 다른 대상들에게 돌진하며 피해를 가한다. 이를 6번 반복한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("위의 해당 효과가 한번 반복될때마다 범위 내에 다른 대상이 없다면 같은 대상에게 효과를 적용한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("2. 폭죽").color(NamedTextColor.GREEN));
                lore.add(Component.text("주변을 폭파시키며 위로 도약한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("지면에 착지하기 전까지 최대 3번까지 0.6초의 쿨타임 간격으로 반복 사용할 수 있다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("3번 연속으로 위의 효과 발동 후 시전 시 전방으로 폭죽을 투사한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("3. 방패").color(NamedTextColor.GREEN));
                lore.add(Component.text("해당 아이템이 할당되자마자 효과가 발동한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("3초간 집중 상태가 되며, 공격 받을 시 적의 공격을 패링한 후 자신의 최대 체력의 16% 만큼의 피해를 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("집중 상태에서 시전 시 전방으로 벽에 피격 시 최대 6번 반사되고 적들에게는 관통하여 피해를 가하는 방패를 투척한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("4. 방망이").color(NamedTextColor.GREEN));
                lore.add(Component.text("차징 : 쉬프트를 통해 최대 1초까지 차징할 수 있다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("차징 0.6초 미만 : 전방을 방망이로 내리찍어 피격당한 대상에게 피해를 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("차징 0.6초 이상 : 전방 180도를 방망이로 휩쓸어 피격당한 대상에게 피해를 가하고 밀쳐낸다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("두 타입의 효과 모두 최대 6번까지 0.6초의 쿨타임 간격으로 반복 사용할 수 있다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("5. 체인 크라운").color(NamedTextColor.GREEN));
                lore.add(Component.text("자신의 반경 6칸 내의 체력이 22% 미만인 적들을 최대 5명까지 처형한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("체력이 22% 이상인 적들은 2초간 고정시키고 남은 체력의 33% 만큼의 피해를 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("처형시킨 대상의 수만큼 달걀 피격을 통한 트릭쇼 재시전 쿨타임이 4초씩 감소한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("처형 시킨 대상의 수가 한명도 존재하지 않는다면, 본인은 남은 체력의 33% 만큼의 피해를 받고 4초간 기절 상태에 빠진다.").color(NamedTextColor.GREEN));
                break;
            case "F":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfF.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 공격").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : -").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("기본 : 전방으로 돌진 후 1.2초간 이동속도가 30% 저하되며 무적 상태가 된다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("트릭쇼 상태 : 효과 진행 중이 아닐 경우 메인 핸드의 아이템을 다음 순서의 트릭쇼 아이템으로 전환시킨다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("트릭쇼 상태 : 교체 후 다음 순서의 아이템이 교체된 기존의 트릭쇼 아이템으로 고정된다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("트릭쇼 상태 : 다음 순서의 아이템이 체인 크라운인 경우 효과 발동이 불가능하다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("트릭쇼 상태 : 할당된 아이템이 체인 크라운이라면 반경 6칸 내의 적들을 2초간 어둠 상태가 되게 하고 피해를 가한 뒤 트릭쇼를 강제 종료시킨다.").color(NamedTextColor.GREEN));
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
        long hpBonus = 0;
        switch (skill) {
            case "R": requireExpList = requireExpOfR; hpBonus = 3; break;
            case "Q": requireExpList = requireExpOfQ; break;
            case "F": requireExpList = requireExpOfF; break;
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