package net.thenextlvl.worlds.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.thenextlvl.worlds.Dimension;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import net.thenextlvl.worlds.view.PaperLevelView;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NullMarked
final class WorldListCommand extends SimpleCommand {
    private WorldListCommand(final WorldsPlugin plugin) {
        super(plugin, "list", "worlds.command.list");
    }

    public static ArgumentBuilder<CommandSourceStack, ?> create(final WorldsPlugin plugin) {
        final var command = new WorldListCommand(plugin);
        return command.create().executes(command);
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();
        final var worlds = plugin.getServer().getWorlds();

        final var entries = new ArrayList<WorldListEntry>();
        worlds.forEach(world -> entries.add(new WorldListEntry(world.key(), plugin.handler().getDimension(world), State.LOADED, null)));
        plugin.getWorldRegistry().entrySet()
                .filter(entry -> plugin.getServer().getWorld(entry.getKey()) == null)
                .forEach(entry -> entries.add(new WorldListEntry(entry.getKey(), entry.getValue().dimension(), State.UNLOADED, null)));

        listUnimported(
                worlds.stream().map(World::getWorldPath).map(path -> path.toAbsolutePath().normalize()).toList(),
                plugin.listLevels().map(path -> path.toAbsolutePath().normalize()).toList()
        ).map(path -> unimportedEntry(path).orElse(null))
                .filter(Objects::nonNull)
                .forEach(entries::add);

        plugin.bundle().sendMessage(sender, "world.list.header",
                Placeholder.parsed("worlds", String.valueOf(count(entries, State.LOADED))),
                Placeholder.parsed("unloaded", String.valueOf(count(entries, State.UNLOADED))),
                Placeholder.parsed("unimported", String.valueOf(count(entries, State.UNIMPORTED))));
        entries.stream()
                .sorted()
                .collect(Collectors.groupingBy(this::namespace, TreeMap::new, Collectors.toList()))
                .forEach((key, value) -> {
                    plugin.bundle().sendMessage(sender, "world.list.namespace",
                            Placeholder.parsed("namespace", key),
                            Placeholder.parsed("amount", String.valueOf(value.size())));
                    for (var index = 0; index < value.size(); index++) {
                        final var world = value.get(index);
                        sender.sendMessage(world.component(plugin, sender, index == value.size() - 1));
                    }
                });
        return SINGLE_SUCCESS;
    }

    private String namespace(final WorldListEntry entry) {
        if (!entry.state().equals(State.UNIMPORTED) || entry.importPath() == null || isModernLevelPath(entry.importPath())
                || isLegacyWorld(entry.importPath()))
            return entry.key().namespace();
        final var root = plugin.getServer().getWorldContainer().toPath().toAbsolutePath().normalize();
        final var parent = entry.importPath().toAbsolutePath().normalize().getParent();
        if (parent == null || parent.equals(root)) return ".";
        return root.relativize(parent).toString();
    }

    private Stream<Path> listUnimported(final List<Path> loadedFolders, final List<Path> managedFolders) {
        return Stream.concat(plugin.levelView().listLevelFolders().stream(), listRootLevelFolders())
                .map(path -> path.toAbsolutePath().normalize())
                .filter(path -> !loadedFolders.contains(path))
                .filter(path -> !managedFolders.contains(path));
    }

    // todo: sorry future me but you have to clean up this mess :)
    private Stream<Path> listRootLevelFolders() {
        final var root = plugin.getServer().getWorldContainer().toPath();
        final var serverLevel = plugin.getServer().getLevelDirectory().toAbsolutePath().normalize();
        final var dimensions = plugin.getDimensionsRoot().toAbsolutePath().normalize();
        try (final var paths = Files.walk(root)) {
            return paths.filter(Files::isDirectory)
                    .filter(path -> !path.equals(root))
                    .filter(path -> {
                        final var normalized = path.toAbsolutePath().normalize();
                        return !normalized.equals(serverLevel) && !normalized.startsWith(dimensions);
                    })
                    .filter(this::looksLikeWorld)
                    .toList().stream();
        } catch (final IOException e) {
            return Stream.empty();
        }
    }

    private boolean looksLikeWorld(final Path path) {
        return Files.isDirectory(path.resolve("region"))
                || Files.isRegularFile(path.resolve("level.dat"))
                || Files.isRegularFile(path.resolve("level.dat_old"));
    }

    private Optional<WorldListEntry> unimportedEntry(final Path path) {
        if (isLegacyWorld(path)) return legacyEntry(path);
        final var key = key(path).orElse(null);
        if (key != null) return plugin.getWorldRegistry().isRegistered(key)
                ? Optional.empty()
                : Optional.of(new WorldListEntry(key, null, State.UNIMPORTED, isModernLevelPath(path) ? null : path));
        return Optional.ofNullable(path.getFileName())
                .map(Path::toString)
                .map(PaperLevelView::createKey)
                .filter(value -> !value.isBlank())
                .map(value -> plugin.levelView().findFreeKey("worlds", value))
                .map(fallback -> new WorldListEntry(fallback, null, State.UNIMPORTED, path));
    }

