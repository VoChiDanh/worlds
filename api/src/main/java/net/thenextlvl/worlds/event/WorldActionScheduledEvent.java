package net.thenextlvl.worlds.event;

import net.thenextlvl.worlds.OperationScheduler;
import org.bukkit.World;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

/**
 * Represents an event triggered when an action is scheduled to be performed on a world.
 * This event allows developers to listen to such actions and optionally cancel them.
 *
 * @since 4.0.0
 */
public final class WorldActionScheduledEvent extends Event implements Cancellable {
    private static final HandlerList handlerList = new HandlerList();

    private boolean cancelled = false;
    private final OperationScheduler.Operation operation;

    @ApiStatus.Internal
    public WorldActionScheduledEvent(final OperationScheduler.Operation operation) {
        super(false);
        this.operation = operation;
    }

    /**
     * Retrieves the scheduled operation.
     *
     * @return the scheduled operation
     * @since 4.0.0
     */
    @Contract(pure = true)
    public OperationScheduler.Operation getOperation() {
        return operation;
    }

    @Override
    @Contract(pure = true)
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    @Contract(mutates = "this")
    public void setCancelled(final boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    /**
     * Represents the type of action scheduled to be performed on a {@link World}.
     *
     * @since 4.0.0
     */
    public enum ActionType {
        /**
         * This action is used to schedule the removal of a {@link World}.
         *
         * @see WorldDeleteEvent
         * @since 4.0.0
         */
        DELETE,
        /**
         * This action is used to schedule the regeneration of a {@link World}.
         *
         * @see WorldRegenerateEvent
         * @since 4.0.0
         */
        REGENERATE,
        /**
         * This action is used to schedule the restoration of a {@link World} backup.
         *
         * @see WorldBackupRestoreEvent
         * @since 4.0.0
         */
        RESTORE_BACKUP
    }
}
