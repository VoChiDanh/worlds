package net.thenextlvl.worlds.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.worlds.Dimension;
import net.thenextlvl.worlds.Level;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.argument.CommandOptionsArgument;
import net.thenextlvl.worlds.command.argument.DimensionArgumentType;
import net.thenextlvl.worlds.command.argument.GeneratorArgument;
import net.thenextlvl.worlds.command.argument.GeneratorTypeArgument;
import net.thenextlvl.worlds.command.argument.KeyArgument;
import net.thenextlvl.worlds.command.argument.SeedArgument;
import net.thenextlvl.worlds.command.argument.WorldPresetArgument;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import net.thenextlvl.worlds.generator.Generator;
import net.thenextlvl.worlds.generator.GeneratorType;
import net.thenextlvl.worlds.preset.Preset;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

import static net.thenextlvl.worlds.command.WorldCommand.worldArgument;
import static org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.COMMAND;

@NullMarked
final class WorldRecreateCommand extends SimpleCommand {
    private WorldRecreateCommand(final WorldsPlugin plugin) {
        super(plugin, "recreate", "worlds.command.recreate");
    }

    public static ArgumentBuilder<CommandSourceStack, ?> create(final WorldsPlugin plugin) {
        final var command = new WorldRecreateCommand(plugin);
        return command.create().then(command.createCommand());
    }

    private RequiredArgumentBuilder<CommandSourceStack, ?> createCommand() {
        final var key = Commands.argument("key", new KeyArgument());
        final var options = Commands.argument("options", new CommandOptionsArgument(Map.of(
                "bonus-chest", BoolArgumentType.bool(),
                "dimension", new DimensionArgumentType(plugin),
                "generator", new GeneratorArgument(plugin),
                "hardcore", BoolArgumentType.bool(),
                "preset", new WorldPresetArgument(plugin),
                "seed", new SeedArgument(),
                "structures", BoolArgumentType.bool(),
                "type", new GeneratorTypeArgument(plugin)
        ))).executes(this);

        return worldArgument(plugin).then(key.then(options).executes(this));
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();
        final var world = context.getArgument("world", World.class);
        final var key = context.getArgument("key", Key.class);
        final var options = tryGetArgument(context, "options", CommandOptionsArgument.Options.class)
                .orElseGet(CommandOptionsArgument.Options::new);

        final var builder = Level.copy(world);

        options.getArgument("bonus-chest", Boolean.class).ifPresent(builder::bonusChest);
        options.getArgument("dimension", Dimension.class).ifPresent(builder::dimension);
        options.getArgument("generator", Generator.class).ifPresent(builder::generator);
        options.getArgument("hardcore", Boolean.class).ifPresent(builder::hardcore);
        options.getArgument("preset", Preset.class).map(GeneratorType.FLAT::with).ifPresent(builder::generatorType);
        options.getArgument("seed", Long.class).ifPresent(builder::seed);
        options.getArgument("structures", Boolean.class).ifPresent(builder::structures);
        options.getArgument("type", GeneratorType.class).ifPresent(builder::generatorType);

        final var level = builder.key(key).build();

        final var placeholder = Placeholder.parsed("world", world.key().asString());

        plugin.bundle().sendMessage(sender, "world.recreate", placeholder);
        level.create().thenAccept(recreated -> {
            plugin.bundle().sendMessage(sender, "world.recreate.success", placeholder);
            if (!(sender instanceof final Entity entity)) return;
            entity.teleportAsync(recreated.getSpawnLocation(), COMMAND);
        }).exceptionally(throwable -> {
            CommandFailureHandler.handle(plugin, sender, throwable, placeholder);
            return null;
        });
        return SINGLE_SUCCESS;
    }
}
