package com.kiwiredstone.displaypresentation.common.render;

import com.kiwiredstone.displaypresentation.common.Constants;
import com.kiwiredstone.displaypresentation.common.geometry.SlideFrame;
import com.kiwiredstone.displaypresentation.common.model.TextElementDefinition;
import com.mojang.math.Transformation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

/**
 * Spawns and configures the vanilla {@code Display} entities that make up a live slideshow, and
 * provides the discovery/cleanup helpers used to tear a presentation down.
 *
 * <p>All entities are tagged ({@link Constants#ROOT_TAG} plus a per-show tag) so they can be found
 * again later — most importantly to remove leftovers after an unexpected server shutdown. The NBT
 * keys written here mirror {@code Display}/{@code TextDisplay} save data exactly.
 */
public final class DisplayEntities {
    /** Per-element scoreboard tag prefix, lets us match a single element across reloads. */
    public static final String ELEMENT_TAG_PREFIX = "dp_el.";

    // --- Text-display background geometry, derived from the vanilla text renderer ---
    // The renderer scales text by 0.025 blocks/px and draws the background quad over px
    // x in [-1, m] and y in [-1, n], where m = max line width and n = lines * 10 - 1.
    // For a single space (m = 4px, one line => n = 9) at transformation-scale 1 the quad is:
    //   width  = 0.025 * (m + 1) = 0.125 blocks,   height = 0.025 * (n + 1) = 0.25 blocks
    //   centre = (0.0125, 0.125) blocks from the transform translation (anchor is horizontally
    //   near-centre and sits at the bottom of the text block).
    // We use these to stretch a single-space background into a rectangle that exactly fills a frame.
    public static final double TEXT_SCALE = 0.025;
    private static final int BG_SPACE_WIDTH_PX = 4;
    public static final double BG_WIDTH_AT_SCALE1 = TEXT_SCALE * (BG_SPACE_WIDTH_PX + 1);
    public static final double BG_HEIGHT_AT_SCALE1 = TEXT_SCALE * (9 + 1);
    public static final double BG_CENTER_X_AT_SCALE1 = TEXT_SCALE * 0.5;
    public static final double BG_CENTER_Y_AT_SCALE1 = BG_HEIGHT_AT_SCALE1 / 2.0;

    private DisplayEntities() {
    }

    public static Display.TextDisplay spawnText(ServerLevel level, SlideFrame frame, String showName, String elementId) {
        return spawnTextAt(level, frame, frame.center(), showName, elementId);
    }

    /**
     * Spawns a text display oriented to the frame but at an explicit world position. Used by the
     * background, which sits slightly behind the slide centre along the plane normal.
     */
    public static Display.TextDisplay spawnTextAt(ServerLevel level, SlideFrame frame, Vec3 pos,
                                                  String showName, String elementId) {
        Display.TextDisplay entity = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
        entity.setPos(pos.x, pos.y, pos.z);
        entity.setYRot(frame.yaw());
        entity.setXRot(frame.pitch());
        entity.setNoGravity(true);
        entity.addTag(Constants.ROOT_TAG);
        entity.addTag(Constants.showTag(showName));
        entity.addTag(ELEMENT_TAG_PREFIX + elementId);
        level.addFreshEntity(entity);
        return entity;
    }

    /**
     * Pushes the full text configuration and transform onto a text display. Opacity is passed
     * separately from the definition so it can be animated.
     */
    public static void applyText(Display.TextDisplay entity, TextElementDefinition def, Component text,
                                 int opacity, ElementTransform transform, int interpolationTicks) {
        Transformation transformation = transform.toTransformation();
        EntityNbt.edit(entity, out -> {
            out.store(Display.TAG_TRANSFORMATION, Transformation.EXTENDED_CODEC, transformation);
            out.putInt(Display.TAG_TRANSFORMATION_INTERPOLATION_DURATION, interpolationTicks);
            out.putInt(Display.TAG_TRANSFORMATION_START_INTERPOLATION, 0);

            out.store(Display.TextDisplay.TAG_TEXT, ComponentSerialization.CODEC, text);
            out.putInt("line_width", def.lineWidth);
            out.putInt("background", def.backgroundColor);
            out.putByte("text_opacity", (byte) opacity);
            out.putBoolean("shadow", def.shadow);
            out.putBoolean("see_through", def.seeThrough);
            out.putBoolean("default_background", def.defaultBackground);
            out.store("alignment", Display.TextDisplay.Align.CODEC, alignOf(def.textAlign));
        });
    }

