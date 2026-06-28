package com.kiwiredstone.displaypresentation;

import com.kiwiredstone.displaypresentation.common.Constants;
import com.kiwiredstone.displaypresentation.common.command.SlideshowCommand;
import com.kiwiredstone.displaypresentation.common.runtime.PresentationManager;
import com.kiwiredstone.displaypresentation.common.setup.ExampleSlideshow;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

/**
 * Fabric entrypoint — the only Fabric-specific class in {@code src/main}.
 *
 * <p>It is deliberately thin: it bootstraps the loader-agnostic {@link Constants} with the game
 * directory and forwards Fabric's command / lifecycle / tick events to the
 * {@link PresentationManager} and {@link SlideshowCommand} living in the {@code common} package. All
 * actual slideshow logic is loader-independent, so porting to another loader means rewriting only
 * this file.
 */
public class DisplayPresentation implements ModInitializer {
    public static final String MOD_ID = Constants.MOD_ID;

    @Override
    public void onInitialize() {
        // Hand the loader-agnostic core the game directory so it can resolve /slideshows.
        Constants.bootstrap(FabricLoader.getInstance().getGameDir());
        ExampleSlideshow.writeIfMissing();

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> SlideshowCommand.register(dispatcher));

        // On start, clean up any presentation that was interrupted by a previous shutdown.
        ServerLifecycleEvents.SERVER_STARTED.register(
                server -> PresentationManager.get().recoverOnStart(server));

        // Drive auto-advancing slides and reap finished presentations every tick.
        ServerTickEvents.END_SERVER_TICK.register(
                server -> PresentationManager.get().tickAll(server));

        Constants.LOGGER.info("Display Presentation initialised");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
