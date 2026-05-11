package com.mod.archetype.advancement;

import com.mod.archetype.Archetype;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class ClassActionTrigger extends SimpleCriterionTrigger<ClassActionTrigger.Instance> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "class_action");
    public static final ClassActionTrigger INSTANCE = new ClassActionTrigger();

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, String action, int level) {
        this.trigger(player, instance -> instance.matches(action, level, ""));
    }

    public void trigger(ServerPlayer player, String action, int level, String classId) {
        this.trigger(player, instance -> instance.matches(action, level, classId));
    }

    public record Instance(String action, int minLevel, String classId) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.optionalFieldOf("action", "").forGetter(Instance::action),
                Codec.INT.optionalFieldOf("min_level", -1).forGetter(Instance::minLevel),
                Codec.STRING.optionalFieldOf("class_id", "").forGetter(Instance::classId)
        ).apply(inst, Instance::new));

        public boolean matches(String action, int level, String classId) {
            if (!this.action.isEmpty() && !this.action.equals(action)) {
                return false;
            }
            if (this.minLevel >= 0 && level < this.minLevel) {
                return false;
            }
            if (!this.classId.isEmpty() && !this.classId.equals(classId)) {
                return false;
            }
            return true;
        }

        @Override
        public Optional<ContextAwarePredicate> player() {
            return Optional.empty();
        }
    }
}
