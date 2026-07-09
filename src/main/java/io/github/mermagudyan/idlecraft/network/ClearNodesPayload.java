package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ClearNodesPayload(String nodeId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClearNodesPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("idlecraft", "clear_nodes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClearNodesPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ClearNodesPayload::nodeId, ClearNodesPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
