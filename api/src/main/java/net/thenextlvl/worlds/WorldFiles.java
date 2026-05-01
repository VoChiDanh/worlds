package net.thenextlvl.worlds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// todo: get this class out of here, move to impl
final class WorldFiles {
    private WorldFiles() {
    }

    static void regenerate(final Path level) {
        delete(level.resolve("region"));
        delete(level.resolve("entities"));
        delete(level.resolve("poi"));
    }

    static void delete(final Path path) {
        try {
            if (!Files.isDirectory(path)) Files.deleteIfExists(path);
            else try (final var files = Files.list(path)) {
                files.forEach(WorldFiles::delete);
                Files.deleteIfExists(path);
            }
        } catch (final IOException e) {
            WorldsAccess.access().getComponentLogger().warn("Failed to delete {}", path, e);
        }
    }
}
