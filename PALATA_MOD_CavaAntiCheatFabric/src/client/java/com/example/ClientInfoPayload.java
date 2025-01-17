package com.example;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;

import java.io.IOException;

public record ClientInfoPayload(byte[] data) implements net.minecraft.network.packet.CustomPayload {
    public static final net.minecraft.network.packet.CustomPayload.Id<ClientInfoPayload> ID =
            new net.minecraft.network.packet.CustomPayload.Id<>(Identifier.of("butterdevelop:anticheat"));

    public static final PacketCodec<RegistryByteBuf, ClientInfoPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.BYTE_ARRAY, ClientInfoPayload::data, ClientInfoPayload::new);

    @Override
    public net.minecraft.network.packet.CustomPayload.Id<? extends net.minecraft.network.packet.CustomPayload> getId() {
        return ID;
    }

    public static ClientInfoPayload fromPacket(ClientInfoPacket packet) throws IOException {
        return new ClientInfoPayload(SerializationUtils.serialize(packet));
    }

    public ClientInfoPacket toClientInfoPacket() throws IOException, ClassNotFoundException {
        return (ClientInfoPacket) SerializationUtils.deserialize(data);
    }
}
