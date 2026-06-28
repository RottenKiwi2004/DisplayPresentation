package com.kiwiredstone.displaypresentation.common.geometry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The invisible plane a slideshow lives on (design point 1, "BaseSlide").
 *
 * <p>The frame is purely geometric data: a centre position in the world, a facing direction
 * (yaw + pitch) and the half-extents {@code w} and {@code h} that define the aspect ratio. It does
 * <em>not</em> own any entity. Spawning the actual {@code Display} entities is the job of the
 * runtime layer; the frame only answers questions such as "where in the world is slide coordinate
 * (x, y)?" and "is this point inside the slide?".
 *
 * <p>Coordinate system: the origin {@code (0, 0)} is the centre of the slide. {@code +x} runs along
 * {@link #right()} and {@code +y} along {@link #up()}. Therefore the top-left corner is
 * {@code (-w, h)} and the bottom-right corner is {@code (w, -h)}.
 */
public record SlideFrame(Vec3 center, float yaw, float pitch, float w, float h) {

    public static final Codec<SlideFrame> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Vec3.CODEC.fieldOf("center").forGetter(SlideFrame::center),
            Codec.FLOAT.fieldOf("yaw").forGetter(SlideFrame::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(SlideFrame::pitch),
            Codec.FLOAT.fieldOf("w").forGetter(SlideFrame::w),
            Codec.FLOAT.fieldOf("h").forGetter(SlideFrame::h)
    ).apply(instance, SlideFrame::new));

    /**
     * Returns a copy of this frame with a new aspect ratio while retaining the centre position and
     * orientation. Used by the slide loop when moving to a slide that overrides the aspect ratio.
     */
    public SlideFrame withAspect(float newW, float newH) {
        return new SlideFrame(center, yaw, pitch, newW, newH);
    }

    /**
     * Orientation of the slide as a quaternion. Local axes map as: {@code +X -> right},
     * {@code +Y -> up}, {@code +Z -> normal} (out of the slide face). This is also the rotation a
     * {@code Display} entity should be given so that a transformation translation of {@code (x, y, 0)}
     * lands the content at slide coordinate {@code (x, y)}.
     */
    public Quaternionf orientation() {
        return new Quaternionf().rotationYXZ(
                (float) Math.toRadians(-yaw),
                (float) Math.toRadians(pitch),
                0.0f);
    }

    public Vector3f right() {
        return orientation().transform(new Vector3f(1.0f, 0.0f, 0.0f));
    }

    public Vector3f up() {
        return orientation().transform(new Vector3f(0.0f, 1.0f, 0.0f));
    }

    public Vector3f normal() {
        return orientation().transform(new Vector3f(0.0f, 0.0f, 1.0f));
    }

    /** Converts a slide-local coordinate to a world position. */
    public Vec3 toWorld(double x, double y) {
        Vector3f r = right();
        Vector3f u = up();
        return new Vec3(
                center.x + r.x * x + u.x * y,
                center.y + r.y * x + u.y * y,
                center.z + r.z * x + u.z * y);
    }

    /** Whether a slide-local coordinate lies within the frame bounds. */
    public boolean contains(double x, double y) {
        return Math.abs(x) <= w && Math.abs(y) <= h;
    }
}
