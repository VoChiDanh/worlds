package net.thenextlvl.worlds.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import org.bukkit.Server;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import static net.thenextlvl.worlds.command.WorldCommand.worldArgument;

@NullMarked
public final class SaveOffCommand extends SimpleCommand {
    private SaveOffCommand(final WorldsPlugin plugin) {
        super(plugin, "save-off", "worlds.command.save-off");
    }

    public static LiteralCommandNode<CommandSourceStack> create(final WorldsPlugin plugin) {
        final var command = new SaveOffCommand(plugin);
        return command.create()
                .then(worldArgument(plugin).executes(command))
                .executes(command)
                .build();
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var world = tryGetArgument(context, "world", World.class).orElse(null);
        final var alreadyOff = isAutoSave(plugin.getServer(), world, false);
        final var message = messageKey(world, alreadyOff ? "already-off" : "off");
        if (!alreadyOff) setAutoSave(plugin.getServer(), world, false);
        if (world != null) plugin.bundle().sendMessage(context.getSource().getSender(), message,
                Placeholder.parsed("world", world.key().asString()));
        else plugin.bundle().sendMessage(context.getSource().getSender(), message);
        return alreadyOff ? 0 : SINGLE_SUCCESS;
    }

    static boolean isAutoSave(final Server server, @Nullable final World world, final boolean save) {
        if (world != null) return world.isAutoSave() == save;
        for (final var w : server.getWorlds()) {
            if (w.isAutoSave() != save) return false;
        }
        return true;
    }

    static void setAutoSave(final Server server, @Nullable final World world, final boolean save) {
        if (world != null) world.setAutoSave(save);
        else server.getWorlds().forEach(w -> w.setAutoSave(save));
    }

    static String messageKey(@Nullable final World world, final String state) {
        return "world.save." + state + (world != null ? ".world" : ".all");
    }
}
