package net.thenextlvl.worlds.command.backup;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

import static net.thenextlvl.worlds.command.WorldCommand.worldArgument;

@NullMarked
final class WorldBackupListCommand extends SimpleCommand {
    private WorldBackupListCommand(final WorldsPlugin plugin) {
        super(plugin, "list", "worlds.command.backup.list");
    }

    public static ArgumentBuilder<CommandSourceStack, ?> create(final WorldsPlugin plugin) {
        final var command = new WorldBackupListCommand(plugin);
        return command.create().then(worldArgument(plugin).executes(command));
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var world = context.getArgument("world", World.class);
        plugin.getBackupProvider().listBackups(world).thenAccept(backups -> {
            final var messages = backups.map(backup -> {
                final var bytes = backup.size();
                final var kb = bytes / 1024d;
                final var mb = kb / 1024d;
                final var gb = mb / 1024d;

                final var seconds = (System.currentTimeMillis() - backup.createdAt().toEpochMilli()) / 1000;
                final var minutes = seconds / 60;
                final var hours = minutes / 60;
                final var days = hours / 24;
                final var weeks = days / 7;
                final var months = weeks / 4;
                final var years = months / 12;

                return plugin.bundle().component("world.backup.info", context.getSource().getSender(),
                        Placeholder.parsed("world", world.key().asString()),
                        Placeholder.parsed("identifier", backup.name()),
                        Formatter.number("size", gb >= 1 ? gb : mb >= 1 ? mb : kb >= 1 ? kb : bytes),
                        Formatter.choice("unit", gb >= 1 ? 0 : mb >= 1 ? 1 : kb >= 1 ? 2 : 3),
                        Formatter.number("time", years >= 1 ? years : months >= 1 ? months : weeks >= 1 ? weeks : days >= 1 ? days : hours >= 1 ? hours : minutes >= 1 ? minutes : seconds),
                        Formatter.choice("timeunit", years >= 1 ? 0 : months >= 1 ? 1 : weeks >= 1 ? 2 : days >= 1 ? 3 : hours >= 1 ? 4 : minutes >= 1 ? 5 : 6));
            }).toList();
            final var message = messages.isEmpty() ? "world.backup.list.empty" : "world.backup.list";
            plugin.bundle().sendMessage(context.getSource().getSender(), message,
                    Placeholder.parsed("world", world.getName()),
                    Formatter.number("amount", messages.size()),
                    Formatter.booleanChoice("singular", messages.size() == 1),
                    Formatter.joining("backups", messages));
        });
        return SINGLE_SUCCESS;
    }
}
