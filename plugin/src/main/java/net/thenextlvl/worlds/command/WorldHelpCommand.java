package net.thenextlvl.worlds.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class WorldHelpCommand extends SimpleCommand {
    private static final String DOCS_URL = "https://thenextlvl.net/docs/worlds";

    private WorldHelpCommand(final WorldsPlugin plugin) {
        super(plugin, "help", null);
    }

    public static ArgumentBuilder<CommandSourceStack, ?> create(final WorldsPlugin plugin) {
        final var command = new WorldHelpCommand(plugin);
        return command.create().executes(command);
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var sender = context.getSource().getSender();
        plugin.bundle().sendMessage(sender, "world.documentation", Placeholder.parsed("url", DOCS_URL));
        return SINGLE_SUCCESS;
    }
}
