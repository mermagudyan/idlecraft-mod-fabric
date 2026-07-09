package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

public record StructureRegionPayload(List<BoundingBox> boxes) implements CustomPacketPayload {
    public static final Type<StructureRegionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("idlecraft", "structure_region"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureRegionPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, value) -> {
                        buf.writeVarInt(value.boxes().size());
                        for (BoundingBox box : value.boxes()) {
                            BoundingBox.STREAM_CODEC.encode(buf, box);
                        }
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        List<BoundingBox> list = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            list.add(BoundingBox.STREAM_CODEC.decode(buf));
                        }
                        return new StructureRegionPayload(list);
                    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
