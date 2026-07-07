package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ResetRewardedPayload() implements CustomPayload {
    public static final Id<ResetRewardedPayload> ID =
            new Id<>(Identifier.of("idlecraft", "reset_rewarded"));
    public static final PacketCodec<RegistryByteBuf, ResetRewardedPayload> CODEC =
            PacketCodec.unit(new ResetRewardedPayload());

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}