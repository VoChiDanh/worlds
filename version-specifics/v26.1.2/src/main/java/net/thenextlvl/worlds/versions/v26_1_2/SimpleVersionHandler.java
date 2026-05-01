package net.thenextlvl.worlds.versions.v26_1_2;

import ca.spottedleaf.moonrise.common.util.TickThread;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.papermc.paper.FeatureHooks;
import io.papermc.paper.math.Rotation;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import io.papermc.paper.world.PaperWorldLoader;
import io.papermc.paper.world.migration.WorldFolderMigration;
import io.papermc.paper.world.saveddata.PaperWorldPDC;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Main;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.DirectoryLock;
import net.minecraft.util.FileUtil;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.npc.CatSpawner;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.thenextlvl.worlds.Dimension;
import net.thenextlvl.worlds.Level;
import net.thenextlvl.worlds.experimental.GeneratorType;
import net.thenextlvl.worlds.preset.Preset;
import net.thenextlvl.worlds.versions.PluginAccess;
import net.thenextlvl.worlds.versions.VersionHandler;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.generator.CraftWorldInfo;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class SimpleVersionHandler extends VersionHandler {
    public SimpleVersionHandler(final PluginAccess plugin) {
        super(plugin, null, false);
    }

    @Override
    public Class<?> getTickThreadClass() {
        return TickThread.class;
    }

    @Override
    public String getOverworldName() {
        final var properties = ((CraftServer) plugin.getServer()).getServer().getProperties();
        return properties.levelName;
    }

    @Override
    public boolean isDirectoryLockException(final Throwable throwable) {
        return throwable instanceof DirectoryLock.LockException;
    }

    @Override
    public CompletableFuture<@Nullable Void> saveAsync(final World world, final boolean flush) {
        try {
            final var level = ((CraftWorld) world).getHandle();
            final var oldSave = level.noSave;
            level.noSave = false;
            level.save(null, flush, false);
            level.noSave = oldSave;
            return CompletableFuture.completedFuture(null);
        } catch (final Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * @see ServerLevel#saveLevelData(boolean)
     */
    @Override
    @SuppressWarnings("JavadocReference")
    public CompletableFuture<@Nullable Void> saveLevelDataAsync(final World world) {
        final var level = ((CraftWorld) world).getHandle();
        final SavedDataStorage savedDataStorage = level.getChunkSource().getDataStorage();
        savedDataStorage.computeIfAbsent(PaperWorldPDC.TYPE).setFrom((CraftPersistentDataContainer) world.getPersistentDataContainer());
        return savedDataStorage.scheduleSave().thenApply(ignored -> null);
    }

    @Override
    public Optional<JavaPlugin> getGenerator(final World world) {
        return Optional.ofNullable(world.getGenerator())
                .map(chunkGenerator -> chunkGenerator.getClass().getClassLoader())
                .filter(ConfiguredPluginClassLoader.class::isInstance)
                .map(ConfiguredPluginClassLoader.class::cast)
                .map(ConfiguredPluginClassLoader::getPlugin);
    }

    @Override
    public void generateEndPlatform(final World world, final Entity entity) {
        final var handle = ((CraftWorld) world).getHandle();
        final var entityHandle = plugin.isRunningFolia() ? null : ((CraftEntity) entity).getHandle();
        EndPlatformFeature.createEndPlatform(handle, new BlockPos(100, 49, 0), true, entityHandle);
    }

    @Override
    public void handleEndCredits(final Player player) {
        final var level = ((CraftWorld) player.getWorld()).getHandle();
        if (plugin.isRunningFolia() || level.paperConfig().misc.disableEndCredits) {
            ((CraftPlayer) player).getHandle().seenCredits = true;
        } else if (!((CraftPlayer) player).getHandle().seenCredits) {
            ((CraftPlayer) player).getHandle().showEndCredits();
        }
    }

    @Override
    public @Nullable Location getRespawnLocation(final Player player, final boolean load) {
        return player.getRespawnLocation(load);
    }

    @Override
    public Boolean hasBonusChest(final World world) {
        return world.hasBonusChest();
    }

    /**
     * @see MinecraftServer#createLevel(LevelStem, PaperWorldLoader.WorldLoadingInfoAndData, LevelDataAndDimensions.WorldDataAndGenSettings)
     * @see CraftServer#createWorld(WorldCreator)
     */
    @Override
    public CompletableFuture<World> createAsync(final Level level) {
        final var server = ((CraftServer) plugin.getServer());
        final var console = server.getServer();

        final var directory = level.getDirectory();
        final var key = level.key();
        final var name = level.getName();

        try {
            Preconditions.checkState(console.getAllLevels().iterator().hasNext(), "Cannot create worlds before main level is created");
            Preconditions.checkArgument(!Files.exists(directory) || Files.isDirectory(directory), "File (%s) exists and isn't a folder", directory);

            Preconditions.checkArgument(server.getWorld(key) == null, "World with key %s already exists", key);
            Preconditions.checkArgument(server.getWorld(name) == null, "World with name %s already exists", name);

            Preconditions.checkState(plugin.getServer().getWorlds().stream()
                            .map(World::getWorldPath)
                            .noneMatch(directory::equals),
                    "World with directory %s already exists", directory);
        } catch (final RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }


        // Worlds start - find generator and biome provider
        final var chunkGenerator = level.getChunkGenerator()
                .orElseGet(() -> level.getGenerator()
                        .flatMap(generator -> generator.generator(name))
                        .orElseGet(() -> server.getGenerator(name)));
        var biomeProvider = level.getBiomeProvider()
                .orElseGet(() -> level.getGenerator()
                        .flatMap(generator -> generator.biomeProvider(name))
                        .orElseGet(() -> server.getBiomeProvider(name)));
        // Worlds end

        final ResourceKey<LevelStem> actualDimension;
        if (level.getDimension().equals(Dimension.OVERWORLD)) {
            actualDimension = LevelStem.OVERWORLD;
        } else if (level.getDimension().equals(Dimension.THE_NETHER)) {
            actualDimension = LevelStem.NETHER;
        } else if (level.getDimension().equals(Dimension.THE_END)) {
            actualDimension = LevelStem.END;
        } else {
            actualDimension = ResourceKey.create(Registries.LEVEL_STEM, toIdentifier(level.getDimension().key()));  // Worlds - allow custom dimensions
        }

        final var resourceKey = ResourceKey.create(Registries.LEVEL_STEM, toIdentifier(key)); // Worlds - create ResourceKey from key
        final ResourceKey<net.minecraft.world.level.Level> dimensionKey = PaperWorldLoader.dimensionKey(resourceKey);
        final WorldLoader.DataLoadContext context = console.worldLoaderContext;
        RegistryAccess.Frozen registryAccess = context.datapackDimensions();
        Registry<LevelStem> contextLevelStemRegistry = registryAccess.lookupOrThrow(Registries.LEVEL_STEM);
        final LevelStem configuredStem = console.registryAccess().lookupOrThrow(Registries.LEVEL_STEM).getValue(actualDimension);
        if (configuredStem == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Missing configured level stem " + actualDimension)); // Worlds - complete exceptionally
        }
        try {
            WorldFolderMigration.migrateApiWorld(
                    console.storageSource,
                    console.registryAccess(),
                    name,
                    actualDimension,
                    dimensionKey
            );
        } catch (final IOException ex) {
            return CompletableFuture.failedFuture(new RuntimeException("Failed to migrate legacy world " + name, ex)); // Worlds - complete exceptionally
        }
        PaperWorldLoader.LoadedWorldData loadedWorldData = PaperWorldLoader.loadWorldData(
                console,
                dimensionKey,
                name
        );
        final PrimaryLevelData primaryLevelData = (PrimaryLevelData) console.getWorldData();
        WorldGenSettings worldGenSettings = LevelStorageSource.readExistingSavedData(console.storageSource, dimensionKey, console.registryAccess(), WorldGenSettings.TYPE)
                .result()
                .orElse(null);

        if (worldGenSettings == null) {
            final WorldOptions worldOptions = new WorldOptions(level.getSeed(), level.hasStructures(), level.hasBonusChest());

            final var generatorSettings = level.getPreset().orElse(Preset.CLASSIC_FLAT).toJson(); // Worlds - serialize preset
            final var levelType = level.getGeneratorType().presetName().asString(); // Worlds - get actual level type name
            final DedicatedServerProperties.WorldDimensionData properties = new DedicatedServerProperties.WorldDimensionData(generatorSettings, levelType);
            WorldDimensions worldDimensions = properties.create(context.datapackWorldgen());

            // Worlds start - replace generators
            if (level.getGeneratorType().equals(GeneratorType.FLAT) || level.getGeneratorType().equals(GeneratorType.DEBUG)) {
                worldDimensions = replaceGenerator(LevelStem.NETHER, context.datapackWorldgen(), worldDimensions.dimensions(), worldDimensions.overworld());
                worldDimensions = replaceGenerator(LevelStem.END, context.datapackWorldgen(), worldDimensions.dimensions(), worldDimensions.overworld());
            }
            // Worlds end

            final WorldDimensions.Complete complete = worldDimensions.bake(contextLevelStemRegistry);
            if (complete.dimensions().getValue(actualDimension) == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("Missing generated level stem " + actualDimension + " for world " + key)); // Worlds - complete exceptionally
            }

            worldGenSettings = new WorldGenSettings(worldOptions, worldDimensions);
            registryAccess = complete.dimensionsRegistryAccess();
            loadedWorldData.levelOverrides().setHardcore(level.isHardcore());
            loadedWorldData = new PaperWorldLoader.LoadedWorldData(
                    loadedWorldData.bukkitName(),
                    loadedWorldData.uuid(),
                    loadedWorldData.pdc(),
                    loadedWorldData.levelOverrides()
            );
        }

        // Worlds start - check world uuid availability
        final var duplicate = server.getWorld(loadedWorldData.uuid());
        if (duplicate != null) return CompletableFuture.failedFuture(
                new IllegalStateException("World %s is a duplicate of %s. Remove or change the duplicated Paper metadata.".formatted(key, duplicate.key()))
        );
        // Worlds end

        final WorldGenSettings genSettingsFinal = worldGenSettings;

        contextLevelStemRegistry = registryAccess.lookupOrThrow(Registries.LEVEL_STEM);

        if (console.options.has("forceUpgrade")) {
            Main.forceUpgrade(console.storageSource, DataFixers.getDataFixer(), console.options.has("eraseCache"), () -> true, registryAccess, console.options.has("recreateRegionFiles"));
        }

        final long biomeZoomSeed = BiomeManager.obfuscateSeed(genSettingsFinal.options().seed());
        LevelStem customStem = genSettingsFinal.dimensions().get(actualDimension).orElse(null);
        if (customStem == null) {
            customStem = contextLevelStemRegistry.getValue(actualDimension);
        }
        if (customStem == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Missing level stem for world " + key + " using key " + actualDimension)); // Worlds - complete exceptionally
        }

        final var environment = toEnvironment(level.getDimension()); // Worlds - get environment from dimension

        final WorldInfo worldInfo = new CraftWorldInfo(loadedWorldData.bukkitName(), CraftNamespacedKey.fromMinecraft(dimensionKey.identifier()), genSettingsFinal.options().seed(), primaryLevelData.enabledFeatures(), environment, customStem.type().value(), customStem.generator(), server.getHandle().getServer().registryAccess(), loadedWorldData.uuid());
        if (biomeProvider == null && chunkGenerator != null) {
            biomeProvider = chunkGenerator.getDefaultBiomeProvider(worldInfo);
        }

        final SavedDataStorage savedDataStorage = new SavedDataStorage(console.storageSource.getDimensionPath(dimensionKey).resolve(LevelResource.DATA.id()), console.getFixerUpper(), console.registryAccess());
        savedDataStorage.set(WorldGenSettings.TYPE, new WorldGenSettings(genSettingsFinal.options(), genSettingsFinal.dimensions()));
        final List<CustomSpawner> list = ImmutableList.of(
                new PhantomSpawner(), new PatrolSpawner(), new CatSpawner(), new VillageSiege(), new WanderingTraderSpawner(savedDataStorage)
        );

        final ServerLevel serverLevel = new ServerLevel(
                console,
                console.executor,
                console.storageSource,
                genSettingsFinal,
                dimensionKey,
                customStem,
                primaryLevelData.isDebugWorld(),
                biomeZoomSeed,
                level.getDimension().equals(Dimension.OVERWORLD) ? list : ImmutableList.of(),
                true,
                actualDimension,
                environment,
                chunkGenerator,
                biomeProvider,
                savedDataStorage,
                loadedWorldData
        );

        // Worlds start - ensure world is memoized before adding to server
        if (server.getWorld(name) == null) return CompletableFuture.failedFuture(
                new IllegalStateException("World " + key + " was not properly memoized")
        );
        // Worlds end

        // Worlds start - setInitialized(false) to reevaluate spawn position, construct WorldCreator for explicit override
        if (level.resetSpawnPosition()) primaryLevelData.setInitialized(false);

        final var worldCreator = level.getForcedSpawnPosition().map(position -> {
            final var worldKey = new NamespacedKey(level.key().namespace(), level.key().value());
            final var creator = new WorldCreator(worldKey);
            final var rotation = level.getForcedSpawnRotation().orElseGet(() -> Rotation.rotation(0, 0));
            creator.forcedSpawnPosition(position, rotation.yaw(), rotation.pitch());
            return creator;
        }).orElse(null);
        // Worlds end

        console.addLevel(serverLevel);
        console.initWorld(serverLevel, worldCreator);

        serverLevel.setSpawnSettings(true);

        FeatureHooks.tickEntityManager(serverLevel); // Worlds - start entity ticking for folia, no-op on paper

        console.prepareLevel(serverLevel);

        return CompletableFuture.completedFuture(serverLevel.getWorld());
    }

    private World.Environment toEnvironment(final Dimension dimension) {
        if (dimension.equals(Dimension.OVERWORLD)) return World.Environment.NORMAL;
        if (dimension.equals(Dimension.THE_END)) return World.Environment.THE_END;
        if (dimension.equals(Dimension.THE_NETHER)) return World.Environment.NETHER;
        return World.Environment.CUSTOM;
    }

    /**
     * @see WorldDimensions#replaceOverworldGenerator
     */
    private static WorldDimensions replaceGenerator(final ResourceKey<LevelStem> key, final HolderLookup.Provider registries, final Map<ResourceKey<LevelStem>, LevelStem> dimensions, final ChunkGenerator chunkGenerator) {
        final HolderLookup<DimensionType> holderLookup = registries.lookupOrThrow(Registries.DIMENSION_TYPE);
        final Map<ResourceKey<LevelStem>, LevelStem> map = withGenerator(key, holderLookup, dimensions, chunkGenerator);
        return new WorldDimensions(map);
    }

    /**
     * @see WorldDimensions#withOverworld(HolderLookup, Map, ChunkGenerator)
     */
    private static Map<ResourceKey<LevelStem>, LevelStem> withGenerator(
            final ResourceKey<LevelStem> key, final HolderLookup<DimensionType> dimensionTypeRegistry, final Map<ResourceKey<LevelStem>, LevelStem> dimensions, final ChunkGenerator chunkGenerator
    ) {
        final LevelStem levelStem = dimensions.get(key);
        final Holder<DimensionType> holder = levelStem == null
                ? dimensionTypeRegistry.getOrThrow(BuiltinDimensionTypes.OVERWORLD)
                : levelStem.type();
        return withGenerator(key, dimensions, holder, chunkGenerator);
    }

    /**
     * @see WorldDimensions#withOverworld(Map, Holder, ChunkGenerator)
     */
    private static Map<ResourceKey<LevelStem>, LevelStem> withGenerator(
            final ResourceKey<LevelStem> key, final Map<ResourceKey<LevelStem>, LevelStem> stemMap, final Holder<DimensionType> dimensionType, final ChunkGenerator chunkGenerator
    ) {
        final ImmutableMap.Builder<ResourceKey<LevelStem>, LevelStem> builder = ImmutableMap.builder();
        builder.putAll(stemMap);
        builder.put(key, new LevelStem(dimensionType, chunkGenerator));
        return builder.buildKeepingLast();
    }

    @Override
    public String findAvailableName(final Path path, final String name, final String format) throws IOException {
        return FileUtil.findAvailableName(path, name, format);
    }

    @Override
    public GeneratorType getGeneratorType(final World world) {
        final var handle = ((CraftWorld) world).getHandle();
        final var generator = handle.getChunkSource().getGenerator();
        return switch (generator) {
            case final DebugLevelSource debugLevelSource -> GeneratorType.DEBUG;
            case final FlatLevelSource flatLevelSource -> GeneratorType.FLAT;
            case final NoiseBasedChunkGenerator noiseGenerator -> {
                if (noiseGenerator.stable(NoiseGeneratorSettings.LARGE_BIOMES))
                    yield GeneratorType.LARGE_BIOMES;
                if (noiseGenerator.stable(NoiseGeneratorSettings.AMPLIFIED))
                    yield GeneratorType.AMPLIFIED;
                if (noiseGenerator.getBiomeSource() instanceof FixedBiomeSource)
                    yield GeneratorType.SINGLE_BIOME;
                yield GeneratorType.NORMAL;
            }
            default -> GeneratorType.NORMAL;
        };
    }

    @Override
    public Stream<Dimension> listDimensions() {
        final var console = ((CraftServer) plugin.getServer()).getServer();
        final var registry = console.worldLoaderContext.datapackDimensions().lookupOrThrow(Registries.LEVEL_STEM);
        return registry.keySet().stream()
                .map(this::fromIdentifier)
                .map(Dimension::of);
    }

    @Override
    public Dimension getDimension(final World world) {
        final var handle = ((CraftWorld) world).getHandle();
        final var identifier = handle.getTypeKey().identifier();
        return Dimension.of(fromIdentifier(identifier));
    }

    @SuppressWarnings("PatternValidation")
    private Key fromIdentifier(final Identifier identifier) {
        return Key.key(identifier.getNamespace(), identifier.getPath());
    }

    private Identifier toIdentifier(final Key key) {
        return Identifier.parse(key.asString());
    }
}
