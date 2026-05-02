package net.thenextlvl.worlds.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
    public int run(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var sender = context.getSource().getSender();
        final var worlds = plugin.getServer().getWorlds();

        final var entries = new ArrayList<WorldListEntry>();
        worlds.forEach(world -> entries.add(new WorldListEntry(world.key(), plugin.handler().getDimension(world), State.LOADED)));
        plugin.getWorldRegistry().entrySet()
                .filter(entry -> plugin.getServer().getWorld(entry.getKey()) == null)
                .forEach(entry -> entries.add(new WorldListEntry(entry.getKey(), entry.getValue().dimension(), State.UNLOADED)));

        listUnimported(worlds.stream().map(World::getWorldPath).toList(), plugin.listLevels().toList())
                .map(path -> plugin.levelView().key(path).orElse(null))
                .filter(Objects::nonNull)
                .forEach(key -> entries.add(new WorldListEntry(key, null, State.UNIMPORTED)));

        plugin.bundle().sendMessage(sender, "world.list.header",
                Placeholder.parsed("worlds", String.valueOf(count(entries, State.LOADED))),
                Placeholder.parsed("unloaded", String.valueOf(count(entries, State.UNLOADED))),
                Placeholder.parsed("unimported", String.valueOf(count(entries, State.UNIMPORTED))));
        entries.stream()
                .sorted()
                .collect(Collectors.groupingBy(entry -> entry.key().namespace(), TreeMap::new, Collectors.toList()))
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

    private Stream<Path> listUnimported(final List<Path> loadedFolders, final List<Path> managedFolders) {
        return plugin.levelView().listLevelFolders().stream()
                .filter(path -> !loadedFolders.contains(path))
                .filter(path -> !managedFolders.contains(path));
    }

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
                                  State state) implements Comparable<WorldListEntry> {
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
                    .clickEvent(ClickEvent.suggestCommand(state.command + key + suffix));
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
