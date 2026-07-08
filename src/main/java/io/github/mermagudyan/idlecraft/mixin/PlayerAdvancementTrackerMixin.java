package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementTrackerMixin {

    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At("RETURN"))
    private void onCriterionGranted(AdvancementHolder advancementHolder, String criterionName,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (player == null || advancementHolder == null) return;

        String advId = advancementHolder.id().toString();

        if (advId.contains("recipes/")) return;
        if (advId.endsWith("story/root")) return;
        try {
            PlayerAdvancements self = (PlayerAdvancements) (Object) this;
            if (!self.getOrStartProgress(advancementHolder).isDone()) return;

            MinecraftServer server = player.level().getServer();
            if (server == null) return;

            PlayerData data = PlayerData.getServer(server);

            if (data.isAdvancementRewarded(player.getUUID(), advId)) return;

            Advancement advancement = advancementHolder.value();
            int reward = 1;
            if (advancement.display().isPresent()) {
                DisplayInfo display = advancement.display().get();
                if (display.getType() == AdvancementType.CHALLENGE) {
                    reward = 5;
                }
            }

            data.markAdvancementRewarded(player.getUUID(), advId);
            data.addPoints(player.getUUID(), reward);
            IdlecraftNetworking.syncPointsToClient(player);

            System.out.println("[IDLECRAFT] +" + reward + " for " + advId
                    + " | total: " + data.getPoints(player.getUUID()));

        } catch (Throwable t) {
            System.err.println("[IDLECRAFT] Mixin error: " + t);
        }
    }
}