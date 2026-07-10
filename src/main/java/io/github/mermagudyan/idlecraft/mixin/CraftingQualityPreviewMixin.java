package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.common.QualityComponent;
import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingQualityPreviewMixin {

    @Shadow @Final private Player player;

    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void idlecraft$previewQuality(Container container, CallbackInfo ci) {
        if (player.level().isClientSide()) return;

        ItemStack result = ((CraftingMenu) (Object) this).getResultSlot().getItem();
        if (result.isEmpty() || !QualityComponent.isEligible(result)) return;

        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        PlayerData data = PlayerData.getServer(server);
        var unlocked = data.getUnlockedNodes(player.getUUID());
        int cap = IdlecraftNetworking.maxCraftableQuality(unlocked);
        int level = data.getFurnaceCounter(player.getUUID(), "selected_quality");
        level = Math.max(QualityComponent.POOR, Math.min(level, cap));

        Integer cur = result.get(QualityComponent.QUALITY);
        if (cur == null || cur != level) {
            QualityComponent.applyQuality(result, level);
        }
    }
}
