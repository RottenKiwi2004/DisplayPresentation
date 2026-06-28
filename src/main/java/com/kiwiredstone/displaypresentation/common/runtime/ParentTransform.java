package com.kiwiredstone.displaypresentation.common.runtime;

import com.kiwiredstone.displaypresentation.common.model.GroupElementDefinition;
import com.kiwiredstone.displaypresentation.common.model.Vec2;

/**
 * The accumulated affine transform contributed by the chain of groups an element is nested inside.
 *
 * <p>It maps a point in an element's own local coordinates into slide space via
 * {@code f(p) = R(theta) * (scale * p) + offset}. The root (an element not in any group) uses
 * {@link #IDENTITY}. Composing with a child group follows the standard nested-frame rule, so groups
 * can be nested to any depth.
 */
public final class ParentTransform {
    public static final ParentTransform IDENTITY = new ParentTransform(0.0, 0.0, 0.0, 1.0);

    public final double offsetX;
    public final double offsetY;
    public final double rotationDeg;
    public final double scale;

    public ParentTransform(double offsetX, double offsetY, double rotationDeg, double scale) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.rotationDeg = rotationDeg;
        this.scale = scale;
    }

    public double[] apply(double x, double y) {
        double rad = Math.toRadians(rotationDeg);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double sx = scale * x;
        double sy = scale * y;
        return new double[] {
                cos * sx - sin * sy + offsetX,
                sin * sx + cos * sy + offsetY
        };
    }

    public double[] apply(Vec2 p) {
        return apply(p.x, p.y);
    }

    /** Returns the transform that applies to the children of {@code group} nested inside this one. */
    public ParentTransform compose(GroupElementDefinition group) {
        double[] childOrigin = apply(group.anchor);
        return new ParentTransform(
                childOrigin[0],
                childOrigin[1],
                rotationDeg + group.rotation,
                scale * group.scale);
    }
}
