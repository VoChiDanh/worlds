package net.thenextlvl.worlds.preset;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class SimplePreset implements Preset {
    private final @Nullable String name;
    private final Key biome;
    private final boolean lakes;
    private final boolean features;
    private final boolean decoration;
    private final List<Layer> layers;
    private final Set<Key> structures;

    private SimplePreset(
            @Nullable final String name, final Key biome, final boolean lakes, final boolean features,
            final boolean decoration, final List<Layer> layers, final Set<Key> structures
    ) {
        this.name = name;
        this.biome = biome;
        this.lakes = lakes;
        this.features = features;
        this.decoration = decoration;
        this.layers = layers;
        this.structures = structures;
    }

    @Override
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    @Override
    public Key biome() {
        return biome;
    }

    @Override
    public boolean lakes() {
        return lakes;
    }

    @Override
    public boolean features() {
        return features;
    }

    @Override
    public boolean decoration() {
        return decoration;
    }

    @Override
    public @Unmodifiable List<Layer> layers() {
        return layers;
    }

    @Override
    public @Unmodifiable Set<Key> structures() {
        return structures;
    }

    @Override
    public String asString() {
        final var layers = this.layers.stream()
                .map(Layer::toString)
                .collect(Collectors.joining(","));
        return layers + ";" + biome();
    }

    @Override
    public JsonObject toJson() {
        final var root = new JsonObject();
        final var layers = new JsonArray();
        final var structures = new JsonArray();
        root.addProperty("name", name);
        root.addProperty("biome", biome.key().asString());
        root.addProperty("lakes", lakes);
        root.addProperty("features", features);
        root.addProperty("decoration", decoration);
        this.layers.forEach(layer -> {
            final var object = new JsonObject();
            object.addProperty("block", layer.block().key().asString());
            object.addProperty("height", layer.height());
            layers.add(object);
        });
        this.structures.forEach(structure -> structures.add(structure.key().asString()));
        root.add("layers", layers);
        root.add("structure_overrides", structures);
        return root;
    }

    @Override
    public Preset.Builder toBuilder() {
        return Preset.builder()
                .name(name)
                .biome(biome)
                .lakes(lakes)
                .features(features)
                .decoration(decoration)
                .layers(layers)
                .structures(structures);
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final SimplePreset that = (SimplePreset) o;
        return lakes == that.lakes
                && features == that.features
                && decoration == that.decoration
                && Objects.equals(name, that.name)
                && Objects.equals(biome, that.biome)
                && Objects.equals(layers, that.layers)
                && Objects.equals(structures, that.structures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, biome, lakes, features, decoration, layers, structures);
    }

    @Override
    public String toString() {
        return "SimplePreset{" +
                "name='" + name + '\'' +
                ", biome=" + biome +
                ", lakes=" + lakes +
                ", features=" + features +
                ", decoration=" + decoration +
                ", layers=" + layers +
                ", structures=" + structures +
                '}';
    }

    public static final class Builder implements Preset.Builder {
        private @Nullable String name;
        private Key biome = Key.key("plains");
        private List<Layer> layers = new LinkedList<>();
        private Set<Key> structures = new LinkedHashSet<>();
        private boolean decoration;
        private boolean features;
        private boolean lakes;

        @Override
        public Preset.Builder name(@Nullable final String name) {
            this.name = name;
            return this;
        }

        @Override
        public Preset.Builder biome(final Key biome) {
            this.biome = biome;
            return this;
        }

        @Override
        public Preset.Builder decoration(final boolean decoration) {
            this.decoration = decoration;
            return this;
        }

        @Override
        public Preset.Builder features(final boolean features) {
            this.features = features;
            return this;
        }

        @Override
        public Preset.Builder lakes(final boolean lakes) {
            this.lakes = lakes;
            return this;
        }

        @Override
        public Preset.Builder layers(final List<Layer> layers) {
            this.layers = new LinkedList<>(layers);
            return this;
        }

        @Override
        public Preset.Builder addLayer(final Layer layer) {
            this.layers.add(layer);
            return this;
        }

        @Override
        public Preset.Builder structures(final Set<Key> structures) {
            this.structures = new LinkedHashSet<>(structures);
            return this;
        }

        @Override
        public Preset.Builder addStructure(final Key structure) {
            this.structures.add(structure);
            return this;
        }

        @Override
        public Preset build() {
            return new SimplePreset(
                    name, biome, lakes, features, decoration,
                    new LinkedList<>(layers), new LinkedHashSet<>(structures)
            );
        }
    }

    @SuppressWarnings("PatternValidation")
    public static Preset fromString(final String presetCode) {
        final var strings = presetCode.trim().split(";", 2);
        Preconditions.checkArgument(!strings[0].isEmpty(), "Invalid preset code: '%s', missing layers", presetCode);
        Preconditions.checkArgument(strings.length == 2, "Invalid preset code: '%s', missing biome", presetCode);
        final var layers = Arrays.stream(strings[0].split(",")).map(layer -> {
            final var parameters = layer.split("\\*", 2);
            final var material = parameters.length == 1 ? parameters[0] : parameters[1];
            final var height = parameters.length == 1 ? 1 : Integer.parseInt(parameters[0]);
            final var matched = Material.matchMaterial(material);
            if (matched != null) return new Layer(matched, height);
            throw new IllegalArgumentException("Invalid material: " + material);
        }).toList();
        return Preset.builder().layers(layers).biome(Key.key(strings[1])).build();
    }

    @SuppressWarnings("PatternValidation")
    public static Preset fromJson(final JsonObject object) throws IllegalArgumentException {
        Preconditions.checkArgument(object.has("layers"), "Missing layers");
        final var preset = Preset.builder().name(object.has("name") ? object.get("name").getAsString() : null);
        if (object.has("biome")) preset.biome(Key.key(object.get("biome").getAsString()));
        if (object.has("lakes")) preset.lakes(object.get("lakes").getAsBoolean());
        if (object.has("features")) preset.features(object.get("features").getAsBoolean());
        if (object.has("decoration")) preset.decoration(object.get("decoration").getAsBoolean());
        object.getAsJsonArray("layers").forEach(layer -> {
            final var layerObject = layer.getAsJsonObject();
            final var material = Material.matchMaterial(layerObject.get("block").getAsString());
            final var height = layerObject.get("height").getAsInt();
            if (material != null) preset.addLayer(new Layer(material, height));
        });
        if (object.has("structure_overrides")) object.getAsJsonArray("structure_overrides")
                .forEach(structure -> preset.addStructure(Key.key(structure.getAsString())));
        return preset.build();
    }

    static final Preset BOTTOMLESS_PIT = Preset.builder()
            .name("Bottomless Pit")
            .addLayer(new Layer(Material.COBBLESTONE, 2))
            .addLayer(new Layer(Material.DIRT, 3))
            .addLayer(new Layer(Material.GRASS_BLOCK, 1))
            .addStructure(Key.key("villages"))
            .build();

    static final Preset CLASSIC_FLAT = Preset.builder()
            .name("Classic Flat")
            .addLayer(new Layer(Material.BEDROCK, 1))
            .addLayer(new Layer(Material.DIRT, 2))
            .addLayer(new Layer(Material.GRASS_BLOCK, 1))
            .addStructure(Key.key("villages"))
            .build();

    static final Preset DESERT = Preset.builder()
            .name("Desert")
            .biome(Key.key("desert"))
            .features(true)
            .addLayer(new Layer(Material.BEDROCK, 1))
            .addLayer(new Layer(Material.STONE, 3))
            .addLayer(new Layer(Material.SANDSTONE, 52))
            .addLayer(new Layer(Material.SAND, 8))
            .addStructure(Key.key("desert_pyramids"))
            .addStructure(Key.key("mineshafts"))
            .addStructure(Key.key("strongholds"))
            .addStructure(Key.key("villages"))
            .build();

    static final Preset OVERWORLD = Preset.builder()
            .name("Overworld")
            .lakes(true)
            .features(true)
            .addLayer(new Layer(Material.BEDROCK, 1))
            .addLayer(new Layer(Material.STONE, 59))
            .addLayer(new Layer(Material.DIRT, 3))
            .addLayer(new Layer(Material.GRASS_BLOCK, 1))
            .addStructure(Key.key("mineshafts"))
            .addStructure(Key.key("pillager_outposts"))
            .addStructure(Key.key("ruined_portals"))
            .addStructure(Key.key("strongholds"))
            .addStructure(Key.key("villages"))
            .build();

    static final Preset REDSTONE_READY = Preset.builder()
            .name("Redstone Ready")
            .biome(Key.key("desert"))
            .addLayer(new Layer(Material.BEDROCK, 1))
            .addLayer(new Layer(Material.STONE, 3))
            .addLayer(new Layer(Material.SANDSTONE, 116))
            .build();

    static final Preset SNOWY_KINGDOM = Preset.builder()
            .name("Snowy Kingdom")
            .biome(Key.key("snowy_plains"))
            .addLayer(new Layer(Material.BEDROCK, 1))
            .addLayer(new Layer(Material.STONE, 59))
            .addLayer(new Layer(Material.DIRT, 3))
            .addLayer(new Layer(Material.GRASS_BLOCK, 1))
            .addLayer(new Layer(Material.SNOW, 1))
            .addStructure(Key.key("igloos"))
            .addStructure(Key.key("villages"))
            .build();

    static final Preset THE_VOID = Preset.builder()
            .name("The Void")
            .features(true)
            .biome(Key.key("the_void"))
            .addLayer(new Layer(Material.AIR, 1))
            .build();

    static final Preset TUNNELERS_DREAM = Preset.builder()
            .name("Tunnelers' Dream")
            .features(true)
            .biome(Key.key("windswept_hills"))
            .addLayer(new Layer(Material.BEDROCK, 1))
            .addLayer(new Layer(Material.STONE, 230))
            .addLayer(new Layer(Material.DIRT, 5))
            .addLayer(new Layer(Material.GRASS_BLOCK, 1))
            .addStructure(Key.key("mineshafts"))
            .addStructure(Key.key("strongholds"))
            .build();

    static final Preset WATER_WORLD = Preset.builder()
            .name("Water World")
            .biome(Key.key("deep_ocean"))
            .addLayer(new Layer(Material.BEDROCK, 1))
            .addLayer(new Layer(Material.DEEPSLATE, 64))
            .addLayer(new Layer(Material.STONE, 5))
            .addLayer(new Layer(Material.DIRT, 5))
            .addLayer(new Layer(Material.GRAVEL, 5))
            .addLayer(new Layer(Material.WATER, 90))
            .addStructure(Key.key("ocean_monuments"))
            .addStructure(Key.key("ocean_ruins"))
            .addStructure(Key.key("shipwrecks"))
            .build();

    static final Set<Preset> PRESETS = Set.of(
            BOTTOMLESS_PIT, CLASSIC_FLAT, DESERT, OVERWORLD,
            REDSTONE_READY, SNOWY_KINGDOM, THE_VOID,
            TUNNELERS_DREAM, WATER_WORLD
    );
}
