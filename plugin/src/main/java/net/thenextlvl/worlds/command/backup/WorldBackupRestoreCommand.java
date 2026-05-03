package net.thenextlvl.worlds.command.backup;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.worlds.OperationScheduler;
import net.thenextlvl.worlds.WorldOperationException;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.CommandFailureHandler;
import net.thenextlvl.worlds.command.argument.CommandFlagsArgument;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import net.thenextlvl.worlds.command.suggestion.BackupSuggestionProvider;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static net.thenextlvl.worlds.event.WorldActionScheduledEvent.ActionType.RESTORE_BACKUP;
import static net.thenextlvl.worlds.command.WorldCommand.worldArgument;

@NullMarked
final class WorldBackupRestoreCommand extends SimpleCommand {
    private WorldBackupRestoreCommand(final WorldsPlugin plugin) {
        super(plugin, "restore", "worlds.command.backup.restore");
    }

    public static ArgumentBuilder<CommandSourceStack, ?> create(final WorldsPlugin plugin) {
        final var command = new WorldBackupRestoreCommand(plugin);
        return command.create().then(command.restore());
    }

    private RequiredArgumentBuilder<CommandSourceStack, World> restore() {
        return worldArgument(plugin)
                .then(x(Commands.argument("backup", StringArgumentType.string())
                        .suggests(new BackupSuggestionProvider(plugin))))
                .then(x(Commands.literal("latest")));
    }

    private ArgumentBuilder<CommandSourceStack, ?> x(final ArgumentBuilder<CommandSourceStack, ?> command) {
        return command.then(Commands.argument("flags", new CommandFlagsArgument(
                        Set.of("--confirm", "--schedule")
                )).executes(this))
                .executes(this::confirmationNeeded);
    }

    private int confirmationNeeded(final CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();
        plugin.bundle().sendMessage(sender, "command.confirmation",
                Placeholder.parsed("action", "/" + context.getInput()),
                Placeholder.parsed("confirmation", "/" + context.getInput() + " --confirm"));
        return 0;
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var flags = context.getArgument("flags", CommandFlagsArgument.Flags.class);
        if (!flags.contains("--confirm") && !flags.contains("--schedule")) return confirmationNeeded(context);
        final var world = context.getArgument("world", World.class);
        final var name = tryGetArgument(context, "backup", String.class);
        final var schedule = flags.contains("--schedule");
        if (!schedule) plugin.bundle().sendMessage(context.getSource().getSender(), "world.backup.restore",
                Placeholder.parsed("world", world.key().asString()));

        final var resolved = name
                .map(value -> plugin.getBackupProvider().findBackup(world, value))
                .orElseGet(() -> plugin.getBackupProvider().findBackup(world));

        resolved.thenAccept(optional -> {
            final var backup = optional.orElse(null);

            if (backup == null) {
                plugin.bundle().sendMessage(context.getSource().getSender(), "world.backup.list.empty",
                        Placeholder.parsed("world", world.key().asString()));
                return;
            }

            if (schedule && plugin.getScheduler().cancel(world.key(), RESTORE_BACKUP)) {
                plugin.bundle().sendMessage(context.getSource().getSender(), "world.backup.restore.schedule-cancelled",
                        Placeholder.parsed("world", world.key().asString()),
                        Placeholder.parsed("backup", backup.name()));
                return;
            }

            final var future = !schedule ? plugin.restoreBackup(world, backup).thenApply(ignored -> true)
                    : CompletableFuture.completedFuture(plugin.getScheduler().schedule(
                    new OperationScheduler.BackupRestoreOperation(world.key(), backup.name())
            ));
            future.thenAccept(success -> {
                if (success) {
                    final var message = schedule ? "world.backup.restore.scheduled" : "world.backup.restore.success";
                    plugin.bundle().sendMessage(context.getSource().getSender(), message,
                            Placeholder.parsed("world", world.key().asString()),
                            Placeholder.parsed("backup", backup.name()));
                } else CommandFailureHandler.handle(plugin, context.getSource().getSender(),
                        new WorldOperationException(WorldOperationException.Reason.EVENT_CANCELLED));
            }).exceptionally(throwable -> {
                CommandFailureHandler.handle(plugin, context.getSource().getSender(), throwable,
                        Placeholder.parsed("world", world.key().asString()),
                        Placeholder.parsed("backup", backup.name()));
                return null;
            });
        });
        return SINGLE_SUCCESS;
    }
}
