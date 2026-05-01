package net.thenextlvl.worlds.view;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.thenextlvl.worlds.Level;
import net.thenextlvl.worlds.WorldOperationException;
import net.thenextlvl.worlds.WorldRegistry;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.event.WorldCloneEvent;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.generator.WorldInfo;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NullMarked
public class PaperLevelView {
    private static final Key OVERWORLD = Key.key("overworld");
    private static final Key NETHER = Key.key("the_nether");
    private static final Key END = Key.key("the_end");

    // todo: update for new folder structure
    private static final Set<String> SKIP_DIRECTORIES = Set.of("advancements", "datapacks", "playerdata", "stats");
    private static final Set<String> SKIP_FILES = Set.of("metadata.dat", "session.lock");

    protected final WorldsPlugin plugin;

    public PaperLevelView(final WorldsPlugin plugin) {
        this.plugin = plugin;
    }

    public World getOverworld() {
        return Optional.ofNullable(plugin.getServer().getWorld(OVERWORLD))
                .orElseThrow(() -> new IllegalStateException("Overworld not found"));
    }

    public boolean isOverworld(final World world) {
        return world.key().equals(OVERWORLD);
    }

    public boolean isNether(final World world) {
        return world.key().equals(NETHER);
    }

    public boolean isEnd(final World world) {
        return world.key().equals(END);
    }

    public Optional<World> getTarget(final World world, final PortalType type) {
        return switch (type) {
            case NETHER -> switch (world.getEnvironment()) {
                case NORMAL, CUSTOM, THE_END -> findRelatedWorld(world, World.Environment.NETHER);
                case NETHER -> findPrimaryWorld(world);
            };
            case ENDER -> switch (world.getEnvironment()) {
                case NORMAL, CUSTOM, NETHER -> findRelatedWorld(world, World.Environment.THE_END);
                case THE_END -> findPrimaryWorld(world);
            };
            default -> Optional.empty();
        };
    }

    private Optional<World> findRelatedWorld(final World source, final World.Environment environment) {
        return plugin.getServer().getWorlds().stream()
                .filter(world -> !world.equals(source))
                .filter(world -> sameDimensionsGroup(source, world))
                .filter(world -> world.getEnvironment().equals(environment))
                .findFirst();
    }

    private Optional<World> findPrimaryWorld(final World source) {
        return plugin.getServer().getWorlds().stream()
                .filter(world -> !world.equals(source))
                .filter(world -> sameDimensionsGroup(source, world))
                .filter(world -> world.getEnvironment().equals(World.Environment.NORMAL)
                        || world.getEnvironment().equals(World.Environment.CUSTOM))
                .findFirst();
    }

    private boolean sameDimensionsGroup(final World first, final World second) {
        final var firstParent = first.getWorldPath().getParent();
        final var secondParent = second.getWorldPath().getParent();
        return firstParent != null && firstParent.equals(secondParent);
    }

    public Level.Builder read(final Key key, final WorldRegistry.Entry entry) {
        return Level.builder(key)
                .dimension(entry.dimension())
                .generator(entry.generator());
    }

    @SuppressWarnings("PatternValidation")
    public Optional<Key> key(final Path directory) {
        final var dimensions = plugin.getDimensionsRoot();
        final var relative = directory.startsWith(dimensions) ? dimensions.relativize(directory) : directory;

        if (relative.getNameCount() != 2) return Optional.empty();

        final var namespace = relative.getName(0).toString();
        if (!namespace.matches("[a-z0-9_\\-.]+")) return Optional.empty();

        final var value = relative.getName(1).toString();
        if (!value.matches("[a-z0-9_\\-./]+")) return Optional.empty();

        return Optional.of(Key.key(namespace, value));
    }

    public Stream<Path> listLevels() {
        return plugin.getWorldRegistry().worlds()
                .map(plugin::resolveLevelDirectory);
    }

    public @Unmodifiable Set<Path> listLevelFolders() {
        return listDirectories().stream()
                .filter(path -> Files.isDirectory(path.resolve("region")))
                .collect(Collectors.toUnmodifiableSet());
    }

    private @Unmodifiable Set<Path> listDirectories() {
        if (!Files.isDirectory(plugin.getDimensionsRoot())) return Set.of();
        try (final var namespaces = Files.list(plugin.getDimensionsRoot())) {
            return namespaces.filter(Files::isDirectory).<Path>mapMulti((path, consumer) -> {
                try (final var files = Files.list(path)) {
                    files.filter(Files::isDirectory).forEach(consumer);
                } catch (final IOException ignored) {
                }
            }).collect(Collectors.toUnmodifiableSet());
        } catch (final IOException e) {
            return Set.of();
        }
    }

    public CompletableFuture<Boolean> unloadAsync(final World world, final boolean save) {
        return saveLevelDataAsync(world).thenCompose(ignored -> {
            plugin.getServer().allowPausing(plugin, false);
            return plugin.supplyGlobal(() -> {
                final var dragonBattle = world.getEnderDragonBattle();
                if (!plugin.getServer().unloadWorld(world, save))
                    return CompletableFuture.completedFuture(false);
                if (dragonBattle != null) dragonBattle.getBossBar().removeAll();
                plugin.getServer().allowPausing(plugin, true);
                return CompletableFuture.completedFuture(true);
            });
        }).exceptionally(throwable -> {
            plugin.getComponentLogger().warn("Failed to save level data before unloading", throwable);
            return false;
        });
    }

    public CompletableFuture<@Nullable Void> saveAsync(final World world, final boolean flush) {
        return plugin.supplyGlobal(() -> {
            try {
                return plugin.handler().saveAsync(world, flush);
            } catch (final Exception e) {
                WorldsPlugin.ERROR_TRACKER.trackError(e);
                return CompletableFuture.failedFuture(e);
            }
        }).thenRun(() -> saveLevelDataAsync(world));
    }

