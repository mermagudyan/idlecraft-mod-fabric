package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.common.QualityComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobChainmailQualityMixin {

    @Inject(method = "dropCustomDeathLoot", at = @At("HEAD"))
    private void idlecraft$rollChainmailQuality(ServerLevel level, DamageSource source, boolean recentlyHit, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) continue;
            ItemStack stack = self.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            if (!QualityComponent.isChainmail(stack)) continue;
            if (stack.get(QualityComponent.QUALITY) != null) continue;
            QualityComponent.applyQuality(stack, QualityComponent.randomTier(self.getRandom()));
        }
    }
}
