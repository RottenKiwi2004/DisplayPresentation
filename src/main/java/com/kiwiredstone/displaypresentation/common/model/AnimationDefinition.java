package com.kiwiredstone.displaypresentation.common.model;

/**
 * One animation applied to an element when the slide reaches a given step.
 *
 * <p>Each entry is keyed to a {@code step} index within the slide. When the {@code SlideProgress}
 * loop advances to that step, the element's target transform is updated and handed to the display
 * entity with an interpolation duration, so the vanilla client smoothly tweens from its current
 * pose to the new one. Animations are absolute targets (not deltas) which keeps re-entrancy and
 * crash-recovery simple.
 */
public final class AnimationDefinition {
    public enum Type {
        MOVE,
        ROTATE,
        SCALE,
        OPACITY;

        public static Type fromString(String value) {
            if (value == null) {
                return MOVE;
            }
            try {
                return Type.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return MOVE;
            }
        }
    }

    /** Step within the slide at which this animation fires (0 = initial state). */
    public int step = 1;

    public Type type = Type.MOVE;

    /** Number of ticks the client should interpolate over (0 = snap instantly). */
    public int durationTicks = 0;

    /** Target anchor position for {@link Type#MOVE}. */
    public Vec2 to;

    /** Target absolute rotation in degrees for {@link Type#ROTATE}. */
    public Double angle;

    /** Target absolute scale multiplier for {@link Type#SCALE}. */
    public Double scale;

    /** Target text opacity (-1 = fully opaque default, otherwise 0..255) for {@link Type#OPACITY}. */
    public Integer opacity;
}
