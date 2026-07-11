package io.github.mermagudyan.idlecraft.common;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.core.component.DataComponentType;

import java.util.List;

public final class QualityComponent {

    public static final DataComponentType<Integer> QUALITY =
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                    Identifier.fromNamespaceAndPath("idlecraft", "quality"),
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .build());

    
    public static final DataComponentType<Integer> CLEANSE_COUNT =
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                    Identifier.fromNamespaceAndPath("idlecraft", "cleanse_count"),
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .build());

    private QualityComponent() {
    }

    public static void init() {
        
    }

    public static final int CORRUPTED = -1;
    public static final int POOR = 0;
    public static final int SO_SO = 5;
    public static final int NORMAL = 10;
    public static final int EXCELLENT = 15;
    public static final int SUPERIOR = 20;

    
    public static final int[] TIERS = { POOR, SO_SO, NORMAL, EXCELLENT, SUPERIOR };

    public static final List<Item> COPPER_ITEMS = List.of(
            Items.COPPER_SWORD, Items.COPPER_PICKAXE, Items.COPPER_AXE,
            Items.COPPER_SHOVEL, Items.COPPER_HOE,
            Items.COPPER_HELMET, Items.COPPER_CHESTPLATE, Items.COPPER_LEGGINGS, Items.COPPER_BOOTS
    );

    public static String tierName(int level) {
        return switch (level) {
            case CORRUPTED -> "Corrupted";
            case SO_SO -> "So-so";
            case NORMAL -> "Normal";
            case EXCELLENT -> "Excellent";
            case SUPERIOR -> "Superior";
            default -> "Poor";
        };
    }

    public static double durabilityMultiplier(int level) {
        return switch (level) {
            case CORRUPTED -> 0.25;
            case POOR -> 0.5;
            case SO_SO -> 0.75;
            case NORMAL -> 1.0;
            case EXCELLENT -> 1.5;
            case SUPERIOR -> 2.0;
            default -> 1.0;
        };
    }

    public static double breakChance(int level) {
        return switch (level) {
            case CORRUPTED -> 0.5;
            case SO_SO -> 0.025;
            case NORMAL -> 0.005;
            case EXCELLENT -> 0.0001;
            case SUPERIOR -> 0.0;
            default -> 0.05;
        };
    }

    public static int enchantCap(int level) {
        return switch (level) {
            case CORRUPTED -> Integer.MAX_VALUE;
            case SO_SO -> 2;
            case NORMAL -> 3;
            case EXCELLENT -> Integer.MAX_VALUE;
            case SUPERIOR -> Integer.MAX_VALUE;
            default -> 1;
        };
    }

    
    public static int getQuality(ItemStack stack) {
        Integer q = stack.get(QUALITY);
        return q == null ? NORMAL : q;
    }

    private static final String[] QUALITY_MATERIALS = {
            "copper_", "iron_", "golden_", "diamond_", "netherite_", "chainmail_"
    };

    public static boolean isEligible(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return false;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (String prefix : QUALITY_MATERIALS) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    public static boolean isChainmail(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().startsWith("chainmail_");
    }

    
    public static final List<Item> REPAIR_MATERIALS = List.of(
            Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.NETHERITE_INGOT
    );

    public static boolean isRepairMaterial(Item item) {
        return REPAIR_MATERIALS.contains(item);
    }

    public static int randomTier(net.minecraft.util.RandomSource random) {
        double d = random.nextDouble();
        if (d < 0.40) return POOR;
        if (d < 0.70) return SO_SO;
        if (d < 0.90) return NORMAL;
        if (d < 0.98) return EXCELLENT;
        return SUPERIOR;
    }

    
    public static int tierIndex(int level) {
        for (int i = 0; i < TIERS.length; i++) {
            if (TIERS[i] == level) return i;
        }
        return 0;
    }

    
    public static int baseMaxDamage(Item item) {
        Integer def = new ItemStack(item).getComponents().get(DataComponents.MAX_DAMAGE);
        return def == null ? 0 : def;
    }

    
    public static void applyQuality(ItemStack stack, int level) {
        stack.set(QUALITY, level);
        int base = baseMaxDamage(stack.getItem());
        if (base > 0) {
            int newMax = Math.max(1, (int) Math.round(base * durabilityMultiplier(level)));
            stack.set(DataComponents.MAX_DAMAGE, newMax);
            if (stack.getDamageValue() > newMax) stack.setDamageValue(newMax - 1);
        }
    }

    
    public static Item repairMaterial(ItemStack stack) {
        Repairable rep = stack.get(DataComponents.REPAIRABLE);
        if (rep == null) return null;
        for (Holder<Item> h : rep.items()) {
            return h.value();
        }
        return null;
    }

    

    public static Item forgeMaterial(ItemStack stack) {
        if (stack.isEmpty()) return null;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.startsWith("copper_")) return Items.COPPER_INGOT;
        if (path.startsWith("iron_")) return Items.IRON_INGOT;
        if (path.startsWith("golden_")) return Items.GOLD_INGOT;
        if (path.startsWith("diamond_")) return Items.DIAMOND;
        if (path.startsWith("netherite_")) return Items.NETHERITE_INGOT;
        return null;
    }

    
    public static int craftMaterialCount(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (path.startsWith("netherite_")) return 1;
        if (path.endsWith("_pickaxe")) return 3;
        if (path.endsWith("_axe")) return 3;
        if (path.endsWith("_sword")) return 2;
        if (path.endsWith("_shovel")) return 1;
        if (path.endsWith("_hoe")) return 2;
        if (path.endsWith("_helmet")) return 5;
        if (path.endsWith("_chestplate")) return 8;
        if (path.endsWith("_leggings")) return 7;
        if (path.endsWith("_boots")) return 4;
        return 1;
    }
}
