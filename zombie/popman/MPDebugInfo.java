package zombie.popman;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import zombie.GameTime;
import zombie.WorldSoundManager;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.network.ByteBufferWriter;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.service.ServerDebugInfo;

public final class MPDebugInfo {
   public static final MPDebugInfo instance = new MPDebugInfo();
   private static final ConcurrentHashMap<Long, MPSoundDebugInfo> debugSounds = new ConcurrentHashMap();
   public final ArrayList<MPCell> loadedCells = new ArrayList();
   public final ObjectPool<MPCell> cellPool = new ObjectPool(MPCell::new);
   public final LoadedAreas loadedAreas = new LoadedAreas(false);
   public ArrayList<MPRepopEvent> repopEvents = new ArrayList();
   public final ObjectPool<MPRepopEvent> repopEventPool = new ObjectPool(MPRepopEvent::new);
   public short repopEpoch = 0;
   public long requestTime = 0L;
   private boolean requestFlag = false;
   public boolean requestPacketReceived = false;
   private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
   private float RESPAWN_EVERY_HOURS = 1.0F;
   private float REPOP_DISPLAY_HOURS = 0.5F;

   public MPDebugInfo() {
   }

   private static native boolean n_hasData(boolean var0);

   private static native void n_requestData();

   private static native int n_getLoadedCellsCount();

   private static native int n_getLoadedCellsData(int var0, ByteBuffer var1);

   private static native int n_getLoadedAreasCount();

   private static native int n_getLoadedAreasData(int var0, ByteBuffer var1);

   private static native int n_getRepopEventCount();

   private static native int n_getRepopEventData(int var0, ByteBuffer var1);

   private void requestServerInfo() {
      if (GameClient.bClient) {
         long var1 = System.currentTimeMillis();
         if (this.requestTime + 1000L <= var1) {
            this.requestTime = var1;
            ServerDebugInfo var3 = new ServerDebugInfo();
            var3.setRequestServerInfo();
            ByteBufferWriter var4 = GameClient.connection.startPacket();
            PacketTypes.PacketType.ServerDebugInfo.doPacket(var4);
            var3.write(var4);
            PacketTypes.PacketType.ServerDebugInfo.send(GameClient.connection);
         }
      }
   }

   public void request() {
      if (GameServer.bServer) {
         this.requestTime = System.currentTimeMillis();
      }
   }

   private void addRepopEvent(int var1, int var2, float var3) {
      float var4 = (float)GameTime.getInstance().getWorldAgeHours();

      while(!this.repopEvents.isEmpty() && ((MPRepopEvent)this.repopEvents.get(0)).worldAge + this.REPOP_DISPLAY_HOURS < var4) {
         this.repopEventPool.release((Object)((MPRepopEvent)this.repopEvents.remove(0)));
      }

      this.repopEvents.add(((MPRepopEvent)this.repopEventPool.alloc()).init(var1, var2, var3));
      ++this.repopEpoch;
   }

