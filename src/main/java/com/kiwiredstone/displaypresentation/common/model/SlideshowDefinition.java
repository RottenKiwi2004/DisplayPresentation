package com.kiwiredstone.displaypresentation.common.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The whole presentation as stored in a single {@code .json} file under {@code /slideshows}.
 *
 * <p>Holds the slideshow {@code name}, the default {@code aspect} ratio applied to slides that do
 * not override it, and the ordered list of {@code slides}. This object is the design-time source of
 * truth: it is authored externally (in game or by a future web tool), saved as JSON, and loaded on
 * any server to drive a live presentation.
 */
public final class SlideshowDefinition {
    public String name;
    public Aspect aspect = new Aspect();
    public List<SlideDefinition> slides = new ArrayList<>();

    public Aspect aspectFor(SlideDefinition slide) {
        return slide.aspect != null ? slide.aspect : aspect;
    }
}
