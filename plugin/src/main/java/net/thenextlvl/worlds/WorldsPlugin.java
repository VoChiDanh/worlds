package net.thenextlvl.worlds;

import dev.faststats.bukkit.BukkitMetrics;
import dev.faststats.core.ErrorTracker;
import dev.faststats.core.data.Metric;
import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.key.Key;
import net.thenextlvl.binder.StaticBinder;
import net.thenextlvl.i18n.ComponentBundle;
import net.thenextlvl.worlds.command.SaveAllCommand;
import net.thenextlvl.worlds.command.SaveOffCommand;
import net.thenextlvl.worlds.command.SaveOnCommand;
import net.thenextlvl.worlds.command.WorldCommand;
import net.thenextlvl.worlds.command.WorldSetSpawnCommand;
import net.thenextlvl.worlds.generator.GeneratorView;
import net.thenextlvl.worlds.listener.PortalListener;
import net.thenextlvl.worlds.listener.TeleportListener;
import net.thenextlvl.worlds.listener.WorldListener;
import net.thenextlvl.worlds.model.MessageMigrator;
import net.thenextlvl.worlds.version.PluginVersionChecker;
import net.thenextlvl.worlds.versions.PluginAccess;
import net.thenextlvl.worlds.versions.VersionHandler;
import net.thenextlvl.worlds.versions.v26_1_2.SimpleVersionHandler;
import net.thenextlvl.worlds.view.FoliaLevelView;
import net.thenextlvl.worlds.view.PaperLevelView;
import org.bstats.bukkit.Metrics;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

@NullMarked
public final class WorldsPlugin extends JavaPlugin implements PluginAccess, WorldsAccess {
    public static final String ISSUES = "https://github.com/TheNextLvl-net/worlds/issues/new?template=bug_report.yml";
    public static final boolean RUNNING_FOLIA = ServerBuildInfo.buildInfo().isBrandCompatible(Key.key("papermc", "folia"));

    private final VersionHandler versionHandler = selectImplementation();

    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware()
            .ignoreError(IllegalStateException.class, "World mismatch: expected .* but got .*")
            .ignoreErrorType(AccessDeniedException.class);

    private final GeneratorView generatorView = GeneratorView.view();
    private final PaperLevelView levelView = versionHandler.foliaSupport()
            .<PaperLevelView>map(support -> new FoliaLevelView(this, support))
            .orElseGet(() -> new PaperLevelView(this));
    private final SimpleWorldRegistry worldRegistry = new SimpleWorldRegistry(this);

    private BackupProvider backupProvider = new SimpleBackupProvider();

    private final Path presetsFolder = getDataPath().resolve("presets");
    private final Path translations = getDataPath().resolve("translations");
    private final Key key = Key.key("worlds", "translations");

    private final ComponentBundle bundle = ComponentBundle.builder(key, translations)
            .migrator(new MessageMigrator())
            .placeholder("prefix", "prefix")
            .resource("worlds.properties", Locale.US)
            .resource("worlds_german.properties", Locale.GERMANY)
            .build();

    private final PluginVersionChecker versionChecker = new PluginVersionChecker(this);
    private final BukkitMetrics fastStats = BukkitMetrics.factory()
            .token("978c4aa9ecf78ae2e9c0776601fd4c6c")
            .errorTracker(ERROR_TRACKER)
            .addMetric(addGeneratorChart())
            .addMetric(addWorldsChart())
            .addMetric(addEnvironmentsChart())
            .create(this);

    private final Metrics metrics = new Metrics(this, 19652);

    public WorldsPlugin() {
        StaticBinder.getInstance(WorldsAccess.class.getClassLoader()).bind(WorldsAccess.class, this);
        getComponentLogger().info("Using implementation: {}", versionHandler.getClass().getName());
        registerCommands();
    }

