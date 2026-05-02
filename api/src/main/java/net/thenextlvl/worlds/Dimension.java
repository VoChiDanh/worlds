package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

import java.util.stream.Stream;

public record Dimension(Key key) implements Keyed {
    public static final Dimension OVERWORLD = new Dimension(Key.key("overworld"));
    public static final Dimension THE_END = new Dimension(Key.key("the_end"));
    public static final Dimension THE_NETHER = new Dimension(Key.key("the_nether"));

    public static Stream<Dimension> dimensions() {
        return Stream.of(OVERWORLD, THE_NETHER, THE_END);
    }
}
