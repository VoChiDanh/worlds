package net.thenextlvl.worlds.command.backup;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.thenextlvl.worlds.Backup;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.argument.DurationArgumentType;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@NullMarked
final class WorldBackupPruneCommand extends SimpleCommand {
    private WorldBackupPruneCommand(final WorldsPlugin plugin) {
        super(plugin, "prune", "worlds.command.backup.prune");
    }

    public static ArgumentBuilder<CommandSourceStack, ?> create(final WorldsPlugin plugin) {
        final var command = new WorldBackupPruneCommand(plugin);
        final var age = Commands.argument("age", DurationArgumentType.duration(Duration.ofMinutes(1)));
        return command.create().then(age.executes(command));
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();
        final var age = context.getArgument("age", Duration.class);

        final var threshold = Instant.now().minus(age);
        plugin.getBackupProvider().listBackups().thenAccept(backups -> {
            final var expired = backups
                    .filter(backup -> backup.createdAt().isBefore(threshold))
                    .toList();
            if (expired.isEmpty()) {
                plugin.bundle().sendMessage(sender, "world.backup.prune.empty", duration(age));
                return;
            }
            prune(sender, age, expired);
        });
        return SINGLE_SUCCESS;
    }

    private void prune(final CommandSender sender, final Duration age, final List<Backup> backups) {
        final var futures = backups.stream()
                .map(backup -> backup.provider().delete(backup))
                .toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
            final var deleted = futures.stream()
                    .filter(CompletableFuture::join)
                    .count();
            plugin.bundle().sendMessage(sender, "world.backup.prune.success", duration(age,
                    Formatter.number("amount", deleted),
                    Formatter.booleanChoice("singular", deleted == 1)));
        });
    }

    private TagResolver[] duration(final Duration duration, final TagResolver... resolvers) {
        final var seconds = Math.max(0, duration.toSeconds());
        final var minutes = seconds / 60;
        final var hours = minutes / 60;
        final var days = hours / 24;
        final var weeks = days / 7;
        final var months = weeks / 4;
        final var years = months / 12;

        final var combined = new TagResolver[resolvers.length + 2];
        combined[0] = Formatter.number("time", years >= 1 ? years : months >= 1 ? months : weeks >= 1 ? weeks : days >= 1 ? days : hours >= 1 ? hours : minutes >= 1 ? minutes : seconds);
        combined[1] = Formatter.choice("timeunit", years >= 1 ? 0 : months >= 1 ? 1 : weeks >= 1 ? 2 : days >= 1 ? 3 : hours >= 1 ? 4 : minutes >= 1 ? 5 : 6);
        System.arraycopy(resolvers, 0, combined, 2, resolvers.length);
        return combined;
    }
}
