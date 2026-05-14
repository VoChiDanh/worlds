package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LevelTest {
    @Test
    void testWorldNameIsFilesystemSafe() {
        Assertions.assertEquals("minecraft_void", LevelNames.fromKey(Key.key("minecraft", "void")));
    }

    @Test
    void testWorldNameReplacesPathSeparators() {
        Assertions.assertEquals("worlds_nested_void", LevelNames.fromKey(Key.key("worlds", "nested/void")));
    }
}
