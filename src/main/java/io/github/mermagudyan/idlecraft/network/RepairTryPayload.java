package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RepairTryPayload(String nodeId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RepairTryPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("idlecraft", "repair_try"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RepairTryPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RepairTryPayload::nodeId, RepairTryPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
