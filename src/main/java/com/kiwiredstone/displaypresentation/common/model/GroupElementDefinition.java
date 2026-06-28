package com.kiwiredstone.displaypresentation.common.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A group of child elements (mirrors a PowerPoint group shape). A group has no visual of its own;
 * its {@code anchor}, {@code rotation} and {@code scale} form a local coordinate frame that its
 * children are placed within. Groups may nest arbitrarily. At load time the runtime flattens groups
 * into their leaf elements, composing each child's transform with the group's transform.
 */
public final class GroupElementDefinition extends ElementDefinition {
    public List<ElementDefinition> children = new ArrayList<>();

    @Override
    public ElementType elementType() {
        return ElementType.GROUP;
    }

    @Override
    public int maxStep() {
        int max = super.maxStep();
        for (ElementDefinition child : children) {
            max = Math.max(max, child.maxStep());
        }
        return max;
    }
}
