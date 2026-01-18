package org.core.effect.crowdControl;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;

public class EffectManager implements Listener {
    private final Map<Entity, Map<Class<? extends Effects>, Effects>> activeEffects = new HashMap<>();

    public void applyEffect(Entity entity, Effects effect) {
        activeEffects.computeIfAbsent(entity, k -> new HashMap<>())
                .put(effect.getClass(), effect);
        effect.applyEffect(entity);
    }

    public void removeEffect(Entity entity, Class<? extends Effects> effectClass) {
        if (activeEffects.containsKey(entity) && activeEffects.get(entity).containsKey(effectClass)) {
            Effects effect = activeEffects.get(entity).get(effectClass);
            effect.removeEffect(entity);
            activeEffects.get(entity).remove(effectClass);
        }
    }

    public boolean hasEffect(Player player, Class<? extends Effects> effectClass) {
        return activeEffects.containsKey(player) && activeEffects.get(player).containsKey(effectClass);
    }
}