package io.github.mermagudyan.idlecraft.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Block.class)
public class SeedDropMixin {

    @Inject(method = "getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private static void filterSeeds(BlockState state, ServerWorld world, BlockPos pos, BlockEntity blockEntity, Entity entity, ItemStack tool, CallbackInfoReturnable<List<ItemStack>> cir) {
        List<ItemStack> original = cir.getReturnValue();
        if (original.isEmpty()) return;

        boolean isHoe = tool.isIn(ItemTags.HOES);
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