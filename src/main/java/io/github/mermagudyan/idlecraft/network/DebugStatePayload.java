package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DebugStatePayload(boolean debug) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DebugStatePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("idlecraft", "debug_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DebugStatePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, DebugStatePayload::debug, DebugStatePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
