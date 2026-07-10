package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RepairStatePayload(String nodeId, long startMs, boolean succeeded) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RepairStatePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("idlecraft", "repair_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RepairStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, RepairStatePayload::nodeId,
                    ByteBufCodecs.VAR_LONG, RepairStatePayload::startMs,
                    ByteBufCodecs.BOOL, RepairStatePayload::succeeded,
                    RepairStatePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
