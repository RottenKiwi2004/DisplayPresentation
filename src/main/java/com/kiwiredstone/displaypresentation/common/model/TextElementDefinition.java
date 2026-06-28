package com.kiwiredstone.displaypresentation.common.model;

/**
 * A text element, backed at runtime by a vanilla {@code TextDisplay} entity. The styling fields map
 * directly onto the text display's persisted NBT (text, line width, background colour, opacity,
 * shadow, see-through, alignment).
 */
public final class TextElementDefinition extends ElementDefinition {
    public String text = "";

    /** Foreground colour as {@code #RRGGBB}; {@code null} keeps the vanilla default white. */
    public String color;

    public boolean bold = false;
    public boolean italic = false;

    /** Maximum line width in pixels before the text wraps (vanilla default 200). */
    public int lineWidth = 200;

    /** Background colour as packed ARGB int. Vanilla default {@code 0x40000000} (25% black). */
    public int backgroundColor = 0x40000000;

    /** Text opacity, {@code -1} for fully opaque, otherwise {@code 0..255}. */
    public int textOpacity = -1;

    public boolean shadow = false;
    public boolean seeThrough = false;
    public boolean defaultBackground = false;

    /** Multi-line justification: {@code CENTER}, {@code LEFT} or {@code RIGHT}. */
    public String textAlign = "CENTER";

    @Override
    public ElementType elementType() {
        return ElementType.TEXT;
    }
}
