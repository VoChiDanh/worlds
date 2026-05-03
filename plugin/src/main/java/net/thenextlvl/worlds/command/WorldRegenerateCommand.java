package net.thenextlvl.worlds.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.worlds.OperationScheduler;
import net.thenextlvl.worlds.WorldOperationException;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.argument.CommandOptionsArgument;
import net.thenextlvl.worlds.command.brigadier.SimpleCommand;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import static net.thenextlvl.worlds.command.WorldCommand.worldArgument;
import static net.thenextlvl.worlds.event.WorldActionScheduledEvent.ActionType.REGENERATE;

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
                .then(Commands.argument("options", new CommandOptionsArgument(
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
    public int run(final CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();
        final var options = context.getArgument("options", CommandOptionsArgument.Options.class);
        if (!options.contains("--confirm") && !options.contains("--schedule")) return confirmationNeeded(context);

        final var world = context.getArgument("world", World.class);
        final var schedule = options.contains("--schedule");

        if (!schedule) plugin.bundle().sendMessage(sender, "world.regenerate",
                Placeholder.parsed("world", world.key().asString()));

        final var regenerateSeed = options.contains("--seed");
        final var seed = regenerateSeed ? ThreadLocalRandom.current().nextLong() : world.getSeed();

        if (schedule && plugin.getScheduler().cancel(world.key(), REGENERATE)) {
            plugin.bundle().sendMessage(sender, "world.regenerate.schedule-cancelled",
                    Placeholder.parsed("world", world.key().asString()));
            return SINGLE_SUCCESS;
        }

        final var future = !schedule ? plugin.regenerate(world, builder -> {
            if (regenerateSeed) builder.seed(seed);
        }).thenApply(ignored -> true) : CompletableFuture.completedFuture(plugin.getScheduler().schedule(
                new OperationScheduler.RegenerateOperation(world.key(), seed)
        ));

        future.thenAccept(success -> {
            if (success) {
                final var message = schedule ? "world.regenerate.scheduled" : "world.regenerate.success";
                plugin.bundle().sendMessage(sender, message,
                        Placeholder.parsed("world", world.key().asString()));
            } else CommandFailureHandler.handle(plugin, sender, new WorldOperationException(
                    WorldOperationException.Reason.EVENT_CANCELLED
            ));
        }).exceptionally(throwable -> {
            CommandFailureHandler.handle(plugin, sender, throwable,
                    Placeholder.parsed("world", world.key().asString()));
            return null;
        });
        return SINGLE_SUCCESS;
    }
}
