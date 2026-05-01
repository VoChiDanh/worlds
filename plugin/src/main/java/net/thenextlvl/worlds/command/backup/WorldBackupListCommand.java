package net.thenextlvl.worlds.command.backup;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.thenextlvl.worlds.Backup;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static net.thenextlvl.worlds.command.WorldCommand.worldArgument;

@NullMarked
final class WorldBackupListCommand extends SimpleCommand {
    private static final int MAX_BACKUPS_PER_WORLD = 50;
    private static final int MAX_BACKUPS_PER_WORLD_OVERVIEW = 5;

    private WorldBackupListCommand(final WorldsPlugin plugin) {
        super(plugin, "list", "worlds.command.backup.list");
    }

    public static ArgumentBuilder<CommandSourceStack, ?> create(final WorldsPlugin plugin) {
        final var command = new WorldBackupListCommand(plugin);
        return command.create()
                .then(worldArgument(plugin).executes(command))
                .executes(command);
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();
        final var world = tryGetArgument(context, "world", World.class);
        final var future = world
                .map(value -> plugin.getBackupProvider().listBackups(value))
                .orElseGet(() -> plugin.getBackupProvider().listBackups());
        future.thenAccept(backups -> sendBackups(sender, backups
                .map(BackupListEntry::new)
                .sorted()
                .toList(), world.map(World::key).orElse(null)));
        return SINGLE_SUCCESS;
    }

    private void sendBackups(final CommandSender sender, final List<BackupListEntry> entries, @Nullable final Key world) {
        if (entries.isEmpty()) {
            plugin.bundle().sendMessage(sender, world == null ? "world.backup.list.empty.all" : "world.backup.list.empty",
                    Placeholder.parsed("world", world != null ? world.asString() : ""));
            return;
        }
        plugin.bundle().sendMessage(sender, world == null ? "world.backup.list.header" : "world.backup.list.header.world",
                Formatter.number("amount", entries.size()),
                Formatter.booleanChoice("singular", entries.size() == 1),
                Placeholder.parsed("world", world != null ? world.asString() : ""));
        entries.stream()
                .collect(Collectors.groupingBy(entry -> entry.key().namespace(), TreeMap::new, Collectors.toList()))
                .forEach((namespace, backups) -> {
                    plugin.bundle().sendMessage(sender, "world.backup.list.namespace",
                            Placeholder.parsed("namespace", namespace),
                            Formatter.number("amount", backups.size()));
                    backups.stream()
                            .collect(Collectors.groupingBy(entry -> entry.key().value(), TreeMap::new, Collectors.toList()))
                            .forEach((worldName, worldBackups) -> {
                                plugin.bundle().sendMessage(sender, "world.backup.list.world",
                                        Placeholder.parsed("world", worldName),
                                        Formatter.number("amount", worldBackups.size()));
                                final var limit = world == null ? MAX_BACKUPS_PER_WORLD_OVERVIEW : MAX_BACKUPS_PER_WORLD;
                                final var hidden = worldBackups.size() - limit;
                                final var visible = Math.min(worldBackups.size(), limit);
                                for (var index = 0; index < visible; index++) {
                                    final var backup = worldBackups.get(index);
                                    sender.sendMessage(backup.component(plugin, sender, hidden <= 0 && index == visible - 1));
                                }
                                if (hidden > 0) {
                                    plugin.bundle().sendMessage(sender, "world.backup.list.more",
                                            Formatter.number("amount", hidden),
                                            Formatter.booleanChoice("singular", hidden == 1));
                                }
                            });
                });
    }

    private record BackupListEntry(Backup backup) implements Comparable<BackupListEntry> {
        private static final Comparator<BackupListEntry> COMPARATOR = Comparator
                .comparing((BackupListEntry entry) -> entry.key().namespace())
                .thenComparing(entry -> entry.key().value())
                .thenComparing(BackupListEntry::createdAt, Comparator.reverseOrder())
                .thenComparing(entry -> entry.backup.name());

        private Component component(final WorldsPlugin plugin, final CommandSender sender, final boolean last) {
            final var world = key().asString();
            return plugin.bundle().component("world.backup.list.entry", sender, resolvers(last))
                    .hoverEvent(HoverEvent.showText(plugin.bundle().component("world.backup.list.hover", sender,
                            Placeholder.parsed("world", world),
                            Placeholder.parsed("identifier", backup.name()))))
                    .clickEvent(ClickEvent.suggestCommand("/world backup restore " + world + " " + backup.name()));
        }

        private TagResolver[] resolvers(final boolean last) {
            final var bytes = backup.size();
            final var kb = bytes / 1024d;
            final var mb = kb / 1024d;
            final var gb = mb / 1024d;

            final var seconds = Math.max(0, (System.currentTimeMillis() - backup.createdAt().toEpochMilli()) / 1000);
            final var minutes = seconds / 60;
            final var hours = minutes / 60;
            final var days = hours / 24;
            final var weeks = days / 7;
            final var months = weeks / 4;
            final var years = months / 12;

            return new TagResolver[]{
                    Placeholder.parsed("tree", last ? "└" : "├"),
                    Placeholder.parsed("identifier", backup.name()),
                    Formatter.number("size", gb >= 1 ? gb : mb >= 1 ? mb : kb >= 1 ? kb : bytes),
                    Formatter.choice("unit", gb >= 1 ? 0 : mb >= 1 ? 1 : kb >= 1 ? 2 : 3),
                    Formatter.number("time", years >= 1 ? years : months >= 1 ? months : weeks >= 1 ? weeks : days >= 1 ? days : hours >= 1 ? hours : minutes >= 1 ? minutes : seconds),
                    Formatter.choice("timeunit", years >= 1 ? 0 : months >= 1 ? 1 : weeks >= 1 ? 2 : days >= 1 ? 3 : hours >= 1 ? 4 : minutes >= 1 ? 5 : 6),
            };
        }

        private Key key() {
            return backup.key();
        }

        private Instant createdAt() {
            return backup.createdAt();
        }

        @Override
        public int compareTo(final BackupListEntry other) {
            return COMPARATOR.compare(this, other);
        }
    }
}
