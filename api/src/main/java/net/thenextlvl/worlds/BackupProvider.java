package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Provides backup storage and restoration for worlds.
 *
 * @since 4.0.0
 */
public interface BackupProvider {
    /**
     * Creates a backup with a generated provider-specific name.
     *
     * @param world the world to back up
     * @return a future completing with the created backup
     * @since 4.0.0
     */
    @Contract(mutates = "io")
    default CompletableFuture<Backup> backup(final World world) {
        return backup(world, null);
    }

    /**
     * Creates a backup for a world.
     *
     * @param world the world to back up
     * @param name  the backup name, or {@code null} to generate one
     * @return a future completing with the created backup
     * @since 4.0.0
     */
    @Contract(mutates = "io")
    CompletableFuture<Backup> backup(World world, @Nullable String name);

    /**
     * Restores a backup into a world.
     *
     * @param world  the world to restore
     * @param backup the backup to restore
     * @return a future completing with the restored world
     * @since 4.0.0
     */
    @Contract(mutates = "param,io")
    CompletableFuture<World> restore(World world, Backup backup);

    /**
     * Restores a backup immediately for the given world key.
     *
     * @param key    the world key
     * @param backup the backup to restore
     * @since 4.0.0
     */
    @ApiStatus.OverrideOnly
    @Contract(mutates = "io")
    void restoreNow(Key key, Backup backup);

    /**
     * Lists all backups.
     *
     * @return a future completing with all backups
     * @since 4.0.0
     */
    @Contract(pure = true)
    CompletableFuture<Stream<Backup>> listBackups();

    /**
     * Lists backups for a world.
     *
     * @param world the world
     * @return a future completing with the world's backups
     * @since 4.0.0
     */
    @Contract(pure = true)
    default CompletableFuture<Stream<Backup>> listBackups(final World world) {
        return listBackups(world.key());
    }

    /**
     * Lists backups for a world key.
     *
     * @param world the world key
     * @return a future completing with the world's backups
     * @since 4.0.0
     */
    @Contract(pure = true)
    CompletableFuture<Stream<Backup>> listBackups(Key world);

    /**
     * Finds the latest backup for a world.
     *
     * @param world the world
     * @return a future completing with the latest backup, or empty
     * @since 4.0.0
     */
    @Contract(pure = true)
    default CompletableFuture<Optional<Backup>> findBackup(final World world) {
        return findBackup(world.key());
    }

    /**
     * Finds the latest backup for a world key.
     *
     * @param world the world key
     * @return a future completing with the latest backup, or empty
     * @since 4.0.0
     */
    @Contract(pure = true)
    CompletableFuture<Optional<Backup>> findBackup(Key world);

    /**
     * Finds a named backup for a world.
     *
     * @param world the world
     * @param name  the backup name
     * @return a future completing with the backup, or empty
     * @since 4.0.0
     */
    @Contract(pure = true)
    default CompletableFuture<Optional<Backup>> findBackup(final World world, final String name) {
        return findBackup(world.key(), name);
    }

    /**
     * Finds a named backup for a world key.
     *
     * @param world the world key
     * @param name  the backup name
     * @return a future completing with the backup, or empty
     * @since 4.0.0
     */
    @Contract(pure = true)
    CompletableFuture<Optional<Backup>> findBackup(Key world, String name);

    /**
     * Deletes a named backup for a world.
     *
     * @param world the world
     * @param name  the backup name
     * @return a future completing with {@code true} if a backup was deleted
     * @since 4.0.0
     */
    default CompletableFuture<Boolean> delete(final World world, final String name) {
        return delete(world.key(), name);
    }

    /**
     * Deletes a named backup for a world key.
     *
     * @param world the world key
     * @param name  the backup name
     * @return a future completing with {@code true} if a backup was deleted
     * @since 4.0.0
     */
    CompletableFuture<Boolean> delete(Key world, String name);

    /**
     * Deletes a backup.
     *
     * @param backup the backup
     * @return a future completing with {@code true} if a backup was deleted
     * @since 4.0.0
     */
    CompletableFuture<Boolean> delete(Backup backup);
}
