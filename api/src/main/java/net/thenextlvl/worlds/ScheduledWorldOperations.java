package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.thenextlvl.worlds.event.WorldActionScheduledEvent;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;

@ApiStatus.NonExtendable
public interface ScheduledWorldOperations {
    @Contract(pure = true)
    Stream<Operation> operations();

    @Contract(pure = true)
    Stream<Operation> operations(Key world);

    @Contract(pure = true)
    Stream<Operation> operations(World world);

    @Contract(pure = true)
    Stream<Operation> operations(WorldActionScheduledEvent.ActionType actionType);

    boolean schedule(World world, WorldActionScheduledEvent.ActionType actionType, Consumer<Path> action);

    boolean scheduleDeletion(final World world);

    boolean scheduleRegeneration(final World world);

    boolean scheduleBackupRestoration(final World world, final Backup backup);

    @Contract(pure = true)
    boolean isScheduled(World world, WorldActionScheduledEvent.ActionType actionType);

    @Contract(mutates = "this")
    boolean cancel(World world, WorldActionScheduledEvent.ActionType actionType);

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
