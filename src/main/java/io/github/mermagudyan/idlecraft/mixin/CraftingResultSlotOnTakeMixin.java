package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.IdleMod;
import io.github.mermagudyan.idlecraft.common.QualityComponent;
import io.github.mermagudyan.idlecraft.data.PlayerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class CraftingResultSlotOnTakeMixin {

    @Inject(method = "onTake", at = @At("RETURN"))
    private void idlecraft$applyQuality(Player player, ItemStack stack, CallbackInfo ci) {
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer sp)) return;
        if (stack.isEmpty()) return;

        MinecraftServer server = sp.level().getServer();
        if (server == null) return;
        if (!QualityComponent.isEligible(stack)) return;

        PlayerData data = PlayerData.getServer(server);
        var unlocked = data.getUnlockedNodes(sp.getUUID());

        int cap = unlocked.contains("good_caster") ? QualityComponent.NORMAL : QualityComponent.POOR;
        int level = data.getFurnaceCounter(sp.getUUID(), "craft_quality");
        level = Math.max(QualityComponent.POOR, Math.min(level, cap));

        int xpCost = level;
        IdleMod.LOGGER.info("[IDLECRAFT][CraftingTake] selected={} level={} xpCost={} playerXP={} hasGoodCaster={}",
                data.getFurnaceCounter(sp.getUUID(), "craft_quality"), level, xpCost, sp.experienceLevel,
                unlocked.contains("good_caster"));
        if (xpCost > 0 && !sp.isCreative()) {
            if (sp.experienceLevel < xpCost) {
                level = QualityComponent.POOR;
                QualityComponent.applyQuality(stack, level);
                stack.setDamageValue(0);
                IdleMod.LOGGER.info("[IDLECRAFT][CraftingTake] not enough XP, forced POOR");
            } else {
                sp.giveExperienceLevels(-xpCost);
                IdleMod.LOGGER.info("[IDLECRAFT][CraftingTake] charged {} XP, final quality={}", xpCost, QualityComponent.getQuality(stack));
            }
        }

        data.setFurnaceCounter(sp.getUUID(), "crafted_quality", 1);
    }
}
