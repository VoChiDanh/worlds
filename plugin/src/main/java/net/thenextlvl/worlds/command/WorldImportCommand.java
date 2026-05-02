package net.thenextlvl.worlds.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.worlds.Dimension;
import net.thenextlvl.worlds.Level;
import net.thenextlvl.worlds.WorldOperationException;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.argument.DimensionArgumentType;
import net.thenextlvl.worlds.command.argument.GeneratorArgument;
import net.thenextlvl.worlds.command.argument.KeyArgument;
import net.thenextlvl.worlds.command.argument.WorldPresetArgument;
import net.thenextlvl.worlds.command.brigadier.OptionCommand;
import net.thenextlvl.worlds.command.suggestion.WorldImportSuggestionProvider;
import net.thenextlvl.worlds.generator.GeneratorType;
import net.thenextlvl.worlds.generator.Generator;
import net.thenextlvl.worlds.preset.Preset;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;

import java.util.Set;

import static org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.COMMAND;

@NullMarked
final class WorldImportCommand extends OptionCommand {
    private WorldImportCommand(final WorldsPlugin plugin) {
        super(plugin, "import", "worlds.command.import");
    }

    public static ArgumentBuilder<CommandSourceStack, ?> create(final WorldsPlugin plugin) {
        final var command = new WorldImportCommand(plugin);
        return command.create().then(command.createCommand());
    }

    @Override
    protected RequiredArgumentBuilder<CommandSourceStack, Key> createCommand() {
        final var command = Commands.argument("key", new KeyArgument())
                .suggests(new WorldImportSuggestionProvider(plugin)).executes(this);

        addOptions(command, false, Set.of(
                new Option("dimension", new DimensionArgumentType(plugin)),
                new Option("generator", new GeneratorArgument(plugin), "preset"),
                new Option("name", StringArgumentType.string()),
                new Option("preset", new WorldPresetArgument(plugin), "generator")
        ), null);

        return command;
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();
        final var key = context.getArgument("key", Key.class);

        final var preset = tryGetArgument(context, "preset", Preset.class).orElse(null);
        final var dimension = tryGetArgument(context, "dimension", Dimension.class).orElse(null);
        final var displayName = tryGetArgument(context, "name", String.class).orElse(null);
        final var generator = tryGetArgument(context, "generator", Generator.class).orElse(null);

        final var name = displayName != null ? displayName : key.asString();

        if (plugin.getWorldRegistry().isRegistered(key)) {
            CommandFailureHandler.handle(plugin, sender, new WorldOperationException(
                    WorldOperationException.Reason.WORLD_KEY_EXISTS
            ).key(key));
            return 0;
        }

        plugin.bundle().sendMessage(sender, "world.import", Placeholder.parsed("world", name));

        final var build = Level.builder(key)
                .name(displayName)
                .generator(generator)
                .generatorType(preset != null ? GeneratorType.FLAT : null)
                .preset(preset)
                .dimension(dimension)
                .build();

        build.create().thenAccept(level -> {
            plugin.getWorldRegistry().register(build, true);
            plugin.bundle().sendMessage(sender, "world.import.success",
                    Placeholder.parsed("world", level.key().asString()));
            if (!(sender instanceof final Entity entity)) return;
            entity.teleportAsync(level.getSpawnLocation(), COMMAND);
        }).exceptionally(throwable -> {
            CommandFailureHandler.handle(plugin, sender, throwable, Placeholder.parsed("world", name));
            return null;
        });

        return SINGLE_SUCCESS;
    }
}
