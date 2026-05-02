package ru.beykerykt.lightapi.server.nms.craftbukkit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import ru.beykerykt.lightapi.server.nms.ChunkInfo;
import ru.beykerykt.lightapi.server.nms.INMSHandler;
import ru.beykerykt.lightapi.server.nms.LightType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * NMS Handler for Minecraft Java Edition 26.1 (v26_1_R1 / Tiny Takeover).
 * CraftBukkit package no longer uses a versioned sub-package (e.g. v1_21_R3).
 */
public class CraftBukkit_v26_1_R1 implements INMSHandler {

    private ServerLevel getServerLevel(World world) {
        return ((CraftWorld) world).getHandle();
    }

    private LightLayer getLightLayer(LightType lightType) {
        return (lightType == LightType.BLOCK) ? LightLayer.BLOCK : LightLayer.SKY;
    }

    @Override
    public int getMaxLightHeight(World world) {
        return world.getMaxHeight();
    }

    @Override
    public void createLight(World world, int x, int y, int z, int light) {
        createLight(world, x, y, z, LightType.BLOCK, light);
    }

    @Override
    public void createLight(World world, int x, int y, int z, LightType lightType, int light) {
        ServerLevel level = getServerLevel(world);
        BlockPos pos = new BlockPos(x, y, z);
        LevelLightEngine engine = level.getLightEngine();
        level.getServer().execute(() -> {
            engine.getLayerListener(getLightLayer(lightType)).onBlockEmissionIncrease(pos, light);
            engine.checkBlock(pos);
        });
    }

    @Override
    public void deleteLight(World world, int x, int y, int z) {
        deleteLight(world, x, y, z, LightType.BLOCK);
    }

    @Override
    public void deleteLight(World world, int x, int y, int z, LightType lightType) {
        createLight(world, x, y, z, lightType, 0);
    }

    @Override
    public void recalculateLight(World world, int x, int y, int z) {
        ServerLevel level = getServerLevel(world);
        BlockPos pos = new BlockPos(x, y, z);
        level.getServer().execute(() -> level.getLightEngine().checkBlock(pos));
    }

    @Override
    public List<ChunkInfo> collectChunks(World world, int x, int y, int z) {
        return collectChunks(world, x, y, z, LightType.BLOCK, 15);
    }

    @Override
    public List<ChunkInfo> collectChunks(World world, int blockX, int blockY, int blockZ, int lightLevel) {
        return collectChunks(world, blockX, blockY, blockZ, LightType.BLOCK, lightLevel);
    }

    @Override
    public List<ChunkInfo> collectChunks(World world, int blockX, int blockY, int blockZ, LightType lightType, int lightLevel) {
        List<ChunkInfo> list = new ArrayList<>();
        ServerLevel level = getServerLevel(world);
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cx = chunkX + dx;
                int cz = chunkZ + dz;
                if (level.getChunkSource().getChunkNow(cx, cz) != null) {
                    list.add(new ChunkInfo(cx, cz));
                }
            }
        }
        return list;
    }

    @Override
    public void sendChunkSectionsUpdate(World world, int chunkX, int chunkZ, int sectionsMask, Collection<? extends Player> players) {
        ServerLevel level = getServerLevel(world);
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        ClientboundLightUpdatePacket packet = new ClientboundLightUpdatePacket(
                chunkPos, level.getLightEngine(), null, null);
        level.getChunkSource().chunkMap.getPlayers(chunkPos, false).forEach(sp -> {
            if (players == null || players.contains(sp.getBukkitEntity())) {
                sp.connection.send(packet);
            }
        });
    }

    @Override
    public void sendChunkSectionsUpdate(World world, int chunkX, int chunkZ, int sectionsMask, Player player) {
        sendChunkSectionsUpdate(world, chunkX, chunkZ, sectionsMask, List.of(player));
    }

    @Override
    public void sendChunkUpdate(World world, int chunkX, int chunkZ, Collection<? extends Player> players) {
        sendChunkSectionsUpdate(world, chunkX, chunkZ, 0xFFFF, players);
    }

    @Override
    public void sendChunkUpdate(World world, int chunkX, int chunkZ, Player player) {
        sendChunkUpdate(world, chunkX, chunkZ, List.of(player));
    }
}
