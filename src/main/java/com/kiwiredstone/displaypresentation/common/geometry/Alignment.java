package com.kiwiredstone.displaypresentation.common.geometry;

/**
 * Anchor alignment for an element relative to its anchor point.
 *
 * <p>The anchor point is a single coordinate in slide space. The alignment decides which point of
 * the element's bounding box (of size {@code width} x {@code height}) is pinned to that anchor.
 * For example {@link #TOP_LEFT} pins the element's top-left corner to the anchor, so the element
 * extends to the right and downwards from there.
 *
 * <p>The factors are expressed so that the element <em>centre</em> can be derived from the anchor:
 * <pre>
 *   centreX = anchorX - hFactor * width  / 2
 *   centreY = anchorY - vFactor * height / 2
 * </pre>
 * where {@code hFactor} is -1 for a left edge, 0 for centred, +1 for a right edge and {@code vFactor}
 * is +1 for a top edge, 0 for centred, -1 for a bottom edge (slide +Y points up).
 */
public enum Alignment {
    TOP_LEFT(-1, 1),
    TOP_CENTER(0, 1),
    TOP_RIGHT(1, 1),
    CENTER_LEFT(-1, 0),
    CENTER(0, 0),
    CENTER_RIGHT(1, 0),
    BOTTOM_LEFT(-1, -1),
    BOTTOM_CENTER(0, -1),
    BOTTOM_RIGHT(1, -1);

    private final int hFactor;
    private final int vFactor;

    Alignment(int hFactor, int vFactor) {
        this.hFactor = hFactor;
        this.vFactor = vFactor;
    }

    public int hFactor() {
        return hFactor;
    }

    public int vFactor() {
        return vFactor;
    }

    /** Horizontal offset from the anchor to the element centre, given the element width. */
    public double centreOffsetX(double width) {
        return -hFactor * width * 0.5;
    }

    /** Vertical offset from the anchor to the element centre, given the element height. */
    public double centreOffsetY(double height) {
        return -vFactor * height * 0.5;
    }

    public static Alignment fromString(String value, Alignment fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Alignment.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
