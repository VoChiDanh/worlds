package net.thenextlvl.worlds.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.worlds.Dimension;
import net.thenextlvl.worlds.Level;
import net.thenextlvl.worlds.WorldOperationException;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.argument.CommandOptionsArgument;
import net.thenextlvl.worlds.command.argument.DimensionArgumentType;
import net.thenextlvl.worlds.command.argument.GeneratorArgument;
import net.thenextlvl.worlds.command.argument.KeyArgument;
import net.thenextlvl.worlds.command.argument.WorldPresetArgument;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import net.thenextlvl.worlds.command.suggestion.WorldImportSuggestionProvider;
import net.thenextlvl.worlds.generator.Generator;
import net.thenextlvl.worlds.generator.GeneratorType;
import net.thenextlvl.worlds.preset.Preset;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

import static org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.COMMAND;

@NullMarked
final class WorldImportCommand extends SimpleCommand {
    private WorldImportCommand(final WorldsPlugin plugin) {
        super(plugin, "import", "worlds.command.import");
    }

    public static ArgumentBuilder<CommandSourceStack, ?> create(final WorldsPlugin plugin) {
        final var command = new WorldImportCommand(plugin);

        final var options = Commands.argument("options", new CommandOptionsArgument(Map.of(
                "dimension", new DimensionArgumentType(plugin),
                "generator", new GeneratorArgument(plugin),
                "preset", new WorldPresetArgument(plugin)
        ))).executes(command);

        final var key = Commands.argument("key", new KeyArgument());
        return command.create().then(key
                .suggests(new WorldImportSuggestionProvider(plugin))
                .then(options)
                .executes(command));
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();
        final var key = context.getArgument("key", Key.class);

        final var options = tryGetArgument(context, "options", CommandOptionsArgument.Options.class)
                .orElseGet(CommandOptionsArgument.Options::new);
        final var generatorType = options.getArgument("preset", Preset.class)
                .map(GeneratorType.FLAT::with).orElse(null);
        final var dimension = options.getArgument("dimension", Dimension.class).orElse(null);
        final var generator = options.getArgument("generator", Generator.class).orElse(null);

        if (plugin.getWorldRegistry().isRegistered(key)) {
            CommandFailureHandler.handle(plugin, sender, new WorldOperationException(
                    WorldOperationException.Reason.WORLD_KEY_EXISTS
            ).key(key));
            return 0;
        }

        final var placeholder = Placeholder.parsed("world", key.asString());
        plugin.bundle().sendMessage(sender, "world.import", placeholder);

        final var build = Level.builder(key)
                .generator(generator)
                .generatorType(generatorType)
                .dimension(dimension)
                .build();

        build.create().thenAccept(level -> {
            plugin.getWorldRegistry().register(build, true);
            plugin.bundle().sendMessage(sender, "world.import.success",
                    Placeholder.parsed("world", level.key().asString()));
            if (!(sender instanceof final Entity entity)) return;
            entity.teleportAsync(level.getSpawnLocation(), COMMAND);
        }).exceptionally(throwable -> {
            CommandFailureHandler.handle(plugin, sender, throwable, placeholder);
            return null;
        });

        return SINGLE_SUCCESS;
    }
}
