package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ClearConfirmPayload(String nodeId, String targetName, String parentName, int removedCount) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClearConfirmPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("idlecraft", "clear_confirm"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClearConfirmPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ClearConfirmPayload::nodeId,
                    ByteBufCodecs.STRING_UTF8, ClearConfirmPayload::targetName,
                    ByteBufCodecs.STRING_UTF8, ClearConfirmPayload::parentName,
                    ByteBufCodecs.VAR_INT, ClearConfirmPayload::removedCount,
                    ClearConfirmPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
