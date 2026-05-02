package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.thenextlvl.worlds.event.WorldActionScheduledEvent;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.util.stream.Stream;

@ApiStatus.NonExtendable
public interface ScheduledWorldOperations {
    @Contract(pure = true)
    Stream<Operation> operations();

    @Contract(pure = true)
    Stream<Operation> operations(Key world);

    @Contract(pure = true)
    default Stream<Operation> operations(final World world) {
        return operations(world.key());
    }

    @Contract(pure = true)
    Stream<Operation> operations(WorldActionScheduledEvent.ActionType actionType);

    boolean scheduleDeletion(final World world);

    boolean scheduleRegeneration(final World world);

    boolean scheduleBackupRestoration(final World world, final Backup backup);

    @Contract(pure = true)
    default boolean isScheduled(final World world, final WorldActionScheduledEvent.ActionType actionType) {
        return isScheduled(world.key(), actionType);
    }

    @Contract(pure = true)
    boolean isScheduled(Key world, WorldActionScheduledEvent.ActionType actionType);

    @Contract(mutates = "this")
    default boolean cancel(final World world, final WorldActionScheduledEvent.ActionType actionType) {
        return cancel(world.key(), actionType);
    }

    @Contract(mutates = "this")
    boolean cancel(Key world, WorldActionScheduledEvent.ActionType actionType);

    @Contract(mutates = "this")
    boolean cancel(Operation operation);

    @Contract(mutates = "this")
    void runScheduledOperations();

    @ApiStatus.NonExtendable
    interface Operation extends Keyed, Runnable {
        @Contract(pure = true)
        WorldActionScheduledEvent.ActionType type();
    }
}
