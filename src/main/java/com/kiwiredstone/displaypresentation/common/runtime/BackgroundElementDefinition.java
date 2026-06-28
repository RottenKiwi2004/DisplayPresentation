package com.kiwiredstone.displaypresentation.common.runtime;

import com.kiwiredstone.displaypresentation.common.geometry.Alignment;
import com.kiwiredstone.displaypresentation.common.model.ElementDefinition;
import com.kiwiredstone.displaypresentation.common.model.ElementType;
import com.kiwiredstone.displaypresentation.common.model.Vec2;

/**
 * Synthetic element definition for a slide background. It is never read from or written to JSON;
 * it is built from the slide's {@link com.kiwiredstone.displaypresentation.common.model.SlideBackground}
 * so the background can flow through the same spawn / morph / cleanup machinery as real elements.
 *
 * <p>It reports {@link ElementType#TEXT} because it is backed by a text-display entity, which lets it
 * morph against the previous slide's background (same reserved id, same kind).
 */
public final class BackgroundElementDefinition extends ElementDefinition {
    public final int argb;

    public BackgroundElementDefinition(int argb) {
        this.argb = argb;
        this.id = BackgroundElement.ID;
        this.anchor = new Vec2(0.0, 0.0);
        this.alignment = Alignment.CENTER;
        this.size = new Vec2(0.0, 0.0);
    }

    @Override
    public ElementType elementType() {
        return ElementType.TEXT;
    }
}
