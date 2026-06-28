package com.kiwiredstone.displaypresentation.common.runtime;

import com.kiwiredstone.displaypresentation.common.geometry.SlideFrame;
import com.kiwiredstone.displaypresentation.common.model.AnimationDefinition;
import com.kiwiredstone.displaypresentation.common.model.ElementDefinition;
import com.kiwiredstone.displaypresentation.common.render.ElementTransform;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;

import java.util.List;
import java.util.UUID;

/**
 * Runtime counterpart of an {@link ElementDefinition} (design point 2, "BaseElement").
 *
 * <p>A {@code BaseElement} owns one spawned display entity and the mutable, animatable state of that
 * element (its current anchor, rotation, scale and — for text — opacity). It knows how to:
 * <ul>
 *   <li>resolve its current state into a slide-local {@link ElementTransform}, applying the anchor
 *       alignment and the parent-group transform;</li>
 *   <li>replay the slide's animations up to a given step;</li>
 *   <li>morph onto a new definition when the same element id survives into the next slide.</li>
 * </ul>
 *
 * <p>Subclasses provide the entity-specific spawn and push behaviour.
 */
public abstract class BaseElement {
    protected final ServerLevel level;
    protected final String showName;

    /**
     * Stable identifier for this element within the slideshow, including any group path (e.g.
     * {@code group1/title}). Used to tag the entity and to match the element across slide
     * boundaries when morphing.
     */
    protected final String elementId;

    protected ElementDefinition def;
    protected SlideFrame frame;
    protected ParentTransform parent;
    protected double depth;

    protected Display entity;

    // Current animatable state, expressed in the element's own local space (before the parent transform).
    protected double curAnchorX;
    protected double curAnchorY;
    protected double curRotation;
    protected double curScale;

    protected BaseElement(ServerLevel level, String showName, String elementId, ElementDefinition def,
                          SlideFrame frame, ParentTransform parent, double depth) {
        this.level = level;
        this.showName = showName;
        this.elementId = elementId;
        this.def = def;
        this.frame = frame;
        this.parent = parent;
        this.depth = depth;
        initBaseState();
    }

    public String id() {
        return elementId;
    }

    public ElementDefinition definition() {
        return def;
    }

    public com.kiwiredstone.displaypresentation.common.model.ElementType kind() {
        return def.elementType();
    }

    public ParentTransform parent() {
        return parent;
    }

    public double depth() {
        return depth;
    }

    public boolean hasEntity() {
        return entity != null;
    }

    public UUID entityUuid() {
        return entity == null ? null : entity.getUUID();
    }

    /** Spawns the backing display entity and pushes the initial (step 0) state. */
    public abstract void spawn();

    /** Pushes the current resolved transform (and any entity-specific data) with interpolation. */
    protected abstract void pushToEntity(ElementTransform transform, int interpolationTicks);

    /** Non-virtual base reset, safe to call from the constructor. */
    private void initBaseState() {
        this.curAnchorX = def.anchor.x;
        this.curAnchorY = def.anchor.y;
        this.curRotation = def.rotation;
        this.curScale = def.scale;
    }

    protected void resetState() {
        initBaseState();
    }

    /**
     * Replays every animation up to and including {@code step}, then pushes the result. When
     * {@code animate} is true the push uses the longest interpolation duration declared at that step
     * so the client tweens; otherwise it snaps.
     */
    public void applyStep(int step, boolean animate) {
        resetState();
        int interpolation = 0;
        List<AnimationDefinition> sorted = def.animations.stream()
                .filter(a -> a.step <= step)
                .sorted((a, b) -> Integer.compare(a.step, b.step))
                .toList();
        for (AnimationDefinition anim : sorted) {
            applyAnimation(anim);
            if (anim.step == step) {
                interpolation = Math.max(interpolation, anim.durationTicks);
            }
        }
        pushToEntity(resolveTransform(), animate ? interpolation : 0);
    }

    protected void applyAnimation(AnimationDefinition anim) {
        switch (anim.type) {
            case MOVE -> {
                if (anim.to != null) {
                    curAnchorX = anim.to.x;
                    curAnchorY = anim.to.y;
                }
            }
            case ROTATE -> {
                if (anim.angle != null) {
                    curRotation = anim.angle;
                }
            }
            case SCALE -> {
                if (anim.scale != null) {
                    curScale = anim.scale;
                }
            }
            case OPACITY -> applyOpacity(anim);
        }
    }

    /** Text elements override this; other element kinds ignore opacity. */
    protected void applyOpacity(AnimationDefinition anim) {
    }

    /** Resolves the element's current state into a slide-local transform. */
    public ElementTransform resolveTransform() {
        double effScale = parent.scale * curScale;
        double effRotationDeg = parent.rotationDeg + curRotation;
        double effW = def.size.x * effScale;
        double effH = def.size.y * effScale;

        // Alignment offset, expressed along the element's own (rotated) axes.
        double offLocalX = def.alignment.centreOffsetX(effW);
        double offLocalY = def.alignment.centreOffsetY(effH);
        double rad = Math.toRadians(effRotationDeg);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double offX = cos * offLocalX - sin * offLocalY;
        double offY = sin * offLocalX + cos * offLocalY;

        double[] anchorSlide = parent.apply(curAnchorX, curAnchorY);

        ElementTransform t = new ElementTransform();
        t.centerX = anchorSlide[0] + offX;
        t.centerY = anchorSlide[1] + offY;
        t.depth = depth;
        t.rotationDeg = effRotationDeg;
        t.scaleX = effScale;
        t.scaleY = effScale;
        return t;
    }

    /** Whether this element's anchor currently lies inside the slide frame (a pure logic check). */
    public boolean isWithinFrame() {
        double[] anchorSlide = parent.apply(curAnchorX, curAnchorY);
        return frame.contains(anchorSlide[0], anchorSlide[1]);
    }

    /**
     * Rebinds this element onto a new definition/frame/parent (used when an element with the same id
     * survives into the next slide) and tweens to that slide's initial layout, reusing the existing
     * entity rather than respawning it.
     */
    public void morphTo(ElementDefinition nextDef, SlideFrame nextFrame, ParentTransform nextParent,
                        double nextDepth, int durationTicks) {
        this.def = nextDef;
        this.frame = nextFrame;
        this.parent = nextParent;
        this.depth = nextDepth;
        resetState();
        onMorph(nextDef);
        pushToEntity(resolveTransform(), durationTicks);
    }

    /** Hook for subclasses to refresh entity-specific data (e.g. text content) when morphing. */
    protected void onMorph(ElementDefinition nextDef) {
    }

    public void remove() {
        if (entity != null) {
            entity.discard();
            entity = null;
        }
    }
}
