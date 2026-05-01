package net.thenextlvl.worlds.listener;

import net.thenextlvl.worlds.Level;
import net.thenextlvl.worlds.WorldsPlugin;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.nio.file.Path;

public final class WorldListener implements Listener {
    private final WorldsPlugin plugin;

    public WorldListener(final WorldsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onOverworldLoad(final WorldLoadEvent event) {
        registerEntryPermission(event.getWorld());
        if (!plugin.levelView().isOverworld(event.getWorld())) return;
        plugin.levelView().listEnabledLevels().stream()
                .filter(plugin.levelView()::canLoad)
                .forEach(this::loadLevel);
    }

    private void registerEntryPermission(final World world) {
        final var manager = plugin.getServer().getPluginManager();
        final var permission = plugin.levelView().getEntryPermission(world);
        if (manager.getPermission(permission) != null) return;
        manager.addPermission(new Permission(
                permission,
                "Allows entering the world " + world.key().asString() + " (" + world.getName() + ")",
                PermissionDefault.TRUE
        ));
    }

    private void loadLevel(final Path path) {
        final var level = plugin.levelView().read(path).map(Level.Builder::build).orElse(null);
        if (level == null) return;

        if (plugin.getServer().getWorld(level.key()) != null) {
            plugin.getComponentLogger().warn("Skip loading dimension '{}' because another world with the same key is already loaded", level.key());
            return;
        }
        if (plugin.getServer().getWorld(level.getName()) != null) {
            plugin.getComponentLogger().warn("Skip loading dimension '{}' because another world with the same name is already loaded", level.getName());
            return;
        }

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
                plugin.getComponentLogger().error("An unexpected error occurred while loading the level {}", path.getFileName(), t);
                plugin.getComponentLogger().error("Please report the error above on GitHub: {}", WorldsPlugin.ISSUES);
            }
            return null;
        });
    }

}
