package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SummonAbility extends AbstractActiveAbility {

    private final String entityTypeId;
    private final int maxSummons;
    private final int durationTicks;
    private final float health;
    private final float damage;
    private final List<UUID> summonedIds = new ArrayList<>();

    public SummonAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.entityTypeId = getString("entity_type", "minecraft:wolf");
        this.maxSummons = getInt("max_summons", 1);
        this.durationTicks = getInt("duration_ticks", 1200);
        this.health = getFloat("health", 20.0f);
        this.damage = getFloat("damage", 4.0f);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;
        Optional<EntityType<?>> typeOpt = EntityType.byString(entityTypeId);
        if (typeOpt.isEmpty()) return ActivationResult.FAILED;
        ServerLevel level = (ServerLevel) player.level();

        summonedIds.removeIf(uuid -> {
            Entity e = level.getEntity(uuid);
            return e == null || !e.isAlive();
        });

        while (summonedIds.size() >= maxSummons) {
            UUID oldest = summonedIds.remove(0);
            Entity old = level.getEntity(oldest);
            if (old != null) old.discard();
        }

        Entity entity = typeOpt.get().create(level);
        if (entity == null) return ActivationResult.FAILED;

        Vec3 spawnPos = player.position().add(player.getLookAngle().scale(2));
        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

        if (entity instanceof LivingEntity living) {
            if (living.getAttribute(Attributes.MAX_HEALTH) != null) {
                living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
                living.setHealth(health);
            }
            if (living.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                living.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
            }
        }

        level.addFreshEntity(entity);
        summonedIds.add(entity.getUUID());
        return ActivationResult.SUCCESS;
    }

    @Override
    public void forceDeactivate(ServerPlayer player) {
        active = false;
        if (player.level() instanceof ServerLevel level) {
            for (UUID uuid : summonedIds) {
                Entity entity = level.getEntity(uuid);
                if (entity != null) entity.discard();
            }
        }
        summonedIds.clear();
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "summon");
    }
}
