package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public record SacrificeStatePayload(Map<String, int[]> progress) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SacrificeStatePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("idlecraft", "sacrifice_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SacrificeStatePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, value) -> {
                        buf.writeVarInt(value.progress().size());
                        for (var entry : value.progress().entrySet()) {
                            buf.writeUtf(entry.getKey());
                            buf.writeVarInt(entry.getValue().length);
                            for (int v : entry.getValue()) buf.writeVarInt(v);
                        }
                    },
                    (buf) -> {
                        int size = buf.readVarInt();
                        Map<String, int[]> map = new HashMap<>();
                        for (int i = 0; i < size; i++) {
                            String key = buf.readUtf();
                            int len = buf.readVarInt();
                            int[] arr = new int[len];
                            for (int j = 0; j < len; j++) arr[j] = buf.readVarInt();
                            map.put(key, arr);
                        }
                        return new SacrificeStatePayload(map);
                    }
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}