package com.caveescape;

import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles building the sealed starter cave, giving the player their gear,
 * and detecting when they have escaped to the surface.
 */
public final class CaveManager {

    // Players we have already set up (avoid re-building the cave each tick).
    private static final Set<UUID> SETUP_PLAYERS = new HashSet<>();
    // Players who have already escaped (avoid spamming the message).
    private static final Set<UUID> ESCAPED_PLAYERS = new HashSet<>();

    // The Y level the cave chamber floor is placed at.
    private static final int CAVE_FLOOR_Y = 20;
    // The Y level considered "surface / escaped".
    private static final int SURFACE_Y = 62;

    private CaveManager() {}

    /**
     * Called every server tick. Detects newly joined players (that haven't been
     * set up yet) and checks whether any player has escaped to the surface.
     */
    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID id = player.getUuid();

            if (!SETUP_PLAYERS.contains(id)) {
                setupPlayer(player);
            }

            checkEscape(player);
        }
    }

    /**
     * Builds the sealed cave chamber around the player, teleports them into it,
     * and gives them their survival gear: 2 wooden pickaxes and 1 torch.
     */
    public static void setupPlayer(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        if (SETUP_PLAYERS.contains(id)) {
            return;
        }
        SETUP_PLAYERS.add(id);

        World world = player.getWorld();
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        // Choose a chamber origin near spawn but deep underground.
        BlockPos origin = new BlockPos(0, CAVE_FLOOR_Y, 0);

        buildSealedCave(serverWorld, origin);

        // Teleport the player into the center of the chamber, standing on the floor.
        double spawnX = origin.getX() + 0.5;
        double spawnY = origin.getY() + 1;
        double spawnZ = origin.getZ() + 0.5;
        player.teleport(serverWorld, spawnX, spawnY, spawnZ, player.getYaw(), player.getPitch());

        giveStartingGear(player);

        player.sendMessage(Text.translatable("text.caveescape.welcome"), false);
        player.sendMessage(Text.translatable("text.caveescape.gear"), false);
    }

    /**
     * Builds a small hollow stone chamber fully enclosed in bedrock-thick stone,
     * with no openings, at the given origin (origin is the floor block position).
     */
    private static void buildSealedCave(ServerWorld world, BlockPos origin) {
        int halfWidth = 3; // interior half-width in X/Z
        int height = 4;    // interior height in Y

        int minX = origin.getX() - halfWidth - 1;
        int maxX = origin.getX() + halfWidth + 1;
        int minY = origin.getY() - 1;
        int maxY = origin.getY() + height + 1;
        int minZ = origin.getZ() - halfWidth - 1;
        int maxZ = origin.getZ() + halfWidth + 1;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    boolean isShell = x == minX || x == maxX
                            || y == minY || y == maxY
                            || z == minZ || z == maxZ;
                    if (isShell) {
                        world.setBlockState(pos, Blocks.STONE.getDefaultState());
                    } else {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
    }

    /**
     * Gives the player their starting gear: 2 wooden pickaxes and 1 torch.
     * Clears any existing inventory first so the challenge is fair.
     */
    private static void giveStartingGear(ServerPlayerEntity player) {
        player.getInventory().clear();

        player.getInventory().insertStack(new ItemStack(Items.WOODEN_PICKAXE, 1));
        player.getInventory().insertStack(new ItemStack(Items.WOODEN_PICKAXE, 1));
        player.getInventory().insertStack(new ItemStack(Items.TORCH, 1));
    }

    /**
     * Checks whether the player has reached the surface. If so, congratulate them.
     */
    private static void checkEscape(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        if (ESCAPED_PLAYERS.contains(id)) {
            return;
        }

        if (player.getBlockY() >= SURFACE_Y) {
            ESCAPED_PLAYERS.add(id);
            player.sendMessage(Text.translatable("text.caveescape.escaped"), false);
        }
    }
}