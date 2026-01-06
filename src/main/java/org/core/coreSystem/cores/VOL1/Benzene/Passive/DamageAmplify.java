package org.core.coreSystem.cores.VOL1.Benzene.Passive;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.core.coreSystem.cores.VOL1.Benzene.coreSystem.Benzene;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class DamageAmplify {

    private final Benzene config;

    public DamageAmplify(Benzene config) {
        this.config = config;
    }

    public double Amplify(Player player, Entity entity, double originalDamage) {
        double amplifiedDamage = originalDamage;

        int t = 0;

        for (Entity chainedEntity : new ArrayList<>(config.Chain.getOrDefault(player.getUniqueId(), new LinkedHashMap<>()).values())) {
            if (chainedEntity == entity) {
                t++;
            }
        }

        amplifiedDamage = switch (t) {
            case 1 -> originalDamage * 1.1;
            case 2 -> originalDamage * 1.2;
            case 3 -> originalDamage * 1.3;
            case 4 -> originalDamage * 1.4;
            case 5 -> originalDamage * 1.5;
            case 6 -> originalDamage * 1.6;
            default -> amplifiedDamage;
        };

        if(config.q_Skill_effect_1.containsValue(entity)){
            amplifiedDamage = amplifiedDamage * ((double) 5/3);
        }

        return amplifiedDamage;
    }

}
