package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.thenextlvl.binder.StaticBinder;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

@ApiStatus.NonExtendable
public interface WorldsAccess extends Plugin {
    static @CheckReturnValue WorldsAccess access() {
        final class Cache {
            private static final WorldsAccess INSTANCE = StaticBinder.getInstance(WorldsAccess.class.getClassLoader()).find(WorldsAccess.class);
        }
        return Cache.INSTANCE;
    }

    @Contract(pure = true)
    WorldRegistry getWorldRegistry();

    @Contract(pure = true)
    Stream<Dimension> listDimensions();

    @Contract(pure = true)
    Dimension getDimension(World world);

    @Contract(pure = true)
    Path getLevelDirectory();

    @Contract(pure = true)
    Stream<Path> listLevels();

    CompletableFuture<World> load(Key key);

    CompletableFuture<World> create(Level level);

    @Contract(mutates = "param1")
    CompletableFuture<Boolean> unload(World world, boolean save);

    @Contract(mutates = "param1")
    CompletableFuture<Boolean> save(World world, boolean flush);

    @Contract(mutates = "param1")
    CompletableFuture<World> clone(World world, boolean full);

    @Contract(mutates = "param1")
    CompletableFuture<World> clone(World world, Consumer<Level.Builder> builder, boolean full);

    @Contract(mutates = "param1")
    CompletableFuture<Boolean> delete(World world);

    @Contract(mutates = "param1")
    CompletableFuture<Boolean> scheduleDeletion(World world);

    @Contract(mutates = "param1")
    CompletableFuture<World> regenerate(World world);

    @Contract(mutates = "param1")
    CompletableFuture<World> regenerate(World world, Consumer<Level.Builder> builder);

    @Contract(mutates = "param1")
    CompletableFuture<Boolean> scheduleRegeneration(World world);

    @Contract(mutates = "param1")
    CompletableFuture<Backup> createBackup(World world, @Nullable String name);

    @Contract(mutates = "param1")
    CompletableFuture<World> restoreBackup(World world, Backup backup);

    @Contract(mutates = "param1")
    CompletableFuture<Boolean> scheduleBackupRestoration(World world, Backup backup);

    @Contract(pure = true)
    boolean isEnabled(World world);

    @Contract(mutates = "param1")
    void setEnabled(World world, boolean enabled);

    @Contract(pure = true)
    String getEntryPermission(World world);

    @Contract(pure = true)
    default ScheduledWorldOperations getScheduler() {
        return SimpleScheduledWorldOperations.INSTANCE;
    }

    @Contract(pure = true)
    BackupProvider getBackupProvider();

    @Contract(mutates = "this")
    void setBackupProvider(BackupProvider provider);

    @Contract(pure = true)
    Path getDimensionsRoot();

    @Contract(pure = true)
    Path resolveLevelDirectory(Key key);
}