    private VersionHandler selectImplementation() {
        final var s = ServerBuildInfo.buildInfo().minecraftVersionId();
        if (s.contains("26.1.2")) {
            return new SimpleVersionHandler(this);
        }
        throw new IllegalStateException("No implementation found for version: " + s + ", check for an update.");
    }

    @Override
    public void onLoad() {
        createPresetsFolder();
        versionChecker.checkVersion();
    }

    @Override
    public void onDisable() {
        getScheduler().runScheduledOperations();
    }

    @Override
    public void onEnable() {
        fastStats.ready();
        worldRegistry.read();
        warnVoidGeneratorPlugin();
        registerListeners();
    }

    private void warnVoidGeneratorPlugin() {
        final var names = Stream.of("VoidWorldGenerator", "VoidGen", "VoidGenerator", "VoidWorld", "VoidGenPlus",
                "DeluxeVoidWorld", "CleanroomGenerator", "CompletelyEmpty");
        if (names.map(getServer().getPluginManager()::getPlugin).filter(Objects::nonNull).findAny().isEmpty()) return;
        getComponentLogger().warn("It appears you are using a plugin to generate void worlds");
        getComponentLogger().warn("This is not required, and incompatible with Vanilla world generation");
        getComponentLogger().warn("Please use the preset 'the-void' instead");
        getComponentLogger().warn("You can do this with the command '/world create <key> preset the-void'");
        getComponentLogger().warn("Read more at https://thenextlvl.net/blog/void-generator-plugins");
    }

    public Path presetsFolder() {
        return presetsFolder;
    }

    public ComponentBundle bundle() {
        return bundle;
    }

    public GeneratorView generatorView() {
        return generatorView;
    }

    public <T> CompletableFuture<T> supplyGlobal(final Supplier<CompletableFuture<T>> supplier) {
        final var foliaTickThread = RUNNING_FOLIA && Thread.currentThread().getClass().equals(handler().getTickThreadClass());
        if (foliaTickThread || getServer().isGlobalTickThread()) try {
            return supplier.get();
        } catch (final Exception e) {
            return CompletableFuture.failedFuture(e);
        }
        final var future = new CompletableFuture<T>();
        getServer().getGlobalRegionScheduler().execute(this, () -> {
            supplier.get().thenAccept(future::complete).exceptionally(e -> {
                future.completeExceptionally(e);
                return null;
            });
        });
        return future;
    }

    public PaperLevelView levelView() {
        return levelView;
    }

    private void createPresetsFolder() {
        try {
            Files.createDirectories(presetsFolder);
        } catch (final IOException e) {
            getComponentLogger().warn("Failed to create presets folder", e);
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PortalListener(this), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        handler().foliaSupport().ifPresent(foliaSupport -> {
            final var foliaPortalListener = foliaSupport.createPortalListener();
            getServer().getPluginManager().registerEvents(foliaPortalListener, this);
        });
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS.newHandler(event -> {
            event.registrar().register(SaveAllCommand.create(this), "Save all worlds");
            event.registrar().register(SaveOffCommand.create(this), "Disable automatic world saving");
            event.registrar().register(SaveOnCommand.create(this), "Enable automatic world saving");
            event.registrar().register(WorldCommand.create(this), "The main command to interact with this plugin");
            event.registrar().register(WorldSetSpawnCommand.create(this, "setworldspawn"), "Set the world spawn");
        }));
    }

    private Metric<?> addGeneratorChart() {
        return Metric.stringArray("generators", () -> Arrays.stream(getServer().getPluginManager().getPlugins())
                .filter(Plugin::isEnabled)
                .filter(plugin -> generatorView().hasGenerator(plugin))
                .map(Plugin::getName)
                .toArray(String[]::new));
    }

    private Metric<?> addWorldsChart() {
        return Metric.number("worlds", () -> getServer().getWorlds().size());
    }