   public void serverUpdate() {
      if (GameServer.bServer) {
         long var1 = System.currentTimeMillis();
         if (this.requestTime + 10000L < var1) {
            this.requestFlag = false;
            this.requestPacketReceived = false;
         } else {
            int var3;
            int var4;
            int var5;
            int var6;
            short var8;
            if (this.requestFlag) {
               if (n_hasData(false)) {
                  this.requestFlag = false;
                  this.cellPool.release((List)this.loadedCells);
                  this.loadedCells.clear();
                  this.loadedAreas.clear();
                  var3 = n_getLoadedCellsCount();
                  var4 = 0;

                  while(var4 < var3) {
                     this.byteBuffer.clear();
                     var5 = n_getLoadedCellsData(var4, this.byteBuffer);
                     var4 += var5;

                     for(var6 = 0; var6 < var5; ++var6) {
                        MPCell var7 = (MPCell)this.cellPool.alloc();
                        var7.cx = this.byteBuffer.getShort();
                        var7.cy = this.byteBuffer.getShort();
                        var7.currentPopulation = this.byteBuffer.getShort();
                        var7.desiredPopulation = this.byteBuffer.getShort();
                        var7.lastRepopTime = this.byteBuffer.getFloat();
                        this.loadedCells.add(var7);
                     }
                  }

                  var3 = n_getLoadedAreasCount();
                  var4 = 0;

                  while(var4 < var3) {
                     this.byteBuffer.clear();
                     var5 = n_getLoadedAreasData(var4, this.byteBuffer);
                     var4 += var5;

                     for(var6 = 0; var6 < var5; ++var6) {
                        boolean var12 = this.byteBuffer.get() == 0;
                        var8 = this.byteBuffer.getShort();
                        short var9 = this.byteBuffer.getShort();
                        short var10 = this.byteBuffer.getShort();
                        short var11 = this.byteBuffer.getShort();
                        this.loadedAreas.add(var8, var9, var10, var11);
                     }
                  }
               }
            } else if (this.requestPacketReceived) {
               n_requestData();
               this.requestFlag = true;
               this.requestPacketReceived = false;
            }

            if (n_hasData(true)) {
               var3 = n_getRepopEventCount();
               var4 = 0;

               while(var4 < var3) {
                  this.byteBuffer.clear();
                  var5 = n_getRepopEventData(var4, this.byteBuffer);
                  var4 += var5;

                  for(var6 = 0; var6 < var5; ++var6) {
                     short var13 = this.byteBuffer.getShort();
                     var8 = this.byteBuffer.getShort();
                     float var14 = this.byteBuffer.getFloat();
                     this.addRepopEvent(var13, var8, var14);
                  }
               }
            }

         }
      }
   }

   boolean isRespawnEnabled() {
      if (IsoWorld.getZombiesDisabled()) {
         return false;
      } else {
         return !(this.RESPAWN_EVERY_HOURS <= 0.0F);
      }
   }

