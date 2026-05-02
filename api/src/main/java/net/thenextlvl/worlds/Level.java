package net.thenextlvl.worlds;

import io.papermc.paper.math.Position;
import io.papermc.paper.math.Rotation;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.thenextlvl.worlds.experimental.BiomeSource;
import net.thenextlvl.worlds.experimental.GeneratorType;
import net.thenextlvl.worlds.generator.Generator;
import net.thenextlvl.worlds.preset.Preset;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

/**
 * Describes a world level that can be created or loaded.
 *
 * @since 4.0.0
 */
public sealed interface Level extends Keyed permits SimpleLevel {
    /**
     * Returns the level directory.
     *
     * @return the level directory
     * @since 4.0.0
     */
    Path getDirectory();

    /**
     * Returns the world name.
     *
     * @return the world name
     * @since 4.0.0
     */
    @Contract(pure = true)
    String getName();

    /**
     * Returns the level dimension.
     *
     * @return the level dimension
     * @since 4.0.0
     */
    @Contract(pure = true)
    Dimension getDimension();

    /**
     * Returns the world seed.
     *
     * @return the world seed
     * @since 4.0.0
     */
    @Contract(pure = true)
    long getSeed();

    /**
     * Returns whether the level is hardcore.
     *
     * @return {@code true} if the level is hardcore
     * @since 4.0.0
     */
    @Contract(pure = true)
    boolean isHardcore();

    /**
     * Returns whether structures are generated.
     *
     * @return {@code true} if structures are generated
     * @since 4.0.0
     */
    @Contract(pure = true)
    boolean hasStructures();

    /**
     * Returns whether the bonus chest is enabled.
     *
     * @return {@code true} if the bonus chest is enabled
     * @since 4.0.0
     */
    @Contract(pure = true)
    boolean hasBonusChest();

    /**
     * Returns whether the spawn position should be reset.
     *
     * @return {@code true} if the spawn position should be reset
     * @since 4.0.0
     */
    @Contract(pure = true)
    boolean resetSpawnPosition();

    /**
     * Returns the generator type.
     *
     * @return the generator type
     * @since 4.0.0
     */
    @Contract(pure = true)
    GeneratorType getGeneratorType();

    /**
     * Returns the experimental biome source.
     *
     * @return the biome source, or empty
     * @since 4.0.0
     */
    @Contract(pure = true)
    Optional<BiomeSource> getBiomeSource();

    /**
     * Returns the configured generator.
     *
     * @return the generator, or empty
     * @since 4.0.0
     */
    @Contract(pure = true)
    Optional<Generator> getGenerator();

    /**
     * Returns the Bukkit chunk generator.
     *
     * @return the chunk generator, or empty
     * @since 4.0.0
     */
    @Contract(pure = true)
    Optional<ChunkGenerator> getChunkGenerator();

    /**
     * Returns the Bukkit biome provider.
     *
     * @return the biome provider, or empty
     * @since 4.0.0
     */
    @Contract(pure = true)
    Optional<BiomeProvider> getBiomeProvider();

    /**
     * Returns the flat-world preset.
     *
     * @return the preset, or empty
     * @since 4.0.0
     */
    @Contract(pure = true)
    Optional<Preset> getPreset();

    /**
     * Returns the forced spawn position.
     *
     * @return the forced spawn position, or empty
     * @since 4.0.0
     */
    @Contract(pure = true)
    Optional<Position> getForcedSpawnPosition();

    /**
     * Returns the forced spawn rotation.
     *
     * @return the forced spawn rotation, or empty
     * @since 4.0.0
     */
    @Contract(pure = true)
    Optional<Rotation> getForcedSpawnRotation();

    /**
     * Creates this level.
     *
     * @return a future completing with the created world
     * @since 4.0.0
     */
    CompletableFuture<World> create();

    /**
     * Creates a builder pre-populated from this level.
     *
     * @return a level builder
     * @since 4.0.0
     */
    @Contract(pure = true)
    Builder toBuilder();

    /**
     * Creates a builder pre-populated from a Bukkit world.
     *
     * @param world the world to copy
     * @return a level builder
     * @since 4.0.0
     */
    @Contract(value = "_ -> new", pure = true)
    static Builder copy(final World world) {
        return SimpleLevel.copy(world);
    }

    /**
     * Creates a new level builder.
     *
     * @param key the world key
     * @return a level builder
     * @since 4.0.0
     */
    @Contract(value = "_ -> new", pure = true)
    static Builder builder(final Key key) {
        return new SimpleLevel.Builder(key);
    }

    /**
     * Builder for {@link Level} instances.
     *
     * @since 4.0.0
     */
    sealed interface Builder permits SimpleLevel.Builder {
        /**
         * Returns the configured world key.
         *
         * @return the world key
         * @since 4.0.0
         */
        @Contract(pure = true)
        Key key();

        /**
         * Sets the world key.
         *
         * @param key the world key
         * @return this builder
         * @since 4.0.0
         */
        @Contract(mutates = "this")
        Builder key(Key key);

        /**
         * Returns the configured world name.
         *
         * @return the world name, or empty
         * @since 4.0.0
         */
        @Contract(pure = true)
        Optional<String> name();

        /**
         * Sets the world name.
         *
         * @param name the world name, or {@code null} to use the key value
         * @return this builder
         * @since 4.0.0
         */
        @Contract(mutates = "this")
        Builder name(@Nullable String name);

        /**
         * Returns the configured dimension.
         *
         * @return the dimension, or empty
         * @since 4.0.0
         */
        @Contract(pure = true)
        Optional<Dimension> dimension();

