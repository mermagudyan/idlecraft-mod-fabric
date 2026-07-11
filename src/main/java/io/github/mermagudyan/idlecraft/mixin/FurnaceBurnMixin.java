package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.common.IdlecraftWorkstation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceBurnMixin {

    @Inject(method = "getBurnDuration", at = @At("RETURN"), cancellable = true)
    private void idlecraft$scaleBurn(FuelValues fuelValues, ItemStack fuel, CallbackInfoReturnable<Integer> cir) {
        net.minecraft.world.level.Level level =
                ((BlockEntity) (Object) this).getLevel();
        
        if (((BlockEntity) (Object) this).getBlockState().getBlock() instanceof IdlecraftWorkstation) {
            return;
        }
        if (!io.github.mermagudyan.idlecraft.event.FurnaceFuelRestriction.isFuelAllowedForBlockEntity(
                (AbstractFurnaceBlockEntity) (Object) this, level, fuel)) {
            cir.setReturnValue(0);
            return;
        }
        int original = cir.getReturnValue();
        double ops = io.github.mermagudyan.idlecraft.event.FurnaceFuelRestriction.getOperations(fuel);
        int base = ops > 0 ? (int) (ops * 200.0) : original;
        if (base <= 0) {
            cir.setReturnValue(0);
            return;
        }
        double mult = 0.125;
        if (io.github.mermagudyan.idlecraft.common.FurnaceState.burningKnowledge) {
            mult = 0.25;
        }
        int result = (int)(base * mult);
        if (result <= 0) {
            result = base > 0 ? 1 : 0;
        }
        cir.setReturnValue(result);
    }
}
