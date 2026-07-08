package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record NodesSyncPayload(List<String> nodes) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NodesSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("idlecraft", "nodes_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NodesSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, value) -> {
                        buf.writeVarInt(value.nodes().size());
                        for (String s : value.nodes()) {
                            buf.writeUtf(s);
                        }
                    },
                    (buf) -> {
                        int size = buf.readVarInt();
                        List<String> list = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            list.add(buf.readUtf());
                        }
                        return new NodesSyncPayload(list);
                    }
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}