        /**
         * Sets the dimension.
         *
         * @param dimension the dimension, or {@code null} to use the default
         * @return this builder
         * @since 4.0.0
         */
        @Contract(mutates = "this")
        Builder dimension(@Nullable Dimension dimension);

        /**
         * Returns the configured experimental biome source.
         *
         * @return the biome source, or empty
         * @since 4.0.0
         */
        @Contract(pure = true)
        @ApiStatus.Experimental
        Optional<BiomeSource> biomeSource();

        /**
         * Sets the experimental biome source.
         *
         * @param source the biome source, or {@code null}
         * @return this builder
         * @since 4.0.0
         */
        @ApiStatus.Experimental
        @Contract(mutates = "this")
        Builder biomeSource(@Nullable BiomeSource source);

        /**
         * Returns the configured seed.
         *
         * @return the seed, or empty
         * @since 4.0.0
         */
        @Contract(pure = true)
        OptionalLong seed();

        /**
         * Sets the seed.
         *
         * @param seed the seed, or {@code null} for a random seed
         * @return this builder
         * @since 4.0.0
         */
        @Contract(mutates = "this")
        Builder seed(@Nullable Long seed);

        /**
         * Returns the configured hardcore flag.
         *
         * @return the hardcore flag, or empty
         * @since 4.0.0
         */
        @Contract(pure = true)
        Optional<Boolean> hardcore();

        /**
         * Sets whether the level is hardcore.
         *
         * @param hardcore the hardcore flag, or {@code null} to use the server default
         * @return this builder
         * @since 4.0.0
         */
        @Contract(mutates = "this")
        Builder hardcore(@Nullable Boolean hardcore);

        /**
         * Returns the configured structure generation flag.
         *
         * @return the structures flag, or empty
         * @since 4.0.0
         */
        @Contract(pure = true)
        Optional<Boolean> structures();

        /**
         * Sets whether structures are generated.
         *
         * @param structures the structures flag, or {@code null} to use the server default
         * @return this builder
         * @since 4.0.0
         */
        @Contract(mutates = "this")
        Builder structures(@Nullable Boolean structures);

        /**
         * Returns the configured bonus chest flag.
         *
         * @return the bonus chest flag, or empty
         * @since 4.0.0
         */
        @Contract(pure = true)
        Optional<Boolean> bonusChest();

        /**
         * Sets whether the bonus chest is enabled.
         *
         * @param bonusChest the bonus chest flag, or {@code null} for disabled
         * @return this builder
         * @since 4.0.0
         */
        @Contract(mutates = "this")
        Builder bonusChest(@Nullable Boolean bonusChest);

        /**
         * Returns the configured spawn reset flag.
         *
         * @return the spawn reset flag, or empty
         * @since 4.0.0
         */
        @Contract(pure = true)
        Optional<Boolean> resetSpawnPosition();

        /**
         * Sets whether the spawn position should be reset.
         *
         * @param reset the spawn reset flag, or {@code null} for disabled
         * @return this builder
         * @since 4.0.0
         */
        @Contract(mutates = "this")
        Builder resetSpawnPosition(@Nullable Boolean reset);

        /**
         * Returns the forced spawn position.
         *
         * @return the forced spawn position, or empty
         * @since 4.0.0
         */
        @Contract(pure = true)
        Optional<Position> forcedSpawnPosition();

        /**
         * Returns the forced spawn rotation.
         *
         * @return the forced spawn rotation, or empty
         * @since 4.0.0
         */
        @Contract(pure = true)
        Optional<Rotation> forcedSpawnRotation();

        /**
         * Sets the forced spawn position and rotation.
         *
         * @param position the forced spawn position, or {@code null}
         * @param rotation the forced spawn rotation, or {@code null}
         * @return this builder
         * @throws IllegalArgumentException if a rotation is provided without a position
         * @since 4.0.0
         */
        @Contract(value = "null, !null -> fail", mutates = "this")
        Builder forcedSpawnPosition(@Nullable Position position, @Nullable Rotation rotation);

        /**
         * Returns the configured generator type.
         *
         * @return the generator type, or empty
         * @since 4.0.0
         */
        @Contract(pure = true)
        Optional<GeneratorType> generatorType();

        /**
         * Sets the generator type.
         *
         * @param generatorType the generator type, or {@code null}
         * @return this builder
         * @since 4.0.0
         */
        @Contract(mutates = "this")
        Builder generatorType(@Nullable GeneratorType generatorType);

        /**
         * Returns the configured generator.
         *
         * @return the generator, or empty
         * @since 4.0.0
         */
        @Contract(pure = true)
        Optional<Generator> generator();

        /**
         * Sets the generator.
         *
         * @param generator the generator, or {@code null}
         * @return this builder
         * @since 4.0.0
         */
        @Contract(mutates = "this")
        Builder generator(@Nullable Generator generator);

        /**
         * Returns the configured flat-world preset.
         *
         * @return the preset, or empty
         * @since 4.0.0
         */
        @Contract(pure = true)
        Optional<Preset> preset();

        /**
         * Sets the flat-world preset.
         *
         * @param preset the preset, or {@code null}
         * @return this builder
         * @since 4.0.0
         */
        @Contract(mutates = "this")
        Builder preset(@Nullable Preset preset);

        /**
         * Builds a level from the configured values.
         *
         * @return the level
         * @since 4.0.0
         */
        @Contract(pure = true)
        Level build();
    }
}
