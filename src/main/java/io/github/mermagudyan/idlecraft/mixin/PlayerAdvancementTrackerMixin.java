package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {

    @Shadow
    private ServerPlayerEntity owner;

    @Inject(method = "grantCriterion", at = @At("RETURN"))
    private void onCriterionGranted(AdvancementEntry advancementEntry, String criterionName,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (owner == null || advancementEntry == null) return;

        String advId = advancementEntry.id().toString();

        // ФИЛЬТР: пропускаем рецепты и технические ачивки
        if (advId.contains("recipes/")) return;
        if (advId.endsWith("story/root")) return;  // корневые ачивки вкладок
        try {
            PlayerAdvancementTracker self = (PlayerAdvancementTracker) (Object) this;
            if (!self.getProgress(advancementEntry).isDone()) return;

            MinecraftServer server = owner.getEntityWorld().getServer();
            if (server == null) return;

            PlayerData data = PlayerData.getServer(server);

            if (data.isAdvancementRewarded(owner.getUuid(), advId)) return;

            Advancement advancement = advancementEntry.value();
            int reward = 1;
            if (advancement.display().isPresent()) {
                AdvancementDisplay display = advancement.display().get();
                if (display.getFrame() == AdvancementFrame.CHALLENGE) {
                    reward = 5;
                }
            }

            data.markAdvancementRewarded(owner.getUuid(), advId);
            data.addPoints(owner.getUuid(), reward);
            IdlecraftNetworking.syncPointsToClient(owner);

            System.out.println("[IDLECRAFT] +" + reward + " for " + advId
                    + " | total: " + data.getPoints(owner.getUuid()));

        } catch (Throwable t) {
            System.err.println("[IDLECRAFT] Mixin error: " + t);
        }

    }
}