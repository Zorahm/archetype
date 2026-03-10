package com.mod.archetype.condition.types;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mod.archetype.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class TimeOfDayCondition implements Condition {
    private final int from;
    private final int to;

    public TimeOfDayCondition(JsonObject params) {
        if (params.has("range") && params.get("range").isJsonArray()) {
            JsonArray range = params.getAsJsonArray("range");
            this.from = range.get(0).getAsInt();
            this.to = range.get(1).getAsInt();
        } else {
            this.from = 0;
            this.to = 24000;
        }
    }

    @Override
    public boolean test(Player player) {
        long dayTime = player.level().getDayTime() % 24000;
        if (from <= to) {
            return dayTime >= from && dayTime <= to;
        }
        // Wraps around midnight (e.g. 13000 to 23000)
        return dayTime >= from || dayTime <= to;
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "time_of_day");
    }
}
