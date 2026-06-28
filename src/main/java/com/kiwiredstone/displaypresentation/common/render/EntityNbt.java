package com.kiwiredstone.displaypresentation.common.render;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.Consumer;

/**
 * Edits an entity's persisted state through public, codec-based NBT I/O.
 *
 * <p>Vanilla {@code Display} entities expose no public setters for their transformation, text, etc.,
 * so we configure them the same way the game saves and loads them: write the entity's full current
 * state to a {@link ValueOutput}, overlay the fields we want to change, then load it back. Because we
 * round-trip the <em>complete</em> state, position, rotation, UUID and scoreboard tags are preserved.
 *
 * <p>This class is the single place that depends on the 1.21.11 {@code ValueInput}/{@code ValueOutput}
 * shape; if a future Minecraft version changes the NBT contract, only this file (and the keys used by
 * {@link DisplayEntities}) need updating.
 */
public final class EntityNbt {
    private EntityNbt() {
    }

    public static void edit(Entity entity, Consumer<ValueOutput> editor) {
        HolderLookup.Provider registries = entity.level().registryAccess();
        ProblemReporter reporter = ProblemReporter.DISCARDING;

        TagValueOutput out = TagValueOutput.createWithContext(reporter, registries);
        entity.saveWithoutId(out);
        editor.accept(out);
        CompoundTag tag = out.buildResult();

        ValueInput in = TagValueInput.create(reporter, registries, tag);
        entity.load(in);
    }
}
