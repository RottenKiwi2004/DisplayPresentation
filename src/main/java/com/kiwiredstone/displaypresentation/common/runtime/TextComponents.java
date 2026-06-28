package com.kiwiredstone.displaypresentation.common.runtime;

import com.kiwiredstone.displaypresentation.common.Constants;
import com.kiwiredstone.displaypresentation.common.model.TextElementDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Builds a styled {@link Component} from a {@link TextElementDefinition}. */
public final class TextComponents {
    private TextComponents() {
    }

    public static Component build(TextElementDefinition def) {
        String raw = def.text == null ? "" : def.text;
        MutableComponent component = Component.literal(raw);

        Style style = Style.EMPTY
                .withBold(def.bold)
                .withItalic(def.italic);

        Integer rgb = parseColor(def.color);
        if (rgb != null) {
            style = style.withColor(rgb);
        }

        return component.setStyle(style);
    }

    /** Parses {@code #RRGGBB} (or {@code RRGGBB}) into a packed RGB int, or {@code null} if invalid. */
    private static Integer parseColor(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }
        String hex = color.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            Constants.LOGGER.warn("Invalid colour '{}' in slideshow text element", color);
            return null;
        }
    }
}
