package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public record ConditionProgressPayload(Map<String, Integer> progress) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConditionProgressPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("idlecraft", "condition_progress"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConditionProgressPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, value) -> {
                        buf.writeVarInt(value.progress().size());
                        for (Map.Entry<String, Integer> e : value.progress().entrySet()) {
                            buf.writeUtf(e.getKey());
                            buf.writeVarInt(e.getValue());
                        }
                    },
                    (buf) -> {
                        int size = buf.readVarInt();
                        Map<String, Integer> map = new HashMap<>();
                        for (int i = 0; i < size; i++) {
                            map.put(buf.readUtf(), buf.readVarInt());
                        }
                        return new ConditionProgressPayload(map);
                    }
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}