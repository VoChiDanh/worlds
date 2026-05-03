package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

import java.util.stream.Stream;

/**
 * Represents a Minecraft dimension identified by a namespaced key.
 *
 * @param key the dimension key
 * @since 4.0.0
 */
public record Dimension(Key key) implements Keyed {
    /**
     * The default overworld dimension.
     *
     * @since 4.0.0
     */
    public static final Dimension OVERWORLD = new Dimension(Key.key("overworld"));
    /**
     * The default End dimension.
     *
     * @since 4.0.0
     */
    public static final Dimension THE_END = new Dimension(Key.key("the_end"));
    /**
     * The default Nether dimension.
     *
     * @since 4.0.0
     */
    public static final Dimension THE_NETHER = new Dimension(Key.key("the_nether"));

    /**
     * Returns the built-in dimensions.
     *
     * @return a stream containing the overworld, nether, and end dimensions
     * @see WorldsAccess#customDimensions()
     * @since 4.0.0
     */
    public static Stream<Dimension> dimensions() {
        return Stream.of(OVERWORLD, THE_NETHER, THE_END);
    }
}
