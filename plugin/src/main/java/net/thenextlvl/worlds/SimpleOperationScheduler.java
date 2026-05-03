package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.thenextlvl.nbt.NBTInputStream;
import net.thenextlvl.nbt.NBTOutputStream;
import net.thenextlvl.nbt.tag.CompoundTag;
import net.thenextlvl.nbt.tag.Tag;
import net.thenextlvl.worlds.event.WorldActionScheduledEvent;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static net.thenextlvl.worlds.event.WorldActionScheduledEvent.ActionType;

@NullMarked
final class SimpleOperationScheduler implements OperationScheduler {
    private final Map<Key, Operation> operations = new ConcurrentHashMap<>();
    private final WorldsPlugin plugin;
    private final Path path;

    public SimpleOperationScheduler(final WorldsPlugin plugin) {
        this.path = plugin.getDataPath().resolve("scheduled-operations.dat");
        this.plugin = plugin;
    }

    @Override
    public Stream<OperationScheduler.Operation> operations() {
        return operations.values().stream();
    }

    @Override
    public Stream<OperationScheduler.Operation> operations(final ActionType actionType) {
        return operations().filter(operation -> actionType.equals(operation.type()));
    }

    @Override
    public Optional<OperationScheduler.Operation> operation(final Key world) {
        return Optional.ofNullable(operations.get(world));
    }

    @Override
    public boolean schedule(final OperationScheduler.Operation operation) {
        final var event = new WorldActionScheduledEvent(operation);
        if (!event.callEvent()) return false;

        operations.put(operation.key(), operation);
        save();
        return true;
    }

    @Override
    public boolean isScheduled(final Key world, final ActionType actionType) {
        return operation(world).filter(operation -> operation.type().equals(actionType)).isPresent();
    }

    @Override
    public boolean cancel(final Key world, final ActionType actionType) {
        final var operation = operations.get(world);
        if (operation == null || !operation.type().equals(actionType)) return false;
        if (!operations.remove(world, operation)) return false;
        save();
        return true;
    }

    @Override
    public boolean cancel(final Key world) {
        if (operations.remove(world) == null) return false;
        save();
        return true;
    }

    @Override
    public boolean cancel(final OperationScheduler.Operation operation) {
        if (!operations.remove(operation.key(), operation)) return false;
        save();
        return true;
    }

    public void runScheduledOperations() {
        if (operations.isEmpty()) return;
        operations.values().forEach(operation -> {
            try {
                run(operation);
            } catch (final Exception e) {
                plugin.getComponentLogger().error("Failed to run scheduled world operations", e);
            }
        });
        operations.clear();
        save();
    }

    @SuppressWarnings("PatternValidation")
    public void load() {
        if (!Files.isRegularFile(path)) return;
        try (final var input = NBTInputStream.create(path)) {
            final var root = input.readTag();
            root.forEach((world, tag) -> {
                try {
                    final var operation = deserialize(Key.key(world), tag.getAsCompound());
                    if (operations.putIfAbsent(operation.key(), operation) == null) return;
                    plugin.getComponentLogger().warn("Skip loading scheduled world operation for {} due to conflicting operation", world);
                } catch (final Exception e) {
                    plugin.getComponentLogger().warn("Failed to read scheduled world operation for {}", world, e);
                }
            });
        } catch (final Exception e) {
            plugin.getComponentLogger().error("Failed to read scheduled world operations from {}", path, e);
        }
    }

    private void save() {
        try (final var output = NBTOutputStream.create(path)) {
            final var root = CompoundTag.builder();
            operations.forEach((key, operation) -> root.put(key.asString(), serialize(operation)));
            Files.createDirectories(path.getParent());
            output.writeTag(null, root.build());
        } catch (final Exception e) {
            plugin.getComponentLogger().warn("Failed to write scheduled world operations to {}", path, e);
        }
    }

    private void run(final OperationScheduler.Operation operation) {
        switch (operation) {
            case final DeleteOperation delete -> {
                final var level = plugin.resolveLevelDirectory(delete.key());
                plugin.levelView().delete(level, delete.key());
                plugin.getWorldRegistry().unregister(delete.key());
                plugin.getComponentLogger().info("Deleted world {}", delete.key());
            }
            case final RegenerateOperation regenerate -> {
                plugin.levelView().regenerate(plugin.resolveLevelDirectory(regenerate.key()), regenerate.seed());
                plugin.getComponentLogger().info("Regenerated world {} using seed {}", regenerate.key(), regenerate.seed());
            }
            case final BackupRestoreOperation restore -> {
                // fixme: we need a blocking operation :/ – bad API choice?
                final var optional = plugin.getBackupProvider().findBackup(restore.key(), restore.backup()).join();
                optional.ifPresentOrElse(backup -> {
                    plugin.getBackupProvider().restoreNow(restore.key(), backup);
                    plugin.getComponentLogger().info("Restored backup '{}' for {}", restore.backup(), restore.key());
                }, () -> {
                    plugin.getComponentLogger().warn("Failed to find and restore backup '{}' for {}", restore.backup(), restore.key());
                });
            }
        }
    }

    private OperationScheduler.Operation deserialize(final Key key, final CompoundTag tag) {
        final var type = tag.optional("type")
                .map(Tag::getAsString)
                .map(ActionType::valueOf)
                .orElseThrow(() -> new IllegalStateException("Invalid or missing action type: " + tag.get("type")));
        return switch (type) {
            case DELETE -> new DeleteOperation(key);
            case REGENERATE -> {
                final var seed = tag.optional("seed").map(Tag::getAsLong).orElse(0L);
                yield new RegenerateOperation(key, seed);
            }
            case RESTORE_BACKUP -> {
                final var backup = tag.optional("backup").map(Tag::getAsString).orElseThrow(() ->
                        new IllegalStateException("Operation is missing a backup name"));
                yield new BackupRestoreOperation(key, backup);
            }
        };
    }

    private CompoundTag serialize(final OperationScheduler.Operation operation) {
        final var builder = CompoundTag.builder().put("type", operation.type().name());
        switch (operation) {
            case final RegenerateOperation regenerate -> builder.put("seed", regenerate.seed());
            case final BackupRestoreOperation restore -> builder.put("backup", restore.backup());
            case final DeleteOperation ignored -> {
            }
        }
        return builder.build();
    }
}
