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
            case "main" -> Component.text("UNDEAD");
            case "R" -> Component.text("DeadlyWeapons");
            case "Q" -> Component.text("FlatLine");
            case "F" -> Component.text("REVENANT");
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
            lore.add(Component.text("장착 : 한 손에 철제 도구류(검, 도끼, 곡괭이, 삽, 괭이) 중 1개, 다른 손에 철 말갑옷 장착.").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("메인핸드가 도구면 냉병기 모드, 철 말갑옷이면 화기 모드.").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("------------").color(NamedTextColor.WHITE));
            lore.add(Component.text("패시브 : 배고픔이 항상 0으로 유지되고 허기 효과가 사라지지 않지만, 굶주림 피해를 받지 않는다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("핸드 스왑 : R, Q, F 중 하나가 핸드 스왑 커맨드로 지정되며, 액션바에 붉은색으로 표시된다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("핸드 스왑 커맨드를 사용하면 해당 스킬이 발동하면서 양손의 아이템이 교체되어 냉병기 모드와 화기 모드가 전환된다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("나머지 두 스킬이 모두 쿨타임일 때 핸드 스왑 커맨드를 사용하면 그 두 스킬의 쿨타임이 초기화된다. (조건 충족 시 굵게 표시)").color(NamedTextColor.GREEN));
            lore.add(Component.text("쿨타임이 초기화되었을 때에만 직전과 다른 스킬이 새 핸드 스왑 커맨드로 무작위 지정된다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("스킬 쿨타임은 냉병기 모드와 화기 모드가 공유한다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("일반 공격 : 냉병기 모드에서는 도구 종류에 따른 근접 공격을 가하고, 화기 모드에서는 전방으로 탄환을 발사한다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("근접 공격은 적중한 적 1명당 최대 체력의 4%를 회복한다. (최대 16%)").color(NamedTextColor.GREEN));
            lore.add(Component.text("탄환은 최대 체력의 6%를 소모하며, 적을 최대 4명까지 관통한다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("회복 : 달리기 키를 누르고 있으면 최대 3초간 충전되며, 충전 중 받는 피해가 16% ~ 64% 감소한다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("키를 떼면 충전량만큼 최대 체력 비율로 회복하며 바라보는 방향으로 대시하고, 회복하는 동안 무적이 된다.").color(NamedTextColor.GREEN));
            lore.add(Component.text("회복 시 충전량만큼 주는 피해와 이동 속도가 증가하지만, 받는 피해 증가 디버프가 누적된다. (최대 400%)").color(NamedTextColor.GREEN));
            lore.add(Component.text("------------").color(NamedTextColor.WHITE));
            lore.add(Component.text(""));
            lore.add(Component.text("메뉴북 아이템 없어도 인벤토리 화면에서 철 도끼를 우클릭해서 메뉴 화면 진입 가능").color(NamedTextColor.AQUA));
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
                lore.add(Component.text("시스템 : 연계").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("냉병기 : 잠시 자세를 잡은 뒤 들고 있는 도구로 4번 연속 벤다. 바라보는 적이 있으면 그 적을 향해 벤다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("한 번 벨 때마다 도구 일반 공격 피해의 66%의 피해를 가하며, 마지막 베기만 도구에 따른 넉백이 있다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("화기 : 최대 체력의 4%씩 소모해 탄환 2발을 연달아 발사한다. 각 탄환은 일반 공격 피해의 0.44배를 가한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("2발이 모두 적에게 적중하면 최대 체력의 12%를 소모해 즉시 꽂히는 3번째 탄환을 발사한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("3번째 탄환은 일반 공격 피해의 2배를 가하고 적을 크게 밀쳐내며, 반동으로 자신은 뒤로 밀려난다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("재사용 대기시간 6초.").color(NamedTextColor.GREEN));
                break;
            case "Q":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfQ.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 효과/공격").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 표식").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 자신 및 적 오브젝트").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("기본 : 바라보는 방향으로 대시하며 0.4초간 무적이 된다. 대시 중 가장 가까운 적 1명에게 표식을 남긴다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("냉병기 : 4블럭 안의 적에게 상처를 낸다. 상처는 4초간 총 8의 피해를 나눠 가하고 적을 둔화시킨다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("상처 난 적을 넉백 없는 공격(도끼·곡괭이 공격, 화기 R의 1·2번째 탄환)으로 맞히면, 남은 피해의 1.3배가 한 번에 터진다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("화기 : 6블럭 안의 적에게 점착 폭탄을 던져 붙인다. 폭탄은 4초 뒤 터져 반경 3블럭 안에 6의 피해를 가하고 밀쳐낸다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("폭탄이 붙은 적을 넉백 있는 공격으로 맞히면 즉시 터지며, 이때는 밀쳐내는 대신 1.2초간 기절시킨다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("재사용 대기시간 12초.").color(NamedTextColor.GREEN));
                break;
            case "F":
                requireXp = (level < 6) ? Component.text("Require EXP : " + requireExpOfF.get((int) level)) : Component.text("Require EXP : MAX");
                lore.add(requireXp.color(NamedTextColor.AQUA));

                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("타입 : 강화").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("시스템 : 강화").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("대상 : 자신").color(NamedTextColor.LIGHT_PURPLE));
                lore.add(Component.text("------------").color(NamedTextColor.WHITE));
                lore.add(Component.text("기본 : 받는 피해 증가 디버프를 모두 소모하고, 디버프 100%당 현재 체력의 10%를 잃는다. (최대 40%)").color(NamedTextColor.GREEN));
                lore.add(Component.text("6초 안에 가하는 다음 일반 공격 1회가 강화된다. 디버프가 없어도 사용할 수 있다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("강화된 공격은 피해가 1.6배, 크기가 1.3배가 되며, 소모한 디버프가 많을수록 커진다. (400% : 피해 2.4배, 크기 1.6배)").color(NamedTextColor.GREEN));
                lore.add(Component.text("냉병기 : 바라보는 적을 향해 붉게 커진 궤적으로 공격한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("화기 : 체력을 소모하지 않고, 적을 수에 제한 없이 관통하는 붉은 탄환을 발사한다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("강화는 핸드 스왑 후에도 유지되며, 공격하는 순간의 모드로 나간다.").color(NamedTextColor.GREEN));
                lore.add(Component.text("재사용 대기시간 9초.").color(NamedTextColor.GREEN));
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

    public List<Long> requireExpOfR = List.of(44L, 66L, 144L, 244L, 344L, 644L);
    public List<Long> requireExpOfQ = List.of(44L, 66L, 144L, 244L, 344L, 644L);
    public List<Long> requireExpOfF = List.of(44L, 66L, 144L, 244L, 344L, 644L);

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
            case "R": requireExpList = requireExpOfR; break;
            case "Q": requireExpList = requireExpOfQ; break;
            case "F": requireExpList = requireExpOfF; hpBonus = 2; break;
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