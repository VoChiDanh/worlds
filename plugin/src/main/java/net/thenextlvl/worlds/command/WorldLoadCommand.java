package net.thenextlvl.worlds.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.worlds.Level;
import net.thenextlvl.worlds.WorldOperationException;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.argument.KeyArgument;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import net.thenextlvl.worlds.command.suggestion.WorldLoadSuggestionProvider;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;

import static org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.COMMAND;

@NullMarked
final class WorldLoadCommand extends SimpleCommand {
    private WorldLoadCommand(final WorldsPlugin plugin) {
        super(plugin, "load", "worlds.command.load");
    }

    public static ArgumentBuilder<CommandSourceStack, ?> create(final WorldsPlugin plugin) {
        final var command = new WorldLoadCommand(plugin);
        return command.create().then(command.load());
    }

    private RequiredArgumentBuilder<CommandSourceStack, Key> load() {
        return Commands.argument("key", new KeyArgument())
                .suggests(new WorldLoadSuggestionProvider(plugin)).executes(this);
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();
        final var key = context.getArgument("key", Key.class);
        final var placeholder = Placeholder.parsed("world", key.asString());

        plugin.bundle().sendMessage(sender, "world.load", placeholder);

        // if (!plugin.getWorldRegistry().isRegistered(key)) return 0; // todo: deny loading worlds that have not been imported
        
        final var build = plugin.levelView().read(key).map(Level.Builder::build);
        final var future = build.map(Level::create).orElse(null);

        if (future == null) {
            sendFailure(sender, WorldOperationException.Reason.MISSING_LEVEL_STEM, key);
            return 0;
        }

        future.thenAccept(level -> {
            plugin.getWorldRegistry().setEnabled(level.key(), true);
            plugin.bundle().sendMessage(sender, "world.load.success", Placeholder.parsed("world", level.getName()));
            if (!(sender instanceof final Entity entity)) return;
            entity.teleportAsync(level.getSpawnLocation(), COMMAND);
        }).exceptionally(throwable -> {
            CommandFailureHandler.handle(plugin, sender, throwable, placeholder);
            return null;
        });
        return SINGLE_SUCCESS;
    }

    private void sendFailure(
            final CommandSender sender,
            final WorldOperationException.Reason reason,
            final Key key
    ) {
        CommandFailureHandler.handle(plugin, sender, new WorldOperationException(reason)
                .key(key).world(key.asString()));
    }
}
