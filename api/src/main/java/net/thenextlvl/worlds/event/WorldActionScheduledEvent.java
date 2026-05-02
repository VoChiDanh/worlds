package net.thenextlvl.worlds.event;

import org.bukkit.World;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.world.WorldEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

/**
 * Represents an event triggered when an action is scheduled to be performed on a {@link World}.
 * This event allows developers to listen to such actions and optionally cancel them.
 * <p>
 * This event provides details about the type of action being scheduled and allows cancellation.
 * <p>
 * The {@link ActionType} enum defines the possible actions that can be scheduled,
 * such as deleting a world or regenerating it.
 *
 * @since 4.0.0
 */
public final class WorldActionScheduledEvent extends WorldEvent implements Cancellable {
    private static final HandlerList handlerList = new HandlerList();

    private boolean cancelled = false;
    private final ActionType
            actionType;

    @ApiStatus.Internal
    public WorldActionScheduledEvent(final World world, final ActionType actionType) {
        super(world, false);
        this.actionType = actionType;
    }

    /**
     * Retrieves the type of action scheduled to be performed on a world.
     *
     * @return the scheduled action type
     */
    @Contract(pure = true)
    public ActionType getActionType() {
        return actionType;
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
     */
    public enum ActionType {
        /**
         * This action is used to schedule the removal of a {@link World}.
         *
         * @see WorldDeleteEvent
         */
        DELETE,
        /**
         * This action is used to schedule the regeneration of a {@link World}.
         *
         * @see WorldRegenerateEvent
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
