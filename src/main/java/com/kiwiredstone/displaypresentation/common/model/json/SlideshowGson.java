package com.kiwiredstone.displaypresentation.common.model.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.kiwiredstone.displaypresentation.common.model.ElementDefinition;
import com.kiwiredstone.displaypresentation.common.model.ElementType;
import com.kiwiredstone.displaypresentation.common.model.GroupElementDefinition;
import com.kiwiredstone.displaypresentation.common.model.LineElementDefinition;
import com.kiwiredstone.displaypresentation.common.model.TextElementDefinition;

import java.lang.reflect.Type;

/**
 * Builds the {@link Gson} instance used to read and write slideshow definitions, including the
 * polymorphic adapter that maps the {@code "type"} discriminator to the right
 * {@link ElementDefinition} subclass. Keeping this in one place means the on-disk schema is defined
 * by a single source of truth.
 */
public final class SlideshowGson {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(ElementDefinition.class, new ElementAdapter())
            .create();

    private SlideshowGson() {
    }

    public static Gson get() {
        return GSON;
    }

    /**
     * Resolves an element's concrete class from its {@code "type"} field, then delegates to Gson for
     * the rest of the fields (so each subclass stays a plain data object). Children of groups are
     * handled recursively because they are themselves {@code ElementDefinition} values.
     */
    private static final class ElementAdapter
            implements JsonDeserializer<ElementDefinition>, JsonSerializer<ElementDefinition> {

        @Override
        public ElementDefinition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String typeName = obj.has("type") ? obj.get("type").getAsString() : "text";
            ElementType kind = ElementType.fromString(typeName);
            return switch (kind) {
                case GROUP -> ctx.deserialize(obj, GroupElementDefinition.class);
                case LINE -> ctx.deserialize(obj, LineElementDefinition.class);
                case TEXT -> ctx.deserialize(obj, TextElementDefinition.class);
            };
        }

        @Override
        public JsonElement serialize(ElementDefinition src, Type typeOfSrc, JsonSerializationContext ctx) {
            // Serialize against the concrete class so subclass fields are emitted, and make sure the
            // discriminator is present and correct on write.
            JsonElement element = ctx.serialize(src, src.getClass());
            if (element.isJsonObject()) {
                element.getAsJsonObject().addProperty(
                        "type", src.elementType().name().toLowerCase(java.util.Locale.ROOT));
            }
            return element;
        }
    }
}
