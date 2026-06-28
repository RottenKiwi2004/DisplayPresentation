package com.kiwiredstone.displaypresentation.common.runtime;

import com.kiwiredstone.displaypresentation.common.Constants;
import com.kiwiredstone.displaypresentation.common.geometry.SlideFrame;
import com.kiwiredstone.displaypresentation.common.model.Aspect;
import com.kiwiredstone.displaypresentation.common.model.SlideDefinition;
import com.kiwiredstone.displaypresentation.common.model.SlideshowDefinition;
import com.kiwiredstone.displaypresentation.common.render.DisplayEntities;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Drives the slideshow main loop described in design point 3.
 *
 * <pre>
 * start
 * for each slide:
 *     adopt the slide aspect ratio, keeping the centre position
 *     load every element at its initial position
 *     for each animation step:
 *         transform the elements per the json
 *         on the last step (end of slide):
 *             remove elements that do not exist in the next slide
 *             morph elements that also exist in the next slide
 *     if the last slide ended:
 *         clean up the whole presentation, leaving no display entity behind
 * </pre>
 *
 * <p>The class owns the authoritative map of currently live elements ({@link #live}). Stepping and
 * morphing operate on that map directly, so a morphed element keeps its entity (and just tweens to
 * the next slide's layout) while removed elements are discarded and new ones are spawned.
 */
public final class SlideProgress {
    /** Default interpolation (ticks) used when morphing elements between slides. */
    private static final int MORPH_TICKS = 10;

    private final ServerLevel level;
    private final SlideshowDefinition show;
    private final String showName;

    /** Centre/orientation of the presentation; per-slide aspect ratios are applied on top of this. */
    private final SlideFrame baseFrame;

    /**
     * Uniform multiplier applied to every slide's aspect half-extents. It converts the authored
     * aspect ratio into an absolute world size: with the default placement, the slideshow's default
     * full width equals the {@code scale} blocks requested at {@code /slideshow place}.
     */
    private final double scaleFactor;

    private final Map<String, BaseElement> live = new LinkedHashMap<>();
    private BaseSlide currentSlide;
    private int slideIndex = -1;
    private int stepIndex = 0;
    private boolean finished = false;

    private int ticksSinceStep = 0;

    public SlideProgress(ServerLevel level, SlideshowDefinition show, SlideFrame baseFrame, double scaleFactor) {
        this.level = level;
        this.show = show;
        this.showName = show.name;
        this.baseFrame = baseFrame;
        this.scaleFactor = scaleFactor;
    }

    public ServerLevel level() {
        return level;
    }

    public SlideFrame baseFrame() {
        return baseFrame;
    }

    public int slideIndex() {
        return slideIndex;
    }

    public int stepIndex() {
        return stepIndex;
    }

    public boolean isFinished() {
        return finished;
    }

    public int slideCount() {
        return show.slides.size();
    }

    public List<UUID> liveEntityUuids() {
        List<UUID> ids = new ArrayList<>();
        for (BaseElement element : live.values()) {
            UUID uuid = element.entityUuid();
            if (uuid != null) {
                ids.add(uuid);
            }
        }
        return ids;
    }

    private SlideFrame frameForSlide(SlideDefinition slide) {
        Aspect aspect = show.aspectFor(slide);
        return baseFrame.withAspect(
                (float) (aspect.w * scaleFactor),
                (float) (aspect.h * scaleFactor));
    }

    /** Begins the presentation at the first slide. */
    public void start() {
        if (show.slides.isEmpty()) {
            Constants.LOGGER.warn("Slideshow '{}' has no slides; nothing to present", showName);
            finished = true;
            return;
        }
        gotoSlide(0);
    }

    /**
     * Cleanly (re)builds a slide from scratch: removes all live entities, spawns the target slide's
     * elements at their initial layout. Used for the first slide and for jumping/going back.
     */
    public void gotoSlide(int index) {
        if (index < 0 || index >= show.slides.size()) {
            return;
        }
        removeAllLive();

        slideIndex = index;
        stepIndex = 0;
        ticksSinceStep = 0;
        SlideDefinition slideDef = show.slides.get(index);
        currentSlide = new BaseSlide(level, showName, frameForSlide(slideDef), slideDef,
                show.backgroundFor(slideDef), scaleFactor);
        for (BaseElement element : currentSlide.elements()) {
            element.spawn();
            live.put(element.id(), element);
        }
    }

    /** Advances one animation step, or transitions to the next slide / finishes. */
    public boolean next() {
        if (finished || currentSlide == null) {
            return finished;
        }
        ticksSinceStep = 0;

        if (stepIndex < currentSlide.resolvedSteps()) {
            stepIndex++;
            applyStepToLive(stepIndex, true);
            return false;
        }

        // End of slide.
        if (slideIndex >= show.slides.size() - 1) {
            cleanup();
            return true;
        }
        transitionToNextSlide();
        return false;
    }

    /** Goes back one step, or rebuilds the previous slide at its initial layout. */
    public void previous() {
        if (finished || currentSlide == null) {
            return;
        }
        ticksSinceStep = 0;
        if (stepIndex > 0) {
            stepIndex--;
            applyStepToLive(stepIndex, true);
        } else if (slideIndex > 0) {
            gotoSlide(slideIndex - 1);
        }
    }

    private void applyStepToLive(int step, boolean animate) {
        for (BaseElement element : live.values()) {
            element.applyStep(step, animate);
        }
    }

    /**
     * Implements the "end of slide" transition: build the next slide, morph elements shared by id
     * (reusing their entities), spawn brand-new ones, and remove elements absent from the next slide.
     */
    private void transitionToNextSlide() {
        SlideDefinition nextDef = show.slides.get(slideIndex + 1);
        BaseSlide next = new BaseSlide(level, showName, frameForSlide(nextDef), nextDef,
                show.backgroundFor(nextDef), scaleFactor);

        Map<String, BaseElement> nextLive = new LinkedHashMap<>();
        for (BaseElement nextElement : next.elements()) {
            String id = nextElement.id();
            BaseElement current = live.get(id);
            if (current != null && current.kind() == nextElement.kind() && current.hasEntity()) {
                // Morph the surviving element onto the next slide's definition, keeping its entity.
                current.morphTo(nextElement.definition(), next.frame(),
                        nextElement.parent(), nextElement.depth(), MORPH_TICKS);
                nextLive.put(id, current);
            } else {
                nextElement.spawn();
                nextLive.put(id, nextElement);
            }
        }

        // Remove any element that does not exist in the next slide.
        for (Map.Entry<String, BaseElement> entry : live.entrySet()) {
            if (!nextLive.containsKey(entry.getKey())) {
                entry.getValue().remove();
            }
        }

        live.clear();
        live.putAll(nextLive);
        currentSlide = next;
        slideIndex++;
        stepIndex = 0;
    }

    private void removeAllLive() {
        for (BaseElement element : live.values()) {
            element.remove();
        }
        live.clear();
    }

    /** Tears the whole presentation down, leaving no display entity behind. */
    public void cleanup() {
        removeAllLive();
        // Safety net: discard any stray tagged entities that escaped the live map (e.g. after a
        // mid-presentation reload that left orphans in the world).
        DisplayEntities.removeShow(level, showName);
        finished = true;
    }

    /** Server-tick hook; advances automatically when the current slide declares an auto-advance. */
    public void tick() {
        if (finished || currentSlide == null) {
            return;
        }
        int autoTicks = currentSlide.definition().autoAdvanceTicks;
        if (autoTicks <= 0) {
            return;
        }
        ticksSinceStep++;
        if (ticksSinceStep >= autoTicks) {
            next();
        }
    }
}
