package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.thenextlvl.worlds.generator.Generator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApiStatus.NonExtendable
public interface WorldRegistry {
    @Contract(pure = true)
    Optional<Entry> get(final Key key);

    @Contract(pure = true)
    Stream<Entry> entries();

    @Contract(pure = true)
    Stream<Map.Entry<Key, Entry>> entrySet();

    @Contract(pure = true)
    Stream<Key> worlds();

    @Contract(pure = true)
    boolean isEnabled(final Key key);

    @Contract(pure = true)
    boolean isRegistered(final Key key);

    @Contract(mutates = "this,io")
    void register(final Key key, final Dimension dimension, final boolean enabled, @Nullable final Generator generator);

    @Contract(mutates = "this,io")
    void register(final Level level, final boolean enabled);

    @Contract(mutates = "this,io")
    void setEnabled(final Key key, final boolean enabled);

    @Contract(mutates = "this,io")
    void unregister(final Key key);

    record Entry(Dimension dimension, boolean enabled, @Nullable Generator generator) {
        public Entry withEnabled(final boolean enabled) {
            return new Entry(dimension, enabled, generator);
        }
    }
}
