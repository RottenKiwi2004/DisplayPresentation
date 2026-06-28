package com.kiwiredstone.displaypresentation.common.persistence;

import com.kiwiredstone.displaypresentation.common.Constants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Access layer for {@link PresentationState} files.
 *
 * <p>All state is stored in the overworld's data storage so the files land in {@code world/data}
 * (named {@code Slides_<name>.dat}), as required. The store also knows how to enumerate those files
 * directly from disk, which the startup recovery pass uses to find presentations that were active
 * when the server last stopped.
 */
public final class PresentationStore {
    private PresentationStore() {
    }

    /**
     * {@link SavedDataType} instances must be stable per name: {@code DimensionDataStorage} caches
     * loaded data keyed by the {@code SavedDataType} record, and that record's equality includes its
     * constructor {@link java.util.function.Supplier}. A fresh supplier lambda each call would never
     * compare equal, defeating the cache and causing lost updates — so we memoise one instance per
     * name here.
     */
    private static final Map<String, SavedDataType<PresentationState>> TYPES = new ConcurrentHashMap<>();

    private static SavedDataType<PresentationState> typeFor(String name) {
        return TYPES.computeIfAbsent(name, n -> new SavedDataType<>(
                Constants.savedDataId(n),
                () -> new PresentationState(n),
                PresentationState.CODEC,
                DataFixTypes.SAVED_DATA_MAP_DATA));
    }

    /** Loads (or creates) the state for a slideshow. Lives in the overworld data storage. */
    public static PresentationState get(MinecraftServer server, String name) {
        return server.overworld().getDataStorage().computeIfAbsent(typeFor(name));
    }

    private static Path dataDir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data");
    }

    /** Names of every persisted presentation state on disk (whether active or not). */
    public static List<String> listStateNames(MinecraftServer server) {
        List<String> names = new ArrayList<>();
        Path dir = dataDir(server);
        if (!Files.isDirectory(dir)) {
            return names;
        }
        String prefix = Constants.SAVED_DATA_PREFIX;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, prefix + "*.dat")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String name = fileName.substring(prefix.length(), fileName.length() - ".dat".length());
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        } catch (IOException e) {
            Constants.LOGGER.warn("Failed to enumerate presentation state files in {}", dir, e);
        }
        return names;
    }
}
