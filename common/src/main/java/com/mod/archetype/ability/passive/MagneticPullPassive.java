package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MagneticPullPassive extends AbstractPassiveAbility {

    private final float itemPullRadius;
    private final float itemPullSpeed;

    public MagneticPullPassive(PassiveAbilityEntry entry) {
        super(entry);
        this.itemPullRadius = getFloat("item_pull_radius", 8.0f);
        this.itemPullSpeed = getFloat("item_pull_speed", 0.05f);
    }

    @Override
    public void tick(ServerPlayer player) {
        if (player.level().isClientSide()) return;

        AABB searchBox = player.getBoundingBox().inflate(itemPullRadius);
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, searchBox);

        Vec3 playerPos = player.position();
        for (ItemEntity item : items) {
            Vec3 direction = playerPos.subtract(item.position()).normalize();
            Vec3 velocity = direction.scale(itemPullSpeed);
            item.setDeltaMovement(item.getDeltaMovement().add(velocity));
        }
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "magnetic_pull");
    }
}
