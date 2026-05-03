package net.thenextlvl.worlds.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.argument.CommandOptionsArgument;
import net.thenextlvl.worlds.command.argument.KeyArgument;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

import static net.thenextlvl.worlds.command.WorldCommand.worldArgument;
import static org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.COMMAND;

@NullMarked
final class WorldCloneCommand extends SimpleCommand {
    private WorldCloneCommand(final WorldsPlugin plugin) {
        super(plugin, "clone", "worlds.command.clone");
    }

    public static ArgumentBuilder<CommandSourceStack, ?> create(final WorldsPlugin plugin) {
        final var command = new WorldCloneCommand(plugin);
        return command.create().then(command.createCommand());
    }

    private RequiredArgumentBuilder<CommandSourceStack, ?> createCommand() {
        final var options = Commands.argument("options", new CommandOptionsArgument(Map.of(
                "full", BoolArgumentType.bool(),
                "key", new KeyArgument()
        ))).executes(this);

        return worldArgument(plugin).then(options).executes(this);
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var world = context.getArgument("world", World.class);
        final var options = tryGetArgument(context, "options", CommandOptionsArgument.Options.class)
                .orElseGet(CommandOptionsArgument.Options::new);

        final var sender = context.getSource().getSender();
        final var placeholder = Placeholder.parsed("world", world.key().asString());

        plugin.bundle().sendMessage(sender, "world.clone", placeholder);
        plugin.clone(world, builder -> {
            options.getArgument("key", Key.class).ifPresent(builder::key);
        }, options.getArgument("full", Boolean.class).orElse(true)).thenAccept(clone -> {
            if (sender instanceof final Player player) player.teleportAsync(clone.getSpawnLocation(), COMMAND);
            plugin.bundle().sendMessage(sender, "world.clone.success", placeholder);
        }).exceptionally(throwable -> {
            CommandFailureHandler.handle(plugin, sender, throwable, placeholder);
            return null;
        });
        return SINGLE_SUCCESS;
    }
}
