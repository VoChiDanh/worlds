package net.thenextlvl.worlds.preset;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PresetsTest {
    @ParameterizedTest
    @MethodSource("unparsedPresets")
    public void testPreset(final Preset preset, final String unparsed) {
        final var parsed = Preset.fromString(unparsed);
        assertEquals(preset.asString(), parsed.asString(), "Parsed preset does not match unparsed preset");
    }

    @ParameterizedTest
    @MethodSource("presets")
    public void testPresetSerialization(final Preset preset) {
        final var serialize = preset.toJson();
        final var deserialize = Preset.fromJson(serialize);
        assertEquals(preset, deserialize, "Deserialized preset does not match original");
    }

    public static Stream<Arguments> unparsedPresets() {
        final var build = Preset.builder()
                .name("test")
                .addLayer(new Layer(Key.key("bedrock"), 3))
                .addLayer(new Layer(Key.key("bedrock"), 4))
                .addLayer(new Layer(Key.key("bedrock"), 5))
                .addLayer(new Layer(Key.key("bedrock"), 5))
                .build();

        return Stream.of(
                Map.entry(build, "3*minecraft:bedrock,4*minecraft:bedrock,5*minecraft:bedrock,5*minecraft:bedrock;minecraft:plains"),
                Map.entry(Preset.CLASSIC_FLAT, "minecraft:bedrock,2*minecraft:dirt,minecraft:grass_block;minecraft:plains"),
                Map.entry(Preset.TUNNELERS_DREAM, "minecraft:bedrock,230*minecraft:stone,5*minecraft:dirt,minecraft:grass_block;minecraft:windswept_hills"),
                Map.entry(Preset.WATER_WORLD, "minecraft:bedrock,64*minecraft:deepslate,5*minecraft:stone,5*minecraft:dirt,5*minecraft:gravel,90*minecraft:water;minecraft:deep_ocean"),
                Map.entry(Preset.OVERWORLD, "minecraft:bedrock,59*minecraft:stone,3*minecraft:dirt,minecraft:grass_block;minecraft:plains"),
                Map.entry(Preset.SNOWY_KINGDOM, "minecraft:bedrock,59*minecraft:stone,3*minecraft:dirt,minecraft:grass_block,minecraft:snow;minecraft:snowy_plains"),
                Map.entry(Preset.BOTTOMLESS_PIT, "2*minecraft:cobblestone,3*minecraft:dirt,minecraft:grass_block;minecraft:plains"),
                Map.entry(Preset.DESERT, "minecraft:bedrock,3*minecraft:stone,52*minecraft:sandstone,8*minecraft:sand;minecraft:desert"),
                Map.entry(Preset.REDSTONE_READY, "minecraft:bedrock,3*minecraft:stone,116*minecraft:sandstone;minecraft:desert"),
                Map.entry(Preset.THE_VOID, "minecraft:air;minecraft:the_void")
        ).map(entry -> Arguments.argumentSet(entry.getKey().name().orElseThrow(), entry.getKey(), entry.getValue()));
    }

    public static Stream<Arguments> presets() {
        return Preset.presets().stream().map(preset -> Arguments.argumentSet(preset.name().orElseThrow(), preset));
    }
}
