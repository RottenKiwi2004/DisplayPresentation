package com.kiwiredstone.displaypresentation.common.model;

/**
 * Discriminator stored as the {@code "type"} field of an element in the {@code .json} file.
 * Only {@link #TEXT} is rendered for now; {@link #GROUP} exists so elements can be nested and
 * organised (mirroring how a PowerPoint group shape contains child shapes).
 */
public enum ElementType {
    TEXT,
    GROUP;

    public static ElementType fromString(String value) {
        if (value == null) {
            return TEXT;
        }
        try {
            return ElementType.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return TEXT;
        }
    }
}
