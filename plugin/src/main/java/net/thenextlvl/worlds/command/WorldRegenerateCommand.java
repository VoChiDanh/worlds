package net.thenextlvl.worlds.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.worlds.WorldOperationException;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.argument.CommandFlagsArgument;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

import java.util.Set;

import static net.thenextlvl.worlds.command.WorldCommand.worldArgument;

@NullMarked
final class WorldRegenerateCommand extends SimpleCommand {
    private WorldRegenerateCommand(final WorldsPlugin plugin) {
        super(plugin, "regenerate", "worlds.command.regenerate");
    }

    public static ArgumentBuilder<CommandSourceStack, ?> create(final WorldsPlugin plugin) {
        final var command = new WorldRegenerateCommand(plugin);
        return command.create().then(command.regenerate());
    }

    private RequiredArgumentBuilder<CommandSourceStack, World> regenerate() {
        return worldArgument(plugin)
                .then(Commands.argument("flags", new CommandFlagsArgument(
                        Set.of("--confirm", "--schedule", "--seed")
                )).executes(this))
                .executes(this::confirmationNeeded);
    }

    private int confirmationNeeded(final CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();
        plugin.bundle().sendMessage(sender, "command.confirmation",
                Placeholder.parsed("action", "/" + context.getInput()),
                Placeholder.parsed("confirmation", "/" + context.getInput() + " --confirm"));
        return 0;
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var flags = context.getArgument("flags", CommandFlagsArgument.Flags.class);
        if (!flags.contains("--confirm")) return confirmationNeeded(context);
        final var world = context.getArgument("world", World.class);
        final var schedule = flags.contains("--schedule");
        if (!schedule) plugin.bundle().sendMessage(context.getSource().getSender(), "world.regenerate",
                Placeholder.parsed("world", world.key().asString()));
        final var future = schedule ? plugin.scheduleRegeneration(world) : plugin.regenerate(world, builder -> {
            if (flags.contains("--seed")) builder.seed(null).resetSpawnPosition(true);
        }).thenApply(ignored -> true);
        future.thenAccept(success -> {
            if (success) {
                final var message = schedule ? "world.regenerate.scheduled" : "world.regenerate.success";
                plugin.bundle().sendMessage(context.getSource().getSender(), message,
                        Placeholder.parsed("world", world.key().asString()));
            } else CommandFailureHandler.handle(plugin, context.getSource().getSender(), new WorldOperationException(
                    WorldOperationException.Reason.EVENT_CANCELLED
            ));
        }).exceptionally(throwable -> {
            CommandFailureHandler.handle(plugin, context.getSource().getSender(), throwable,
                    Placeholder.parsed("world", world.key().asString()));
            return null;
        });
        return SINGLE_SUCCESS;
    }
}
