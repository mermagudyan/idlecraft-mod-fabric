package io.github.mermagudyan.idlecraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StructureBlockedPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<StructureBlockedPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("idlecraft", "structure_blocked"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureBlockedPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, StructureBlockedPayload::pos, StructureBlockedPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
