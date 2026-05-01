package net.thenextlvl.worlds;

import io.papermc.paper.math.Position;
import io.papermc.paper.math.Rotation;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.thenextlvl.worlds.experimental.GeneratorType;
import net.thenextlvl.worlds.generator.Generator;
import net.thenextlvl.worlds.preset.Preset;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

public sealed interface Level extends Keyed permits SimpleLevel {
    Path getDirectory();

    @Contract(pure = true)
    String getName();

    @Contract(pure = true)
    Dimension getDimension();

    @Contract(pure = true)
    long getSeed();

    @Contract(pure = true)
    boolean isHardcore();

    @Contract(pure = true)
    boolean hasStructures();

    @Contract(pure = true)
    boolean hasBonusChest();

    @Contract(pure = true)
    boolean resetSpawnPosition();

    @Contract(pure = true)
    GeneratorType getGeneratorType();

    @Contract(pure = true)
    Optional<Generator> getGenerator();

    @Contract(pure = true)
    Optional<ChunkGenerator> getChunkGenerator();

    @Contract(pure = true)
    Optional<BiomeProvider> getBiomeProvider();

    @Contract(pure = true)
    Optional<Preset> getPreset();

    @Contract(pure = true)
    Optional<Position> getForcedSpawnPosition();

    @Contract(pure = true)
    Optional<Rotation> getForcedSpawnRotation();

    CompletableFuture<World> create();

    @Contract(pure = true)
    Builder toBuilder();

    @Contract(value = "_ -> new", pure = true)
    static Builder copy(final World world) {
        return SimpleLevel.copy(world);
    }

    @Contract(value = "_ -> new", pure = true)
    static Builder builder(final Key key) {
        return new SimpleLevel.Builder(key);
    }

    sealed interface Builder permits SimpleLevel.Builder {
        @Contract(pure = true)
        Key key();

        @Contract(mutates = "this")
        Builder key(Key key);

        @Contract(pure = true)
        Optional<String> name();

        @Contract(mutates = "this")
        Builder name(@Nullable String name);

        @Contract(pure = true)
        Optional<Dimension> dimension();

        @Contract(mutates = "this")
        Builder dimension(@Nullable Dimension dimension);

        @Contract(pure = true)
        OptionalLong seed();

        @Contract(mutates = "this")
        Builder seed(@Nullable Long seed);

        @Contract(pure = true)
        Optional<Boolean> hardcore();

        @Contract(mutates = "this")
        Builder hardcore(@Nullable Boolean hardcore);

        @Contract(pure = true)
        Optional<Boolean> structures();

        @Contract(mutates = "this")
        Builder structures(@Nullable Boolean structures);

        @Contract(pure = true)
        Optional<Boolean> bonusChest();

        @Contract(mutates = "this")
        Builder bonusChest(@Nullable Boolean bonusChest);

        @Contract(pure = true)
        Optional<Boolean> resetSpawnPosition();

        @Contract(mutates = "this")
        Builder resetSpawnPosition(@Nullable Boolean reset);

        @Contract(pure = true)
        Optional<Position> forcedSpawnPosition();

        @Contract(pure = true)
        Optional<Rotation> forcedSpawnRotation();

        @Contract(value = "null, !null -> fail", mutates = "this")
        Builder forcedSpawnPosition(@Nullable Position position, @Nullable Rotation rotation);

        @Contract(pure = true)
        Optional<GeneratorType> generatorType();

        @Contract(mutates = "this")
        Builder generatorType(@Nullable GeneratorType generatorType);

        @Contract(pure = true)
        Optional<Generator> generator();

        @Contract(mutates = "this")
        Builder generator(@Nullable Generator generator);

        @Contract(pure = true)
        Optional<Preset> preset();

        @Contract(mutates = "this")
        Builder preset(@Nullable Preset preset);

        @Contract(pure = true)
        Level build();
    }
}
