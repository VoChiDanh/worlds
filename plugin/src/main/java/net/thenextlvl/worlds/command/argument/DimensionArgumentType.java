package net.thenextlvl.worlds.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.key.Key;
import net.thenextlvl.worlds.Dimension;
import net.thenextlvl.worlds.WorldsPlugin;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@NullMarked
public final class DimensionArgumentType implements SimpleArgumentType<Dimension, Key> {
    private final WorldsPlugin plugin;

    public DimensionArgumentType(final WorldsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Dimension convert(final StringReader reader, final Key type) {
        return new Dimension(type);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        Stream.concat(plugin.listDimensions(), Dimension.dimensions())
                .map(dimension -> dimension.key().asString())
                .filter(s -> s.contains(builder.getRemaining()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    @Override
    public ArgumentType<Key> getNativeType() {
        return ArgumentTypes.key();
    }
}
