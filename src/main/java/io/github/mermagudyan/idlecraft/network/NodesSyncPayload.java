package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record NodesSyncPayload(List<String> nodes) implements CustomPayload {
    public static final Id<NodesSyncPayload> ID =
            new Id<>(Identifier.of("idlecraft", "nodes_sync"));
    public static final PacketCodec<RegistryByteBuf, NodesSyncPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING.collect(PacketCodecs.toList()),
                    NodesSyncPayload::nodes, NodesSyncPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}