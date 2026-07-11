package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.IdleMod;
import io.github.mermagudyan.idlecraft.common.QualityComponent;
import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.ClientState;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
        if (input.isEmpty() || !QualityComponent.isEligible(input)) return;

        int current = QualityComponent.getQuality(input);
        if (current == QualityComponent.CORRUPTED) return; 

        List<String> unlocked = player.level().isClientSide()
                ? ClientState.getUnlockedNodes()
                : idlecraft$serverNodes(player);
        
        
        int capLevel = unlocked.contains("good_caster")
                ? QualityComponent.SUPERIOR : QualityComponent.NORMAL;
        int capIdx = QualityComponent.tierIndex(capLevel);

        int currentIdx = QualityComponent.tierIndex(current);
        if (currentIdx >= capIdx) return; 

        int selected = idlecraft$selectedQuality(player);
        int selectedIdx = QualityComponent.tierIndex(selected);

        
        
        int targetIdx = Math.max(currentIdx, selectedIdx);
        if (targetIdx > capIdx) targetIdx = capIdx;
        if (targetIdx <= currentIdx) return; 
        if (targetIdx >= QualityComponent.TIERS.length) return;

        int targetLevel = QualityComponent.TIERS[targetIdx];

        
        
        
        
        ItemStack mat = inputSlots.getItem(1);
        Item expectedMat = QualityComponent.forgeMaterial(input);
        int consume = 0;
        if (expectedMat != null && !mat.isEmpty() && mat.getItem() == expectedMat && mat.getCount() >= 1) {
            consume = 1;
        }
        IdleMod.LOGGER.info("[IDLECRAFT][Anvil] forge check inputQ={} selected={} targetIdx={} currentIdx={} capIdx={} mat={} expectedMat={} consume={}",
                current, selected, targetIdx, currentIdx, capIdx, mat.getItem(), expectedMat, consume);

        ItemStack out = input.copy();
        out.setDamageValue(0);
        QualityComponent.applyQuality(out, targetLevel);
        resultSlots.setItem(0, out);
        ((AbstractContainerMenu) (Object) this).broadcastChanges();

        IdleMod.LOGGER.info("[IDLECRAFT][Anvil] forge OK inputQ={} -> targetLevel={} matConsumed={}", current, targetLevel, consume);

        
        
        int xpCost = Math.max(1, targetLevel - current);
        cost.set(xpCost);
        repairItemCountCost = consume;
        idlecraft$forgeUpgrade = true;
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void idlecraft$logSlots(CallbackInfo ci) {
        ItemCombinerMenuAccessor acc = (ItemCombinerMenuAccessor) (Object) this;
        Container inputSlots = acc.idlecraft$inputSlots();
        ItemStack in = inputSlots.getItem(0);
        ItemStack add = inputSlots.getItem(1);
        IdleMod.LOGGER.info("[IDLECRAFT][Anvil] slotsChanged input={} q={} additional={} q={}",
                in.getItem(), QualityComponent.getQuality(in), add.getItem(), QualityComponent.getQuality(add));
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
