package com.kiwiredstone.displaypresentation.common.command;

import com.kiwiredstone.displaypresentation.common.model.json.SlideshowRepository;
import com.kiwiredstone.displaypresentation.common.runtime.PresentationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The {@code /slideshow} command tree, built with vanilla Brigadier so it carries no loader-specific
 * dependency. The Fabric layer just hands us the dispatcher to register against.
 *
 * <p>Subcommands: {@code place}, {@code stop}, {@code next}, {@code prev}, {@code goto}, {@code list}.
 * All require permission level 2 (operator), matching other world-affecting commands.
 */
public final class SlideshowCommand {
    /** Distance in front of the player the slide is placed at by the no-coordinate form. */
    private static final double PLACE_DISTANCE = 3.0;

    private SlideshowCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("slideshow")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("place")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(SlideshowCommand::placeFacingPlayer)
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .then(Commands.argument("yaw", FloatArgumentType.floatArg())
                                                .then(Commands.argument("pitch", FloatArgumentType.floatArg())
                                                        .executes(SlideshowCommand::placeExplicit))))))
                .then(Commands.literal("stop")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(SlideshowCommand::stop)))
                .then(Commands.literal("next")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> advance(ctx, true))))
                .then(Commands.literal("prev")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> advance(ctx, false))))
                .then(Commands.literal("goto")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                        .executes(SlideshowCommand::gotoSlide))))
                .then(Commands.literal("list")
                        .executes(SlideshowCommand::list)));
    }

    private static int placeFacingPlayer(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");

        Vec3 base = source.getPosition();
        float yaw;
        float pitch;
        Vec3 center;

        Entity entity = source.getEntity();
        if (entity != null) {
            Vec3 look = entity.getLookAngle();
            center = base.add(look.scale(PLACE_DISTANCE));
            // Face the player: the slide normal points back toward them.
            yaw = Mth.wrapDegrees(entity.getYRot() + 180.0f);
            pitch = -entity.getXRot();
        } else {
            center = base;
            yaw = 0.0f;
            pitch = 0.0f;
        }

        return doPlace(source, name, center, yaw, pitch);
    }

    private static int placeExplicit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");
        Vec3 center = Vec3Argument.getVec3(ctx, "pos");
        float yaw = FloatArgumentType.getFloat(ctx, "yaw");
        float pitch = FloatArgumentType.getFloat(ctx, "pitch");
        return doPlace(source, name, center, yaw, pitch);
    }

    private static int doPlace(CommandSourceStack source, String name, Vec3 center, float yaw, float pitch) {
        MinecraftServer server = source.getServer();
        ServerLevel level = source.getLevel();
        boolean ok = PresentationManager.get().place(server, level, name, center, yaw, pitch);
        if (!ok) {
            source.sendFailure(Component.literal("No slideshow '" + name + "' found in /slideshows"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Started slideshow '" + name + "'"), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");
        PresentationManager.get().stop(source.getServer(), name);
        source.sendSuccess(() -> Component.literal("Stopped slideshow '" + name + "'"), true);
        return 1;
    }

    private static int advance(CommandContext<CommandSourceStack> ctx, boolean forward) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");
        boolean running = forward
                ? PresentationManager.get().next(source.getServer(), name)
                : PresentationManager.get().previous(source.getServer(), name);
        if (!running) {
            source.sendFailure(Component.literal("Slideshow '" + name + "' is not running"));
            return 0;
        }
        return 1;
    }

    private static int gotoSlide(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");
        int index = IntegerArgumentType.getInteger(ctx, "index");
        boolean running = PresentationManager.get().gotoSlide(source.getServer(), name, index);
        if (!running) {
            source.sendFailure(Component.literal("Slideshow '" + name + "' is not running"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Jumped to slide " + index), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<String> defs = SlideshowRepository.list();
        List<String> running = PresentationManager.get().activeNames();
        source.sendSuccess(() -> Component.literal(
                "Slideshows on disk: " + (defs.isEmpty() ? "(none)" : String.join(", ", defs))
                        + "\nRunning: " + (running.isEmpty() ? "(none)" : String.join(", ", running))), false);
        return 1;
    }
}
