package io.github.mermagudyan.idlecraft.common;

import io.github.mermagudyan.idlecraft.event.PlayerPlacedTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class StructureProtection {

    private StructureProtection() {
    }

    private static boolean ENABLED = true;

    private static final Set<Identifier> ALLOWLIST = new HashSet<>();
    static {
        for (String id : new String[] {
                "minecraft:village", "minecraft:stronghold", "minecraft:bastion_remnant",
                "minecraft:fortress", "minecraft:end_city", "minecraft:mansion",
                "minecraft:monument", "minecraft:desert_pyramid", "minecraft:jungle_pyramid",
                "minecraft:swamp_hut", "minecraft:igloo", "minecraft:shipwreck",
                "minecraft:pillager_outpost", "minecraft:mineshaft", "minecraft:ocean_ruin",
                "minecraft:ruined_portal", "minecraft:ancient_city", "minecraft:trail_ruins",
                "minecraft:nether_fossil", "minecraft:buried_treasure"
        }) {
            ALLOWLIST.add(Identifier.parse(id));
        }
    }

    public static void init() {
    }

    public static boolean isProtectionEnabled() {
        return ENABLED;
    }

    public static void setProtectionEnabled(boolean enabled) {
        ENABLED = enabled;
    }

    public static boolean isAllowedStructure(Holder<Structure> structure) {
        return structure.unwrapKey().map(key -> ALLOWLIST.contains(key.identifier())).orElse(false);
    }

    public static boolean isAllowedStructure(ServerLevel level, Structure structure) {
        var reg = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Identifier id = reg.getKey(structure);
        return id != null && ALLOWLIST.contains(id);
    }

    public static boolean isOp(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        PlayerList list = server.getPlayerList();
        return list.isOp(new NameAndId(player.getGameProfile()));
    }

    
    public static boolean isProtected(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (player.isCreative() || player.isSpectator()) return false;
        if (!ENABLED) return false;
        if (isOp(player)) return false;
        if (PlayerPlacedTracker.isPlayerPlaced(level, pos)) return false;
        Optional<UUID> owner = ClaimStore.owner(level, pos);
        if (owner.isPresent() && player.getUUID().equals(owner.get())) return false;
        StructureStart start = level.structureManager()
                .getStructureWithPieceAt(pos, StructureProtection::isAllowedStructure);
        return start.isValid();
    }
}
