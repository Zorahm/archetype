package com.mod.archetype.command;

import com.mod.archetype.core.ClassManager;
import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.network.OpenClassSelectionPacket;
import com.mod.archetype.platform.NetworkHandler;
import com.mod.archetype.platform.PlayerDataAccess;
import com.mod.archetype.registry.ClassRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.stream.Collectors;

public class ArchetypeCommand {

    private static final SimpleCommandExceptionType ERROR_CLASS_NOT_FOUND =
            new SimpleCommandExceptionType(Component.translatable("commands.archetype.error.class_not_found"));
    private static final SimpleCommandExceptionType ERROR_NO_CLASS =
            new SimpleCommandExceptionType(Component.translatable("commands.archetype.error.no_class"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("archetype")
                .then(Commands.literal("set")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("class", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                ClassRegistry.getInstance().getAllIds(), builder))
                                        .executes(ctx -> executeSet(ctx, false))
                                        .then(Commands.argument("force", BoolArgumentType.bool())
                                                .executes(ctx -> executeSet(ctx, BoolArgumentType.getBool(ctx, "force")))
                                        )
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ArchetypeCommand::executeRemove)
                        )
                )
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ArchetypeCommand::executeGet)
                        )
                )
                .then(Commands.literal("list")
                        .executes(ArchetypeCommand::executeList)
                )
                .then(Commands.literal("select")
                        .executes(ctx -> executeSelect(ctx, null))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> executeSelect(ctx, EntityArgument.getPlayer(ctx, "player")))
                        )
                )
                .then(Commands.literal("reload")
                        .requires(src -> src.hasPermission(2))
                        .executes(ArchetypeCommand::executeReload)
                )
                .then(Commands.literal("ability")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("cooldown")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("ability_id", ResourceLocationArgument.id())
                                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0))
                                                        .executes(ArchetypeCommand::executeAbilityCooldown)
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ArchetypeCommand::executeAbilityReset)
                                )
                        )
                )
                .then(Commands.literal("resource")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("set")
                                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                                .executes(ctx -> executeResource(ctx, false))
                                        )
                                )
                                .then(Commands.literal("add")
                                        .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                                .executes(ctx -> executeResource(ctx, true))
                                        )
                                )
                        )
                )
                .then(Commands.literal("level")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("set")
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                .executes(ctx -> executeLevel(ctx, false))
                                        )
                                )
                                .then(Commands.literal("add")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(ctx -> executeLevel(ctx, true))
                                        )
                                )
                        )
                )
        );
    }

    private static int executeSet(CommandContext<CommandSourceStack> ctx, boolean force) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ResourceLocation classId = ResourceLocationArgument.getId(ctx, "class");
        if (!ClassRegistry.getInstance().exists(classId)) {
            throw ERROR_CLASS_NOT_FOUND.create();
        }
        ClassManager.getInstance().assignClass(player, classId);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.archetype.set.success", player.getDisplayName(), classId.toString()), true);
        return 1;
    }

    private static int executeRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (!data.hasClass()) {
            throw ERROR_NO_CLASS.create();
        }
        ClassManager.getInstance().removeClass(player);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.archetype.remove.success", player.getDisplayName()), true);
        return 1;
    }

    private static int executeGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (!data.hasClass()) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("commands.archetype.get.no_class", player.getDisplayName()), false);
            return 1;
        }
        ResourceLocation classId = data.getCurrentClassId();
        int level = data.getClassLevel();
        float resource = data.getResourceCurrent();
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("%s: class=%s, level=%d, resource=%.0f",
                        player.getName().getString(), classId, level, resource)), false);
        return 1;
    }

    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        Set<ResourceLocation> ids = ClassRegistry.getInstance().getAllIds();
        String classList = ids.stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("Available classes (%d): %s", ids.size(), classList)), false);
        return 1;
    }

    private static int executeSelect(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer player = target != null ? target : ctx.getSource().getPlayerOrException();
        NetworkHandler.INSTANCE.sendToPlayer(player, new OpenClassSelectionPacket((byte) 0));
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.archetype.select.success", player.getDisplayName()), true);
        return 1;
    }

    private static int executeReload(CommandContext<CommandSourceStack> ctx) {
        ClassRegistry.getInstance().reload(ctx.getSource().getServer().getResourceManager());
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.archetype.reload.success", ClassRegistry.getInstance().getClassCount()), true);
        return 1;
    }

    private static int executeAbilityCooldown(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ResourceLocation abilityId = ResourceLocationArgument.getId(ctx, "ability_id");
        int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        data.setCooldown(abilityId, ticks);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.archetype.ability.cooldown.success", abilityId.toString(), ticks), true);
        return 1;
    }

    private static int executeAbilityReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        data.getCooldowns().clear();
        data.getToggleStates().clear();
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.archetype.ability.reset.success", player.getDisplayName()), true);
        return 1;
    }

    private static int executeResource(CommandContext<CommandSourceStack> ctx, boolean isAdd) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        float amount = FloatArgumentType.getFloat(ctx, "amount");
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (!data.hasClass()) throw ERROR_NO_CLASS.create();

        float maxResource = 100f;
        PlayerClass playerClass = ClassRegistry.getInstance().get(data.getCurrentClassId()).orElse(null);
        if (playerClass != null && playerClass.getResource() != null) {
            maxResource = playerClass.getResource().maxValue();
        }

        float newValue = isAdd ? data.getResourceCurrent() + amount : amount;
        newValue = Math.max(0, Math.min(newValue, maxResource));
        data.setResourceCurrent(newValue);

        float finalValue = newValue;
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.archetype.resource.success",
                        player.getDisplayName(), String.format("%.0f", finalValue)), true);
        return 1;
    }

    private static int executeLevel(CommandContext<CommandSourceStack> ctx, boolean isAdd) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (!data.hasClass()) throw ERROR_NO_CLASS.create();

        int maxLevel = 20;
        int newLevel;
        if (isAdd) {
            int addAmount = IntegerArgumentType.getInteger(ctx, "amount");
            newLevel = data.getClassLevel() + addAmount;
        } else {
            newLevel = IntegerArgumentType.getInteger(ctx, "level");
        }
        newLevel = Math.max(1, Math.min(newLevel, maxLevel));
        data.setClassLevel(newLevel);

        int finalLevel = newLevel;
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.archetype.level.success", player.getDisplayName(), finalLevel), true);
        return 1;
    }
}
