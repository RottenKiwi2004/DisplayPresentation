package com.kiwiredstone.displaypresentation.common.render;

import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The resolved, slide-local transform that is pushed onto a display entity's
 * {@code transformation} NBT.
 *
 * <p>The display entity is spawned at the slide centre and oriented to the frame, so a translation of
 * {@code (centerX, centerY, depth)} here is interpreted in slide space: {@code centerX} runs along the
 * frame's right axis, {@code centerY} along its up axis, and {@code depth} along the outward normal
 * (used to nudge overlapping elements apart and avoid z-fighting).
 */
public final class ElementTransform {
    public double centerX;
    public double centerY;
    public double depth;
    public double rotationDeg;
    public double scaleX = 1.0;
    public double scaleY = 1.0;

    public Transformation toTransformation() {
        Vector3f translation = new Vector3f((float) centerX, (float) centerY, (float) depth);
        Quaternionf leftRotation = new Quaternionf().rotationZ((float) Math.toRadians(rotationDeg));
        Vector3f scale = new Vector3f((float) scaleX, (float) scaleY, 1.0f);
        Quaternionf rightRotation = new Quaternionf();
        return new Transformation(translation, leftRotation, scale, rightRotation);
    }
}
