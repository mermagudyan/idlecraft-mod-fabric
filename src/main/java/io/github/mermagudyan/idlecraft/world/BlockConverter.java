package io.github.mermagudyan.idlecraft.world;

import io.github.mermagudyan.idlecraft.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockConverter {

    private BlockConverter() {
    }

    private static final Set<LevelChunk> PENDING = ConcurrentHashMap.newKeySet();

    public static void queue(LevelChunk chunk) {
        PENDING.add(chunk);
    }

    public static void drain() {
        Iterator<LevelChunk> it = PENDING.iterator();
        while (it.hasNext()) {
            LevelChunk chunk = it.next();
            it.remove();
            Level level = chunk.getLevel();
            if (level instanceof ServerLevel serverLevel) {
                convertChunk(serverLevel, chunk, Block.UPDATE_ALL);
            }
        }
    }

    public static Block modBlockFor(Block block) {
        if (block instanceof CraftingTableBlock) return ModBlocks.CRAFTING_TABLE;
        if (block instanceof FurnaceBlock) return ModBlocks.FURNACE;
        if (block instanceof BlastFurnaceBlock) return ModBlocks.BLAST_FURNACE;
        if (block instanceof SmokerBlock) return ModBlocks.SMOKER;
        if (block instanceof AnvilBlock) return ModBlocks.ANVIL;
        if (block instanceof EnchantingTableBlock) return ModBlocks.ENCHANTING_TABLE;
        if (block instanceof BrewingStandBlock) return ModBlocks.BREWING_STAND;
        if (block instanceof SmithingTableBlock) return ModBlocks.SMITHING_TABLE;
        if (block instanceof GrindstoneBlock) return ModBlocks.GRINDSTONE;
        if (block instanceof HopperBlock) return ModBlocks.HOPPER;
        if (block instanceof CauldronBlock) return ModBlocks.CAULDRON;
        return null;
    }

    public static int convertAround(ServerPlayer player, int radiusChunks) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pb = player.blockPosition();
        int pcx = pb.getX() >> 4;
        int pcz = pb.getZ() >> 4;
        int r = Math.max(0, radiusChunks);
        int converted = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                LevelChunk chunk = level.getChunk(pcx + dx, pcz + dz);
                converted += convertChunk(level, chunk, Block.UPDATE_ALL);
            }
        }
        return converted;
    }

    public static int convertChunk(ServerLevel level, LevelChunk chunk, int updateFlag) {
        int converted = 0;
        ChunkPos cp = chunk.getPos();
        int minX = cp.getMinBlockX();
        int minZ = cp.getMinBlockZ();
        int minY = level.dimensionType().minY();
        int maxY = minY + level.dimensionType().height();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = chunk.getBlockState(pos);
                    Block mod = modBlockFor(state.getBlock());
                    if (mod == null || state.getBlock() == mod) continue;
                    level.setBlock(pos, copyState(state, mod), updateFlag);
                    converted++;
                }
            }
        }
        return converted;
    }

    private static BlockState copyState(BlockState oldState, Block mod) {
        BlockState ns = mod.defaultBlockState();
        for (Property<?> property : oldState.getProperties()) {
            if (ns.hasProperty(property)) {
                ns = copyProperty(ns, oldState, property);
            }
        }
        return ns;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState target, BlockState source, Property<T> property) {
        return target.setValue(property, source.getValue(property));
    }
}
