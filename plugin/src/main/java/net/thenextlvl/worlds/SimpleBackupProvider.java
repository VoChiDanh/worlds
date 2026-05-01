package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.thenextlvl.worlds.event.WorldActionScheduledEvent;
import org.bukkit.Keyed;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@NullMarked
public class SimpleBackupProvider implements BackupProvider {
    private static final BackupProvider INSTANCE = new SimpleBackupProvider();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            .withZone(ZoneId.systemDefault());

    @Override
    public CompletableFuture<Backup> backup(final World world, @Nullable final String name) {
        return CompletableFuture.supplyAsync(() -> createBackup(world.getWorldPath(), world, name));
    }

    @Override
    public CompletableFuture<ActionResult<World>> restore(final World world, final Backup backup) {
        final var worldKey = world.key();
        return WorldsAccess.access().unload(world, true).thenComposeAsync(success -> {
            if (!success) return CompletableFuture.completedFuture(
                    ActionResult.result(null, ActionResult.Status.UNLOAD_FAILED)
            );
            final var status = restoreNow(worldKey, backup);
            if (status != ActionResult.Status.SUCCESS) {
                return CompletableFuture.completedFuture(ActionResult.result(null, status));
            }
            WorldsAccess.access().getScheduler().cancel(world, WorldActionScheduledEvent.ActionType.RESTORE_BACKUP);
            return WorldsAccess.access().load(worldKey);
        });
    }

    @Override
    public ActionResult.Status restoreNow(final Key key, final Backup backup) {
        if (!(backup instanceof final FileBackup fileBackup))
            throw new IllegalStateException("Tried to restore backup from different provider");
        try {
            restoreBackup(key, fileBackup.path());
            return ActionResult.Status.SUCCESS;
        } catch (final IOException e) {
            throw new WorldOperationException(
                    WorldOperationException.Reason.BACKUP_RESTORE_FAILED,
                    e
            ).key(backup.key()).backup(backup.name());
        }
    }

    @Override
    public CompletableFuture<Stream<Backup>> listBackups() {
        return CompletableFuture.supplyAsync(() -> WorldsAccess.access()
                .getServer().getWorlds().stream()
                .map(Keyed::key)
                .flatMap(this::listBackupFiles));
    }

    @Override
    public CompletableFuture<Stream<Backup>> listBackups(final Key world) {
        return CompletableFuture.supplyAsync(() -> listBackupFiles(world));
    }

    @Override
    public CompletableFuture<Optional<Backup>> findBackup(final Key world) {
        return CompletableFuture.supplyAsync(() -> listBackupFiles(world).findFirst());
    }

    @Override
    public CompletableFuture<Optional<Backup>> findBackup(final Key world, final String name) {
        return CompletableFuture.supplyAsync(() -> {
            final var path = resolveBackupPath(world, name);
            if (!Files.isRegularFile(path)) return Optional.empty();
            return Optional.of(toBackup(path, world));
        });
    }

