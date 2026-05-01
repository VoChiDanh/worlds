package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.thenextlvl.worlds.event.WorldActionScheduledEvent;
import org.bukkit.World;
import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;

public sealed interface ScheduledWorldOperations permits SimpleScheduledWorldOperations {
    @Contract(pure = true)
    Stream<Operation> operations();

    @Contract(pure = true)
    Stream<Operation> operations(Key world);

    @Contract(pure = true)
    Stream<Operation> operations(World world);

    @Contract(pure = true)
    Stream<Operation> operations(WorldActionScheduledEvent.ActionType actionType);

    ActionResult.Status schedule(World world, WorldActionScheduledEvent.ActionType actionType, Consumer<Path> action);

    ActionResult.Status scheduleDeletion(final World world);

    ActionResult.Status scheduleRegeneration(final World world);

    ActionResult.Status scheduleBackupRestoration(final World world, final Backup backup);

    @Contract(pure = true)
    boolean isScheduled(World world, WorldActionScheduledEvent.ActionType actionType);

    @Contract(mutates = "this")
    boolean cancel(World world, WorldActionScheduledEvent.ActionType actionType);

    @Contract(mutates = "this")
    boolean cancel(Operation operation);

    @Contract(mutates = "this")
    void runScheduledOperations();

    sealed interface Operation extends Keyed, Runnable permits SimpleScheduledWorldOperations.Operation {
        @Contract(pure = true)
        WorldActionScheduledEvent.ActionType type();
    }
}
