package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ResetRewardedPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ResetRewardedPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("idlecraft", "reset_rewarded"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResetRewardedPayload> STREAM_CODEC =
            StreamCodec.unit(new ResetRewardedPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}