package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.thenextlvl.worlds.event.WorldActionScheduledEvent;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.util.stream.Stream;

/**
 * Tracks and executes world operations that must be deferred.
 *
 * @since 4.0.0
 */
@ApiStatus.NonExtendable
public interface ScheduledWorldOperations {
    /**
     * Returns all scheduled operations.
     *
     * @return a stream of scheduled operations
     * @since 4.0.0
     */
    @Contract(pure = true)
    Stream<Operation> operations();

    /**
     * Returns scheduled operations for a world key.
     *
     * @param world the world key
     * @return a stream of scheduled operations
     * @since 4.0.0
     */
    @Contract(pure = true)
    Stream<Operation> operations(Key world);

    /**
     * Returns scheduled operations for a world.
     *
     * @param world the world
     * @return a stream of scheduled operations
     * @since 4.0.0
     */
    @Contract(pure = true)
    default Stream<Operation> operations(final World world) {
        return operations(world.key());
    }

    /**
     * Returns scheduled operations of the specified action type.
     *
     * @param actionType the action type
     * @return a stream of scheduled operations
     * @since 4.0.0
     */
    @Contract(pure = true)
    Stream<Operation> operations(WorldActionScheduledEvent.ActionType actionType);

    /**
     * Schedules a world for deletion.
     *
     * @param world the world
     * @return {@code true} if the operation was scheduled
     * @since 4.0.0
     */
    boolean scheduleDeletion(final World world);

    /**
     * Schedules a world for regeneration.
     *
     * @param world the world
     * @return {@code true} if the operation was scheduled
     * @since 4.0.0
     */
    boolean scheduleRegeneration(final World world);

    /**
     * Schedules a backup restoration for a world.
     *
     * @param world  the world
     * @param backup the backup to restore
     * @return {@code true} if the operation was scheduled
     * @since 4.0.0
     */
    boolean scheduleBackupRestoration(final World world, final Backup backup);

    /**
     * Checks whether an operation is scheduled for a world.
     *
     * @param world      the world
     * @param actionType the action type
     * @return {@code true} if the operation is scheduled
     * @since 4.0.0
     */
    @Contract(pure = true)
    default boolean isScheduled(final World world, final WorldActionScheduledEvent.ActionType actionType) {
        return isScheduled(world.key(), actionType);
    }

    /**
     * Checks whether an operation is scheduled for a world key.
     *
     * @param world      the world key
     * @param actionType the action type
     * @return {@code true} if the operation is scheduled
     * @since 4.0.0
     */
    @Contract(pure = true)
    boolean isScheduled(Key world, WorldActionScheduledEvent.ActionType actionType);

    /**
     * Cancels a scheduled operation for a world.
     *
     * @param world      the world
     * @param actionType the action type
     * @return {@code true} if an operation was cancelled
     * @since 4.0.0
     */
    @Contract(mutates = "this")
    default boolean cancel(final World world, final WorldActionScheduledEvent.ActionType actionType) {
        return cancel(world.key(), actionType);
    }

    /**
     * Cancels a scheduled operation for a world key.
     *
     * @param world      the world key
     * @param actionType the action type
     * @return {@code true} if an operation was cancelled
     * @since 4.0.0
     */
    @Contract(mutates = "this")
    boolean cancel(Key world, WorldActionScheduledEvent.ActionType actionType);

    /**
     * Cancels a scheduled operation.
     *
     * @param operation the operation
     * @return {@code true} if the operation was cancelled
     * @since 4.0.0
     */
    @Contract(mutates = "this")
    boolean cancel(Operation operation);

    /**
     * Runs all scheduled operations.
     *
     * @since 4.0.0
     */
    @Contract(mutates = "this")
    void runScheduledOperations();

    /**
     * A scheduled world operation.
     *
     * @since 4.0.0
     */
    @ApiStatus.NonExtendable
    interface Operation extends Keyed, Runnable {
        /**
         * Returns the scheduled action type.
         *
         * @return the action type
         * @since 4.0.0
         */
        @Contract(pure = true)
        WorldActionScheduledEvent.ActionType type();
    }
}
