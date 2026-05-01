package net.thenextlvl.worlds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class WorldFiles {
    private WorldFiles() {
    }

    static void regenerate(final Path level) {
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
