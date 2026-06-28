package com.kiwiredstone.displaypresentation.common.model.json;

import com.kiwiredstone.displaypresentation.common.Constants;
import com.kiwiredstone.displaypresentation.common.model.SlideshowDefinition;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads and writes {@link SlideshowDefinition} files in the {@code /slideshows} directory.
 *
 * <p>This is the bridge between the design-time JSON authored by users (in game or by an external
 * tool) and the runtime. A slideshow named {@code intro} lives in {@code /slideshows/intro.json}.
 */
public final class SlideshowRepository {
    private SlideshowRepository() {
    }

    private static Path fileFor(String name) {
        return Constants.slideshowsDir().resolve(name + ".json");
    }

    public static void ensureDirectory() throws IOException {
        Files.createDirectories(Constants.slideshowsDir());
    }

    /** Lists the names (without extension) of every slideshow definition on disk. */
    public static List<String> list() {
        List<String> names = new ArrayList<>();
        Path dir = Constants.slideshowsDir();
        if (!Files.isDirectory(dir)) {
            return names;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                names.add(fileName.substring(0, fileName.length() - ".json".length()));
            }
        } catch (IOException e) {
            Constants.LOGGER.warn("Failed to list slideshows in {}", dir, e);
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static Optional<SlideshowDefinition> load(String name) {
        Path file = fileFor(name);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            SlideshowDefinition def = SlideshowGson.get().fromJson(reader, SlideshowDefinition.class);
            if (def == null) {
                return Optional.empty();
            }
            if (def.name == null || def.name.isBlank()) {
                def.name = name;
            }
            return Optional.of(def);
        } catch (IOException | RuntimeException e) {
            Constants.LOGGER.error("Failed to load slideshow '{}' from {}", name, file, e);
            return Optional.empty();
        }
    }

    public static void save(SlideshowDefinition definition) throws IOException {
        ensureDirectory();
        Path file = fileFor(definition.name);
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            SlideshowGson.get().toJson(definition, writer);
        }
    }
}
