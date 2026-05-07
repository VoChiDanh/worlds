package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.thenextlvl.worlds.generator.Generator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Registry for worlds known to the plugin.
 *
 * @since 4.0.0
 */
@ApiStatus.NonExtendable
public interface WorldRegistry {
    /**
     * Returns the registry entry for the given world key.
     *
     * @param key the world key
     * @return the registry entry, or empty if the world is not registered
     * @since 4.0.0
     */
    @Contract(pure = true)
    Optional<Entry> get(final Key key);

    /**
     * Returns all registered world entries.
     *
     * @return a stream of registered world entries
     * @since 4.0.0
     */
    @Contract(pure = true)
    Stream<Entry> entries();

    /**
     * Returns all registered world keys and entries.
     *
     * @return a stream of world key and entry pairs
     * @since 4.0.0
     */
    @Contract(pure = true)
    Stream<Map.Entry<Key, Entry>> entrySet();

    /**
     * Returns all registered world keys.
     *
     * @return a stream of registered world keys
     * @since 4.0.0
     */
    @Contract(pure = true)
    Stream<Key> worlds();

    /**
     * Checks whether a registered world is enabled.
     *
     * @param key the world key
     * @return {@code true} if the world is enabled
     * @since 4.0.0
     */
    @Contract(pure = true)
    boolean isEnabled(final Key key);

    /**
     * Checks whether a world key is registered.
     *
     * @param key the world key
     * @return {@code true} if the world is registered
     * @since 4.0.0
     */
    @Contract(pure = true)
    boolean isRegistered(final Key key);

    /**
     * Registers a world with the specified generation settings.
     *
     * @param key       the world key
     * @param dimension the world dimension
     * @param enabled   whether the world is enabled
     * @param generator the world generator, or {@code null}
     * @since 4.0.0
     */
    @Contract(mutates = "this,io")
    void register(final Key key, final Dimension dimension, final boolean enabled, @Nullable final Generator generator);

    /**
     * Registers a world only when the key is not already registered.
     *
     * @param key       the world key
     * @param dimension the world dimension
     * @param enabled   whether the world is enabled
     * @param generator the world generator, or {@code null}
     * @return whether the world has been registered
     * @since 4.0.0
     */
    @Contract(mutates = "this,io")
    boolean registerIfAbsent(final Key key, final Dimension dimension, final boolean enabled, @Nullable final Generator generator);

    /**
     * Registers a level.
     *
     * @param level   the level to register
     * @param enabled whether the world is enabled
     * @since 4.0.0
     */
    @Contract(mutates = "this,io")
    void register(final Level level, final boolean enabled);

    /**
     * Sets whether a registered world is enabled.
     *
     * @param key     the world key
     * @param enabled whether the world is enabled
     * @since 4.0.0
     */
    @Contract(mutates = "this,io")
    void setEnabled(final Key key, final boolean enabled);

    /**
     * Unregisters a world.
     *
     * @param key the world key
     * @since 4.0.0
     */
    @Contract(mutates = "this,io")
    void unregister(final Key key);

    /**
     * A registered world entry.
     *
     * @param dimension the world dimension
     * @param enabled   whether the world is enabled
     * @param generator the configured generator, or {@code null}
     * @since 4.0.0
     */
    record Entry(Dimension dimension, boolean enabled, @Nullable Generator generator) {
        /**
         * Returns a copy of this entry with the enabled flag changed.
         *
         * @param enabled whether the copied entry is enabled
         * @return the copied entry
         * @since 4.0.0
         */
        public Entry withEnabled(final boolean enabled) {
            return new Entry(dimension, enabled, generator);
        }
    }
}
