package com.kiwiredstone.displaypresentation.common.setup;

import com.kiwiredstone.displaypresentation.common.Constants;
import com.kiwiredstone.displaypresentation.common.geometry.Alignment;
import com.kiwiredstone.displaypresentation.common.model.AnimationDefinition;
import com.kiwiredstone.displaypresentation.common.model.Aspect;
import com.kiwiredstone.displaypresentation.common.model.GroupElementDefinition;
import com.kiwiredstone.displaypresentation.common.model.SlideDefinition;
import com.kiwiredstone.displaypresentation.common.model.SlideshowDefinition;
import com.kiwiredstone.displaypresentation.common.model.TextElementDefinition;
import com.kiwiredstone.displaypresentation.common.model.Vec2;
import com.kiwiredstone.displaypresentation.common.model.json.SlideshowRepository;

import java.io.IOException;

/**
 * Writes a small {@code example.json} into {@code /slideshows} on first run so there is something to
 * present (and so the JSON schema is self-documenting). It is only written when the directory has no
 * definitions yet, so it never clobbers user content.
 */
public final class ExampleSlideshow {
    private ExampleSlideshow() {
    }

    public static void writeIfMissing() {
        try {
            SlideshowRepository.ensureDirectory();
            if (!SlideshowRepository.list().isEmpty()) {
                return;
            }
            SlideshowRepository.save(build());
            Constants.LOGGER.info("Wrote example slideshow to /slideshows/example.json");
        } catch (IOException | RuntimeException e) {
            Constants.LOGGER.warn("Failed to write example slideshow", e);
        }
    }

    private static SlideshowDefinition build() {
        SlideshowDefinition show = new SlideshowDefinition();
        show.name = "example";
        show.aspect = new Aspect(1.6f, 0.9f);

        // --- Slide 1 ---
        SlideDefinition slide1 = new SlideDefinition();
        slide1.id = "intro";

        TextElementDefinition title = text("title", 0.0, 0.6, Alignment.TOP_CENTER, "Welcome");
        title.color = "#FFD700";
        title.bold = true;
        title.size = new Vec2(1.4, 0.25);

        TextElementDefinition subtitle = text("subtitle", 0.0, 0.0, Alignment.CENTER,
                "A display-entity slideshow");
        AnimationDefinition slideUp = new AnimationDefinition();
        slideUp.step = 1;
        slideUp.type = AnimationDefinition.Type.MOVE;
        slideUp.to = new Vec2(0.0, -0.3);
        slideUp.durationTicks = 10;
        subtitle.animations.add(slideUp);

        slide1.elements.add(title);
        slide1.elements.add(subtitle);

        // --- Slide 2 ---
        SlideDefinition slide2 = new SlideDefinition();
        slide2.id = "details";

        // Same id "title" => morphs from slide 1 instead of being respawned.
        TextElementDefinition title2 = text("title", 0.0, 0.6, Alignment.TOP_CENTER, "Slide Two");
        title2.color = "#80FF80";
        title2.bold = true;
        title2.size = new Vec2(1.4, 0.25);

        GroupElementDefinition info = new GroupElementDefinition();
        info.id = "info";
        info.anchor = new Vec2(-0.4, -0.1);
        info.children.add(text("line1", 0.0, 0.1, Alignment.CENTER_LEFT, "- first point"));
        info.children.add(text("line2", 0.0, -0.1, Alignment.CENTER_LEFT, "- second point"));

        slide2.elements.add(title2);
        slide2.elements.add(info);

        show.slides.add(slide1);
        show.slides.add(slide2);
        return show;
    }

    private static TextElementDefinition text(String id, double x, double y, Alignment alignment, String value) {
        TextElementDefinition def = new TextElementDefinition();
        def.id = id;
        def.anchor = new Vec2(x, y);
        def.alignment = alignment;
        def.size = new Vec2(1.2, 0.2);
        def.text = value;
        return def;
    }
}
