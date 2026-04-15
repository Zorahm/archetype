package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public class UndeadTypePassive extends AbstractPassiveAbility {

    private final boolean smiteVulnerability;
    private final boolean healingPotionsDamage;

    public UndeadTypePassive(PassiveAbilityEntry entry) {
        super(entry);
        this.smiteVulnerability = getBool("smite_vulnerability", true);
        this.healingPotionsDamage = getBool("healing_potions_damage", true);
    }

    @Override
    public void tick(ServerPlayer player) {
        // No tick behavior - interactions handled in onPlayerHurt
    }

    @Override
    public void onPlayerHurt(ServerPlayer player, DamageSource source, float amount) {
        // Placeholder for smite/healing potion interaction
        // Smite enchantment bonus damage and healing potion inversion
        // will be handled by a mixin or event hook that checks for this passive
    }

    public boolean hasSmiteVulnerability() {
        return smiteVulnerability;
    }

    public boolean hasHealingPotionsDamage() {
        return healingPotionsDamage;
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "undead_type");
    }
}
