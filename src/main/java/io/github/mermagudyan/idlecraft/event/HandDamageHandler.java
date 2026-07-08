package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.mixin.InteractionManagerAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HandDamageHandler {

    private static final Map<UUID, Integer> miningTicks = new HashMap<>();
    private static final float HARDNESS_THRESHOLD = 1.5f;
    private static final int DAMAGE_INTERVAL = 10;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                checkHandBreaking(player);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            miningTicks.remove(handler.getPlayer().getUuid());
        });
    }

    private static void checkHandBreaking(ServerPlayerEntity player) {
        if (player.isCreative() || player.isSpectator()) {
            miningTicks.remove(player.getUuid());
            return;
        }

        ItemStack mainHand = player.getMainHandStack();
        if (StickToolHandler.isToolOrStick(mainHand)) {
            miningTicks.remove(player.getUuid());
            return;
        }

        InteractionManagerAccessor accessor = (InteractionManagerAccessor) player.interactionManager;
        if (!accessor.isMining()) {
            miningTicks.remove(player.getUuid());
            return;
        }

        BlockPos miningPos = accessor.getMiningPos();
        if (miningPos == null) return;

        ServerWorld world = (ServerWorld) player.getEntityWorld();
        BlockState state = world.getBlockState(miningPos);
        float hardness = state.getHardness(world, miningPos);

        if (hardness >= HARDNESS_THRESHOLD) {
            UUID id = player.getUuid();
            int ticks = miningTicks.getOrDefault(id, 0) + 1;
            miningTicks.put(id, ticks);

            if (ticks >= DAMAGE_INTERVAL) {
                player.damage(world, world.getDamageSources().generic(), 2.0f);
                miningTicks.put(id, 0);
            }
        } else {
            miningTicks.remove(player.getUuid());
        }
    }
}