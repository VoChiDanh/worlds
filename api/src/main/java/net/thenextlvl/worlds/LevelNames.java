package net.thenextlvl.worlds;

import net.kyori.adventure.key.Key;

final class LevelNames {
    private LevelNames() {
    }

    static String fromKey(final Key key) {
        return key.asString().replace(':', '_').replace('/', '_');
    }
}
