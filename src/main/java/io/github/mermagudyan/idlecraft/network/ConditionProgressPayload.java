package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public record ConditionProgressPayload(Map<String, Integer> progress) implements CustomPayload {
    public static final Id<ConditionProgressPayload> ID =
            new Id<>(Identifier.of("idlecraft", "condition_progress"));

    public static final PacketCodec<RegistryByteBuf, ConditionProgressPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeVarInt(value.progress().size());
                        for (Map.Entry<String, Integer> e : value.progress().entrySet()) {
                            buf.writeString(e.getKey());
                            buf.writeVarInt(e.getValue());
                        }
                    },
                    (buf) -> {
                        int size = buf.readVarInt();
                        Map<String, Integer> map = new HashMap<>();
                        for (int i = 0; i < size; i++) {
                            map.put(buf.readString(), buf.readVarInt());
                        }
                        return new ConditionProgressPayload(map);
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}