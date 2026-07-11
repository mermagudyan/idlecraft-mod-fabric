package io.github.mermagudyan.idlecraft.block;

import io.github.mermagudyan.idlecraft.IdleMod;
import io.github.mermagudyan.idlecraft.common.IdlecraftWorkstation;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {

    private ModBlocks() {
    }

    public static final Block CRAFTING_TABLE = register("crafting_table",
            p -> new IdlecraftCraftingTableBlock(p.mapColor(MapColor.WOOD).ignitedByLava().strength(2.5F).sound(SoundType.WOOD)));
    public static final Block FURNACE = register("furnace",
            p -> new IdlecraftFurnaceBlock(furnaceProperties(p, MapColor.STONE)));
    public static final Block BLAST_FURNACE = register("blast_furnace",
            p -> new IdlecraftBlastFurnaceBlock(furnaceProperties(p, MapColor.STONE)));
    public static final Block SMOKER = register("smoker",
            p -> new IdlecraftSmokerBlock(furnaceProperties(p, MapColor.WOOD)));
    public static final Block ANVIL = register("anvil",
            p -> new IdlecraftAnvilBlock(p.mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0F, 1200.0F)
                    .sound(SoundType.ANVIL)));
    public static final Block ENCHANTING_TABLE = register("enchanting_table",
            p -> new IdlecraftEnchantingTableBlock(p.mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS)
                    .strength(5.0F).sound(SoundType.GLASS).noOcclusion()));
    public static final Block BREWING_STAND = register("brewing_stand",
            p -> new IdlecraftBrewingStandBlock(p.mapColor(MapColor.METAL).strength(0.5F).sound(SoundType.GLASS).noOcclusion()));
    public static final Block SMITHING_TABLE = register("smithing_table",
            p -> new IdlecraftSmithingTableBlock(p.mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL)));
    public static final Block GRINDSTONE = register("grindstone",
            p -> new IdlecraftGrindstoneBlock(p.mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion()));
    public static final Block HOPPER = register("hopper",
            p -> new IdlecraftHopperBlock(p.mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL).noOcclusion()));
    public static final Block CAULDRON = register("cauldron",
            p -> new IdlecraftCauldronBlock(p.mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion()));

    private static BlockBehaviour.Properties furnaceProperties(BlockBehaviour.Properties properties, MapColor color) {
        return properties.mapColor(color).requiresCorrectToolForDrops()
                .strength(3.5F).sound(SoundType.STONE)
                .lightLevel(state -> state.getValue(AbstractFurnaceBlock.LIT) ? 13 : 0);
    }

    private static Block register(String name, java.util.function.Function<BlockBehaviour.Properties, Block> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(IdleMod.MOD_ID, name);
        BlockBehaviour.Properties blockProperties = BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), id));
        Block block = factory.apply(blockProperties);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        Item.Properties itemProperties = new Item.Properties()
                .setId(ResourceKey.create(BuiltInRegistries.ITEM.key(), id));
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, itemProperties));
        return block;
    }

    public static void init() {
    }

    public static final class IdlecraftCraftingTableBlock extends CraftingTableBlock implements IdlecraftWorkstation {
        public IdlecraftCraftingTableBlock(BlockBehaviour.Properties properties) { super(properties); }
    }

    public static final class IdlecraftFurnaceBlock extends FurnaceBlock implements IdlecraftWorkstation {
        public IdlecraftFurnaceBlock(BlockBehaviour.Properties properties) { super(properties); }
    }

    public static final class IdlecraftBlastFurnaceBlock extends BlastFurnaceBlock implements IdlecraftWorkstation {
        public IdlecraftBlastFurnaceBlock(BlockBehaviour.Properties properties) { super(properties); }
    }

    public static final class IdlecraftSmokerBlock extends SmokerBlock implements IdlecraftWorkstation {
        public IdlecraftSmokerBlock(BlockBehaviour.Properties properties) { super(properties); }
    }

    public static final class IdlecraftAnvilBlock extends AnvilBlock implements IdlecraftWorkstation {
        public IdlecraftAnvilBlock(BlockBehaviour.Properties properties) { super(properties); }
    }

    public static final class IdlecraftEnchantingTableBlock extends EnchantingTableBlock implements IdlecraftWorkstation {
        public IdlecraftEnchantingTableBlock(BlockBehaviour.Properties properties) { super(properties); }
    }

    public static final class IdlecraftBrewingStandBlock extends BrewingStandBlock implements IdlecraftWorkstation {
        public IdlecraftBrewingStandBlock(BlockBehaviour.Properties properties) { super(properties); }
    }

    public static final class IdlecraftSmithingTableBlock extends SmithingTableBlock implements IdlecraftWorkstation {
        public IdlecraftSmithingTableBlock(BlockBehaviour.Properties properties) { super(properties); }
    }

    public static final class IdlecraftGrindstoneBlock extends GrindstoneBlock implements IdlecraftWorkstation {
        public IdlecraftGrindstoneBlock(BlockBehaviour.Properties properties) { super(properties); }
    }

    public static final class IdlecraftHopperBlock extends HopperBlock implements IdlecraftWorkstation {
        public IdlecraftHopperBlock(BlockBehaviour.Properties properties) { super(properties); }
    }

    public static final class IdlecraftCauldronBlock extends CauldronBlock implements IdlecraftWorkstation {
        public IdlecraftCauldronBlock(BlockBehaviour.Properties properties) { super(properties); }
    }
}
