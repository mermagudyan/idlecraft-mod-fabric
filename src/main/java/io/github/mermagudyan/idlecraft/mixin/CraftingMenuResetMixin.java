package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.IdleMod;
import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.mixin.CraftingMenuAccessor;
import io.github.mermagudyan.idlecraft.network.ClientState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuResetMixin {

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("TAIL"))
    private void idlecraft$resetSelection(int syncId, Inventory inv, ContainerLevelAccess access, CallbackInfo ci) {
        Player player = inv.player;
        if (player == null) return;
        if (player.level().isClientSide()) {
            ClientState.setSelectedCraftQuality(0);
            IdleMod.LOGGER.info("[IDLECRAFT][CraftingReset] client craft_quality -> 0");
        } else {
            MinecraftServer server = player.level().getServer();
            if (server != null) {
                
                
                PlayerData.getServer(server).setFurnaceCounter(player.getUUID(), "craft_quality", 0);
                IdleMod.LOGGER.info("[IDLECRAFT][CraftingReset] server craft_quality -> 0");
                CraftingMenu menu = (CraftingMenu) (Object) this;
                menu.slotsChanged(((CraftingMenuAccessor) menu).idlecraft$craftSlots());
            }
        }
    }
}
