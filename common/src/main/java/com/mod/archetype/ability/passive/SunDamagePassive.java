package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;

public class SunDamagePassive extends AbstractPassiveAbility {

    private final float damagePerSecond;
    private final boolean ignoreIfHelmet;
    private final boolean ignoreIfUnderground;
    private final int setFireTicks;

    public SunDamagePassive(PassiveAbilityEntry entry) {
        super(entry);
        this.damagePerSecond = getFloat("damage_per_second", 1.0f);
        this.ignoreIfHelmet = getBool("ignore_if_helmet", true);
        this.ignoreIfUnderground = getBool("ignore_if_underground", true);
        this.setFireTicks = getInt("set_fire_ticks", 60);
    }

    @Override
    public void tick(ServerPlayer player) {
        if (player.level().isClientSide()) return;

        long dayTime = player.level().getDayTime() % 24000;
        boolean isDaytime = dayTime >= 0 && dayTime < 12000;
        if (!isDaytime) return;

        if (ignoreIfUnderground && !player.level().canSeeSky(player.blockPosition())) return;

        if (ignoreIfHelmet && !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) return;

        // Damage once per second (every 20 ticks)
        if (player.tickCount % 20 == 0) {
            player.hurt(player.damageSources().onFire(), damagePerSecond);
        }
        player.setRemainingFireTicks(setFireTicks);
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "sun_damage");
    }
}
