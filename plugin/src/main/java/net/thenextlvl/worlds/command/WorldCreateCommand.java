package net.thenextlvl.worlds.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
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
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.argument.DimensionArgumentType;
import net.thenextlvl.worlds.command.argument.GeneratorArgument;
import net.thenextlvl.worlds.command.argument.GeneratorTypeArgument;
import net.thenextlvl.worlds.command.argument.KeyArgument;
import net.thenextlvl.worlds.command.argument.SeedArgument;
import net.thenextlvl.worlds.command.argument.WorldPresetArgument;
import net.thenextlvl.worlds.command.brigadier.OptionCommand;
import net.thenextlvl.worlds.generator.GeneratorType;
import net.thenextlvl.worlds.generator.Generator;
import net.thenextlvl.worlds.preset.Preset;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Set;

import static net.thenextlvl.worlds.view.PaperLevelView.createKey;
import static org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.COMMAND;

@NullMarked
final class WorldCreateCommand extends OptionCommand {
    private WorldCreateCommand(final WorldsPlugin plugin) {
        super(plugin, "create", "worlds.command.create");
    }

    public static ArgumentBuilder<CommandSourceStack, ?> create(final WorldsPlugin plugin) {
        final var command = new WorldCreateCommand(plugin);
        return command.create().then(command.createCommand());
    }

    @Override
    protected RequiredArgumentBuilder<CommandSourceStack, ?> createCommand() {
        final var name = Commands.argument("name", StringArgumentType.string());

        addOptions(name, true, Set.of(
                new Option("generator", new GeneratorArgument(plugin)),
                new Option("preset", new WorldPresetArgument(plugin)),
                new Option("type", new GeneratorTypeArgument(plugin))
        ), builder -> addOptions(builder, false, Set.of(
                new Option("bonus-chest", BoolArgumentType.bool()),
                new Option("hardcore", BoolArgumentType.bool()),
                new Option("dimension", new DimensionArgumentType(plugin)),
                new Option("key", new KeyArgument()),
                new Option("seed", new SeedArgument()),
                new Option("structures", BoolArgumentType.bool())
        ), null));

        return name.executes(this);
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();
        final var level = buildLevel(context, sender);
        if (level == null) return 0;
        final var placeholder = Placeholder.parsed("world", level.getName());

        plugin.bundle().sendMessage(sender, "world.create", placeholder);
        level.create().thenAccept(world -> {
            plugin.getWorldRegistry().register(level, true);
            plugin.bundle().sendMessage(sender, "world.create.success", placeholder);
            if (!(sender instanceof final Entity entity)) return;
            entity.teleportAsync(world.getSpawnLocation(), COMMAND);
        }).exceptionally(throwable -> {
            CommandFailureHandler.handle(plugin, sender, throwable, placeholder);
            return null;
        });
        return SINGLE_SUCCESS;
    }

    private @Nullable Level buildLevel(final CommandContext<CommandSourceStack> context, final CommandSender sender) {
        final var name = context.getArgument("name", String.class);
        if (Path.of(name).getNameCount() != 1) {
            plugin.bundle().sendMessage(sender, "world.container.create");
            return null;
        } else try {
            final var key = tryGetArgument(context, "key", Key.class)
                    .orElseGet(() -> plugin.levelView().findFreeKey(Key.key("worlds", createKey(name))));
            return Level.builder(key)
                    .dimension(tryGetArgument(context, "dimension", Dimension.class).orElse(null))
                    .generator(tryGetArgument(context, "generator", Generator.class).orElse(null))
                    .preset(tryGetArgument(context, "preset", Preset.class).orElse(null))
                    .seed(tryGetArgument(context, "seed", Long.class).orElse(null))
                    .structures(tryGetArgument(context, "structures", Boolean.class).orElse(null))
                    .generatorType(tryGetArgument(context, "type", GeneratorType.class).orElse(null))
                    .bonusChest(tryGetArgument(context, "bonus-chest", Boolean.class).orElse(null))
                    .hardcore(tryGetArgument(context, "hardcore", Boolean.class).orElse(null))
                    .name(name)
                    .build();
        } catch (final Exception e) {
            CommandFailureHandler.handle(plugin, sender, e, Placeholder.parsed("world", name));
            return null;
        }
    }
}
