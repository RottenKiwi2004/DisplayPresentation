package com.kiwiredstone.displaypresentation.common.runtime;

import com.kiwiredstone.displaypresentation.common.geometry.SlideFrame;
import com.kiwiredstone.displaypresentation.common.model.AnimationDefinition;
import com.kiwiredstone.displaypresentation.common.model.ElementDefinition;
import com.kiwiredstone.displaypresentation.common.model.LineElementDefinition;
import com.kiwiredstone.displaypresentation.common.render.DisplayEntities;
import com.kiwiredstone.displaypresentation.common.render.ElementTransform;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;

/**
 * A line element: a thin filled rectangle whose short ends are centred on the two endpoints
 * (design "line from A to B"). Backed by the same stretched text-display background quad as the slide
 * background, but rotated to align its length with the A&rarr;B direction.
 *
 * <p>The endpoints are transformed through the parent transform (slideshow scale + any group), so the
 * line's position and length scale with the slideshow; the thickness scales with the parent/own scale.
 */
public final class LineElement extends BaseElement {
    private static final double MIN_EXTENT = 1.0e-4;

    private int rgb;
    private int curOpacity;
    private double cullSize = 1.0;

    public LineElement(ServerLevel level, String showName, String elementId, LineElementDefinition def,
                       SlideFrame frame, ParentTransform parent, double depth) {
        super(level, showName, elementId, def, frame, parent, depth);
        this.rgb = parseRgb(def.color);
        this.curOpacity = alphaToByte(def.alpha);
    }

    private LineElementDefinition lineDef() {
        return (LineElementDefinition) def;
    }

    @Override
    public void spawn() {
        this.entity = DisplayEntities.spawnText(level, frame, showName, elementId);
        applyStep(0, false);
    }

    @Override
    protected void resetState() {
        super.resetState();
        this.curOpacity = alphaToByte(lineDef().alpha);
    }

    @Override
    protected void applyOpacity(AnimationDefinition anim) {
        if (anim.opacity != null) {
            this.curOpacity = anim.opacity & 0xFF;
        }
    }

    @Override
    protected void onMorph(ElementDefinition nextDef) {
        LineElementDefinition line = (LineElementDefinition) nextDef;
        this.rgb = parseRgb(line.color);
        this.curOpacity = alphaToByte(line.alpha);
    }

    @Override
    public ElementTransform resolveTransform() {
        LineElementDefinition d = lineDef();

        // Endpoints in slide space: the (animatable) anchor offset is added in local space, then the
        // parent transform (slideshow scale + groups) is applied to both points.
        double[] a = parent.apply(d.pointA.x + curAnchorX, d.pointA.y + curAnchorY);
        double[] b = parent.apply(d.pointB.x + curAnchorX, d.pointB.y + curAnchorY);

        double midX = (a[0] + b[0]) * 0.5;
        double midY = (a[1] + b[1]) * 0.5;
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];

        double length = Math.max(Math.hypot(dx, dy), MIN_EXTENT);
        double thickness = Math.max(d.lineWidth * parent.scale * curScale, MIN_EXTENT);
        double angleDeg = Math.toDegrees(Math.atan2(dy, dx)) + curRotation;

        ElementTransform t = new ElementTransform();
        t.scaleX = length / DisplayEntities.BG_WIDTH_AT_SCALE1;
        t.scaleY = thickness / DisplayEntities.BG_HEIGHT_AT_SCALE1;
        t.rotationDeg = angleDeg;
        t.depth = depth;

        // Re-centre the quad on the midpoint, accounting for the rotation applied by the transform.
        double rad = Math.toRadians(angleDeg);
        double offX = DisplayEntities.BG_CENTER_X_AT_SCALE1 * t.scaleX;
        double offY = DisplayEntities.BG_CENTER_Y_AT_SCALE1 * t.scaleY;
        t.centerX = midX - (Math.cos(rad) * offX - Math.sin(rad) * offY);
        t.centerY = midY - (Math.sin(rad) * offX + Math.cos(rad) * offY);

        // Culling box must reach from the slide centre (entity position) out to the far end.
        this.cullSize = 2.0 * (Math.hypot(midX, midY) + length * 0.5 + thickness + 0.5);
        return t;
    }

    @Override
    protected void pushToEntity(ElementTransform transform, int interpolationTicks) {
        if (entity == null) {
            return;
        }
        int argb = (curOpacity << 24) | (rgb & 0xFFFFFF);
        DisplayEntities.applyFilledRect(
                (Display.TextDisplay) entity, argb, cullSize, transform, interpolationTicks);
    }

    private static int alphaToByte(float alpha) {
        float clamped = Math.max(0.0f, Math.min(1.0f, alpha));
        return Math.round(clamped * 255.0f) & 0xFF;
    }

    private static int parseRgb(String value) {
        if (value == null || value.isBlank()) {
            return 0xFFFFFF;
        }
        String hex = value.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
}
