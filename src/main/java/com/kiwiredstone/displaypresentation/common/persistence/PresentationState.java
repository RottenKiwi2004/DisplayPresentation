package com.kiwiredstone.displaypresentation.common.persistence;

import com.kiwiredstone.displaypresentation.common.geometry.SlideFrame;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persistent record of one active presentation, written to {@code world/data/Slides_<name>.dat}.
 *
 * <p>It exists so a presentation can be cleaned up even if the server is shut down (or crashes) in
 * the middle of it: the {@code active} flag plus the stored frame, dimension and spawned entity ids
 * give the recovery pass on next start everything it needs to remove the leftover display entities.
 *
 * <p>This is a vanilla {@link SavedData}; the file is an ordinary {@code .dat} a vanilla server would
 * simply ignore, so worlds remain loadable without this mod.
 */
public final class PresentationState extends SavedData {
    public static final Codec<PresentationState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(s -> s.name),
            Codec.STRING.optionalFieldOf("dimension", "minecraft:overworld").forGetter(s -> s.dimension),
            SlideFrame.CODEC.optionalFieldOf("frame").forGetter(s -> java.util.Optional.ofNullable(s.frame)),
            Codec.INT.optionalFieldOf("slideIndex", 0).forGetter(s -> s.slideIndex),
            Codec.INT.optionalFieldOf("stepIndex", 0).forGetter(s -> s.stepIndex),
            Codec.BOOL.optionalFieldOf("active", false).forGetter(s -> s.active),
            UUIDUtil.CODEC.listOf().optionalFieldOf("entities", List.of()).forGetter(s -> s.entities)
    ).apply(instance, PresentationState::fromCodec));

    public String name = "";
    public String dimension = "minecraft:overworld";
    public SlideFrame frame;
    public int slideIndex = 0;
    public int stepIndex = 0;
    public boolean active = false;
    public List<UUID> entities = new ArrayList<>();

    public PresentationState() {
    }

    public PresentationState(String name) {
        this.name = name;
    }

    private static PresentationState fromCodec(String name, String dimension, java.util.Optional<SlideFrame> frame,
                                               int slideIndex, int stepIndex, boolean active, List<UUID> entities) {
        PresentationState state = new PresentationState(name);
        state.dimension = dimension;
        state.frame = frame.orElse(null);
        state.slideIndex = slideIndex;
        state.stepIndex = stepIndex;
        state.active = active;
        state.entities = new ArrayList<>(entities);
        return state;
    }
}
