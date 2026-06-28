package com.kiwiredstone.displaypresentation.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Loader-agnostic constants and shared paths for the slideshow system.
 *
 * <p>Nothing in {@code com.kiwiredstone.displaypresentation.common} may reference a mod-loader
 * specific API (e.g. {@code net.fabricmc.*}). Only the vanilla Minecraft / Mojang libraries,
 * the JDK, JOML and Gson are allowed here so the logic can be reused on another loader later.
 * The Fabric specific glue lives outside this package and feeds the loader specific values
 * (such as the game directory) in through {@link #bootstrap(Path)}.
 */
public final class Constants {
    public static final String MOD_ID = "display-presentation";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Scoreboard tag attached to every display entity spawned by this mod. Scoreboard tags are
     * vanilla data, so a vanilla server can still load the world; they merely let us rediscover
     * and clean up our entities (for example after an unexpected shutdown).
     */
    public static final String ROOT_TAG = "display_presentation";

    /** Per-slideshow scoreboard tag prefix, e.g. {@code dp_show.intro}. */
    public static final String SHOW_TAG_PREFIX = "dp_show.";

    /** Prefix for the per-slideshow {@code SavedData} file: {@code world/data/Slides_<name>.dat}. */
    public static final String SAVED_DATA_PREFIX = "Slides_";

    /** Directory (relative to the game/server run directory) holding the design-time {@code .json} files. */
    public static final String SLIDESHOWS_DIRNAME = "slideshows";

    private static Path slideshowsDir;

    private Constants() {
    }

    /**
     * Initialised once by the loader specific entrypoint with the game directory. Resolves the
     * {@code /slideshows} directory used to store and retrieve presentation definitions.
     */
    public static void bootstrap(Path gameDir) {
        slideshowsDir = gameDir.resolve(SLIDESHOWS_DIRNAME);
    }

    public static Path slideshowsDir() {
        if (slideshowsDir == null) {
            throw new IllegalStateException("Constants.bootstrap(gameDir) was never called");
        }
        return slideshowsDir;
    }

    public static String showTag(String slideshowName) {
        return SHOW_TAG_PREFIX + slideshowName;
    }

    public static String savedDataId(String slideshowName) {
        return SAVED_DATA_PREFIX + slideshowName;
    }
}
