package net.noiilive.jojowor.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.registry.ModStands;
import net.noiilive.jojowor.stand.Stand;
import net.noiilive.jojowor.stand.Stands;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.stream.Stream;

public final class StandCommand {
    private static final String NONE = "none";

    private static final int PERMISSION_LEVEL = 2;

    private static final DynamicCommandExceptionType ERROR_UNKNOWN_STAND =
            new DynamicCommandExceptionType(name -> Component.translatable("commands.jojowor.stand.unknown", name));

    private static final SuggestionProvider<CommandSourceStack> STAND_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    Stream.concat(Stream.of(NONE), ModStands.REGISTRY.keySet().stream().map(StandCommand::shortId)),
                    builder);

    private StandCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("stand")
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("stand", StringArgumentType.word())
                                        .suggests(STAND_SUGGESTIONS)
                                        .executes(context -> setStand(
                                                context,
                                                EntityArgument.getPlayers(context, "targets"),
                                                StringArgumentType.getString(context, "stand"))))));
    }

    private static int setStand(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, String input)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        if (NONE.equalsIgnoreCase(input)) {
            targets.forEach(Stands::clear);
            if (targets.size() == 1) {
                ServerPlayer target = targets.iterator().next();
                source.sendSuccess(() -> Component.translatable(
                        "commands.jojowor.stand.clear.success.single", target.getDisplayName()), true);
            } else {
                source.sendSuccess(() -> Component.translatable(
                        "commands.jojowor.stand.clear.success.multiple", targets.size()), true);
            }
            return targets.size();
        }

        Stand stand = resolveStand(input);
        if (stand == null) {
            throw ERROR_UNKNOWN_STAND.create(input);
        }

        targets.forEach(target -> Stands.set(target, stand));
        if (targets.size() == 1) {
            ServerPlayer target = targets.iterator().next();
            source.sendSuccess(() -> Component.translatable(
                    "commands.jojowor.stand.set.success.single", stand.getDisplayName(), target.getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable(
                    "commands.jojowor.stand.set.success.multiple", stand.getDisplayName(), targets.size()), true);
        }
        return targets.size();
    }

    @Nullable
    private static Stand resolveStand(String input) {
        ResourceLocation id = input.indexOf(':') >= 0
                ? ResourceLocation.tryParse(input)
                : ResourceLocation.tryBuild(JoJoWoR.MODID, input);
        return id == null ? null : ModStands.REGISTRY.get(id);
    }

    private static String shortId(ResourceLocation id) {
        return JoJoWoR.MODID.equals(id.getNamespace()) ? id.getPath() : id.toString();
    }
}
