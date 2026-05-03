package net.thenextlvl.worlds.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

class CommandOptionsArgumentTest {
    @Test
    void testUnclosedQuotedArgumentDoesNotSuggestOptions() {
        final var argument = new CommandOptionsArgument(Map.of(
                "seed", StringArgumentType.string(),
                "other", StringArgumentType.string()
        ));

        final var suggestions = argument.listSuggestions(null, new SuggestionsBuilder("seed \"hello ", 0)).join();

        Assertions.assertTrue(suggestions.isEmpty());
    }

    @Test
    void testCompletedArgumentSuggestsRemainingOptions() {
        final var argument = new CommandOptionsArgument(Map.of(
                "seed", StringArgumentType.string(),
                "other", StringArgumentType.string()
        ));

        final var suggestions = argument.listSuggestions(null, new SuggestionsBuilder("seed \"hello\" ", 0)).join();

        Assertions.assertEquals(1, suggestions.getList().size());
        Assertions.assertEquals("other", suggestions.getList().getFirst().getText());
    }

    @Test
    void testCompletedArgumentDoesNotSuggestRemainingOptionsWithoutTrailingSpace() {
        final var argument = new CommandOptionsArgument(Map.of(
                "seed", StringArgumentType.string(),
                "other", StringArgumentType.string()
        ));

        final var suggestions = argument.listSuggestions(null, new SuggestionsBuilder("seed \"hello\"", 0)).join();

        Assertions.assertTrue(suggestions.isEmpty());
    }

    @Test
    void testParsedPartialArgumentStillSuggestsSubArgumentValues() {
        final var argument = new CommandOptionsArgument(Map.of(
                "seed", StringArgumentType.string(),
                "dimension", new SuggestingWordArgument("minecraft:the_nether")
        ));

        final var suggestions = argument.listSuggestions(null, new SuggestionsBuilder("dimension minecraft:the_ne", 0)).join();

        Assertions.assertEquals(1, suggestions.getList().size());
        Assertions.assertEquals("minecraft:the_nether", suggestions.getList().getFirst().getText());
    }

    @Test
    void testUnclosedQuotedArgumentErrorStopsBeforeNextOption() {
        final var argument = new CommandOptionsArgument(Map.of(
                "dimension", StringArgumentType.string(),
                "seed", StringArgumentType.string()
        ));

        final var exception = Assertions.assertThrows(CommandSyntaxException.class, () ->
                argument.parse(new StringReader("seed \"hello dimension minecraft:overworld")));

        Assertions.assertFalse(exception.getContext().contains("dimension"));
    }

    @Test
    void testOptionImmediatelyAfterArgumentThrows() {
        final var argument = new CommandOptionsArgument(Map.of(
                "dimension", StringArgumentType.string(),
                "seed", StringArgumentType.string()
        ));

        Assertions.assertThrows(CommandSyntaxException.class, () ->
                argument.parse(new StringReader("seed \"hello\"dimension minecraft:overworld")));
    }

    private record SuggestingWordArgument(String suggestion) implements ArgumentType<String> {
        @Override
        public String parse(final StringReader reader) throws CommandSyntaxException {
            return StringArgumentType.greedyString().parse(reader);
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(
                final CommandContext<S> context,
                final SuggestionsBuilder builder
        ) {
            if (suggestion.startsWith(builder.getRemaining())) builder.suggest(suggestion);
            return builder.buildFuture();
        }
    }
}
