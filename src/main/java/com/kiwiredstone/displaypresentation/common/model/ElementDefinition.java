package com.kiwiredstone.displaypresentation.common.model;

import com.kiwiredstone.displaypresentation.common.geometry.Alignment;

import java.util.ArrayList;
import java.util.List;

/**
 * Base of every element in a slide (design point 2, "BaseElement").
 *
 * <p>Holds the data common to all element kinds: a stable {@code id} (used to morph an element
 * across slide boundaries), the {@code anchor} point in slide coordinates, the {@code alignment}
 * that decides which part of the element sits on the anchor, the element {@code size} (used both
 * for the anchor translation and as the design bounding box) and the list of {@code animations}.
 *
 * <p>Concrete subclasses ({@link TextElementDefinition}, {@link GroupElementDefinition}) add their
 * own fields. The {@code type} field is the on-disk discriminator and is filled in by the JSON
 * adapter.
 */
public abstract class ElementDefinition {
    public String id;
    public String type;

    public Vec2 anchor = new Vec2(0.0, 0.0);
    public Alignment alignment = Alignment.CENTER;
    public Vec2 size = new Vec2(1.0, 0.5);

    /** Uniform scale multiplier applied on top of {@link #size}. */
    public double scale = 1.0;

    /** Initial in-plane rotation in degrees (positive = counter-clockwise about the slide normal). */
    public double rotation = 0.0;

    public List<AnimationDefinition> animations = new ArrayList<>();

    public abstract ElementType elementType();

    /** Highest animation step referenced by this element (0 if it never animates). */
    public int maxStep() {
        int max = 0;
        for (AnimationDefinition a : animations) {
            max = Math.max(max, a.step);
        }
        return max;
    }
}
