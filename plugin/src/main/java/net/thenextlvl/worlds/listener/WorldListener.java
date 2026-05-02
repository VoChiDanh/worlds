package net.thenextlvl.worlds.listener;

import net.kyori.adventure.key.Key;
import net.thenextlvl.worlds.Dimension;
import net.thenextlvl.worlds.WorldRegistry;
import net.thenextlvl.worlds.WorldsPlugin;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public final class WorldListener implements Listener {
    private final WorldsPlugin plugin;

    public WorldListener(final WorldsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldLoad(final WorldLoadEvent event) {
        registerEntryPermission(event.getWorld());

        if (plugin.levelView().isNether(event.getWorld())) {
            plugin.getWorldRegistry().registerIfAbsent(event.getWorld().key(), Dimension.THE_NETHER, true, null);
        } else if (plugin.levelView().isEnd(event.getWorld())) {
            plugin.getWorldRegistry().registerIfAbsent(event.getWorld().key(), Dimension.THE_END, true, null);
        }

        if (!plugin.levelView().isOverworld(event.getWorld())) return;
        plugin.getWorldRegistry().entrySet()
                .filter(entry -> entry.getValue().enabled())
                .forEach(entry -> loadLevel(entry.getKey(), entry.getValue()));
    }

    private void registerEntryPermission(final World world) {
        final var manager = plugin.getServer().getPluginManager();
        final var permission = plugin.getEntryPermission(world);
        if (manager.getPermission(permission) != null) return;
        manager.addPermission(new Permission(
                permission,
                "Allows entering the world " + world.key().asString() + " (" + world.getName() + ")",
                PermissionDefault.TRUE
        ));
    }

    private void loadLevel(final Key key, final WorldRegistry.Entry entry) {
        final var level = plugin.levelView().read(key, entry).build();

        if (plugin.getServer().getWorld(level.key()) != null) return;
        if (plugin.getServer().getWorld(level.getName()) != null) return;

        level.create().thenAccept(world -> plugin.getComponentLogger().debug(
                "Loaded dimension {} ({}) from {}",
                world.key().asString(), level.getGeneratorType().key().asString(),
                world.getWorldPath()
        )).exceptionally(throwable -> {
            final var t = throwable.getCause() != null ? throwable.getCause() : throwable;
            if (plugin.handler().isDirectoryLockException(t)) {
                plugin.getComponentLogger().error("Failed to start the minecraft server", t);
                plugin.getServer().shutdown();
            } else {
                plugin.getComponentLogger().error("An unexpected error occurred while loading the level {}", key, t);
                plugin.getComponentLogger().error("Please report the error above on GitHub: {}", WorldsPlugin.ISSUES);
            }
            return null;
        });
    }

}
