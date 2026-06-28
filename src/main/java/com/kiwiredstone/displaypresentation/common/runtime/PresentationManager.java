package com.kiwiredstone.displaypresentation.common.runtime;

import com.kiwiredstone.displaypresentation.common.Constants;
import com.kiwiredstone.displaypresentation.common.geometry.SlideFrame;
import com.kiwiredstone.displaypresentation.common.model.SlideshowDefinition;
import com.kiwiredstone.displaypresentation.common.model.json.SlideshowRepository;
import com.kiwiredstone.displaypresentation.common.persistence.PresentationState;
import com.kiwiredstone.displaypresentation.common.persistence.PresentationStore;
import com.kiwiredstone.displaypresentation.common.render.DisplayEntities;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Top-level controller for live presentations.
 *
 * <p>It owns the in-memory map of running {@link SlideProgress} state machines, mirrors their
 * progress into the persistent {@link PresentationState}, ticks them, and — crucially — runs the
 * startup recovery pass that removes display entities left behind by a presentation that was
 * interrupted by a shutdown.
 *
 * <p>This class is loader-agnostic; the Fabric layer simply forwards server lifecycle, tick and
 * command events to it.
 */
public final class PresentationManager {
    private static final PresentationManager INSTANCE = new PresentationManager();

    private final Map<String, SlideProgress> active = new LinkedHashMap<>();

    private PresentationManager() {
    }

    public static PresentationManager get() {
        return INSTANCE;
    }

    public boolean isActive(String name) {
        return active.containsKey(name);
    }

    public List<String> activeNames() {
        return List.copyOf(active.keySet());
    }

    /**
     * Loads the named slideshow definition and starts presenting it centred at {@code center} facing
     * {@code (yaw, pitch)}. When {@code scaleWidth} is present it sets the slideshow's absolute width
     * in blocks (so {@code 1} means the default slide is one block wide); otherwise the authored
     * aspect values are used directly as block half-extents. Returns {@code false} if no such
     * definition exists on disk.
     */
    public boolean place(MinecraftServer server, ServerLevel level, String name,
                         Vec3 center, float yaw, float pitch, OptionalDouble scaleWidth) {
        Optional<SlideshowDefinition> loaded = SlideshowRepository.load(name);
        if (loaded.isEmpty()) {
            return false;
        }
        SlideshowDefinition def = loaded.get();
        def.name = name;

        if (active.containsKey(name)) {
            stop(server, name);
        }

        // Convert the requested absolute width into a uniform factor on the aspect half-extents:
        // full width = 2 * aspect.w, and we want it to equal scaleWidth blocks.
        double scaleFactor = 1.0;
        if (scaleWidth.isPresent() && scaleWidth.getAsDouble() > 0.0 && def.aspect.w > 0.0f) {
            scaleFactor = scaleWidth.getAsDouble() / (2.0 * def.aspect.w);
        }

        SlideFrame baseFrame = new SlideFrame(center, yaw, pitch,
                (float) (def.aspect.w * scaleFactor), (float) (def.aspect.h * scaleFactor));
        SlideProgress progress = new SlideProgress(level, def, baseFrame, scaleFactor);
        progress.start();
        active.put(name, progress);

        PresentationState state = PresentationStore.get(server, name);
        state.name = name;
        state.dimension = level.dimension().identifier().toString();
        state.frame = baseFrame;
        state.active = true;
        syncState(state, progress);
        return true;
    }

    public boolean next(MinecraftServer server, String name) {
        SlideProgress progress = active.get(name);
        if (progress == null) {
            return false;
        }
        boolean finished = progress.next();
        persist(server, name, progress);
        if (finished) {
            active.remove(name);
        }
        return true;
    }

    public boolean previous(MinecraftServer server, String name) {
        SlideProgress progress = active.get(name);
        if (progress == null) {
            return false;
        }
        progress.previous();
        persist(server, name, progress);
        return true;
    }

