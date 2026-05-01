package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.Translatable;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public final class WorldOperationException extends RuntimeException {
    private final Reason reason;
    private final @Nullable String world;
    private final @Nullable Key key;
    private final @Nullable Path path;
    private final @Nullable String backup;
    private final @Nullable String plugin;

    public WorldOperationException(final Reason reason) {
        this(reason, null);
    }

    public WorldOperationException(final Reason reason, @Nullable final Throwable cause) {
        this(reason, cause, null, null, null, null, null);
    }

    private WorldOperationException(
            final Reason reason,
            @Nullable final Throwable cause,
            @Nullable final String world,
            @Nullable final Key key,
            @Nullable final Path path,
            @Nullable final String backup,
            @Nullable final String plugin
    ) {
        super(cause);
        this.reason = reason;
        this.world = world;
        this.key = key;
        this.path = path;
        this.backup = backup;
        this.plugin = plugin;
    }

    public Reason reason() {
        return reason;
    }

    public @Nullable String world() {
        return world;
    }

    public @Nullable Key key() {
        return key;
    }

    public @Nullable Path path() {
        return path;
    }

    public @Nullable String backup() {
        return backup;
    }

    public @Nullable String plugin() {
        return plugin;
    }

    public WorldOperationException world(final String world) {
        return new WorldOperationException(reason, getCause(), world, key, path, backup, plugin);
    }

    public WorldOperationException key(final Key key) {
        return new WorldOperationException(reason, getCause(), world, key, path, backup, plugin);
    }

    public WorldOperationException path(final Path path) {
        return new WorldOperationException(reason, getCause(), world, key, path, backup, plugin);
    }

    public WorldOperationException backup(final String backup) {
        return new WorldOperationException(reason, getCause(), world, key, path, backup, plugin);
    }

    public WorldOperationException plugin(final String plugin) {
        return new WorldOperationException(reason, getCause(), world, key, path, backup, plugin);
    }

    public enum Reason implements Translatable {
        WORLD_KEY_EXISTS("world.failure.key-exists"),
        WORLD_NAME_EXISTS("world.failure.name-exists"),
        WORLD_PATH_EXISTS("world.failure.path-exists"),
        TARGET_PATH_IS_FILE("world.failure.path-file"),
        WORLD_DIRECTORY_LOADED("world.failure.directory-loaded"),
        DUPLICATE_METADATA_UUID("world.failure.duplicate-uuid"),
        MISSING_LEVEL_STEM("world.failure.missing-level-stem"),
        LEGACY_MIGRATION_FAILED("world.failure.legacy-migration"),
        MEMOIZATION_FAILED("world.failure.internal"),
        GENERATOR_PLUGIN_MISSING("world.failure.generator-missing"),
        GENERATOR_PLUGIN_DISABLED("world.failure.generator-disabled"),
        GENERATOR_PLUGIN_HAS_NO_GENERATOR("world.failure.generator-empty"),
        BACKUP_NAME_EXISTS("world.failure.backup-exists"),
        BACKUP_DIRECTORY_FAILED("world.failure.backup-directory"),
        BACKUP_WRITE_FAILED("world.failure.backup-write"),
        BACKUP_READ_FAILED("world.failure.backup-read"),
        BACKUP_ZIP_FAILED("world.failure.backup-write"),
        BACKUP_RESTORE_FAILED("world.failure.backup-restore"),
        SAVE_FAILED("world.failure.save"),
        EVENT_CANCELLED("world.failure.cancelled"),
        UNLOAD_FAILED("world.unload.failed"),
        INTERNAL_ERROR("world.failure.internal");
        
        private final String translationKey;

        Reason(final String key) {
            this.translationKey = key;
        }

        @Override
        public String translationKey() {
            return translationKey;
        }
    }
}
