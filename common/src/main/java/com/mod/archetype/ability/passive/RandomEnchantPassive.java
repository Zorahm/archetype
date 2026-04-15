package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RandomEnchantPassive extends AbstractPassiveAbility {
    private final Random random = new Random();
    private int tickCounter = 0;

    private static final Set<ResourceKey<Enchantment>> BLACKLISTED = Set.of(
            Enchantments.MENDING,
            Enchantments.SILK_TOUCH,
            Enchantments.FORTUNE,
            Enchantments.AQUA_AFFINITY
    );

    public RandomEnchantPassive(PassiveAbilityEntry entry) { super(entry); }

    @Override
    public void tick(ServerPlayer player) {
        tickCounter++;
        if (tickCounter % 20 != 0) return;

        HolderLookup.RegistryLookup<Enchantment> enchantmentRegistry = player.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);

        // Only enchant items held in main hand or offhand
        ItemStack[] handItems = { player.getMainHandItem(), player.getOffhandItem() };
        for (ItemStack stack : handItems) {
            if (stack.isEmpty()) continue;
            if (!stack.isEnchantable() && !stack.isEnchanted()) continue;

            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            if (customData.copyTag().getBooleanOr("archetype_enchanted", false)) continue;

            List<Holder.Reference<Enchantment>> valid = new ArrayList<>();
            for (Holder.Reference<Enchantment> enchHolder : enchantmentRegistry.listElements().toList()) {
                if (!BLACKLISTED.contains(enchHolder.key())) {
                    valid.add(enchHolder);
                }
            }
            if (valid.isEmpty()) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack,
                        tag -> tag.putBoolean("archetype_enchanted", true));
                continue;
            }

            Holder.Reference<Enchantment> chosen = valid.get(random.nextInt(valid.size()));
            stack.enchant(chosen, 1);

            // 15% chance curse of vanishing
            if (random.nextFloat() < 0.15f) {
                enchantmentRegistry.get(Enchantments.VANISHING_CURSE)
                        .ifPresent(vanishing -> stack.enchant(vanishing, 1));
            }

            // Damage tool by 10% of max durability
            if (stack.isDamageableItem()) {
                int dmg = Math.max(1, stack.getMaxDamage() / 10);
                stack.setDamageValue(stack.getDamageValue() + dmg);
                if (stack.getDamageValue() >= stack.getMaxDamage()) {
                    stack.shrink(1);
                }
            }

            CustomData.update(DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.putBoolean("archetype_enchanted", true));

            // Sound + particles
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        player.getX(), player.getY(), player.getZ(),
                        50, 0.5, 0.5, 0.5, 1.0);
            }
        }
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "random_enchant");
    }
}
