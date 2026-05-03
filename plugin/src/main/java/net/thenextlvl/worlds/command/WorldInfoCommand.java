package net.thenextlvl.worlds.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static net.thenextlvl.worlds.command.WorldCommand.worldArgument;

@NullMarked
final class WorldInfoCommand extends SimpleCommand {
    private WorldInfoCommand(final WorldsPlugin plugin) {
        super(plugin, "info", "worlds.command.info");
    }

    public static ArgumentBuilder<CommandSourceStack, ?> create(final WorldsPlugin plugin) {
        final var command = new WorldInfoCommand(plugin);
        return command.create()
                .then(worldArgument(plugin).executes(command))
                .executes(command);
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();
        final var world = tryGetArgument(context, "world", World.class)
                .orElseGet(() -> context.getSource().getLocation().getWorld());
        final var path = world.getWorldPath();

        final var rows = new ArrayList<InfoRow>();
        rows.add(new InfoRow("world.info.name", Placeholder.parsed("name", world.getName())));
        rows.add(new InfoRow("world.info.players", Formatter.number("players", world.getPlayers().size())));
        rows.add(new InfoRow("world.info.type", Placeholder.parsed("type", plugin.handler().getGeneratorType(world).name())));
        rows.add(new InfoRow("world.info.dimension",
                Placeholder.parsed("dimension", WorldListCommand.displayDimension(plugin.handler().getDimension(world)))));
        plugin.handler().getGenerator(world).ifPresent(generator -> rows.add(new InfoRow(
                "world.info.generator", Placeholder.parsed("generator", generator.getName()))));
        rows.add(new InfoRow("world.info.seed", Placeholder.parsed("seed", String.valueOf(world.getSeed()))));
        try {
            final var bytes = getSize(path);
            final var kb = bytes / 1024d;
            final var mb = kb / 1024d;
            final var gb = mb / 1024d;
            rows.add(new InfoRow("world.info.size",
                    Formatter.number("size", gb >= 1 ? gb : mb >= 1 ? mb : kb >= 1 ? kb : bytes),
                    Formatter.choice("unit", gb >= 1 ? 0 : mb >= 1 ? 1 : kb >= 1 ? 2 : 3)));
        } catch (final IOException e) {
            plugin.getComponentLogger().warn("Failed to get world size for {}", world.key(), e);
        }

        final var worldKey = world.key().asString();
        plugin.bundle().sendMessage(sender, "world.info.section", Placeholder.parsed("world", worldKey));
        for (var index = 0; index < rows.size(); index++) {
            final var row = rows.get(index);
            sendInfoRow(sender, row, index == rows.size() - 1);
        }
        return SINGLE_SUCCESS;
    }

    private void sendInfoRow(final CommandSender sender, final InfoRow row, final boolean last) {
        final var placeholders = new TagResolver[row.resolvers.length + 1];
        placeholders[0] = Placeholder.parsed("tree", last ? "└" : "├");
        System.arraycopy(row.resolvers, 0, placeholders, 1, row.resolvers.length);
        plugin.bundle().sendMessage(sender, row.key, placeholders);
    }

    private record InfoRow(String key, TagResolver... resolvers) {
    }

    private long getSize(final Path path) throws IOException {
        final var size = new AtomicLong(0);
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, @Nullable final IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
        return size.get();
    }
}
