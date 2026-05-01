package net.thenextlvl.worlds.command;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.thenextlvl.worlds.WorldOperationException;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.generator.GeneratorException;
import org.bukkit.command.CommandSender;

import java.nio.file.Path;
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
        messageKey(plugin, cause).ifPresentOrElse(messageKey -> {
            plugin.bundle().sendMessage(sender, messageKey, merge(cause, placeholders));
        }, () -> plugin.getComponentLogger().error("Unhandled command failure", cause));
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
        return Optional.empty();
    }

    private static TagResolver merge(final Throwable throwable, final TagResolver... placeholders) {
        final var exception = Optional.ofNullable(throwable instanceof final WorldOperationException operation ? operation : null);
        final var generator = Optional.ofNullable(throwable instanceof final GeneratorException gen ? gen : null);

        final var builder = TagResolver.builder();
        builder.resolvers(placeholders);

        exception.map(WorldOperationException::key).map(Key::asString)
                .ifPresent(key -> builder.resolver(Placeholder.parsed("key", key)));
        exception.map(WorldOperationException::path).map(Path::toString)
                .ifPresent(path -> builder.resolver(Placeholder.parsed("path", path)));
        exception.map(WorldOperationException::backup)
                .ifPresent(backup -> builder.resolver(Placeholder.parsed("backup", backup)));
        exception.map(WorldOperationException::world)
                .ifPresent(world -> builder.resolver(Placeholder.parsed("world", world)));
        exception.map(WorldOperationException::plugin).or(() -> generator.map(GeneratorException::getPlugin))
                .ifPresent(plugin -> builder.resolver(Placeholder.parsed("plugin", plugin)));
        Optional.ofNullable(throwable.getMessage())
                .ifPresent(reason -> builder.resolver(Placeholder.parsed("reason", reason)));
        return builder.build();
    }
}
