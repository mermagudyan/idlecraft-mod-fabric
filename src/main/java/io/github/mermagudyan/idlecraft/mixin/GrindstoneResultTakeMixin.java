package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.common.GrindstoneCleanser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public abstract class GrindstoneResultTakeMixin {

    @Inject(method = "onTake", at = @At("RETURN"))
    private void idlecraft$afterTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (player.containerMenu instanceof GrindstoneCleanser cleanser) {
            cleanser.idlecraft$grindstoneCleanseTaken(player);
        }
    }
}
