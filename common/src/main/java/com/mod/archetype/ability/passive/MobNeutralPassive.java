package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class MobNeutralPassive extends AbstractPassiveAbility {

    private final List<String> mobTypes;
    private final float radius;

    public MobNeutralPassive(PassiveAbilityEntry entry) {
        super(entry);
        this.mobTypes = getStringList("mob_types");
        this.radius = getFloat("radius", 16.0f);
    }

    @Override
    public void tick(ServerPlayer player) {
        if (player.level().isClientSide()) return;
        if (player.tickCount % 10 != 0) return; // Check every half second

        List<EntityType<?>> targetTypes = new ArrayList<>();
        for (String typeStr : mobTypes) {
            ResourceLocation typeId = new ResourceLocation(typeStr);
            BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).ifPresent(targetTypes::add);
        }

        AABB searchBox = player.getBoundingBox().inflate(radius);
        List<Mob> nearbyMobs = player.level().getEntitiesOfClass(Mob.class, searchBox, mob -> {
            EntityType<?> mobType = mob.getType();
            return targetTypes.isEmpty() || targetTypes.contains(mobType);
        });

        for (Mob mob : nearbyMobs) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "mob_neutral");
    }
}
