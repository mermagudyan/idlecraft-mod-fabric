package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.network.StructureRegionPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(PlayerChunkSender.class)
public abstract class PlayerChunkSenderMixin {

    @Inject(method = "sendChunk", at = @At("RETURN"))
    private static void idlecraft$sendStructureRegions(ServerGamePacketListenerImpl connection, ServerLevel level, LevelChunk chunk, CallbackInfo ci) {
        ServerPlayer player = connection.player;
        if (player == null || player.isCreative() || player.isSpectator()) return;

        ChunkPos cpos = chunk.getPos();
        BlockPos center = new BlockPos(cpos.x() * 16 + 8, 0, cpos.z() * 16 + 8);

        List<net.minecraft.world.level.levelgen.structure.BoundingBox> boxes = new ArrayList<>();
        for (var entry : level.structureManager().getAllStructuresAt(center).entrySet()) {
            StructureStart start = level.structureManager().getStructureAt(center, entry.getKey());
            if (start.isValid()) {
                boxes.add(start.getBoundingBox());
            }
        }

        if (!boxes.isEmpty()) {
            ServerPlayNetworking.send(player, new StructureRegionPayload(boxes));
        }
    }
}
