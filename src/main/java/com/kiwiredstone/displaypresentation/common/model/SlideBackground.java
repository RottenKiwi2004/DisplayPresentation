package com.kiwiredstone.displaypresentation.common.model;

/**
 * Per-slide background rectangle.
 *
 * <p>The default preset is a translucent black rectangle at {@code 0.25} opacity (the same value as
 * vanilla's default text-display background, {@code 0x40000000}). Both the colour and the alpha can
 * be overridden per slide in the {@code .json} file, e.g.:
 * <pre>
 *   "background": { "color": "#101830", "alpha": 0.6 }
 * </pre>
 * Set {@code "enabled": false} to give a slide no background at all.
 */
public final class SlideBackground {
    public boolean enabled = true;

    /** Background colour as {@code #RRGGBB}. */
    public String color = "#000000";

    /** Opacity in {@code [0, 1]}; 0.25 is the default translucent-black preset. */
    public float alpha = 0.25f;

    public SlideBackground() {
    }

    public SlideBackground(String color, float alpha, boolean enabled) {
        this.color = color;
        this.alpha = alpha;
        this.enabled = enabled;
    }

    /** Packs the colour and alpha into the ARGB int the text-display {@code background} NBT expects. */
    public int toArgb() {
        float clamped = Math.max(0.0f, Math.min(1.0f, alpha));
        int a = Math.round(clamped * 255.0f) & 0xFF;
        return (a << 24) | (parseRgb(color) & 0xFFFFFF);
    }

    private static int parseRgb(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String hex = value.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public SlideBackground copy() {
        return new SlideBackground(color, alpha, enabled);
    }
}
