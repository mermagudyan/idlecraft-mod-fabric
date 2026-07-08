package io.github.mermagudyan.idlecraft.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Block.class)
public class SeedDropMixin {

    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private static void filterSeeds(BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity, Entity entity, ItemInstance tool, CallbackInfoReturnable<List<ItemStack>> cir) {
        List<ItemStack> original = cir.getReturnValue();
        if (original.isEmpty()) return;

        boolean isHoe = tool.is(ItemTags.HOES);
        if (isHoe) return;

        List<ItemStack> filtered = new ArrayList<>();
        for (ItemStack stack : original) {
            if (stack.getItem() != Items.WHEAT_SEEDS
                    && stack.getItem() != Items.BEETROOT_SEEDS
                    && stack.getItem() != Items.MELON_SEEDS
                    && stack.getItem() != Items.PUMPKIN_SEEDS
                    && stack.getItem() != Items.TORCHFLOWER_SEEDS
                    && stack.getItem() != Items.PITCHER_POD) {
                filtered.add(stack);
            }
        }
        cir.setReturnValue(filtered);
    }
}