package io.github.mermagudyan.idlecraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilMenu.class)
public abstract class AnvilNoLevelCapMixin {

    
    
    @ModifyExpressionValue(method = "createResult",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/DataSlot;get()I",
                    ordinal = 1),
            require = 1)
    private int idlecraft$removeFortyCap(int original) {
        return 0;
    }
}
