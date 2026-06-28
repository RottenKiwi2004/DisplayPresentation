package com.kiwiredstone.displaypresentation.common.model;

/**
 * A straight line drawn from {@link #pointA} to {@link #pointB} in slide coordinates. It is rendered
 * as a thin filled rectangle whose two short ends are centred exactly on A and B, with the adjustable
 * {@link #lineWidth} as its thickness.
 *
 * <p>Unlike text, a line is positioned entirely by its two endpoints, so the inherited {@code anchor}
 * acts only as an optional translation offset for the whole line (and is what {@code MOVE} animations
 * adjust); {@code size} and {@code alignment} are unused. The inherited {@code rotation} adds an extra
 * spin about the line's midpoint and {@code scale} multiplies the thickness.
 */
public final class LineElementDefinition extends ElementDefinition {
    public Vec2 pointA = new Vec2(0.0, 0.0);
    public Vec2 pointB = new Vec2(0.0, 0.0);

    /** Thickness of the line in slide units. */
    public double lineWidth = 0.05;

    /** Line colour as {@code #RRGGBB}. */
    public String color = "#FFFFFF";

    /** Opacity in {@code [0, 1]}. */
    public float alpha = 1.0f;

    @Override
    public ElementType elementType() {
        return ElementType.LINE;
    }
}
