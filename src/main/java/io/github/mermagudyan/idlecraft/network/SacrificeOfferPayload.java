package io.github.mermagudyan.idlecraft.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.network.codec.PacketCodecs;

public record SacrificeOfferPayload(String nodeId) implements CustomPayload {
    public static final Id<SacrificeOfferPayload> ID =
            new Id<>(Identifier.of("idlecraft", "sacrifice_offer"));
    public static final PacketCodec<RegistryByteBuf, SacrificeOfferPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, SacrificeOfferPayload::nodeId, SacrificeOfferPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}