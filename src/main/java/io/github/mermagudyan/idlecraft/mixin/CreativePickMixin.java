package io.github.mermagudyan.idlecraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.mermagudyan.idlecraft.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CreativePickMixin {

    
    
    @ModifyVariable(method = "handlePickItemFromBlock",
            at = @At(value = "STORE", ordinal = 0),
            require = 1)
    private ItemStack idlecraft$replace(ItemStack stack, @Local BlockState blockState) {
        Item modItem = idlecraft$modItem(blockState.getBlock());
        if (modItem != null) {
            return new ItemStack(modItem, stack.getCount());
        }
        return stack;
    }

    private static Item idlecraft$modItem(Block block) {
        if (block instanceof CraftingTableBlock) return ModBlocks.CRAFTING_TABLE.asItem();
        if (block instanceof FurnaceBlock) return ModBlocks.FURNACE.asItem();
        if (block instanceof BlastFurnaceBlock) return ModBlocks.BLAST_FURNACE.asItem();
        if (block instanceof SmokerBlock) return ModBlocks.SMOKER.asItem();
        if (block instanceof AnvilBlock) return ModBlocks.ANVIL.asItem();
        if (block instanceof EnchantingTableBlock) return ModBlocks.ENCHANTING_TABLE.asItem();
        if (block instanceof BrewingStandBlock) return ModBlocks.BREWING_STAND.asItem();
        if (block instanceof SmithingTableBlock) return ModBlocks.SMITHING_TABLE.asItem();
        if (block instanceof GrindstoneBlock) return ModBlocks.GRINDSTONE.asItem();
        if (block instanceof HopperBlock) return ModBlocks.HOPPER.asItem();
        if (block instanceof CauldronBlock) return ModBlocks.CAULDRON.asItem();
        return null;
    }
}
