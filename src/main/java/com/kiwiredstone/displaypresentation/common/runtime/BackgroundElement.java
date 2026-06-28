package com.kiwiredstone.displaypresentation.common.runtime;

import com.kiwiredstone.displaypresentation.common.geometry.SlideFrame;
import com.kiwiredstone.displaypresentation.common.render.DisplayEntities;
import com.kiwiredstone.displaypresentation.common.render.ElementTransform;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;

/**
 * Runtime slide background: a text-display entity whose translucent background quad is stretched to
 * exactly fill the slide frame and sits behind every other element.
 *
 * <p>Unlike a normal element it ignores anchor alignment and uniform scale; it computes its own
 * non-uniform scale and centring from the frame size (see {@link #resolveTransform()}). Because it
 * carries the reserved id {@link #ID}, it morphs across slides like any other element — recolouring
 * and resizing instead of being respawned.
 */
public final class BackgroundElement extends BaseElement {
    public static final String ID = "__bg__";

    /** Depth (along the slide normal) placing the background just behind the content layer. */
    public static final double DEPTH = -0.01;

    public BackgroundElement(ServerLevel level, String showName, BackgroundElementDefinition def, SlideFrame frame) {
        super(level, showName, ID, def, frame, ParentTransform.IDENTITY, DEPTH);
    }

    private int argb() {
        return ((BackgroundElementDefinition) def).argb;
    }

    @Override
    public void spawn() {
        this.entity = DisplayEntities.spawnText(level, frame, showName, ID);
        applyStep(0, false);
    }

    @Override
    public ElementTransform resolveTransform() {
        double targetWidth = frame.w() * 2.0;
        double targetHeight = frame.h() * 2.0;

        ElementTransform t = new ElementTransform();
        t.scaleX = targetWidth / DisplayEntities.BG_WIDTH_AT_SCALE1;
        t.scaleY = targetHeight / DisplayEntities.BG_HEIGHT_AT_SCALE1;
        // Re-centre the quad: at scale 1 its centre sits at (BG_CENTER_X, BG_CENTER_Y) from the
        // transform translation, and that offset scales with the stretch.
        t.centerX = -DisplayEntities.BG_CENTER_X_AT_SCALE1 * t.scaleX;
        t.centerY = -DisplayEntities.BG_CENTER_Y_AT_SCALE1 * t.scaleY;
        t.depth = depth;
        t.rotationDeg = 0.0;
        return t;
    }

    @Override
    protected void pushToEntity(ElementTransform transform, int interpolationTicks) {
        if (entity == null) {
            return;
        }
        double cullSize = Math.max(frame.w(), frame.h()) * 2.5;
        DisplayEntities.applyBackgroundRect(
                (Display.TextDisplay) entity, argb(), cullSize, transform, interpolationTicks);
    }
}
