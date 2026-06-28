package com.kiwiredstone.displaypresentation.common.model;

/** A 2D point in slide space, used for anchors and animation targets. */
public final class Vec2 {
    public double x;
    public double y;

    public Vec2() {
    }

    public Vec2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec2 copy() {
        return new Vec2(x, y);
    }
}
