package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record QualitySelectPayload(int quality) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<QualitySelectPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("idlecraft", "quality_select"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QualitySelectPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.INT, QualitySelectPayload::quality, QualitySelectPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