    public CompletableFuture<@Nullable Void> saveLevelDataAsync(final World world) {
        return plugin.handler().saveLevelDataAsync(world);
    }

    public String findFreeName(final String name) {
        final var usedNames = plugin.getServer().getWorlds().stream()
                .map(WorldInfo::getName)
                .collect(Collectors.toSet());
        return findFreeName(usedNames, name);
    }

    @SuppressWarnings("PatternValidation")
    public Key findFreeKey(final Key key) {
        return findFreeKey(key.namespace(), key.value());
    }

    @SuppressWarnings("PatternValidation")
    public Key findFreeKey(@KeyPattern.Namespace final String namespace, @KeyPattern.Value final String value) {
        final var usedValues = plugin.getServer().getWorlds().stream()
                .map(World::key)
                .filter(key -> key.namespace().equals(namespace))
                .map(Key::value)
                .collect(Collectors.toSet());
        return Key.key(namespace, findFreeValue(usedValues, value));
    }

    public Path findFreePath(final String name) {
        final var usedPaths = listDirectories().stream()
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toSet());
        return Path.of(findFreeName(usedPaths, name));
    }

    public static String findFreeName(final Set<String> usedNames, final String name) {
        if (!usedNames.contains(name)) return name;

        var baseName = name;
        int suffix = 1;
        String candidate = baseName + " (1)";

        final var pattern = Pattern.compile("^(.+) \\((\\d+)\\)$");
        final var matcher = pattern.matcher(name);

        if (matcher.matches()) {
            baseName = matcher.group(1);
            suffix = Integer.parseInt(matcher.group(2)) + 1;
            candidate = baseName + " (" + suffix + ")";
            suffix++;
        }

        while (usedNames.contains(candidate)) {
            candidate = baseName + " (" + suffix++ + ")";
        }

        return candidate;
    }

    public static String findFreeValue(final Set<String> usedValues, final String value) {
        if (!usedValues.contains(value)) return value;

        var baseValue = value;
        int suffix = 1;
        String candidate = baseValue + "_1";

        final var pattern = Pattern.compile("^(.+) \\((\\d+)\\)$");
        final var matcher = pattern.matcher(value);

        if (matcher.matches()) {
            baseValue = matcher.group(1);
            suffix = Integer.parseInt(matcher.group(2)) + 1;
            candidate = baseValue + "_" + suffix;
            suffix++;
        }

        while (usedValues.contains(candidate)) {
            candidate = baseValue + "_" + suffix++;
        }

        return candidate;
    }

    public static @Subst("pattern") String createKey(final String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9_\\-./ ]+", "")
                .replace(" ", "_");
    }

    public CompletableFuture<World> cloneAsync(final World world, final Consumer<Level.Builder> builder, final boolean full) {
        return plugin.supplyGlobal(() -> cloneInternal(world, builder, full));
    }

    private CompletableFuture<World> cloneInternal(final World world, final Consumer<Level.Builder> builder, final boolean full) {
        final var levelBuilder = Level.copy(world);

        levelBuilder.name(findFreeName(world.getName()));
        levelBuilder.key(findFreeKey(world.key()));

        builder.accept(levelBuilder);
        final var clone = levelBuilder.build();

        try {
            if (plugin.getServer().getWorld(clone.key()) != null) throw new WorldOperationException(
                    WorldOperationException.Reason.WORLD_KEY_EXISTS
            ).key(clone.key()).world(clone.getName());
            if (plugin.getServer().getWorld(clone.getName()) != null) throw new WorldOperationException(
                    WorldOperationException.Reason.WORLD_NAME_EXISTS
            ).key(clone.key()).world(clone.getName());
            if (Files.exists(clone.getDirectory())) throw new WorldOperationException(
                    Files.isDirectory(clone.getDirectory())
                            ? WorldOperationException.Reason.WORLD_PATH_EXISTS
                            : WorldOperationException.Reason.TARGET_PATH_IS_FILE
            ).key(clone.key()).world(clone.getName()).path(clone.getDirectory());
        } catch (final RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }

        final var event = new WorldCloneEvent(world, clone, full);
        event.callEvent();

        return full ? saveAsync(world, true).thenCompose(ignored -> {
            try {
                copyDirectory(world.getWorldPath(), clone.getDirectory(), event.getFileFilter());
                return clone.create();
            } catch (final IOException e) {
                return CompletableFuture.failedFuture(new WorldOperationException(
                        WorldOperationException.Reason.BACKUP_WRITE_FAILED,
                        e
                ).key(clone.key()).world(clone.getName()).path(clone.getDirectory()));
            }
        }) : clone.create();
    }

    public void copyDirectory(final Path source, final Path destination, @Nullable final BiPredicate<Path, BasicFileAttributes> filter) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path path, final BasicFileAttributes attributes) throws IOException {
                if (SKIP_DIRECTORIES.contains(path.getFileName().toString())) return FileVisitResult.SKIP_SUBTREE;
                if (filter != null && !filter.test(path, attributes)) return FileVisitResult.SKIP_SUBTREE;
                Files.createDirectories(destination.resolve(source.relativize(path)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path path, final BasicFileAttributes attributes) throws IOException {
                if (SKIP_FILES.contains(path.getFileName().toString())) return FileVisitResult.CONTINUE;
                if (filter != null && !filter.test(path, attributes)) return FileVisitResult.CONTINUE;
                Files.copy(path, destination.resolve(source.relativize(path)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(final Path path, final IOException exc) {
                plugin.getComponentLogger().warn("Failed to copy file: {}", path, exc);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