    public boolean gotoSlide(MinecraftServer server, String name, int index) {
        SlideProgress progress = active.get(name);
        if (progress == null) {
            return false;
        }
        progress.gotoSlide(index);
        persist(server, name, progress);
        return true;
    }

    public boolean stop(MinecraftServer server, String name) {
        SlideProgress progress = active.remove(name);
        if (progress != null) {
            progress.cleanup();
        } else {
            // Not running in memory, but there may still be leftover entities from a prior session.
            cleanupOrphans(server, name);
        }
        PresentationState state = PresentationStore.get(server, name);
        state.active = false;
        state.entities.clear();
        state.setDirty();
        return true;
    }

    /** Ticks every running presentation; auto-advancing slides and reaping finished ones. */
    public void tickAll(MinecraftServer server) {
        if (active.isEmpty()) {
            return;
        }
        for (String name : List.copyOf(active.keySet())) {
            SlideProgress progress = active.get(name);
            if (progress == null) {
                continue;
            }
            progress.tick();
            if (progress.isFinished()) {
                active.remove(name);
                PresentationState state = PresentationStore.get(server, name);
                state.active = false;
                state.entities.clear();
                state.setDirty();
            } else {
                syncState(PresentationStore.get(server, name), progress);
            }
        }
    }

    private void persist(MinecraftServer server, String name, SlideProgress progress) {
        PresentationState state = PresentationStore.get(server, name);
        if (progress.isFinished()) {
            state.active = false;
            state.entities.clear();
        } else {
            syncState(state, progress);
        }
        state.setDirty();
    }

    private void syncState(PresentationState state, SlideProgress progress) {
        state.slideIndex = progress.slideIndex();
        state.stepIndex = progress.stepIndex();
        state.active = !progress.isFinished();
        state.entities.clear();
        state.entities.addAll(progress.liveEntityUuids());
        state.setDirty();
    }

    /**
     * Startup recovery: any presentation still marked active in a saved state was interrupted by a
     * shutdown, so its display entities are still in the world. Remove them and clear the flag, so no
     * display entity is left behind (matching the cleanup guarantee of the main loop).
     */
    public void recoverOnStart(MinecraftServer server) {
        for (String name : PresentationStore.listStateNames(server)) {
            PresentationState state = PresentationStore.get(server, name);
            if (!state.active) {
                continue;
            }
            Constants.LOGGER.info("Recovering interrupted presentation '{}'; cleaning up leftovers", name);
            cleanupOrphans(server, name, state);
            state.active = false;
            state.entities.clear();
            state.setDirty();
        }
    }

    private void cleanupOrphans(MinecraftServer server, String name) {
        cleanupOrphans(server, name, PresentationStore.get(server, name));
    }

    /**
     * Removes leftover display entities for a slideshow. Loads the chunk around the stored frame so
     * the entities are present, removes everything carrying the show's tag, then removes any stored
     * entity ids that somehow escaped the tag scan.
     */
    private void cleanupOrphans(MinecraftServer server, String name, PresentationState state) {
        ServerLevel level = resolveLevel(server, state.dimension);
        if (level == null) {
            return;
        }
        if (state.frame != null) {
            Vec3 c = state.frame.center();
            int chunkX = ((int) Math.floor(c.x)) >> 4;
            int chunkZ = ((int) Math.floor(c.z)) >> 4;
            level.getChunk(chunkX, chunkZ); // force-load so the entities exist before we scan
        }
        DisplayEntities.removeShow(level, name);

        if (!state.entities.isEmpty()) {
            for (UUID uuid : state.entities) {
                var entity = level.getEntityInAnyDimension(uuid);
                if (entity instanceof Display display) {
                    display.discard();
                }
            }
        }
    }

    private ServerLevel resolveLevel(MinecraftServer server, String dimension) {
        Identifier location = Identifier.tryParse(dimension);
        if (location == null) {
            return server.overworld();
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, location);
        ServerLevel level = server.getLevel(key);
        return level != null ? level : server.overworld();
    }
}
