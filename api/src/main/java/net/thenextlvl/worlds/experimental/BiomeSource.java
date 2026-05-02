package net.thenextlvl.worlds.experimental;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

/**
 * @since 4.0.0
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface BiomeSource extends Keyed {
    /**
     * Creates a CheckerboardColumnBiomeSource instance using the specified biomes.
     * This biome source generates a checkerboard-like world layout with a pattern
     * based on the provided set of biomes.
     *
     * @param biomes the set of biomes to use for the checkerboard pattern
     * @return a new CheckerboardColumnBiomeSource configured with the specified biomes
     * @since 4.0.0
     */
    @Contract(value = "_ -> new", pure = true)
    static CheckerboardColumnBiomeSource checkerboard(final Set<Key> biomes) {
        return new CheckerboardColumnBiomeSource(biomes);
    }

    /**
     * Creates a FixedBiomeSource instance with the specified biome.
     * This biome source generates a world consisting entirely of the given biome.
     *
     * @param biome the biome to use for the world
     * @return a new FixedBiomeSource configured with the specified biome
     * @since 4.0.0
     */
    @Contract(value = "_ -> new", pure = true)
    static FixedBiomeSource fixed(final Key biome) {
        return new FixedBiomeSource(biome);
    }

    /**
     * Represents the "fixed" biome source.
     * <p>
     * This preset generates a world consisting of only one biome.
     * <a href="https://minecraft.wiki/w/Dimension_definition#fixed">Wiki</a>
     *
     * @since 4.0.0
     */
    final class FixedBiomeSource implements BiomeSource {
        private final Key key = Key.key("minecraft", "fixed");
        private final Key biome;

        /**
         * Constructs a new FixedBiomeSource with the specified biome.
         *
         * @param biome the biome to use for the world
         * @since 4.0.0
         */
        private FixedBiomeSource(final Key biome) {
            this.biome = biome;
        }

        /**
         * Gets the biome key associated with this biome source.
         *
         * @return the biome key
         * @since 4.0.0
         */
        @Contract(pure = true)
        public Key biome() {
            return biome;
        }

        @Override
        @Contract(pure = true)
        public Key key() {
            return key;
        }
    }

    /**
     * Represents the "checkerboard" biome source.
     * <p>
     * This biome source generates a unique checkerboard-like pattern of biomes, creating a grid-like world layout.
     * <a href="https://minecraft.wiki/w/Dimension_definition#checkerboard">Wiki</a>
     *
     * @since 4.0.0
     */
    final class CheckerboardColumnBiomeSource implements BiomeSource {
        private final Key key = Key.key("minecraft", "checkerboard");
        private final Set<Key> biomes;

        /**
         * Constructs a new CheckerboardColumnBiomeSource with the specified biomes.
         *
         * @param biomes the set of biomes to use for the checkerboard pattern
         * @since 4.0.0
         */
        private CheckerboardColumnBiomeSource(final Set<Key> biomes) {
            this.biomes = Set.copyOf(biomes);
        }

        /**
         * Gets the set of biomes associated with this biome source.
         *
         * @return an unmodifiable set of biome keys
         * @since 4.0.0
         */
        @Contract(pure = true)
        public @Unmodifiable Set<Key> biomes() {
            return Set.copyOf(biomes);
        }

        @Override
        @Contract(pure = true)
        public Key key() {
            return key;
        }
    }
}