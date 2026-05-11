package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Random;

public class SpaceRiftAbility extends AbstractActiveAbility {
    private static final Identifier RIFT_SPEED_ID = Identifier.fromNamespaceAndPath("archetype", "rift_speed");
    private static final Identifier RIFT_HP_ID = Identifier.fromNamespaceAndPath("archetype", "rift_hp");

    private final Random random = new Random();
    private final int realCooldown; // actual cooldown applied after effect ends

    private enum RiftEffect { NONE, LOW_GRAVITY, HIGH_GRAVITY, SHRINK, GROW, SPEED_UP, SPEED_DOWN, HP_UP, HP_DOWN }
    private RiftEffect currentEffect = RiftEffect.NONE;
    private int effectTicksRemaining = 0;
    private int effectAge = 0;

    public SpaceRiftAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.realCooldown = getInt("real_cooldown", 1200);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        // If effect is active and older than 5 seconds, cancel it
        if (currentEffect != RiftEffect.NONE && effectAge > 100) {
            removeCurrentEffect(player);
            currentEffect = RiftEffect.NONE;
            effectTicksRemaining = 0;
            effectAge = 0;
            active = false;
            applyRealCooldown(player);
            return ActivationResult.SUCCESS;
        }

        if (currentEffect != RiftEffect.NONE) {
            return ActivationResult.FAILED; // Effect active but too young to cancel
        }

        if (!canActivate(player)) return ActivationResult.FAILED;

        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        int level = data.getClassLevel();
        int levelTier5 = level / 5;
        int levelTier10 = level / 10;

        // Calculate chances
        int nothingChance = Math.max(0, 64 - levelTier5 * 8);
        int lowGravity = 4 + levelTier10;
        int highGravity = 5 + levelTier10;
        int shrink = 4 + levelTier10;
        int grow = 5 + levelTier10;
        int speedUp = 4 + levelTier10;
        int speedDown = 5 + levelTier10;
        int hpUp = 4 + levelTier10;
        int hpDown = 5 + levelTier10;

        int total = nothingChance + lowGravity + highGravity + shrink + grow + speedUp + speedDown + hpUp + hpDown;
        int roll = random.nextInt(total);

        int cumulative = 0;
        RiftEffect effect = RiftEffect.NONE;

        cumulative += nothingChance;
        if (roll < cumulative) { effect = RiftEffect.NONE; }
        else { cumulative += lowGravity; if (roll < cumulative) effect = RiftEffect.LOW_GRAVITY;
        else { cumulative += highGravity; if (roll < cumulative) effect = RiftEffect.HIGH_GRAVITY;
        else { cumulative += shrink; if (roll < cumulative) effect = RiftEffect.SHRINK;
        else { cumulative += grow; if (roll < cumulative) effect = RiftEffect.GROW;
        else { cumulative += speedUp; if (roll < cumulative) effect = RiftEffect.SPEED_UP;
        else { cumulative += speedDown; if (roll < cumulative) effect = RiftEffect.SPEED_DOWN;
        else { cumulative += hpUp; if (roll < cumulative) effect = RiftEffect.HP_UP;
        else { effect = RiftEffect.HP_DOWN;
        }}}}}}}}

        if (effect == RiftEffect.NONE) {
            // Nothing happened
            return ActivationResult.SUCCESS;
        }

        // Apply effect
        currentEffect = effect;
        effectTicksRemaining = 1200; // 60 seconds
        effectAge = 0;
        active = true;
        applyEffect(player, effect);

        return ActivationResult.SUCCESS;
    }

    private void applyEffect(ServerPlayer player, RiftEffect effect) {
        switch (effect) {
            case LOW_GRAVITY -> {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 1200, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 1200, 2, true, false));
            }
            case HIGH_GRAVITY -> {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 1200, 1, true, false));
            }
            case SHRINK -> {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 0, true, false));
            }
            case GROW -> {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 1200, 0, true, false));
            }
            case SPEED_UP -> {
                AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (attr != null) {
                    attr.removeModifier(RIFT_SPEED_ID);
                    attr.addTransientModifier(new AttributeModifier(RIFT_SPEED_ID, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
            case SPEED_DOWN -> {
                AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (attr != null) {
                    attr.removeModifier(RIFT_SPEED_ID);
                    attr.addTransientModifier(new AttributeModifier(RIFT_SPEED_ID, -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
            case HP_UP -> {
                AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
                if (attr != null) {
                    attr.removeModifier(RIFT_HP_ID);
                    attr.addTransientModifier(new AttributeModifier(RIFT_HP_ID, 4.0, AttributeModifier.Operation.ADD_VALUE));
                }
            }
            case HP_DOWN -> {
                AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
                if (attr != null) {
                    attr.removeModifier(RIFT_HP_ID);
                    attr.addTransientModifier(new AttributeModifier(RIFT_HP_ID, -4.0, AttributeModifier.Operation.ADD_VALUE));
                    if (player.getHealth() > player.getMaxHealth()) {
                        player.setHealth(player.getMaxHealth());
                    }
                }
            }
        }
    }

    private void removeCurrentEffect(ServerPlayer player) {
        switch (currentEffect) {
            case LOW_GRAVITY -> {
                player.removeEffect(MobEffects.SLOW_FALLING);
                player.removeEffect(MobEffects.JUMP_BOOST);
            }
            case HIGH_GRAVITY, GROW -> player.removeEffect(MobEffects.SLOWNESS);
            case SHRINK -> player.removeEffect(MobEffects.SPEED);
            case SPEED_UP, SPEED_DOWN -> {
                AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (attr != null) attr.removeModifier(RIFT_SPEED_ID);
            }
            case HP_UP, HP_DOWN -> {
                AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
                if (attr != null) attr.removeModifier(RIFT_HP_ID);
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        }
    }

    @Override
    public void tickActive(ServerPlayer player) {
        if (currentEffect == RiftEffect.NONE) {
            active = false;
            return;
        }
        effectAge++;
        effectTicksRemaining--;
        if (effectTicksRemaining <= 0) {
            removeCurrentEffect(player);
            currentEffect = RiftEffect.NONE;
            active = false;
            effectAge = 0;
            applyRealCooldown(player);
        }
    }

    @Override
    public void forceDeactivate(ServerPlayer player) {
        if (currentEffect != RiftEffect.NONE) {
            removeCurrentEffect(player);
        }
        currentEffect = RiftEffect.NONE;
        effectTicksRemaining = 0;
        effectAge = 0;
        active = false;
    }

    /**
     * Manually set the real cooldown after effect ends or is cancelled.
     * The JSON "cooldown" is kept minimal to allow re-press cancel.
     */
    private void applyRealCooldown(ServerPlayer player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        Identifier abilityId = Identifier.fromNamespaceAndPath(entry.type().getNamespace(), entry.slot());
        data.setCooldown(abilityId, realCooldown);
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "space_rift");
    }
}
