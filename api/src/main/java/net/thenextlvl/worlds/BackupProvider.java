package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface BackupProvider {
    @Contract(mutates = "io")
    default CompletableFuture<Backup> backup(final World world) {
        return backup(world, null);
    }

    @Contract(mutates = "io")
    CompletableFuture<Backup> backup(World world, @Nullable String name);

    @Contract(mutates = "param,io")
    CompletableFuture<World> restore(World world, Backup backup);

    @ApiStatus.OverrideOnly
    @Contract(mutates = "io")
    void restoreNow(Key key, Backup backup);

    @Contract(pure = true)
    CompletableFuture<Stream<Backup>> listBackups();

    @Contract(pure = true)
    default CompletableFuture<Stream<Backup>> listBackups(final World world) {
        return listBackups(world.key());
    }

    @Contract(pure = true)
    CompletableFuture<Stream<Backup>> listBackups(Key world);

    @Contract(pure = true)
    default CompletableFuture<Optional<Backup>> findBackup(final World world) {
        return findBackup(world.key());
    }

    @Contract(pure = true)
    CompletableFuture<Optional<Backup>> findBackup(Key world);

    @Contract(pure = true)
    default CompletableFuture<Optional<Backup>> findBackup(final World world, final String name) {
        return findBackup(world.key(), name);
    }

    @Contract(pure = true)
    CompletableFuture<Optional<Backup>> findBackup(Key world, String name);

    default CompletableFuture<Boolean> delete(final World world, final String name) {
        return delete(world.key(), name);
    }

    CompletableFuture<Boolean> delete(Key world, String name);

    CompletableFuture<Boolean> delete(Backup backup);
}
