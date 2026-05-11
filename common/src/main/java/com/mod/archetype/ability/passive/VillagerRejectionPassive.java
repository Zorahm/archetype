package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;

public class VillagerRejectionPassive extends AbstractPassiveAbility {

    public VillagerRejectionPassive(PassiveAbilityEntry entry) {
        super(entry);
    }

    @Override
    public boolean onEntityInteract(ServerPlayer player, Entity entity) {
        if (!(entity instanceof AbstractVillager villager)) return false;
        if (player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) return false;

        // Play the vanilla "no" sound — same audio as when a villager has no profession
        villager.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
        return true; // cancel the interaction — trading screen never opens
    }

    @Override
    public void tick(ServerPlayer player) {}

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "villager_rejection");
    }
}
