package com.mod.archetype.advancement;

import com.google.gson.JsonObject;
import com.mod.archetype.Archetype;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

public class ClassActionTrigger extends SimpleCriterionTrigger<ClassActionTrigger.Instance> {

    public static final ResourceLocation ID = new ResourceLocation(Archetype.MOD_ID, "class_action");
    public static final ClassActionTrigger INSTANCE = new ClassActionTrigger();

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate predicate, DeserializationContext context) {
        String action = GsonHelper.getAsString(json, "action", "");
        int minLevel = GsonHelper.getAsInt(json, "min_level", -1);
        return new Instance(predicate, action, minLevel);
    }

    public void trigger(ServerPlayer player, String action, int level) {
        this.trigger(player, instance -> instance.matches(action, level));
    }

    public static class Instance extends AbstractCriterionTriggerInstance {

        private final String action;
        private final int minLevel;

        public Instance(ContextAwarePredicate predicate, String action, int minLevel) {
            super(ID, predicate);
            this.action = action;
            this.minLevel = minLevel;
        }

        public boolean matches(String action, int level) {
            if (!this.action.isEmpty() && !this.action.equals(action)) {
                return false;
            }
            return this.minLevel < 0 || level >= this.minLevel;
        }

        @Override
        public JsonObject serializeToJson(net.minecraft.advancements.critereon.SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            if (!action.isEmpty()) {
                json.addProperty("action", action);
            }
            if (minLevel >= 0) {
                json.addProperty("min_level", minLevel);
            }
            return json;
        }
    }
}
