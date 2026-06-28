package com.kiwiredstone.displaypresentation.common.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One slide of a slideshow.
 *
 * <p>A slide owns its element tree and an optional aspect-ratio override (the slideshow's default
 * aspect is used when this is {@code null}). A slide advances through a number of discrete animation
 * {@code steps}; step 0 is the initial layout and each subsequent step applies the animations whose
 * {@code step} matches. If {@code steps} is left at 0 it is inferred from the highest animation step
 * found in the slide's elements.
 */
public final class SlideDefinition {
    public String id;

    /** Optional aspect-ratio override; {@code null} means inherit the slideshow default. */
    public Aspect aspect;

    /** Optional background override; {@code null} means inherit the slideshow default background. */
    public SlideBackground background;

    public List<ElementDefinition> elements = new ArrayList<>();

    /** Number of animation steps after the initial layout. 0 = infer from animations. */
    public int steps = 0;

    /** If &gt; 0, the slide auto-advances one step every this many server ticks. */
    public int autoAdvanceTicks = 0;

    /** Resolved number of steps, inferring from animations when {@link #steps} is unset. */
    public int resolvedSteps() {
        if (steps > 0) {
            return steps;
        }
        int max = 0;
        for (ElementDefinition e : elements) {
            max = Math.max(max, e.maxStep());
        }
        return max;
    }
}
