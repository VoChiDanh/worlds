package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.thenextlvl.binder.StaticBinder;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Main access point for world management services.
 *
 * @since 4.0.0
 */
@ApiStatus.NonExtendable
public interface WorldsAccess extends Plugin {
    /**
     * Returns the active worlds plugin access instance.
     *
     * @return the access instance
     * @since 4.0.0
     */
    static @CheckReturnValue WorldsAccess access() {
        final class Cache {
            private static final WorldsAccess INSTANCE = StaticBinder.getInstance(WorldsAccess.class.getClassLoader()).find(WorldsAccess.class);
        }
        return Cache.INSTANCE;
    }

    /**
     * Returns the world registry.
     *
     * @return the world registry
     * @since 4.0.0
     */
    @Contract(pure = true)
    WorldRegistry getWorldRegistry();

    /**
     * Lists available dimensions.
     *
     * @return a stream of dimensions
     * @since 4.0.0
     */
    @Contract(pure = true)
    Stream<Dimension> listDimensions();

    /**
     * Returns the dimension of a loaded world.
     *
     * @param world the world
     * @return the world's dimension
     * @since 4.0.0
     */
    @Contract(pure = true)
    Dimension getDimension(World world);

    /**
     * Returns the root level directory.
     *
     * @return the root level directory
     * @since 4.0.0
     */
    @Contract(pure = true)
    Path getLevelDirectory();

    /**
     * Lists known level directories.
     *
     * @return a stream of level paths
     * @since 4.0.0
     */
    @Contract(pure = true)
    Stream<Path> listLevels();

    /**
     * Loads a world by key.
     *
     * @param key the world key
     * @return a future completing with the loaded world
     * @since 4.0.0
     */
    CompletableFuture<World> load(Key key);

    /**
     * Creates a world from a level description.
     *
     * @param level the level description
     * @return a future completing with the created world
     * @since 4.0.0
     */
    CompletableFuture<World> create(Level level);

    /**
     * Unloads a world.
     *
     * @param world the world
     * @param save  whether to save before unloading
     * @return a future completing with {@code true} if the world was unloaded
     * @since 4.0.0
     */
    @Contract(mutates = "param1")
    CompletableFuture<Boolean> unload(World world, boolean save);

    /**
     * Saves a world.
     *
     * @param world the world
     * @param flush whether to flush data to disk
     * @return a future completing with {@code true} if the world was saved
     * @since 4.0.0
     */
    @Contract(mutates = "param1")
    CompletableFuture<Boolean> save(World world, boolean flush);

    /**
     * Clones a world.
     *
     * @param world the world to clone
     * @param full  whether to clone all world files
     * @return a future completing with the cloned world
     * @since 4.0.0
     */
    @Contract(mutates = "param1")
    CompletableFuture<World> clone(World world, boolean full);

    /**
     * Clones a world with builder customization.
     *
     * @param world   the world to clone
     * @param builder the builder customizer
     * @param full    whether to clone all world files
     * @return a future completing with the cloned world
     * @since 4.0.0
     */
    @Contract(mutates = "param1")
    CompletableFuture<World> clone(World world, Consumer<Level.Builder> builder, boolean full);

    /**
     * Deletes a world.
     *
     * @param world the world
     * @return a future completing with {@code true} if the world was deleted
     * @since 4.0.0
     */
    @Contract(mutates = "param1")
    CompletableFuture<Boolean> delete(World world);

    /**
     * Regenerates a world.
     *
     * @param world the world
     * @return a future completing with the regenerated world
     * @since 4.0.0
     */
    @Contract(mutates = "param1")
    CompletableFuture<World> regenerate(World world);

    /**
     * Regenerates a world with builder customization.
     *
     * @param world   the world
     * @param builder the builder customizer
     * @return a future completing with the regenerated world
     * @since 4.0.0
     */
    @Contract(mutates = "param1")
    CompletableFuture<World> regenerate(World world, Consumer<Level.Builder> builder);

    /**
     * Creates a backup for a world.
     *
     * @param world the world
     * @param name  the backup name, or {@code null}
     * @return a future completing with the created backup
     * @since 4.0.0
     */
    @Contract(mutates = "param1")
    CompletableFuture<Backup> createBackup(World world, @Nullable String name);

    /**
     * Restores a backup into a world.
     *
     * @param world  the world
     * @param backup the backup to restore
     * @return a future completing with the restored world
     * @since 4.0.0
     */
    @Contract(mutates = "param1")
    CompletableFuture<World> restoreBackup(World world, Backup backup);

    /**
     * Returns the permission required to enter a world.
     *
     * @param world the world
     * @return the entry permission
     * @since 4.0.0
     */
    @Contract(pure = true)
    String getEntryPermission(World world);

    /**
     * Returns the scheduled operation manager.
     *
     * @return the scheduler
     * @since 4.0.0
     */
    @Contract(pure = true)
    ScheduledWorldOperations getScheduler();

    /**
     * Returns the backup provider.
     *
     * @return the backup provider
     * @since 4.0.0
     */
    @Contract(pure = true)
    BackupProvider getBackupProvider();

    /**
     * Sets the backup provider.
     *
     * @param provider the backup provider
     * @since 4.0.0
     */
    @Contract(mutates = "this")
    void setBackupProvider(BackupProvider provider);

    /**
     * Returns the dimensions root directory.
     *
     * @return the dimensions root directory
     * @since 4.0.0
     */
    @Contract(pure = true)
    Path getDimensionsRoot();

    /**
     * Resolves the level directory for a world key.
     *
     * @param key the world key
     * @return the level directory
     * @since 4.0.0
     */
    @Contract(pure = true)
    Path resolveLevelDirectory(Key key);
}
