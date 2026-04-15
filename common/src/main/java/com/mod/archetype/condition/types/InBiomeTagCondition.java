package com.mod.archetype.condition.types;

import com.google.gson.JsonObject;
import com.mod.archetype.condition.Condition;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;

public class InBiomeTagCondition implements Condition {
    private final TagKey<Biome> biomeTag;

    public InBiomeTagCondition(JsonObject params) {
        String tag = params.has("tag") ? params.get("tag").getAsString() : "minecraft:is_ocean";
        this.biomeTag = TagKey.create(Registries.BIOME, Identifier.parse(tag));
    }

    @Override
    public boolean test(Player player) {
        Holder<Biome> biome = player.level().getBiome(player.blockPosition());
        return biome.is(biomeTag);
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "in_biome_tag");
    }
}
