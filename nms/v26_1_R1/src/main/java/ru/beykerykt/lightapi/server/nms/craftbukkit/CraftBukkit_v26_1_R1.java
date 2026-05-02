package ru.beykerykt.lightapi.server.nms.craftbukkit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import ru.beykerykt.lightapi.server.nms.IChunkSectionsData;
import ru.beykerykt.lightapi.server.nms.INMSHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * NMS Handler for Minecraft Java Edition 26.1 (v26_1_R1 / Tiny Takeover).
   * CraftBukkit package no longer uses a versioned sub-package (e.g. v1_21_R3).
   * Classes live directly under org.bukkit.craftbukkit.*
   */
public class CraftBukkit_v26_1_R1 implements INMSHandler {

    private ServerLevel getServerLevel(World world) {
              return ((CraftWorld) world).getHandle();
    }

    @Override
      public int getMinHeight(World world) { return world.getMinHeight(); }

    @Override
      public int getMaxHeight(World world) { return world.getMaxHeight(); }

    @Override
      public boolean isValidBlockHeight(World world, int y) {
                return y >= getMinHeight(world) && y < getMaxHeight(world);
      }

    @Override
      public int getRawLightLevel(World world, int x, int y, int z, int lightType) {
                ServerLevel level = getServerLevel(world);
                BlockPos pos = new BlockPos(x, y, z);
                LightLayer layer = (lightType == 0) ? LightLayer.BLOCK : LightLayer.SKY;
                return level.getBrightness(layer, pos);
      }

    @Override
      public int setRawLightLevel(World world, int x, int y, int z, int lightLevel, int lightType) {
                ServerLevel level = getServerLevel(world);
                BlockPos pos = new BlockPos(x, y, z);
                LevelLightEngine lightEngine = level.getLightEngine();
                LightLayer layer = (lightType == 0) ? LightLayer.BLOCK : LightLayer.SKY;
                try {
                              lightEngine.getLayerListener(layer).onBlockEmissionIncrease(pos, lightLevel);
                } catch (Exception e) {
                              level.getServer().execute(() -> lightEngine.checkBlock(pos));
                }
                return lightLevel;
      }

    @Override
      public int recalculateLighting(World world, int x, int y, int z, int lightType) {
                ServerLevel level = getServerLevel(world);
                BlockPos pos = new BlockPos(x, y, z);
                level.getServer().execute(() -> level.getLightEngine().checkBlock(pos));
                return 0;
      }

    @Override
      public Collection<IChunkSectionsData> collectChunkSectionsToUpdate(
                    World world, int x, int y, int z, int lightLevel, int lightType) {
                List<IChunkSectionsData> list = new ArrayList<>();
                ServerLevel level = getServerLevel(world);
                int chunkX = x >> 4;
                int chunkZ = z >> 4;
                int sectionY = level.getSectionIndex(y);
                for (int dx = -1; dx <= 1; dx++) {
                              for (int dz = -1; dz <= 1; dz++) {
                                                int cx = chunkX + dx;
                                                int cz = chunkZ + dz;
                                                if (level.getChunkSource().getChunkNow(cx, cz) == null) continue;
                                                long key = SectionPos.asLong(cx, sectionY, cz);
                                                list.add(new ChunkSectionsDataImpl(cx, cz, sectionY, key));
                              }
                }
                return list;
      }

    @Override
      public void sendChunkSectionsUpdate(World world, IChunkSectionsData data, Collection<Player> players) {
                ServerLevel level = getServerLevel(world);
                if (!(data instanceof ChunkSectionsDataImpl csd)) return;
                ChunkPos chunkPos = new ChunkPos(csd.chunkX(), csd.chunkZ());
                ClientboundLightUpdatePacket packet = new ClientboundLightUpdatePacket(chunkPos, level.getLightEngine(), null, null);
                level.getChunkSource().chunkMap.getPlayers(chunkPos, false).forEach(sp -> {
                              if (players == null || players.contains(sp.getBukkitEntity())) {
                                                sp.connection.send(packet);
                              }
                });
      }

    @Override
      public void sendChunkSectionsUpdate(World world, IChunkSectionsData data) {
                sendChunkSectionsUpdate(world, data, null);
      }

    private record ChunkSectionsDataImpl(int chunkX, int chunkZ, int sectionY, long sectionKey)
              implements IChunkSectionsData {
                        @Override public int getChunkX() { return chunkX; }
                        @Override public int getChunkZ() { return chunkZ; }
                        @Override public long getSectionKey() { return sectionKey; }
              }
}
