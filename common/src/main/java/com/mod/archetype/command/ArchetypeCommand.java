package com.mod.archetype.command;

import com.mod.archetype.core.ClassManager;
import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import com.mod.archetype.registry.ClassRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.Set;

public class ArchetypeCommand {

    private static final int C_BRAND = 0xFFAA00; // amber  — [Archetype] prefix
    private static final int C_MUTED = 0x666666; // gray   — brackets, separators, ids
    private static final int C_TEXT  = 0xEEEEEE; // white  — player names, plain text
    private static final int C_NUM   = 0x55FFFF; // cyan   — numbers

    private static final SimpleCommandExceptionType ERROR_CLASS_NOT_FOUND =
            new SimpleCommandExceptionType(Component.translatable("commands.archetype.error.class_not_found"));
    private static final SimpleCommandExceptionType ERROR_NO_CLASS =
            new SimpleCommandExceptionType(Component.translatable("commands.archetype.error.no_class"));

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static MutableComponent prefix() {
        return Component.literal("[")
                .withStyle(Style.EMPTY.withColor(C_MUTED))
                .append(Component.literal("Archetype").withStyle(Style.EMPTY.withColor(C_BRAND).withBold(true)))
                .append(Component.literal("] ").withStyle(Style.EMPTY.withColor(C_MUTED)));
    }

    /** Mid-dot field separator: " · " */
    private static Component sep() {
        return Component.literal(" \u00b7 ").withStyle(Style.EMPTY.withColor(C_MUTED));
    }

    private static Component classComp(Identifier classId) {
        return ClassRegistry.getInstance().get(classId)
                .map(cls -> (Component) Component.translatable(cls.getNameKey())
                        .withStyle(Style.EMPTY.withColor(0xFF000000 | cls.getColor()).withBold(true)))
                .orElse(Component.literal(classId.toString()).withStyle(Style.EMPTY.withColor(C_MUTED)));
    }

    private static Component num(int n) {
        return Component.literal(String.valueOf(n)).withStyle(Style.EMPTY.withColor(C_NUM));
    }

    private static Component num(float f) {
        return Component.literal(String.format("%.0f", f)).withStyle(Style.EMPTY.withColor(C_NUM));
    }

    private static Component playerComp(ServerPlayer player) {
        return player.getDisplayName().copy().withStyle(Style.EMPTY.withColor(C_TEXT));
    }

    // ── Registration ─────────────────────────────────────────────────────────

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("archetype")
                .then(Commands.literal("set")
                        .requires(src -> Commands.LEVEL_GAMEMASTERS.check(src.permissions()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("class", IdentifierArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                ClassRegistry.getInstance().getAllIds(), builder))
                                        .executes(ArchetypeCommand::executeSet)
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .requires(src -> Commands.LEVEL_GAMEMASTERS.check(src.permissions()))
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
                .then(Commands.literal("reload")
                        .requires(src -> Commands.LEVEL_GAMEMASTERS.check(src.permissions()))
                        .executes(ArchetypeCommand::executeReload)
                )
        );
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    private static int executeSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Identifier classId = IdentifierArgument.getId(ctx, "class");
        if (!ClassRegistry.getInstance().exists(classId)) throw ERROR_CLASS_NOT_FOUND.create();

        ClassManager.getInstance().assignClass(player, classId);

        ctx.getSource().sendSuccess(() -> prefix()
                .append(playerComp(player))
                .append(sep())
                .append(classComp(classId)), true);
        return 1;
    }

    private static int executeRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (!data.hasClass()) throw ERROR_NO_CLASS.create();

        ClassManager.getInstance().removeClass(player);

        ctx.getSource().sendSuccess(() -> prefix()
                .append(playerComp(player))
                .append(sep())
                .append(Component.translatable("commands.archetype.remove.success")
                        .withStyle(Style.EMPTY.withColor(C_TEXT))), true);
        return 1;
    }

    private static int executeGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);

        if (!data.hasClass()) {
            ctx.getSource().sendSuccess(() -> prefix()
                    .append(playerComp(player))
                    .append(sep())
                    .append(Component.translatable("commands.archetype.get.no_class")
                            .withStyle(Style.EMPTY.withColor(C_MUTED))), false);
            return 1;
        }

        Identifier classId = data.getCurrentClassId();
        int level = data.getClassLevel();
        float resource = data.getResourceCurrent();

        PlayerClass cls = ClassRegistry.getInstance().get(classId).orElse(null);
        float maxRes = cls != null && cls.getResource() != null ? cls.getResource().maxValue() : 100f;

        ctx.getSource().sendSuccess(() -> prefix()
                .append(playerComp(player))
                .append(sep())
                .append(classComp(classId))
                .append(sep())
                .append(Component.literal("Lv ").withStyle(Style.EMPTY.withColor(C_MUTED))
                        .append(num(level)))
                .append(sep())
                .append(num(resource))
                .append(Component.literal(" / " + (int) maxRes)
                        .withStyle(Style.EMPTY.withColor(C_MUTED))),
                false);
        return 1;
    }

    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        Set<Identifier> ids = ClassRegistry.getInstance().getAllIds();

        MutableComponent msg = prefix()
                .append(Component.translatable("commands.archetype.list", num(ids.size()))
                        .withStyle(Style.EMPTY.withColor(C_MUTED)));

        ids.stream()
                .sorted(Comparator.comparing(Identifier::getPath))
                .forEach(id -> msg
                        .append(Component.literal("\n  \u2022 ").withStyle(Style.EMPTY.withColor(C_MUTED)))
                        .append(classComp(id))
                        .append(Component.literal("  " + id)
                                .withStyle(Style.EMPTY.withColor(C_MUTED).withItalic(true)))
                );

        ctx.getSource().sendSuccess(() -> msg, false);
        return 1;
    }

    private static int executeReload(CommandContext<CommandSourceStack> ctx) {
        ClassRegistry.getInstance().reload(ctx.getSource().getServer().getResourceManager());
        int count = ClassRegistry.getInstance().getClassCount();

        ctx.getSource().sendSuccess(() -> prefix()
                .append(Component.translatable("commands.archetype.reload.success", num(count))
                        .withStyle(Style.EMPTY.withColor(C_TEXT))), true);
        return 1;
    }
}
