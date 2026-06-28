package com.kiwiredstone.displaypresentation.common.runtime;

import com.kiwiredstone.displaypresentation.common.geometry.SlideFrame;
import com.kiwiredstone.displaypresentation.common.model.AnimationDefinition;
import com.kiwiredstone.displaypresentation.common.model.ElementDefinition;
import com.kiwiredstone.displaypresentation.common.model.TextElementDefinition;
import com.kiwiredstone.displaypresentation.common.render.DisplayEntities;
import com.kiwiredstone.displaypresentation.common.render.ElementTransform;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** A text element backed by a vanilla {@code TextDisplay} entity. */
public final class TextElement extends BaseElement {
    private int curOpacity;

    public TextElement(ServerLevel level, String showName, String elementId, TextElementDefinition def,
                       SlideFrame frame, ParentTransform parent, double depth) {
        super(level, showName, elementId, def, frame, parent, depth);
        this.curOpacity = def.textOpacity;
    }

    private TextElementDefinition textDef() {
        return (TextElementDefinition) def;
    }

    @Override
    public void spawn() {
        this.entity = DisplayEntities.spawnText(level, frame, showName, elementId);
        applyStep(0, false);
    }

    @Override
    protected void resetState() {
        super.resetState();
        this.curOpacity = textDef().textOpacity;
    }

    @Override
    protected void applyOpacity(AnimationDefinition anim) {
        if (anim.opacity != null) {
            this.curOpacity = anim.opacity;
        }
    }

    @Override
    protected void onMorph(ElementDefinition nextDef) {
        this.curOpacity = ((TextElementDefinition) nextDef).textOpacity;
    }

    @Override
    protected void pushToEntity(ElementTransform transform, int interpolationTicks) {
        if (entity == null) {
            return;
        }
        Component text = TextComponents.build(textDef());
        DisplayEntities.applyText(
                (net.minecraft.world.entity.Display.TextDisplay) entity,
                textDef(), text, curOpacity, transform, interpolationTicks);
    }
}
