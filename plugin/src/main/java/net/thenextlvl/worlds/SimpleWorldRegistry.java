package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import net.thenextlvl.nbt.NBTInputStream;
import net.thenextlvl.nbt.NBTOutputStream;
import net.thenextlvl.nbt.tag.CompoundTag;
import net.thenextlvl.nbt.tag.Tag;
import net.thenextlvl.worlds.generator.Generator;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@NullMarked
public final class SimpleWorldRegistry implements WorldRegistry {
    private final Map<Key, Entry> entries = new ConcurrentHashMap<>();
    private final WorldsPlugin plugin;
    private final Path path;

    public SimpleWorldRegistry(final WorldsPlugin plugin) {
        this.path = plugin.getDataPath().resolve("worlds.dat");
        this.plugin = plugin;
    }

    @Override
    public Optional<Entry> get(final Key key) {
        return Optional.ofNullable(entries.get(key));
    }

    @Override
    public Stream<Entry> entries() {
        return entries.values().stream();
    }

    @Override
    public Stream<Key> enabledWorlds() {
        return entries.entrySet().stream()
                .filter(entry -> entry.getValue().enabled())
                .map(Map.Entry::getKey);
    }

    @Override
    public Stream<Key> worlds() {
        return entries.keySet().stream();
    }

    @Override
    public boolean isEnabled(final Key key) {
        return get(key).map(Entry::enabled).orElse(false);
    }

    @Override
    public boolean isRegistered(final Key key) {
        return entries.containsKey(key);
    }

    @Override
    public void register(final Key key, final Environment environment, final boolean enabled, @Nullable final Generator generator) {
        entries.put(key, new Entry(environment, enabled, generator));
        save();
    }

    @Override
    public void register(final Level level, final boolean enabled) {
        register(level.key(), level.getEnvironment(), enabled, level.getGenerator().orElse(null));
    }

    @Override
    public void setEnabled(final Key key, final boolean enabled) {
        entries.computeIfPresent(key, (ignored, entry) -> entry.withEnabled(enabled));
        save();
    }

    @Override
    public void unregister(final Key key) {
        entries.remove(key);
        save();
    }

    @SuppressWarnings("PatternValidation")
    public void read() {
        if (!Files.isRegularFile(path)) return;
        try (final var input = NBTInputStream.create(path)) {
            final var root = input.readTag();
            root.forEach((key, tag) -> {
                final var object = tag.getAsCompound();
                final var enabled = object.optional("enabled").map(Tag::getAsBoolean).orElse(false);
                final var environment = object.optional("environment").map(Tag::getAsString).map(this::fromString).orElse(Environment.OVERWORLD);
                final var generator = object.optional("generator").map(Tag::getAsString).map(Generator::fromString).orElse(null);
                entries.put(Key.key(key), new Entry(environment, enabled, generator));
            });
        } catch (final Exception e) {
            plugin.getComponentLogger().warn("Failed to read managed worlds from {}", path, e);
        }
    }

    public void save() {
        try (final var output = NBTOutputStream.create(path)) {
            final var root = CompoundTag.builder();
            entries.forEach((key, entry) -> {
                final var compound = CompoundTag.builder()
                        .put("enabled", entry.enabled())
                        .put("environment", entry.environment().key().asString());
                if (entry.generator() != null) {
                    compound.put("generator", entry.generator().asString());
                }
                root.put(key.asString(), compound.build());
            });
            Files.createDirectories(path.getParent());
            output.writeTag(null, root.build());
        } catch (final Exception e) {
            plugin.getComponentLogger().warn("Failed to write managed worlds to {}", path, e);
        }
    }

    @SuppressWarnings("PatternValidation")
    private Environment fromString(final String string) {
        return Environment.of(Key.key(string));
    }
}
