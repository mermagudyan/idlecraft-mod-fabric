package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public record SacrificeStatePayload(Map<String, int[]> progress) implements CustomPayload {
    public static final Id<SacrificeStatePayload> ID =
            new Id<>(Identifier.of("idlecraft", "sacrifice_state"));

    public static final PacketCodec<RegistryByteBuf, SacrificeStatePayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeVarInt(value.progress().size());
                        for (var entry : value.progress().entrySet()) {
                            buf.writeString(entry.getKey());
                            buf.writeVarInt(entry.getValue().length);
                            for (int v : entry.getValue()) buf.writeVarInt(v);
                        }
                    },
                    (buf) -> {
                        int size = buf.readVarInt();
                        Map<String, int[]> map = new HashMap<>();
                        for (int i = 0; i < size; i++) {
                            String key = buf.readString();
                            int len = buf.readVarInt();
                            int[] arr = new int[len];
                            for (int j = 0; j < len; j++) arr[j] = buf.readVarInt();
                            map.put(key, arr);
                        }
                        return new SacrificeStatePayload(map);
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}