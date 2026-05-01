package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.thenextlvl.worlds.event.WorldActionScheduledEvent;
import org.bukkit.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static net.thenextlvl.worlds.event.WorldActionScheduledEvent.ActionType;

final class SimpleScheduledWorldOperations implements ScheduledWorldOperations {
    public static final SimpleScheduledWorldOperations INSTANCE = new SimpleScheduledWorldOperations();
    private final CopyOnWriteArraySet<ScheduledWorldOperations.Operation> operations = new CopyOnWriteArraySet<>();

    private SimpleScheduledWorldOperations() {
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
    public Stream<ScheduledWorldOperations.Operation> operations(final World world) {
        return operations(world.key());
    }

    @Override
    public Stream<ScheduledWorldOperations.Operation> operations(final ActionType actionType) {
        return operations().filter(operation -> actionType.equals(operation.type()));
    }

    @Override
    public ActionResult.Status schedule(final World world, final ActionType type, final Consumer<Path> consumer) {
        if (isScheduled(world, type)) return ActionResult.Status.SCHEDULED;

        final var event = new WorldActionScheduledEvent(world, type);
        if (!event.callEvent()) return ActionResult.Status.FAILED;

        final var action = event.getAction() == null ? consumer : event.getAction().andThen(consumer);

        final var path = world.getWorldFolder().toPath();
        operations.add(new Operation(type, world.key(), () -> action.accept(path)));
        return ActionResult.Status.SCHEDULED;
    }

    @Override
    public ActionResult.Status scheduleDeletion(final World world) {
        return schedule(world, WorldActionScheduledEvent.ActionType.DELETE, this::delete);
    }

    @Override
    public ActionResult.Status scheduleRegeneration(final World world) {
        return schedule(world, WorldActionScheduledEvent.ActionType.REGENERATE, this::regenerate);
    }

    @Override
    public ActionResult.Status scheduleBackupRestoration(final World world, final Backup backup) {
        return schedule(world, WorldActionScheduledEvent.ActionType.RESTORE_BACKUP, path -> backup.provider().restoreNow(path, backup));
    }

    @Override
    public boolean isScheduled(final World world, final ActionType actionType) {
        return operations(world).anyMatch(operation -> operation.type().equals(actionType));
    }

    @Override
    public boolean cancel(final World world, final ActionType actionType) {
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

    private void regenerate(final Path level) {
        // todo: upgrade to 26.1+ format
        delete(level.resolve("DIM-1"));
        delete(level.resolve("DIM1"));
        delete(level.resolve("advancements"));
        delete(level.resolve("data"));
        delete(level.resolve("entities"));
        delete(level.resolve("playerdata"));
        delete(level.resolve("poi"));
        delete(level.resolve("region"));
        delete(level.resolve("stats"));
    }

    private void delete(final Path path) {
        try {
            if (!Files.isDirectory(path)) Files.deleteIfExists(path);
            else try (final var files = Files.list(path)) {
                files.forEach(this::delete);
                Files.deleteIfExists(path);
            }
        } catch (final IOException e) {
            WorldsAccess.access().getComponentLogger().warn("Failed to delete {}", path, e);
        }
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