    /**
     * Configures a text display as a solid translucent rectangle: a single (invisible) space whose
     * background quad is stretched by the transform to fill the frame, coloured by {@code argb}.
     * {@code cullSize} enlarges the culling box so the stretched quad is not culled when the anchor
     * block leaves the screen.
     */
    public static void applyBackgroundRect(Display.TextDisplay entity, int argb, double cullSize,
                                           ElementTransform transform, int interpolationTicks) {
        Transformation transformation = transform.toTransformation();
        EntityNbt.edit(entity, out -> {
            out.store(Display.TAG_TRANSFORMATION, Transformation.EXTENDED_CODEC, transformation);
            out.putInt(Display.TAG_TRANSFORMATION_INTERPOLATION_DURATION, interpolationTicks);
            out.putInt(Display.TAG_TRANSFORMATION_START_INTERPOLATION, 0);

            out.store(Display.TextDisplay.TAG_TEXT, ComponentSerialization.CODEC, Component.literal(" "));
            out.putInt("line_width", 1000);
            out.putInt("background", argb);
            out.putByte("text_opacity", (byte) 0);
            out.putBoolean("shadow", false);
            out.putBoolean("see_through", false);
            out.putBoolean("default_background", false);
            out.store("alignment", Display.TextDisplay.Align.CODEC, Display.TextDisplay.Align.CENTER);

            out.putFloat(Display.TAG_WIDTH, (float) cullSize);
            out.putFloat(Display.TAG_HEIGHT, (float) cullSize);
        });
    }

    /** Pushes only a transform (with interpolation) onto any display entity. */
    public static void applyTransform(Display entity, ElementTransform transform, int interpolationTicks) {
        Transformation transformation = transform.toTransformation();
        EntityNbt.edit(entity, out -> {
            out.store(Display.TAG_TRANSFORMATION, Transformation.EXTENDED_CODEC, transformation);
            out.putInt(Display.TAG_TRANSFORMATION_INTERPOLATION_DURATION, interpolationTicks);
            out.putInt(Display.TAG_TRANSFORMATION_START_INTERPOLATION, 0);
        });
    }

    public static Display.TextDisplay.Align alignOf(String value) {
        if (value == null) {
            return Display.TextDisplay.Align.CENTER;
        }
        return switch (value.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "LEFT" -> Display.TextDisplay.Align.LEFT;
            case "RIGHT" -> Display.TextDisplay.Align.RIGHT;
            default -> Display.TextDisplay.Align.CENTER;
        };
    }

    /** All loaded display entities belonging to the given slideshow. */
    public static List<Display> findByShow(ServerLevel level, String showName) {
        String showTag = Constants.showTag(showName);
        Predicate<Display> predicate = e -> e.getTags().contains(showTag);
        return List.copyOf(level.getEntities(EntityTypeTest.forClass(Display.class), predicate));
    }

    /** All loaded display entities spawned by this mod, regardless of slideshow. */
    public static List<Display> findAll(ServerLevel level) {
        Predicate<Display> predicate = e -> e.getTags().contains(Constants.ROOT_TAG);
        return List.copyOf(level.getEntities(EntityTypeTest.forClass(Display.class), predicate));
    }

    /** Removes every loaded display entity belonging to the slideshow; returns how many were removed. */
    public static int removeShow(ServerLevel level, String showName) {
        int count = 0;
        for (Display display : findByShow(level, showName)) {
            display.discard();
            count++;
        }
        return count;
    }

    public static void discard(Entity entity) {
        entity.discard();
    }
}
