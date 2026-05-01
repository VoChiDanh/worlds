package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

import java.util.stream.Stream;

sealed public interface Dimension extends Keyed permits SimpleDimension {
    Dimension OVERWORLD = of(Key.key("overworld"));
    Dimension THE_END = of(Key.key("the_end"));
    Dimension THE_NETHER = of(Key.key("the_nether"));

    static Dimension of(final Key key) {
        return new SimpleDimension(key);
    }

    static Stream<Dimension> dimensions() {
        return Stream.of(OVERWORLD, THE_NETHER, THE_END);
    }
}
