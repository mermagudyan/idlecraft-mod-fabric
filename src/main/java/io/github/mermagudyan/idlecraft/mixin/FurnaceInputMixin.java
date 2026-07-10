package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceInputMixin {

    private static final Field CONTAINER_FIELD;
    static {
        Field f = null;
        try {
            f = net.minecraft.world.inventory.AbstractFurnaceMenu.class.getDeclaredField("container");
            f.setAccessible(true);
        } catch (Exception ignored) {
        }
        CONTAINER_FIELD = f;
    }

    @Inject(method = "canPlaceItemThroughFace", at = @At("HEAD"), cancellable = true)
    private void idlecraft$gateInput(int index, ItemStack stack, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (index != 0) return;
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level == null || level.isClientSide()) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof net.minecraft.world.inventory.AbstractFurnaceMenu menu && CONTAINER_FIELD != null) {
                try {
                    if (CONTAINER_FIELD.get(menu) == (Object) this) {
                        var unlocked = PlayerData.getServer(server).getUnlockedNodes(player.getUUID());
                        if (stack.is(ItemTags.LOGS) && !unlocked.contains("filigree")) {
                            cir.setReturnValue(false);
                            return;
                        }
                        if (stack.is(net.minecraft.world.item.Items.COPPER_ORE)
                                && !unlocked.contains("copper_smelting")) {
                            cir.setReturnValue(false);
                            return;
                        }
                        if (((BlockEntity) (Object) this).getBlockState().getBlock()
                                instanceof net.minecraft.world.level.block.SmokerBlock
                                && unlocked.contains("smoking_rack")
                                && !PlayerData.getServer(server).hasHeldMeat(player.getUUID())) {
                            cir.setReturnValue(false);
                            return;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
}
