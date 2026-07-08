package io.github.mermagudyan.idlecraft.mixin;

import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayerInteractionManager.class)
public interface InteractionManagerAccessor {

    @Accessor("mining")
    boolean isMining();

    @Accessor("miningPos")
    BlockPos getMiningPos();
}