package net.thenextlvl.worlds.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

import static net.thenextlvl.worlds.command.WorldCommand.worldArgument;

@NullMarked
public final class SaveOnCommand extends SimpleCommand {
    private SaveOnCommand(final WorldsPlugin plugin) {
        super(plugin, "save-on", "worlds.command.save-on");
    }

    public static LiteralCommandNode<CommandSourceStack> create(final WorldsPlugin plugin) {
        final var command = new SaveOnCommand(plugin);
        return command.create()
                .then(worldArgument(plugin).executes(command))
                .executes(command)
                .build();
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) {
        final var world = tryGetArgument(context, "world", World.class).orElse(null);
        final var alreadyOn = SaveOffCommand.isAutoSave(plugin.getServer(), world, true);
        final var message = SaveOffCommand.messageKey(world, alreadyOn ? "already-on" : "on");
        if (!alreadyOn) SaveOffCommand.setAutoSave(plugin.getServer(), world, true);
        if (world != null) plugin.bundle().sendMessage(context.getSource().getSender(), message,
                Placeholder.parsed("world", world.key().asString()));
        else plugin.bundle().sendMessage(context.getSource().getSender(), message);
        return alreadyOn ? 0 : SINGLE_SUCCESS;
    }
}
