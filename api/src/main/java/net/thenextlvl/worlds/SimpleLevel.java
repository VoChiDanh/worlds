package net.thenextlvl.worlds;

import io.papermc.paper.math.Position;
import io.papermc.paper.math.Rotation;
import net.kyori.adventure.key.Key;
import net.thenextlvl.worlds.generator.GeneratorType;
import net.thenextlvl.worlds.generator.Generator;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

final class SimpleLevel implements Level {
    private final Key key;

    private final Dimension dimension;
    private final GeneratorType generatorType;

    private final @Nullable BiomeProvider biomeProvider;
    private final @Nullable ChunkGenerator chunkGenerator;

    private final @Nullable Generator generator;

    private final @Nullable Position spawnPositionOverride;
    private final @Nullable Rotation spawnRotationOverride;

    private final boolean resetSpawnPosition;

    private final boolean bonusChest;
    private final boolean hardcore;
    private final boolean structures;
    private final long seed;

    SimpleLevel(final Builder builder) {
        final var server = WorldsAccess.access().getServer();

        this.key = builder.key;

        this.dimension = builder.dimension().orElse(Dimension.OVERWORLD);
        this.hardcore = builder.hardcore().orElseGet(server::isHardcore);
        this.seed = builder.seed().orElseGet(ThreadLocalRandom.current()::nextLong);
        this.structures = builder.structures().orElseGet(server::getGenerateStructures);

        this.bonusChest = builder.bonusChest().orElse(false);

        this.spawnPositionOverride = builder.forcedSpawnPosition().orElse(null);
        this.spawnRotationOverride = builder.forcedSpawnRotation().orElse(null);
        this.resetSpawnPosition = builder.resetSpawnPosition().orElse(false);

        this.generator = builder.generator;

        // todo: doublecheck if key.value is used as the name by bukkit now?
        this.biomeProvider = builder.generator().flatMap(generator -> generator.biomeProvider(key.value())).orElse(null);
        this.chunkGenerator = builder.generator().flatMap(generator -> generator.generator(key.value())).orElse(null);

        this.generatorType = builder.generatorType().orElse(GeneratorType.NORMAL);
    }

    @Override
    public Path getDirectory() {
        return WorldsAccess.access().resolveLevelDirectory(key);
    }

    @Override
    public Dimension getDimension() {
        return dimension;
    }

    @Override
    public long getSeed() {
        return seed;
    }

    @Override
    public boolean isHardcore() {
        return hardcore;
    }

    @Override
    public boolean hasStructures() {
        return structures;
    }

    @Override
    public boolean hasBonusChest() {
        return bonusChest;
    }

    @Override
    public GeneratorType getGeneratorType() {
        return generatorType;
    }

    @Override
    public Optional<Generator> getGenerator() {
        return Optional.ofNullable(generator);
    }

    @Override
    public Optional<ChunkGenerator> getChunkGenerator() {
        return Optional.ofNullable(chunkGenerator);
    }

    @Override
    public Optional<BiomeProvider> getBiomeProvider() {
        return Optional.ofNullable(biomeProvider);
    }

    @Override
    public Optional<Position> getForcedSpawnPosition() {
        return Optional.ofNullable(spawnPositionOverride);
    }

    @Override
    public Optional<Rotation> getForcedSpawnRotation() {
        return Optional.ofNullable(spawnRotationOverride);
    }

    @Override
    public CompletableFuture<World> create() {
        return WorldsAccess.access().create(this);
    }

    @Override
    public boolean resetSpawnPosition() {
        return resetSpawnPosition;
    }

    @Override
    public Level.Builder toBuilder() {
        return new Builder(key)
                .bonusChest(bonusChest)
                .dimension(dimension)
                .generator(generator)
                .generatorType(generatorType)
                .hardcore(hardcore)
                .seed(seed)
                .structures(structures);
    }

    @Override
    public Key key() {
        return key;
    }

    public static Level.Builder copy(final World world) {
        final var access = WorldsAccess.access();
        return Level.builder(world.key())
                .bonusChest(world.hasBonusChest())
                .hardcore(world.isHardcore())
                .structures(world.canGenerateStructures())
                .seed(world.getSeed())
                .key(world.key())
                .dimension(access.getDimension(world))
                .seed(world.getSeed());
    }

    static final class Builder implements Level.Builder {
        private @Nullable Boolean bonusChest;
        private @Nullable Boolean hardcore;
        private @Nullable Boolean resetSpawnPosition;
        private @Nullable Boolean structures;
        private @Nullable Dimension dimension;
        private @Nullable Generator generator;
        private @Nullable GeneratorType generatorType;
        private @Nullable Long seed;
        private @Nullable Position spawnPositionOverride;
        private @Nullable Rotation spawnRotationOverride;
        private Key key;

        public Builder(final Key key) {
            this.key = key;
        }

        @Override
        public Optional<Dimension> dimension() {
            return Optional.ofNullable(dimension);
        }

        @Override
        public Level.Builder dimension(final @Nullable Dimension dimension) {
            this.dimension = dimension;
            return this;
        }

        @Override
        public Key key() {
            return key;
        }

        @Override
        public Level.Builder key(final Key key) {
            this.key = key;
            return this;
        }

        @Override
        public OptionalLong seed() {
            return seed != null ? OptionalLong.of(seed) : OptionalLong.empty();
        }

        @Override
        public Level.Builder seed(@Nullable final Long seed) {
            this.seed = seed;
            return this;
        }

        @Override
        public Optional<Boolean> hardcore() {
            return Optional.ofNullable(hardcore);
        }

        @Override
        public Level.Builder hardcore(@Nullable final Boolean hardcore) {
            this.hardcore = hardcore;
            return this;
        }

        @Override
        public Optional<Boolean> structures() {
            return Optional.ofNullable(structures);
        }

        @Override
        public Level.Builder structures(@Nullable final Boolean structures) {
            this.structures = structures;
            return this;
        }

        @Override
        public Optional<Boolean> bonusChest() {
            return Optional.ofNullable(bonusChest);
        }

        @Override
        public Level.Builder bonusChest(@Nullable final Boolean bonusChest) {
            this.bonusChest = bonusChest;
            return this;
        }

        @Override
        public Optional<Boolean> resetSpawnPosition() {
            return Optional.ofNullable(resetSpawnPosition);
        }

        @Override
        public Level.Builder resetSpawnPosition(@Nullable final Boolean reset) {
            this.resetSpawnPosition = reset;
            return this;
        }

        @Override
        public Optional<Position> forcedSpawnPosition() {
            return Optional.ofNullable(spawnPositionOverride);
        }

        @Override
        public Optional<Rotation> forcedSpawnRotation() {
            return Optional.ofNullable(spawnRotationOverride);
        }

        @Override
        public Level.Builder forcedSpawnPosition(@Nullable final Position position, @Nullable final Rotation rotation) {
            if (position == null && rotation != null)
                throw new IllegalArgumentException("Cannot force spawn rotation only");
            this.spawnPositionOverride = position;
            this.spawnRotationOverride = rotation;
            return this;
        }

        @Override
        public Optional<GeneratorType> generatorType() {
            return Optional.ofNullable(generatorType);
        }

        @Override
        public Level.Builder generatorType(@Nullable final GeneratorType generatorType) {
            this.generatorType = generatorType;
            return this;
        }

        @Override
        public Optional<Generator> generator() {
            return Optional.ofNullable(generator);
        }

        @Override
        public Level.Builder generator(@Nullable final Generator generator) {
            this.generator = generator;
            return this;
        }

        @Override
        public Level build() {
            return new SimpleLevel(this);
        }
    }
}
