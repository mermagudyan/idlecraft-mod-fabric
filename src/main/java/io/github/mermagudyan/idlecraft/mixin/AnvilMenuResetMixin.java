package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.IdleMod;
import io.github.mermagudyan.idlecraft.common.QualityComponent;
import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.mixin.ItemCombinerMenuAccessor;
import io.github.mermagudyan.idlecraft.network.ClientState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuResetMixin {

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("TAIL"))
    private void idlecraft$resetSelection(int syncId, Inventory inv, ContainerLevelAccess access, CallbackInfo ci) {
        Player player = inv.player;
        if (player == null) return;
        if (player.level().isClientSide()) {
            ClientState.setSelectedQuality(QualityComponent.NORMAL);
            IdleMod.LOGGER.info("[IDLECRAFT][AnvilReset] client selected_quality -> NORMAL");
        } else {
            MinecraftServer server = player.level().getServer();
            if (server != null) {
                PlayerData.getServer(server).setFurnaceCounter(player.getUUID(), "selected_quality", QualityComponent.NORMAL);
                IdleMod.LOGGER.info("[IDLECRAFT][AnvilReset] server selected_quality -> NORMAL");
                
                AnvilMenu menu = (AnvilMenu) (Object) this;
                menu.slotsChanged(((ItemCombinerMenuAccessor) menu).idlecraft$inputSlots());
            }
        }
    }
}
