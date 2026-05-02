package net.thenextlvl.worlds;

import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.Contract;

import java.time.Instant;

/**
 * Represents a stored backup for a world.
 *
 * @since 4.0.0
 */
public interface Backup extends Keyed {
    /**
     * Returns the provider that owns this backup.
     *
     * @return the backup provider
     * @since 4.0.0
     */
    @Contract(pure = true)
    BackupProvider provider();

    /**
     * Returns the backup name.
     *
     * @return the backup name
     * @since 4.0.0
     */
    @Contract(pure = true)
    String name();

    /**
     * Returns the creation timestamp of this backup.
     *
     * @return the creation timestamp
     * @since 4.0.0
     */
    @Contract(pure = true)
    Instant createdAt();

    /**
     * Returns the backup size in bytes.
     *
     * @return the backup size in bytes
     * @since 4.0.0
     */
    @Contract(pure = true)
    long size();
}
