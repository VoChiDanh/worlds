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
import net.thenextlvl.worlds.command.argument.DimensionArgumentType;
import net.thenextlvl.worlds.command.argument.KeyArgument;
import net.thenextlvl.worlds.command.argument.SeedArgument;
import net.thenextlvl.worlds.command.brigadier.OptionCommand;
import net.thenextlvl.worlds.generator.Generator;
import net.thenextlvl.worlds.generator.GeneratorType;
import net.thenextlvl.worlds.preset.Preset;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;

import java.util.Set;

import static net.thenextlvl.worlds.command.WorldCommand.worldArgument;
import static org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.COMMAND;

@NullMarked
final class WorldRecreateCommand extends OptionCommand {
    private WorldRecreateCommand(final WorldsPlugin plugin) {
        super(plugin, "recreate", "worlds.command.recreate");
    }

    public static ArgumentBuilder<CommandSourceStack, ?> create(final WorldsPlugin plugin) {
        final var command = new WorldRecreateCommand(plugin);
        return command.create().then(command.createCommand());
    }

    @Override
    protected RequiredArgumentBuilder<CommandSourceStack, ?> createCommand() {
        final var key = Commands.argument("key", new KeyArgument());

        addOptions(key, false, Set.of(
                new Option("bonus-chest", BoolArgumentType.bool()),
                new Option("hardcore", BoolArgumentType.bool()),
                new Option("dimension", new DimensionArgumentType(plugin)),
                new Option("seed", new SeedArgument()),
                new Option("structures", BoolArgumentType.bool())
        ), null);

        return worldArgument(plugin).then(key.executes(this));
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();
        final var world = context.getArgument("world", World.class);
        final var key = context.getArgument("key", Key.class);

        final var builder = Level.copy(world);

        tryGetArgument(context, "bonus-chest", Boolean.class).ifPresent(builder::bonusChest);
        tryGetArgument(context, "dimension", Dimension.class).ifPresent(builder::dimension);
        tryGetArgument(context, "generator", Generator.class).ifPresent(builder::generator);
        tryGetArgument(context, "hardcore", Boolean.class).ifPresent(builder::hardcore);
        tryGetArgument(context, "preset", Preset.class).map(GeneratorType.FLAT::with).ifPresent(builder::generatorType);
        tryGetArgument(context, "seed", Long.class).ifPresent(builder::seed);
        tryGetArgument(context, "structures", Boolean.class).ifPresent(builder::structures);
        tryGetArgument(context, "type", GeneratorType.class).ifPresent(builder::generatorType);

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
