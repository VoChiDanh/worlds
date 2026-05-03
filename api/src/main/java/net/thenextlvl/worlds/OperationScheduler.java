package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.thenextlvl.worlds.event.WorldActionScheduledEvent;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Keeps track of, and schedules, world operations that are performed on server startup.
 *
 * @since 4.0.0
 */
@ApiStatus.NonExtendable
public interface OperationScheduler {
    /**
     * Returns all scheduled operations.
     *
     * @return a stream of scheduled operations
     * @since 4.0.0
     */
    @Contract(pure = true)
    Stream<Operation> operations();

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
     * Returns the scheduled operation for a world key.
     *
     * @param world the world key
     * @return the scheduled operation
     * @since 4.0.0
     */
    @Contract(pure = true)
    Optional<Operation> operation(Key world);

    /**
     * Returns the scheduled operation for a world.
     *
     * @param world the world
     * @return the scheduled operation
     * @since 4.0.0
     */
    @Contract(pure = true)
    default Optional<Operation> operation(final World world) {
        return operation(world.key());
    }

    /**
     * Schedules an operation.
     *
     * @param operation the operation to schedule
     * @return {@code true} if the operation was scheduled, {@code false} otherwise
     * @since 4.0.0
     */
    @Contract(mutates = "this")
    boolean schedule(Operation operation);

    /**
     * Checks whether an operation is scheduled for a world.
     *
     * @param world      the world
     * @param actionType the action type
     * @return {@code true} if the operation is scheduled, {@code false} otherwise
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
     * @return {@code true} if the operation is scheduled, {@code false} otherwise
     * @since 4.0.0
     */
    @Contract(pure = true)
    boolean isScheduled(Key world, WorldActionScheduledEvent.ActionType actionType);

    /**
     * Cancels a scheduled operation for a world.
     *
     * @param world      the world
     * @param actionType the action type
     * @return {@code true} if an operation was canceled, {@code false} otherwise
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
     * @return {@code true} if an operation was canceled, {@code false} otherwise
     * @since 4.0.0
     */
    @Contract(mutates = "this")
    boolean cancel(Key world, WorldActionScheduledEvent.ActionType actionType);

    /**
     * Cancels a scheduled operation.
     *
     * @param operation the operation
     * @return {@code true} if the operation was canceled, {@code false} otherwise
     * @since 4.0.0
     */
    @Contract(mutates = "this")
    boolean cancel(Operation operation);

    /**
     * A scheduled world operation.
     *
     * @since 4.0.0
     */
    sealed interface Operation extends Keyed permits DeleteOperation, RegenerateOperation, BackupRestoreOperation {
        /**
         * Returns the scheduled action type.
         *
         * @return the action type
         * @since 4.0.0
         */
        @Contract(pure = true)
        WorldActionScheduledEvent.ActionType type();
    }

    /**
     * A scheduled world deletion operation.
     *
     * @param key the world key
     * @since 4.0.0
     */
    record DeleteOperation(Key key) implements Operation {
        @Override
        public WorldActionScheduledEvent.ActionType type() {
            return WorldActionScheduledEvent.ActionType.DELETE;
        }
    }

    /**
     * A scheduled world regeneration operation.
     *
     * @param key  the world key
     * @param seed the seed to use when regenerating the world
     * @since 4.0.0
     */
    record RegenerateOperation(Key key, long seed) implements Operation {
        @Override
        public WorldActionScheduledEvent.ActionType type() {
            return WorldActionScheduledEvent.ActionType.REGENERATE;
        }
    }

    /**
     * A scheduled backup restoration operation.
     *
     * @param key    the world key
     * @param backup the backup name
     * @since 4.0.0
     */
    record BackupRestoreOperation(Key key, String backup) implements Operation {
        @Override
        public WorldActionScheduledEvent.ActionType type() {
            return WorldActionScheduledEvent.ActionType.RESTORE_BACKUP;
        }
    }
}
