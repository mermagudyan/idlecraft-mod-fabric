package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record NodePurchasePayload(String nodeId) implements CustomPayload {
    public static final Id<NodePurchasePayload> ID =
            new Id<>(Identifier.of("idlecraft", "node_purchase"));
    public static final PacketCodec<RegistryByteBuf, NodePurchasePayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, NodePurchasePayload::nodeId, NodePurchasePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}