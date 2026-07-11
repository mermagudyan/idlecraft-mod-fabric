package io.github.mermagudyan.idlecraft.block;

import io.github.mermagudyan.idlecraft.IdleMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {

    private ModCreativeTabs() {
    }

    public static void register() {
        CreativeModeTab tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.idlecraft.idlecraft"))
                .icon(() -> new ItemStack(ModBlocks.CRAFTING_TABLE))
                .displayItems((parameters, output) -> {
                    output.accept(ModBlocks.CRAFTING_TABLE);
                    output.accept(ModBlocks.FURNACE);
                    output.accept(ModBlocks.BLAST_FURNACE);
                    output.accept(ModBlocks.SMOKER);
                    output.accept(ModBlocks.ANVIL);
                    output.accept(ModBlocks.ENCHANTING_TABLE);
                    output.accept(ModBlocks.BREWING_STAND);
                    output.accept(ModBlocks.SMITHING_TABLE);
                    output.accept(ModBlocks.GRINDSTONE);
                    output.accept(ModBlocks.CAULDRON);
                    output.accept(ModBlocks.HOPPER);
                })
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath(IdleMod.MOD_ID, "idlecraft"), tab);
        IdleMod.LOGGER.info("Registered Idlecraft creative tab.");
    }
}
