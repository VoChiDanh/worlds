package net.thenextlvl.worlds.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
        final var loadedFolders = worlds.stream()
                .map(World::getWorldPath)
                .collect(Collectors.toSet());

        final var loaded = worlds.stream().map(world -> Component.text(world.key().asString())
                .hoverEvent(HoverEvent.showText(plugin.bundle().component("world.list.hover", sender,
                        Placeholder.parsed("world", world.key().asString()))))
                .clickEvent(ClickEvent.runCommand("/world teleport " + world.key().asString()))
        ).toList();
        final var unloaded = plugin.getWorldRegistry().entrySet()
                .map(Map.Entry::getKey)
                .filter(key -> plugin.getServer().getWorld(key) == null)
                .map(Key::asString)
                .sorted()
                .map(key -> {
                    return Component.text(key)
                            .hoverEvent(HoverEvent.showText(plugin.bundle().component("world.list.load.hover", sender,
                                    Placeholder.parsed("world", key))))
                            .clickEvent(ClickEvent.runCommand("/world load " + key));
                })
                .toList();
        final var managedFolders = plugin.levelView().listLevels();
        final var unimported = listUnimported(loadedFolders, managedFolders).stream()
                .sorted(Comparator.comparing(Path::toString))
                .map(path -> plugin.levelView().key(path).orElse(null))
                .filter(Objects::nonNull)
                .map(Key::asString)
                .map(key -> {
                    return Component.text(key)
                            .hoverEvent(HoverEvent.showText(plugin.bundle().component("world.list.import.hover", sender,
                                    Placeholder.parsed("world", key))))
                            .clickEvent(ClickEvent.runCommand("/world import " + key));
                }).toList();

        plugin.bundle().sendMessage(sender, "world.list",
                Placeholder.parsed("amount", String.valueOf(worlds.size())),
                Formatter.joining("worlds", loaded));
        if (!unloaded.isEmpty()) plugin.bundle().sendMessage(sender, "world.list.unloaded",
                Placeholder.parsed("amount", String.valueOf(unloaded.size())),
                Formatter.joining("worlds", unloaded));
        if (!unimported.isEmpty()) plugin.bundle().sendMessage(sender, "world.list.unimported",
                Placeholder.parsed("amount", String.valueOf(unimported.size())),
                Formatter.joining("worlds", unimported));
        return SINGLE_SUCCESS;
    }

    private Set<Path> listUnimported(final Set<Path> loadedFolders, final Set<Path> managedFolders) {
        return plugin.levelView().listLevelFolders().stream()
                .filter(path -> !loadedFolders.contains(path))
                .filter(path -> !managedFolders.contains(path))
                .collect(Collectors.toSet());
    }

    private Set<Path> listWorldFolders(final Path namespace) {
        try (final var paths = Files.list(namespace)) {
            return paths.filter(Files::isDirectory).collect(Collectors.toSet());
        } catch (final IOException e) {
            return Set.of();
        }
    }
}