    private Metric<?> addEnvironmentsChart() {
        return Metric.stringArray("environments", () -> getServer().getWorlds().stream()
                .map(world -> switch (world.getEnvironment()) {
                    case NORMAL -> "Overworld";
                    case NETHER -> "Nether";
                    case THE_END -> "End";
                    case CUSTOM -> "Custom";
                })
                .toArray(String[]::new));
    }

    public VersionHandler handler() {
        return versionHandler;
    }

    @Override
    public ErrorTracker getErrorTracker() {
        return ERROR_TRACKER;
    }

    @Override
    public boolean isRunningFolia() {
        return RUNNING_FOLIA;
    }

    @Override
    public WorldRegistry getWorldRegistry() {
        return worldRegistry;
    }

    @Override
    public Stream<Dimension> listDimensions() {
        return handler().listDimensions();
    }

    @Override
    public Dimension getDimension(final World world) {
        return handler().getDimension(world);
    }

    @Override
    public Path getLevelDirectory() {
        return getServer().getLevelDirectory();
    }

    @Override
    public Stream<Path> listLevels() {
        return levelView.listLevels().stream();
    }

    @Override
    public CompletableFuture<ActionResult<World>> load(final Key key) {
        return levelView.read(key)
                .map(Level.Builder::build)
                .map(level -> level.create().thenApply(world -> {
                    worldRegistry.register(level, true);
                    return ActionResult.result(world, ActionResult.Status.SUCCESS);
                }))
                .orElseGet(() -> CompletableFuture.completedFuture(ActionResult.result(null, ActionResult.Status.FAILED)));
    }

    @Override
    public CompletableFuture<World> create(final Level level) {
        return supplyGlobal(() -> handler().createAsync(level));
    }

    @Override
    public CompletableFuture<Boolean> unload(final World world, final boolean save) {
        worldRegistry.setEnabled(world.key(), false);
        return levelView.unloadAsync(world, save);
    }

    @Override
    public CompletableFuture<Boolean> save(final World world, final boolean flush) {
        return levelView.saveAsync(world, flush).thenApply(ignored -> true);
    }

    @Override
    public CompletableFuture<ActionResult<World>> clone(final World world, final boolean full) {
        return levelView.cloneAsync(world, builder -> {
                }, full)
                .thenApply(clone -> ActionResult.result(clone, ActionResult.Status.SUCCESS));
    }

    @Override
    public CompletableFuture<ActionResult<Void>> delete(final World world) {
        return levelView.deleteAsync(world, false).thenApply(status -> ActionResult.result(null, status));
    }

    @Override
    public CompletableFuture<ActionResult<World>> regenerate(final World world) {
        return regenerate(world, builder -> {
        });
    }

    @Override
    public CompletableFuture<ActionResult<World>> regenerate(final World world, final Consumer<Level.Builder> builder) {
        return levelView.regenerateAsync(world, false, builder)
                .thenApply(status -> ActionResult.result(status == ActionResult.Status.SUCCESS ? getServer().getWorld(world.key()) : null, status));
    }

    @Override
    public boolean isEnabled(final World world) {
        return worldRegistry.isEnabled(world.key());
    }

    @Override
    public void setEnabled(final World world, final boolean enabled) {
        worldRegistry.setEnabled(world.key(), enabled);
    }

    @Override
    public String getEntryPermission(final World world) {
        return levelView.getEntryPermission(world);
    }

    @Override
    public BackupProvider getBackupProvider() {
        return backupProvider;
    }

    @Override
    public void setBackupProvider(final BackupProvider provider) {
        this.backupProvider = provider;
    }

    @Override
    public Path getDimensionsRoot() {
        return getServer().getLevelDirectory().resolve("dimensions");
    }

    @Override
    public Path resolveLevelDirectory(final Key key) {
        return getDimensionsRoot().resolve(key.namespace()).resolve(key.value());
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(final String worldName, @Nullable final String id) {
        return super.getDefaultWorldGenerator(worldName, id);
    }
}
