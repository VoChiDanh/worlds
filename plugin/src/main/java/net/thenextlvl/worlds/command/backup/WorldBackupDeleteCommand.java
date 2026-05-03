package net.thenextlvl.worlds.command.backup;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import net.thenextlvl.worlds.command.suggestion.BackupSuggestionProvider;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

import static net.thenextlvl.worlds.command.WorldCommand.worldArgument;

@NullMarked
final class WorldBackupDeleteCommand extends SimpleCommand {
    private WorldBackupDeleteCommand(final WorldsPlugin plugin) {
        super(plugin, "delete", "worlds.command.backup.delete");
    }

    public static ArgumentBuilder<CommandSourceStack, ?> create(final WorldsPlugin plugin) {
        final var command = new WorldBackupDeleteCommand(plugin);
        return command.create().then(worldArgument(plugin)
                .then(Commands.argument("backup", StringArgumentType.string())
                        .suggests(new BackupSuggestionProvider(plugin))
                        .executes(command)));
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var world = context.getArgument("world", World.class);
        final var backup = context.getArgument("backup", String.class);
        plugin.getBackupProvider().delete(world, backup).thenAccept(success -> {
            final var message = success ? "world.backup.delete.success" : "world.backup.delete.failed";
            plugin.bundle().sendMessage(context.getSource().getSender(), message,
                    Placeholder.parsed("world", world.key().asString()),
                    Placeholder.parsed("backup", backup));
        });
        return SINGLE_SUCCESS;
    }
}
