package com.kiwiredstone.displaypresentation.common.runtime;

import com.kiwiredstone.displaypresentation.common.geometry.SlideFrame;
import com.kiwiredstone.displaypresentation.common.model.ElementDefinition;
import com.kiwiredstone.displaypresentation.common.model.GroupElementDefinition;
import com.kiwiredstone.displaypresentation.common.model.LineElementDefinition;
import com.kiwiredstone.displaypresentation.common.model.SlideBackground;
import com.kiwiredstone.displaypresentation.common.model.SlideDefinition;
import com.kiwiredstone.displaypresentation.common.model.TextElementDefinition;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A single slide at runtime: a {@link SlideFrame} plus the flattened list of {@link BaseElement}s
 * that live on it.
 *
 * <p>Groups in the definition are flattened into their leaf elements here, with each leaf receiving
 * the composed parent transform and a path-style id (e.g. {@code group1/title}). Leaves are given a
 * small, increasing depth so overlapping elements do not z-fight, with later elements sitting
 * slightly in front.
 */
public final class BaseSlide {
    private static final double DEPTH_STEP = 0.0005;

    private final ServerLevel level;
    private final String showName;
    private final SlideFrame frame;
    private final SlideDefinition def;

    private final List<BaseElement> elements = new ArrayList<>();
    private final Map<String, BaseElement> elementsById = new LinkedHashMap<>();
    private int depthCounter;

    public BaseSlide(ServerLevel level, String showName, SlideFrame frame, SlideDefinition def,
                     SlideBackground background, double scaleFactor) {
        this.level = level;
        this.showName = showName;
        this.frame = frame;
        this.def = def;
        // The background is the first (back-most) element so it morphs/cleans up like any other.
        // It sizes itself from the (already scaled) frame, so it needs no extra scale factor.
        if (background != null && background.enabled) {
            BackgroundElement bg = new BackgroundElement(
                    level, showName, new BackgroundElementDefinition(background.toArgb()), frame);
            elements.add(bg);
            elementsById.put(bg.id(), bg);
        }
        // Root transform carries the slideshow scale, so every element's anchor position, alignment
        // offset and visual scale are multiplied by it (and it composes through nested groups).
        build(def.elements, new ParentTransform(0.0, 0.0, 0.0, scaleFactor), "");
    }

    private void build(List<ElementDefinition> defs, ParentTransform parent, String prefix) {
        int index = 0;
        for (ElementDefinition d : defs) {
            String localId = (d.id == null || d.id.isBlank()) ? ("e" + index) : d.id;
            String pathId = prefix.isEmpty() ? localId : prefix + "/" + localId;

            if (d instanceof GroupElementDefinition group) {
                build(group.children, parent.compose(group), pathId);
            } else if (d instanceof TextElementDefinition text) {
                double depth = depthCounter++ * DEPTH_STEP;
                TextElement element = new TextElement(level, showName, pathId, text, frame, parent, depth);
                elements.add(element);
                elementsById.put(pathId, element);
            } else if (d instanceof LineElementDefinition line) {
                double depth = depthCounter++ * DEPTH_STEP;
                LineElement element = new LineElement(level, showName, pathId, line, frame, parent, depth);
                elements.add(element);
                elementsById.put(pathId, element);
            }
            index++;
        }
    }

    public SlideFrame frame() {
        return frame;
    }

    public SlideDefinition definition() {
        return def;
    }

    public int resolvedSteps() {
        return def.resolvedSteps();
    }

    public List<BaseElement> elements() {
        return elements;
    }

    public Map<String, BaseElement> elementsById() {
        return elementsById;
    }

    /** Spawns every element at its initial (step 0) state. */
    public void load() {
        for (BaseElement element : elements) {
            element.spawn();
        }
    }

    /** Applies a given animation step to every element on the slide. */
    public void applyStep(int step, boolean animate) {
        for (BaseElement element : elements) {
            element.applyStep(step, animate);
        }
    }

    /** Removes every element's entity from the world. */
    public void removeAll() {
        for (BaseElement element : elements) {
            element.remove();
        }
        elements.clear();
        elementsById.clear();
    }
}
