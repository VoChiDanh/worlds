package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

import java.util.stream.Stream;

sealed public interface Environment extends Keyed permits SimpleEnvironment {
    Environment OVERWORLD = of(Key.key("overworld"));
    Environment THE_END = of(Key.key("the_end"));
    Environment THE_NETHER = of(Key.key("the_nether"));

    static Environment of(final Key key) {
        return new SimpleEnvironment(key);
    }

    static Stream<Environment> environments() {
        return Stream.of(OVERWORLD, THE_NETHER, THE_END);
    }
}
