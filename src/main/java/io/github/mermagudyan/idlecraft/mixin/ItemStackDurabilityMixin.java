package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.network.ClientState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackDurabilityMixin {

    private static final ThreadLocal<Boolean> IDLECRAFT_REENTER = ThreadLocal.withInitial(() -> false);

    @Inject(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"), cancellable = true)
    private void idlecraft$scaleDurability(int amount, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {
        if (IDLECRAFT_REENTER.get()) return;
        if (entity == null) return;

        ItemStack self = (ItemStack) (Object) this;

        // Quality-based random break: on block break / mob kill a quality item may shatter.
        if (amount > 0 && !entity.level().isClientSide()
                && io.github.mermagudyan.idlecraft.common.QualityComponent.isEligible(self)) {
            int quality = io.github.mermagudyan.idlecraft.common.QualityComponent.getQuality(self);
            double chance = io.github.mermagudyan.idlecraft.common.QualityComponent.breakChance(quality);
            if (chance > 0.0 && entity.getRandom().nextDouble() < chance) {
                int destroy = self.getMaxDamage() - self.getDamageValue();
                if (destroy < 1) destroy = 1;
                IDLECRAFT_REENTER.set(true);
                try {
                    self.hurtAndBreak(destroy, entity, slot);
                } finally {
                    IDLECRAFT_REENTER.set(false);
                }
                ci.cancel();
                return;
            }
        }

        boolean durabilityUnlocked;
        if (entity.level().isClientSide()) {
            durabilityUnlocked = ClientState.getUnlockedNodes().contains("durability");
        } else {
            var server = entity.level().getServer();
            if (server == null) return;
            var data = io.github.mermagudyan.idlecraft.data.PlayerData.getServer(server);
            if (entity instanceof ServerPlayer sp) {
                durabilityUnlocked = data.getUnlockedNodes(sp.getUUID()).contains("durability");
            } else {
                return;
            }
        }
        if (durabilityUnlocked) return;

        int extra = amount * 3;
        IDLECRAFT_REENTER.set(true);
        try {
            ((ItemStack) (Object) this).hurtAndBreak(extra, entity, slot);
        } finally {
            IDLECRAFT_REENTER.set(false);
        }
        ci.cancel();
    }
}
