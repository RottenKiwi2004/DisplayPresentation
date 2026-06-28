package com.kiwiredstone.displaypresentation.common.runtime;

import com.kiwiredstone.displaypresentation.common.geometry.SlideFrame;
import com.kiwiredstone.displaypresentation.common.render.DisplayEntities;
import com.kiwiredstone.displaypresentation.common.render.ElementTransform;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.Vec3;

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

    /**
     * Distance (in blocks) the background is pushed behind the slide centre, along the plane normal
     * derived from yaw/pitch, so the base canvas never z-fights with the elements drawn on it.
     */
    public static final double OFFSET = 0.01;

    public BackgroundElement(ServerLevel level, String showName, BackgroundElementDefinition def, SlideFrame frame) {
        // Depth (transform-Z) is 0; the backward offset is applied to the entity position instead,
        // so the separation is a true perpendicular shift rather than an in-transform nudge.
        super(level, showName, ID, def, frame, ParentTransform.IDENTITY, 0.0);
    }

    private int argb() {
        return ((BackgroundElementDefinition) def).argb;
    }

    @Override
    public void spawn() {
        // Sit a hair behind the slide plane along its normal (perpendicular axis from yaw/pitch).
        Vec3 behind = frame.alongNormal(-OFFSET);
        this.entity = DisplayEntities.spawnTextAt(level, frame, behind, showName, ID);
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
        DisplayEntities.applyFilledRect(
                (Display.TextDisplay) entity, argb(), cullSize, transform, interpolationTicks);
    }
}
