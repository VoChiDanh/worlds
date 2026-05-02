package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.thenextlvl.worlds.event.WorldActionScheduledEvent;
import net.thenextlvl.worlds.view.PaperLevelView;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

import static net.thenextlvl.worlds.event.WorldActionScheduledEvent.ActionType;

@NullMarked
final class SimpleScheduledWorldOperations implements ScheduledWorldOperations {
    private final CopyOnWriteArraySet<ScheduledWorldOperations.Operation> operations = new CopyOnWriteArraySet<>();
    private final WorldsPlugin plugin;

    public SimpleScheduledWorldOperations(final WorldsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Stream<ScheduledWorldOperations.Operation> operations() {
        return operations.stream();
    }

    @Override
    public Stream<ScheduledWorldOperations.Operation> operations(final Key world) {
        return operations().filter(operation -> world.equals(operation.key()));
    }

    @Override
    public Stream<ScheduledWorldOperations.Operation> operations(final ActionType actionType) {
        return operations().filter(operation -> actionType.equals(operation.type()));
    }

    private boolean schedule(final World world, final ActionType type, final Runnable action) {
        if (isScheduled(world, type)) return false;

        final var event = new WorldActionScheduledEvent(world, type);
        if (!event.callEvent()) return false;

        operations.add(new Operation(type, world.key(), action));
        return true;
    }

    @Override
    public boolean scheduleDeletion(final World world) {
        final var path = world.getWorldPath();
        final var key = world.key();
        return schedule(world, ActionType.DELETE, () -> {
            PaperLevelView.delete(path);
            plugin.getWorldRegistry().unregister(key);
        });
    }

    @Override
    public boolean scheduleRegeneration(final World world) {
        final var path = world.getWorldPath();
        return schedule(world, ActionType.REGENERATE, () -> PaperLevelView.regenerate(path));
    }

    @Override
    public boolean scheduleBackupRestoration(final World world, final Backup backup) {
        final var key = world.key();
        return schedule(world, ActionType.RESTORE_BACKUP, () -> backup.provider().restoreNow(key, backup));
    }

    @Override
    public boolean isScheduled(final Key world, final ActionType actionType) {
        return operations(world).anyMatch(operation -> operation.type().equals(actionType));
    }

    @Override
    public boolean cancel(final Key world, final ActionType actionType) {
        return operations.removeIf(operation -> operation.key().equals(world.key()) && operation.type().equals(actionType));
    }

    @Override
    public boolean cancel(final ScheduledWorldOperations.Operation operation) {
        return operations.remove(operation);
    }

    @Override
    public void runScheduledOperations() {
        operations.forEach(operation -> {
            try {
                operation.run();
            } finally {
                operations.remove(operation);
            }
        });
    }

    public record Operation(
            ActionType type, Key key, Runnable runnable
    ) implements ScheduledWorldOperations.Operation {
        @Override
        public void run() {
            runnable.run();
        }
    }
}
