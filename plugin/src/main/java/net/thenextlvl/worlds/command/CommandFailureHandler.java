package net.thenextlvl.worlds.command;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.thenextlvl.worlds.WorldOperationException;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.generator.GeneratorException;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletionException;

public final class CommandFailureHandler {
    private CommandFailureHandler() {
    }

    public static Throwable unwrap(final Throwable throwable) {
        var current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    public static void handle(
            final WorldsPlugin plugin,
            final CommandSender sender,
            final Throwable throwable,
            final TagResolver... placeholders
    ) {
        final var cause = unwrap(throwable);
        final var messageKey = messageKey(plugin, cause);
        if (messageKey.isPresent()) {
            plugin.bundle().sendMessage(sender, messageKey.orElseThrow(), merge(cause, placeholders));
        } else {
            plugin.getComponentLogger().error("Unhandled command failure", cause);
        }
    }

    public static Optional<String> messageKey(final WorldsPlugin plugin, final Throwable throwable) {
        if (throwable instanceof final WorldOperationException exception) {
            return Optional.of(exception.reason().translationKey());
        }
        if (throwable instanceof final GeneratorException exception) {
            return switch (exception.getMessage()) {
                case "Plugin not found" -> Optional.of("world.failure.generator-missing");
                case "Plugin is not enabled, is it 'load: STARTUP'?" -> Optional.of("world.failure.generator-disabled");
                case "Plugin has no generator" -> Optional.of("world.failure.generator-empty");
                default -> Optional.empty();
            };
        }
        if (plugin.handler().isDirectoryLockException(throwable)) return Optional.of("world.failure.directory-loaded");
        if (throwable instanceof IOException) return Optional.of("world.failure.filesystem");
        return Optional.empty();
    }

    private static TagResolver[] merge(final Throwable throwable, final TagResolver... placeholders) {
        final var exception = Optional.ofNullable(throwable instanceof final WorldOperationException operation ? operation : null);
        final var generator = Optional.ofNullable(throwable instanceof final GeneratorException gen ? gen : null);
        final var merged = new ArrayList<TagResolver>(placeholders.length + 6);
        merged.addAll(Arrays.asList(placeholders));
        merged.add(Placeholder.parsed("key", exception.map(WorldOperationException::key).map(Key::asString).orElse("")));
        merged.add(Placeholder.parsed("path", exception.map(WorldOperationException::path).map(Path::toString).orElse("")));
        merged.add(Placeholder.parsed("backup", exception.map(WorldOperationException::backup).orElse("")));
        merged.add(Placeholder.parsed("world", exception.map(WorldOperationException::world).orElse("")));
        merged.add(Placeholder.parsed("plugin", exception.map(WorldOperationException::plugin)
                .or(() -> generator.map(GeneratorException::getPlugin)).orElse("")));
        merged.add(Placeholder.parsed("reason", Optional.ofNullable(throwable.getMessage()).orElse("")));
        return merged.toArray(TagResolver[]::new);
    }
}
