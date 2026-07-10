package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.ClientState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;

import java.lang.reflect.Field;
import java.util.List;

public class FurnaceFuelRestriction {

    private static final Field CONTAINER_FIELD;
    static {
        Field f = null;
        try {
            f = AbstractFurnaceMenu.class.getDeclaredField("container");
            f.setAccessible(true);
        } catch (Exception ignored) {
        }
        CONTAINER_FIELD = f;
    }


    private static final Item[] WOODEN_TOOLS = {
            Items.WOODEN_SWORD, Items.WOODEN_PICKAXE, Items.WOODEN_AXE,
            Items.WOODEN_SHOVEL, Items.WOODEN_HOE
    };

    public static boolean isFuelAllowed(AbstractFurnaceMenu menu, Level level, ItemStack stack) {
        boolean firstSteps = false;
        boolean enhanced = false;
        boolean filigree = false;
        MinecraftServer server = level.getServer();
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.containerMenu == menu) {
                    List<String> unlocked = PlayerData.getServer(server).getUnlockedNodes(p.getUUID());
                    firstSteps = unlocked.contains("first_steps");
                    enhanced = unlocked.contains("enhanced_smelting");
                    filigree = unlocked.contains("filigree");
                    break;
                }
            }
        } else {
            firstSteps = ClientState.getUnlockedNodes().contains("first_steps");
            enhanced = ClientState.getUnlockedNodes().contains("enhanced_smelting");
            filigree = ClientState.getUnlockedNodes().contains("filigree");
        }
        return allowed(stack, firstSteps, enhanced, filigree);
    }

    public static boolean isFuelAllowedForBlockEntity(AbstractFurnaceBlockEntity blockEntity, Level level, ItemStack stack) {
        MinecraftServer server = level.getServer();
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.containerMenu instanceof AbstractFurnaceMenu menu && CONTAINER_FIELD != null) {
                    try {
                        if (CONTAINER_FIELD.get(menu) == blockEntity) {
                            List<String> unlocked = PlayerData.getServer(server).getUnlockedNodes(p.getUUID());
                            return allowed(stack, unlocked.contains("first_steps"), unlocked.contains("enhanced_smelting"), unlocked.contains("filigree"));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            return allowed(stack, false, false, false);
        }
        boolean firstSteps = ClientState.getUnlockedNodes().contains("first_steps");
        boolean enhanced = ClientState.getUnlockedNodes().contains("enhanced_smelting");
        boolean filigree = ClientState.getUnlockedNodes().contains("filigree");
        return allowed(stack, firstSteps, enhanced, filigree);
    }

    public static boolean allowed(ItemStack stack, boolean firstSteps, boolean enhanced, boolean filigree) {
        if (stack.is(Items.STICK)) return firstSteps;
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) return filigree;
        if (!enhanced) return false;
        return getOperations(stack) > 0.0;
    }

    public static double getOperations(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.STICK) return 0.5;
        if (item == Items.COAL) return 8.0;
        if (item == Items.CHARCOAL) return 8.0 / 1.5;
        if (stack.is(ItemTags.PLANKS)) return 1.0;
        if (stack.is(ItemTags.WOODEN_SLABS)) return 0.75;
        if (stack.is(ItemTags.WOODEN_BUTTONS)) return 0.5;
        if (stack.is(ItemTags.WOODEN_DOORS)) return 1.0;
        if (stack.is(ItemTags.SIGNS) || stack.is(ItemTags.HANGING_SIGNS)) return 1.0;
        if (stack.is(ItemTags.SAPLINGS)) return 0.5;
        if (stack.is(ItemTags.WOOL)) return 0.5;
        if (stack.is(ItemTags.WOOL_CARPETS)) return 0.335;
        for (Item t : WOODEN_TOOLS) {
            if (item == t) return 1.0;
        }
        if (item == Items.BOWL) return 0.5;
        if (item == Items.DEAD_BUSH) return 0.5;
        if (item == Items.AZALEA || item == Items.FLOWERING_AZALEA) return 0.5;
        if (item == Items.BAMBOO) return 0.25;
        if (item == Items.SCAFFOLDING) return 0.25;
        return 0.0;
    }
}