   public void render(ZombiePopulationRenderer var1, float var2) {
      this.requestServerInfo();
      float var3 = (float)GameTime.getInstance().getWorldAgeHours();
      IsoMetaGrid var4 = IsoWorld.instance.MetaGrid;
      var1.outlineRect((float)(var4.minX * ZombiePopulationManager.SQUARES_PER_CELL) * 1.0F, (float)(var4.minY * ZombiePopulationManager.SQUARES_PER_CELL) * 1.0F, (float)((var4.maxX - var4.minX + 1) * ZombiePopulationManager.SQUARES_PER_CELL) * 1.0F, (float)((var4.maxY - var4.minY + 1) * ZombiePopulationManager.SQUARES_PER_CELL) * 1.0F, 1.0F, 1.0F, 1.0F, 0.25F);

      int var5;
      MPCell var6;
      float var7;
      for(var5 = 0; var5 < this.loadedCells.size(); ++var5) {
         var6 = (MPCell)this.loadedCells.get(var5);
         var1.outlineRect((float)(var6.cx * ZombiePopulationManager.SQUARES_PER_CELL), (float)(var6.cy * ZombiePopulationManager.SQUARES_PER_CELL), (float)ZombiePopulationManager.SQUARES_PER_CELL, (float)ZombiePopulationManager.SQUARES_PER_CELL, 1.0F, 1.0F, 1.0F, 0.25F);
         if (this.isRespawnEnabled()) {
            var7 = Math.min(var3 - var6.lastRepopTime, this.RESPAWN_EVERY_HOURS) / this.RESPAWN_EVERY_HOURS;
            if (var6.lastRepopTime > var3) {
               var7 = 0.0F;
            }

            var1.outlineRect((float)(var6.cx * ZombiePopulationManager.SQUARES_PER_CELL + 1), (float)(var6.cy * ZombiePopulationManager.SQUARES_PER_CELL + 1), (float)(ZombiePopulationManager.SQUARES_PER_CELL - 2), (float)(ZombiePopulationManager.SQUARES_PER_CELL - 2), 0.0F, 1.0F, 0.0F, var7 * var7);
         }
      }

      for(var5 = 0; var5 < this.loadedAreas.count; ++var5) {
         int var12 = var5 * 4;
         int var14 = this.loadedAreas.areas[var12++];
         int var8 = this.loadedAreas.areas[var12++];
         int var9 = this.loadedAreas.areas[var12++];
         int var10 = this.loadedAreas.areas[var12++];
         var1.outlineRect((float)(var14 * 8), (float)(var8 * 8), (float)(var9 * 8), (float)(var10 * 8), 0.7F, 0.7F, 0.7F, 1.0F);
      }

      for(var5 = 0; var5 < this.repopEvents.size(); ++var5) {
         MPRepopEvent var15 = (MPRepopEvent)this.repopEvents.get(var5);
         if (!(var15.worldAge + this.REPOP_DISPLAY_HOURS < var3)) {
            var7 = 1.0F - (var3 - var15.worldAge) / this.REPOP_DISPLAY_HOURS;
            var7 = Math.max(var7, 0.1F);
            var1.outlineRect((float)(var15.wx * 8), (float)(var15.wy * 8), 40.0F, 40.0F, 0.0F, 0.0F, 1.0F, var7);
         }
      }

      if (var2 > 0.25F) {
         for(var5 = 0; var5 < this.loadedCells.size(); ++var5) {
            var6 = (MPCell)this.loadedCells.get(var5);
            var1.renderCellInfo(var6.cx, var6.cy, var6.currentPopulation, var6.desiredPopulation, var6.lastRepopTime + this.RESPAWN_EVERY_HOURS - var3);
         }
      }

      try {
         debugSounds.entrySet().removeIf((var0) -> {
            return System.currentTimeMillis() > (Long)var0.getKey() + 1000L;
         });
         Iterator var13 = debugSounds.entrySet().iterator();

         while(var13.hasNext()) {
            Map.Entry var17 = (Map.Entry)var13.next();
            Color var18 = Colors.LightBlue;
            if (((MPSoundDebugInfo)var17.getValue()).sourceIsZombie) {
               var18 = Colors.GreenYellow;
            } else if (((MPSoundDebugInfo)var17.getValue()).bRepeating) {
               var18 = Colors.Coral;
            }

            float var16 = 1.0F - Math.max(0.0F, Math.min(1.0F, (float)(System.currentTimeMillis() - (Long)var17.getKey()) / 1000.0F));
            var1.renderCircle((float)((MPSoundDebugInfo)var17.getValue()).x, (float)((MPSoundDebugInfo)var17.getValue()).y, (float)((MPSoundDebugInfo)var17.getValue()).radius, var18.r, var18.g, var18.b, var16);
         }
      } catch (Exception var11) {
      }

   }

   public static void AddDebugSound(WorldSoundManager.WorldSound var0) {
      try {
         debugSounds.put(System.currentTimeMillis(), new MPSoundDebugInfo(var0));
      } catch (Exception var2) {
      }

   }

   public static final class MPRepopEvent {
      public int wx;
      public int wy;
      public float worldAge;

      public MPRepopEvent() {
      }

      public MPRepopEvent init(int var1, int var2, float var3) {
         this.wx = var1;
         this.wy = var2;
         this.worldAge = var3;
         return this;
      }
   }

   public static final class MPCell {
      public short cx;
      public short cy;
      public short currentPopulation;
      public short desiredPopulation;
      public float lastRepopTime;

      public MPCell() {
      }

      MPCell init(int var1, int var2, int var3, int var4, float var5) {
         this.cx = (short)var1;
         this.cy = (short)var2;
         this.currentPopulation = (short)var3;
         this.desiredPopulation = (short)var4;
         this.lastRepopTime = var5;
         return this;
      }
   }

   private static class MPSoundDebugInfo {
      int x;
      int y;
      int radius;
      boolean bRepeating;
      boolean sourceIsZombie;

      MPSoundDebugInfo(WorldSoundManager.WorldSound var1) {
         this.x = var1.x;
         this.y = var1.y;
         this.radius = var1.radius;
         this.bRepeating = var1.bRepeating;
         this.sourceIsZombie = var1.sourceIsZombie;
      }
   }
}
