package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.common.QualityComponent;
import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.ClientState;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Repairable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AnvilMenu.class)
public abstract class AnvilQualityMixin {

    @Shadow @Final private DataSlot cost;
    @Shadow private int repairItemCountCost;

    @Unique private boolean idlecraft$forgeUpgrade = false;

    @Inject(method = "createResult", at = @At("TAIL"))
    private void idlecraft$forge(CallbackInfo ci) {
        idlecraft$forgeUpgrade = false;

        ItemCombinerMenuAccessor acc = (ItemCombinerMenuAccessor) (Object) this;
        Player player = acc.idlecraft$player();
        Container inputSlots = acc.idlecraft$inputSlots();
        ResultContainer resultSlots = acc.idlecraft$resultSlots();

        ItemStack input = inputSlots.getItem(0);
        ItemStack additional = inputSlots.getItem(1);
        if (input.isEmpty() || !QualityComponent.isEligible(input)) return;

        Repairable rep = input.get(DataComponents.REPAIRABLE);
        boolean matValid = !additional.isEmpty() && rep != null && rep.isValidRepairItem(additional);
        if (!matValid) return;

        List<String> unlocked = player.level().isClientSide()
                ? ClientState.getUnlockedNodes()
                : idlecraft$serverNodes(player);
        int capLevel = IdlecraftNetworking.maxCraftableQuality(unlocked);
        if (capLevel < QualityComponent.POOR) return;
        int capIdx = QualityComponent.tierIndex(capLevel);

        int current = QualityComponent.getQuality(input);
        int currentIdx = QualityComponent.tierIndex(current);
        if (current == QualityComponent.CORRUPTED) return; // corrupted tools are cleansed at the grindstone, not the anvil

        int selected = idlecraft$selectedQuality(player);
        int selectedIdx = QualityComponent.tierIndex(selected);

        ItemStack result = resultSlots.getItem(0);
        boolean canRepair = !result.isEmpty();

        int targetIdx;
        if (selectedIdx > currentIdx) {
            targetIdx = selectedIdx;
        } else {
            return; // no scroll-up: leave vanilla result untouched (repair only, quality preserved)
        }

        if (targetIdx > capIdx) return; // cannot forge above the currently available tier
        if (targetIdx >= QualityComponent.TIERS.length) return;

        int targetLevel = QualityComponent.TIERS[targetIdx];

        ItemStack out;
        if (canRepair) {
            out = result.copy();
        } else {
            out = input.copy();
            out.setDamageValue(0);
        }
        QualityComponent.applyQuality(out, targetLevel);
        resultSlots.setItem(0, out);

        int repairLevel = Math.max(1, cost.get());
        int extra = (int) Math.ceil((targetLevel - current) * 1.5);
        cost.set(repairLevel + Math.max(0, extra));
        if (!canRepair) {
            repairItemCountCost = 1;
        }
        idlecraft$forgeUpgrade = true;
    }

    @Inject(method = "onTake", at = @At("RETURN"))
    private void idlecraft$corruptOnForge(Player takingPlayer, ItemStack stack, CallbackInfo ci) {
        if (takingPlayer.level().isClientSide()) return;
        if (!idlecraft$forgeUpgrade) return;
        idlecraft$forgeUpgrade = false;
        if (stack.isEmpty() || !QualityComponent.isEligible(stack)) return;
        if (takingPlayer.getRandom().nextDouble() < 0.01) {
            QualityComponent.applyQuality(stack, QualityComponent.CORRUPTED);
        }
    }

    @Unique
    private List<String> idlecraft$serverNodes(Player player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return List.of();
        return PlayerData.getServer(server).getUnlockedNodes(player.getUUID());
    }

    @Unique
    private int idlecraft$selectedQuality(Player player) {
        if (player.level().isClientSide()) {
            return ClientState.getSelectedQuality();
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) return QualityComponent.POOR;
        return PlayerData.getServer(server).getFurnaceCounter(player.getUUID(), "selected_quality");
    }
}
