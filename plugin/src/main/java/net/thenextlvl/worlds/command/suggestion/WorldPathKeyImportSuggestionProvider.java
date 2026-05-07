package net.thenextlvl.worlds.command.suggestion;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.key.Key;
import net.thenextlvl.worlds.LegacyWorldRegistry;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.view.PaperLevelView;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@NullMarked
// todo: sorry future me but you have to clean up this mess :)
public final class WorldPathKeyImportSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
    private final WorldsPlugin plugin;

    public WorldPathKeyImportSuggestionProvider(final WorldsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(final CommandContext<CommandSourceStack> context, final SuggestionsBuilder builder) {
        suggestedKey(context)
                .map(plugin.levelView()::findFreeKey)
                .map(Key::asString)
                .filter(s -> s.contains(builder.getRemaining()))
                .ifPresent(builder::suggest);
        return builder.buildFuture();
    }

    private Optional<Key> suggestedKey(final CommandContext<?> context) {
        final var source = resolveSource(context.getArgument("path", String.class));
        return legacyKey(source)
                .or(() -> key(source))
                .or(() -> Optional.ofNullable(source.getFileName())
                        .map(Path::toString)
                        .map(PaperLevelView::createKey)
                        .filter(value -> !value.isBlank())
                        .map(value -> Key.key("worlds", value)));
    }

    private Optional<Key> key(final Path source) {
        final var root = plugin.getServer().getWorldContainer().toPath().toAbsolutePath().normalize();
        final var absolute = source.toAbsolutePath().normalize();
        final var directory = absolute.startsWith(root) ? root.relativize(absolute) : absolute;
        return plugin.levelView().lenientKey(absolute)
                .or(() -> plugin.levelView().lenientKey(directory));
    }

    private Optional<Key> legacyKey(final Path source) {
        return plugin.legacyWorldRegistry().read(source).map(LegacyWorldRegistry.LegacyWorldData::key);
    }

    private Path resolveSource(final String input) {
        final var path = Path.of(input);
        return plugin.getServer().getWorldContainer().toPath().resolve(path).normalize();
    }
}
