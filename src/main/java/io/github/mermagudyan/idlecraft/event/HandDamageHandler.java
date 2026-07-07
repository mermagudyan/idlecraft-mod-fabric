package io.github.mermagudyan.idlecraft.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HandDamageHandler {

    // UUID -> ticks mining hard block
    private static final Map<UUID, Integer> miningTicks = new HashMap<>();
    private static final float HARDNESS_THRESHOLD = 1.5f;
    private static final int DAMAGE_INTERVAL = 10;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                checkHandBreaking(player);
            }
        });
    }

    private static void checkHandBreaking(ServerPlayerEntity player) {
        if (player.isCreative() || player.isSpectator()) {
            miningTicks.remove(player.getUuid());
            return;
        }

        ItemStack mainHand = player.getMainHandStack();
        boolean isTool = StickToolHandler.isToolOrStick(mainHand);
        if (isTool) {
            miningTicks.remove(player.getUuid());
            return;
        }
        try {
            var im = player.interactionManager;
            // Доступ к приватному полю через reflection (надёжнее accessor)
            var field = im.getClass().getDeclaredField("mining");
            field.setAccessible(true);
            boolean isMining = field.getBoolean(im);

            if (!isMining) {
                miningTicks.remove(player.getUuid());
                return;
            }

            var posField = im.getClass().getDeclaredField("miningPos");
            posField.setAccessible(true);
            BlockPos miningPos = (BlockPos) posField.get(im);

            if (miningPos == null) return;

            ServerWorld world = (ServerWorld) player.getEntityWorld();
            BlockState state = world.getBlockState(miningPos);
            float hardness = state.getHardness(world, miningPos);

            if (hardness >= HARDNESS_THRESHOLD) {
                UUID id = player.getUuid();
                int ticks = miningTicks.getOrDefault(id, 0) + 1;
                miningTicks.put(id, ticks);

                if (ticks >= DAMAGE_INTERVAL) {
                    player.damage(world, world.getDamageSources().generic(), 1.0f);
                    miningTicks.put(id, 0);
                }
            } else {
                miningTicks.remove(player.getUuid());
            }
        } catch (Exception e) {
            // Skip
        }
    }
}