package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.common.QualityComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemQualityTooltipMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void idlecraft$addQualityLine(Item.TooltipContext ctx, Player player, TooltipFlag flag,
                                          CallbackInfoReturnable<List<Component>> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (!QualityComponent.isEligible(self)) return;

        List<Component> lines = cir.getReturnValue();
        if (lines == null) return;

        int quality = QualityComponent.getQuality(self);
        Component qLine = Component.literal("Quality: " + QualityComponent.tierName(quality))
                .withStyle(idlecraft$color(quality));

        int header = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (idlecraft$isModifierHeader(lines.get(i))) {
                header = i;
                break;
            }
        }
        if (header >= 0) {
            int idx = header;
            if (idx > 0 && lines.get(idx - 1).getString().isEmpty()) idx = idx - 1;
            lines.add(idx, qLine);
        } else {
            lines.add(Math.min(1, lines.size()), qLine);
        }
    }

    private static boolean idlecraft$isModifierHeader(Component c) {
        return c.getContents() instanceof TranslatableContents tc
                && tc.getKey() != null
                && tc.getKey().startsWith("item.modifiers.");
    }

    private static ChatFormatting idlecraft$color(int quality) {
        return switch (quality) {
            case QualityComponent.CORRUPTED -> ChatFormatting.DARK_RED;
            case QualityComponent.SO_SO -> ChatFormatting.WHITE;
            case QualityComponent.NORMAL -> ChatFormatting.GREEN;
            case QualityComponent.EXCELLENT -> ChatFormatting.AQUA;
            case QualityComponent.SUPERIOR -> ChatFormatting.GOLD;
            default -> ChatFormatting.GRAY;
        };
    }
}
