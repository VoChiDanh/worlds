package net.thenextlvl.worlds;

import net.thenextlvl.worlds.view.PaperLevelView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class FreeKeyTest {
    private static final Map<String, String> usedValues = new LinkedHashMap<>(Map.ofEntries(
            Map.entry("caves", "caves_3"),
            Map.entry("caves_1", "caves_3"),
            Map.entry("caves_2", "caves_3"),
            Map.entry("checkerboard", "checkerboard_1"),
            Map.entry("end", "end_1"),
            Map.entry("nether", "nether_2"),
            Map.entry("nether_1", "nether_2"),
            Map.entry("singlebiome", "singlebiome_1"),
            Map.entry("test", "test_1"),
            Map.entry("void", "void_2"),
            Map.entry("void_1", "void_2"),
            Map.entry("world", "world_1"),
            Map.entry("world_nether", "world_nether_1")
    ));

    @ParameterizedTest
    @MethodSource("usedValues")
    public void testFreeValue(final String value, final String expectedValue) {
        final var freeValue = PaperLevelView.findFreeValue(usedValues.keySet(), value);
        Assertions.assertEquals(expectedValue, freeValue, "Unexpected value for '" + value + "'");
    }

    public static Stream<Arguments> usedValues() {
        return usedValues.entrySet().stream().map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
    }
}