    @Override
    public CompletableFuture<Boolean> delete(final Key world, final String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Files.deleteIfExists(resolveBackupPath(world, name));
            } catch (final IOException e) {
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> delete(final Backup backup) {
        if (!(backup instanceof final FileBackup fileBackup))
            throw new IllegalStateException("Tried to delete backup from different provider");
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Files.deleteIfExists(fileBackup.path());
            } catch (final IOException e) {
                return false;
            }
        });
    }

    private Path resolveBackupFolder(final Key key) {
        var backupFolder = System.getenv("WORLDS_BACKUP_FOLDER");
        if (backupFolder == null) backupFolder = System.getProperty("worlds.backup.folder");
        if (backupFolder != null) return Path.of(backupFolder).resolve(key.namespace()).resolve(key.value());
        final var parent = WorldsAccess.access().getServer().getLevelDirectory().getParent();
        final var root = parent != null ? parent.resolve("backups") : Path.of("backups");
        return root.resolve(key.namespace()).resolve(key.value());
    }

    private Path resolveBackupPath(final Key key, final String name) {
        return resolveBackupFolder(key).resolve(name + ".zip");
    }

    private Backup createBackup(final Path worldDirectory, final World world, @Nullable final String name) {
        final var folder = resolveBackupFolder(world.key());
        try {
            Files.createDirectories(folder);
        } catch (final IOException e) {
            throw new WorldOperationException(
                    WorldOperationException.Reason.BACKUP_DIRECTORY_FAILED,
                    e
            ).key(world.key()).world(world.getName()).path(folder);
        }
        final var timestamp = FORMATTER.format(Instant.now());
        final var fileName = name != null ? name + ".zip" : findAvailableName(folder, timestamp);
        final var backupPath = folder.resolve(fileName);
        if (name != null && Files.isRegularFile(backupPath)) {
            throw new WorldOperationException(
                    WorldOperationException.Reason.BACKUP_NAME_EXISTS
            ).key(world.key()).world(world.getName()).backup(name).path(backupPath);
        }
        try (final var output = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(
                backupPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE
        )))) {
            Files.walkFileTree(worldDirectory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    if (!file.endsWith("session.lock")) {
                        final var relative = worldDirectory.relativize(file).toString().replace('\\', '/');
                        output.putNextEntry(new ZipEntry(relative));
                        Files.copy(file, output);
                        output.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final IOException e) {
            throw new WorldOperationException(
                    WorldOperationException.Reason.BACKUP_ZIP_FAILED,
                    e
            ).key(world.key()).world(world.getName()).backup(name != null ? name : fileName.substring(0, fileName.length() - 4)).path(backupPath);
        }
        try {
            final var attrs = Files.readAttributes(backupPath, BasicFileAttributes.class);
            final var backupName = backupPath.getFileName().toString();
            return new FileBackup(
                    backupName.substring(0, backupName.length() - 4),
                    attrs.creationTime().toInstant(),
                    attrs.size(),
                    backupPath,
                    world.key()
            );
        } catch (final IOException e) {
            throw new WorldOperationException(
                    WorldOperationException.Reason.BACKUP_READ_FAILED,
                    e
            ).key(world.key()).world(world.getName()).path(backupPath);
        }
    }

    private void restoreBackup(final Key key, final Path backupFile) throws IOException {
        final var worldDirectory = WorldsAccess.access().resolveLevelDirectory(key);
        Path tempPath;
        do {
            tempPath = worldDirectory.resolveSibling("." + UUID.randomUUID());
        } while (Files.isDirectory(tempPath));
        try (final var input = new ZipInputStream(Files.newInputStream(backupFile, StandardOpenOption.READ))) {
            ZipEntry entry;
            final var root = tempPath.toAbsolutePath().normalize();
            while ((entry = input.getNextEntry()) != null) {
                final Path resolved;
                try {
                    resolved = resolveZipEntry(root, entry);
                } catch (final IOException e) {
                    continue;
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    final var parent = resolved.getParent();
                    if (parent != null) Files.createDirectories(parent);
                    Files.copy(input, resolved);
                }
            }
            deleteRecursively(worldDirectory);
            Files.move(tempPath, worldDirectory, StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            try {
                if (Files.exists(tempPath)) deleteRecursively(tempPath);
            } catch (final IOException ignored) {
            }
            throw e;
        }
    }

    private Stream<Backup> listBackupFiles(final Key key) {
        final var folder = resolveBackupFolder(key);
        if (!Files.isDirectory(folder)) return Stream.empty();
        try (final var files = Files.list(folder)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .map(path -> toBackup(path, key))
                    .sorted(Comparator.comparing(Backup::createdAt).reversed());
        } catch (final IOException e) {
            return Stream.empty();
        }
    }

    private static Backup toBackup(final Path path, final Key key) {
        try {
            final var attributes = Files.readAttributes(path, BasicFileAttributes.class);
            final var fileName = path.getFileName().toString();
            return new FileBackup(
                    fileName.substring(0, fileName.length() - 4),
                    attributes.lastModifiedTime().toInstant(),
                    attributes.size(), path, key
            );
        } catch (final IOException e) {
            final var fileName = path.getFileName().toString();
            return new FileBackup(
                    fileName.substring(0, fileName.length() - 4),
                    Instant.EPOCH, 0, path, key
            );
        }
    }

    private static String findAvailableName(final Path directory, final String baseName) {
        var candidate = baseName + ".zip";
        if (!Files.exists(directory.resolve(candidate))) return candidate;
        for (var i = 1; ; i++) {
            candidate = baseName + "-" + i + ".zip";
            if (!Files.exists(directory.resolve(candidate))) return candidate;
        }
    }

    private static Path resolveZipEntry(final Path root, final ZipEntry entry) throws IOException {
        final var target = root.resolve(entry.getName()).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Zip entry outside target dir: " + entry.getName());
        }
        return target;
    }

    private static void deleteRecursively(final Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, @Nullable final IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public record FileBackup(String name, Instant createdAt, long size, Path path, Key key) implements Backup {
        @Override
        public BackupProvider provider() {
            return SimpleBackupProvider.INSTANCE;
        }
    }
}
