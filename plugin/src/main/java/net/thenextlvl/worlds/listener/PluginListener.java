package net.thenextlvl.worlds.listener;

import net.thenextlvl.worlds.WorldsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

import java.util.Set;

@NullMarked
public final class PluginListener implements Listener {
    private static final Set<String> knownVoidGenerators = Set.of(
            "VoidWorldGenerator",
            "VoidGen",
            "VoidGenerator",
            "VoidWorld",
            "VoidGenPlus",
            "DeluxeVoidWorld",
            "CleanroomGenerator",
            "CompletelyEmpty"
    );
    private final WorldsPlugin plugin;
    private static boolean warn = false;

    public PluginListener(final WorldsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean warnVoidGeneratorPlugin(final Plugin plugin) {
        if (warn) return true;
        if (!isKnownVoidGenerator(plugin)) return false;

        this.plugin.getServer().getGlobalRegionScheduler().execute(this.plugin, () -> {
            this.plugin.getComponentLogger().warn("It appears you are using a plugin to generate void worlds");
            this.plugin.getComponentLogger().warn("This is not required, and incompatible with Vanilla world generation");
            this.plugin.getComponentLogger().warn("Please use the preset 'the-void' instead");
            this.plugin.getComponentLogger().warn("You can do this with the command '/world create <key> preset the-void'");
            this.plugin.getComponentLogger().warn("Read more at https://thenextlvl.net/blog/void-generator-plugins");
        });

        return warn = true;
    }

    private boolean isKnownVoidGenerator(final Plugin plugin) {
        if (knownVoidGenerators.contains(plugin.getName())) return true;
        for (final var provided : plugin.getPluginMeta().getProvidedPlugins())
            if (knownVoidGenerators.contains(provided)) return true;
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(final PluginEnableEvent event) {
        if (warnVoidGeneratorPlugin(event.getPlugin()))
            HandlerList.unregisterAll(this);
    }

    public void init() {
        for (final var p : plugin.getServer().getPluginManager().getPlugins()) {
            if (warnVoidGeneratorPlugin(p)) return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
