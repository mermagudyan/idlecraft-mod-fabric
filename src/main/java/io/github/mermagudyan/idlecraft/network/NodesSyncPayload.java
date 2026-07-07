package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record NodesSyncPayload(List<String> nodes) implements CustomPayload {
    public static final Id<NodesSyncPayload> ID =
            new Id<>(Identifier.of("idlecraft", "nodes_sync"));

    public static final PacketCodec<RegistryByteBuf, NodesSyncPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeVarInt(value.nodes().size());
                        for (String s : value.nodes()) {
                            buf.writeString(s);
                        }
                    },
                    (buf) -> {
                        int size = buf.readVarInt();
                        List<String> list = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            list.add(buf.readString());
                        }
                        return new NodesSyncPayload(list);
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}