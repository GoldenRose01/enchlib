package goldenrose01.enchlib.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

data class WorldConfigSyncPayload(val available: String, val details: String, val mobs: String) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<WorldConfigSyncPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<WorldConfigSyncPayload>(Identifier.parse("enchlib:world_config_sync"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, WorldConfigSyncPayload> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, WorldConfigSyncPayload::available,
            ByteBufCodecs.STRING_UTF8, WorldConfigSyncPayload::details,
            ByteBufCodecs.STRING_UTF8, WorldConfigSyncPayload::mobs,
            ::WorldConfigSyncPayload
        )
    }
}

data class WorldConfigUpdatePayload(val available: String, val details: String) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<WorldConfigUpdatePayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<WorldConfigUpdatePayload>(Identifier.parse("enchlib:world_config_update"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, WorldConfigUpdatePayload> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, WorldConfigUpdatePayload::available,
            ByteBufCodecs.STRING_UTF8, WorldConfigUpdatePayload::details,
            ::WorldConfigUpdatePayload
        )
    }
}