    private Optional<WorldListEntry> legacyEntry(final Path path) {
        return plugin.legacyWorldRegistry().read(path)
                .filter(data -> !plugin.getWorldRegistry().isRegistered(data.key()))
                .map(data -> new WorldListEntry(data.key(), null, State.UNIMPORTED, path));
    }

    private Optional<Key> key(final Path path) {
        final var root = plugin.getServer().getWorldContainer().toPath().toAbsolutePath().normalize();
        final var absolute = path.toAbsolutePath().normalize();
        final var relative = absolute.startsWith(root) ? root.relativize(absolute) : absolute;
        return plugin.levelView().key(absolute)
                .or(() -> plugin.levelView().lenientKey(absolute))
                .or(() -> plugin.levelView().lenientKey(relative))
                .or(() -> plugin.levelView().lenientKey(lastTwoSegments(absolute)));
    }

    private boolean isLegacyWorld(final Path path) {
        return Files.isRegularFile(path.resolve("level.dat"))
                || Files.isRegularFile(path.resolve("level.dat_old"));
    }

    private Path lastTwoSegments(final Path path) {
        final var count = path.getNameCount();
        return count > 2 ? path.subpath(count - 2, count) : path;
    }

    private boolean isModernLevelPath(final Path path) {
        return path.toAbsolutePath().normalize().startsWith(plugin.getDimensionsRoot().toAbsolutePath().normalize());
    }
    // todo: cleanup end

    private long count(final Iterable<WorldListEntry> entries, final State state) {
        var count = 0;
        for (final var entry : entries) {
            if (entry.state == state) count++;
        }
        return count;
    }

    static String displayDimension(final Dimension dimension) {
        final var key = dimension.key();
        if (key.equals(Dimension.OVERWORLD.key())) return "normal";
        if (key.equals(Dimension.THE_NETHER.key())) return "nether";
        if (key.equals(Dimension.THE_END.key())) return "the_end";
        return key.asString();
    }

    private enum State {
        LOADED("world.list.loaded", "world.list.hover", "/world teleport "),
        UNLOADED("world.list.unloaded", "world.list.load.hover", "/world load "),
        UNIMPORTED("world.list.unimported", "world.list.import.hover", "/world import ");

        private final String translationKey;
        private final String hoverKey;
        private final String command;

        State(final String translationKey, final String hoverKey, final String command) {
            this.translationKey = translationKey;
            this.hoverKey = hoverKey;
            this.command = command;
        }
    }

    private record WorldListEntry(Key key, @Nullable Dimension dimension,
                                  State state, @Nullable Path importPath) implements Comparable<WorldListEntry> {
        private static final Comparator<WorldListEntry> COMPARATOR = Comparator
                .comparing((WorldListEntry entry) -> entry.key.namespace())
                .thenComparing(entry -> entry.state)
                .thenComparingInt(WorldListEntry::dimensionOrder)
                .thenComparing(entry -> entry.dimension != null ? entry.dimension.key().asString() : "")
                .thenComparing(entry -> entry.key.value());

        private Component component(final WorldsPlugin plugin, final CommandSender sender, final boolean last) {
            final var key = key().asString();
            final var placeholders = dimension != null
                    ? new TagResolver[]{
                    Placeholder.parsed("tree", last ? "└" : "├"),
                    Placeholder.component("world", label()),
                    Placeholder.parsed("dimension", WorldListCommand.displayDimension(dimension)),
            } : new TagResolver[]{
                    Placeholder.parsed("tree", last ? "└" : "├"),
                    Placeholder.component("world", label()),
            };
            final var suffix = state.equals(State.UNIMPORTED) ? " " : "";
            return plugin.bundle().component(state.translationKey, sender, placeholders)
                    .hoverEvent(HoverEvent.showText(plugin.bundle().component(state.hoverKey, sender,
                            Placeholder.parsed("world", key))))
                    .clickEvent(ClickEvent.suggestCommand(command(plugin) + suffix));
        }

        private String command(final WorldsPlugin plugin) {
            if (!state.equals(State.UNIMPORTED) || importPath == null) return state.command + key().asString();
            final var root = plugin.getServer().getWorldContainer().toPath().toAbsolutePath().normalize();
            final var path = root.relativize(importPath.toAbsolutePath().normalize()).toString();
            final var command = new StringBuilder(state.command)
                    .append("\"")
                    .append(path.replace("\\", "\\\\").replace("\"", "\\\""))
                    .append("\" ")
                    .append(key().asString());
            plugin.legacyWorldRegistry().read(importPath).ifPresent(data -> {
                command.append(" dimension ").append(data.dimension().key().asString());
                final var generator = data.generator();
                if (generator != null) command.append(" generator ").append(generator);
            });
            return command.toString();
        }

        private Component label() {
            return Component.text(key.value());
        }

        @Override
        public int compareTo(final WorldListEntry other) {
            return COMPARATOR.compare(this, other);
        }

        private int dimensionOrder() {
            if (dimension == null) return Integer.MAX_VALUE;
            final var key = dimension.key();
            if (key.equals(Dimension.OVERWORLD.key())) return 0;
            if (key.equals(Dimension.THE_NETHER.key())) return 1;
            if (key.equals(Dimension.THE_END.key())) return 2;
            return 3;
        }
    }
}
