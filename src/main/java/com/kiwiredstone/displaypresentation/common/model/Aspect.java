package com.kiwiredstone.displaypresentation.common.model;

/**
 * Slide aspect ratio expressed as half-extents: the slide spans {@code [-w, w]} horizontally and
 * {@code [-h, h]} vertically, with {@code (0, 0)} at its centre.
 */
public final class Aspect {
    public float w = 1.6f;
    public float h = 0.9f;

    public Aspect() {
    }

    public Aspect(float w, float h) {
        this.w = w;
        this.h = h;
    }

    public Aspect copy() {
        return new Aspect(w, h);
    }
}
