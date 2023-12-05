package zombie.globalObjects;

import java.util.ArrayList;
import java.util.Arrays;
import zombie.debug.DebugLog;
import zombie.iso.IsoMetaGrid;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class GlobalObjectLookup {
   private static final int SQUARES_PER_CHUNK = 10;
   private static final int SQUARES_PER_CELL = 300;
   private static final int CHUNKS_PER_CELL = 30;
   private static IsoMetaGrid metaGrid;
   private static final Shared sharedServer = new Shared();
   private static final Shared sharedClient = new Shared();
   private final GlobalObjectSystem system;
   private final Shared shared;
   private final Cell[] cells;

   public GlobalObjectLookup(GlobalObjectSystem var1) {
      this.system = var1;
      this.shared = var1 instanceof SGlobalObjectSystem ? sharedServer : sharedClient;
      this.cells = this.shared.cells;
   }

   private Cell getCellAt(int var1, int var2, boolean var3) {
      int var4 = var1 - metaGrid.minX * 300;
      int var5 = var2 - metaGrid.minY * 300;
      if (var4 >= 0 && var5 >= 0 && var4 < metaGrid.getWidth() * 300 && var5 < metaGrid.getHeight() * 300) {
         int var6 = var4 / 300;
         int var7 = var5 / 300;
         int var8 = var6 + var7 * metaGrid.getWidth();
         if (this.cells[var8] == null && var3) {
            this.cells[var8] = new Cell(metaGrid.minX + var6, metaGrid.minY + var7);
         }

         return this.cells[var8];
      } else {
         DebugLog.log("ERROR: GlobalObjectLookup.getCellForObject object location invalid " + var1 + "," + var2);
         return null;
      }
   }

   private Cell getCellForObject(GlobalObject var1, boolean var2) {
      return this.getCellAt(var1.x, var1.y, var2);
   }

   private Chunk getChunkForChunkPos(int var1, int var2, boolean var3) {
      Cell var4 = this.getCellAt(var1 * 10, var2 * 10, var3);
      return var4 == null ? null : var4.getChunkAt(var1 * 10, var2 * 10, var3);
   }

   public void addObject(GlobalObject var1) {
      Cell var2 = this.getCellForObject(var1, true);
      if (var2 == null) {
         DebugLog.log("ERROR: GlobalObjectLookup.addObject object location invalid " + var1.x + "," + var1.y);
      } else {
         var2.addObject(var1);
      }
   }

   public void removeObject(GlobalObject var1) {
      Cell var2 = this.getCellForObject(var1, false);
      if (var2 == null) {
         DebugLog.log("ERROR: GlobalObjectLookup.removeObject object location invalid " + var1.x + "," + var1.y);
      } else {
         var2.removeObject(var1);
      }
   }

   public GlobalObject getObjectAt(int var1, int var2, int var3) {
      Cell var4 = this.getCellAt(var1, var2, false);
      if (var4 == null) {
         return null;
      } else {
         Chunk var5 = var4.getChunkAt(var1, var2, false);
         if (var5 == null) {
            return null;
         } else {
            for(int var6 = 0; var6 < var5.objects.size(); ++var6) {
               GlobalObject var7 = (GlobalObject)var5.objects.get(var6);
               if (var7.system == this.system && var7.x == var1 && var7.y == var2 && var7.z == var3) {
                  return var7;
               }
            }

            return null;
         }
      }
   }

   public boolean hasObjectsInChunk(int var1, int var2) {
      Chunk var3 = this.getChunkForChunkPos(var1, var2, false);
      if (var3 == null) {
         return false;
      } else {
         for(int var4 = 0; var4 < var3.objects.size(); ++var4) {
            GlobalObject var5 = (GlobalObject)var3.objects.get(var4);
            if (var5.system == this.system) {
               return true;
            }
         }

         return false;
      }
   }

   public ArrayList<GlobalObject> getObjectsInChunk(int var1, int var2, ArrayList<GlobalObject> var3) {
      Chunk var4 = this.getChunkForChunkPos(var1, var2, false);
      if (var4 == null) {
         return var3;
      } else {
         for(int var5 = 0; var5 < var4.objects.size(); ++var5) {
            GlobalObject var6 = (GlobalObject)var4.objects.get(var5);
            if (var6.system == this.system) {
               var3.add(var6);
            }
         }

         return var3;
      }
   }

   public ArrayList<GlobalObject> getObjectsAdjacentTo(int var1, int var2, int var3, ArrayList<GlobalObject> var4) {
      for(int var5 = -1; var5 <= 1; ++var5) {
         for(int var6 = -1; var6 <= 1; ++var6) {
            GlobalObject var7 = this.getObjectAt(var1 + var6, var2 + var5, var3);
            if (var7 != null && var7.system == this.system) {
               var4.add(var7);
            }
         }
      }

      return var4;
   }

   public static void init(IsoMetaGrid var0) {
      metaGrid = var0;
      if (GameServer.bServer) {
         sharedServer.init(var0);
      } else if (GameClient.bClient) {
         sharedClient.init(var0);
      } else {
         sharedServer.init(var0);
         sharedClient.init(var0);
      }

   }

   public static void Reset() {
      sharedServer.reset();
      sharedClient.reset();
   }

   private static final class Shared {
      Cell[] cells;

      private Shared() {
      }

      void init(IsoMetaGrid var1) {
         this.cells = new Cell[var1.getWidth() * var1.getHeight()];
      }

      void reset() {
         if (this.cells != null) {
            for(int var1 = 0; var1 < this.cells.length; ++var1) {
               Cell var2 = this.cells[var1];
               if (var2 != null) {
                  var2.Reset();
               }
            }

            this.cells = null;
         }
      }
   }

   private static final class Cell {
      final int cx;
      final int cy;
      final Chunk[] chunks = new Chunk[900];

      Cell(int var1, int var2) {
         this.cx = var1;
         this.cy = var2;
      }

      Chunk getChunkAt(int var1, int var2, boolean var3) {
         int var4 = (var1 - this.cx * 300) / 10;
         int var5 = (var2 - this.cy * 300) / 10;
         int var6 = var4 + var5 * 30;
         if (this.chunks[var6] == null && var3) {
            this.chunks[var6] = new Chunk();
         }

         return this.chunks[var6];
      }

      Chunk getChunkForObject(GlobalObject var1, boolean var2) {
         return this.getChunkAt(var1.x, var1.y, var2);
      }

      void addObject(GlobalObject var1) {
         Chunk var2 = this.getChunkForObject(var1, true);
         if (var2.objects.contains(var1)) {
            throw new IllegalStateException("duplicate object");
         } else {
            var2.objects.add(var1);
         }
      }

      void removeObject(GlobalObject var1) {
         Chunk var2 = this.getChunkForObject(var1, false);
         if (var2 != null && var2.objects.contains(var1)) {
            var2.objects.remove(var1);
         } else {
            throw new IllegalStateException("chunk doesn't contain object");
         }
      }

      void Reset() {
         for(int var1 = 0; var1 < this.chunks.length; ++var1) {
            Chunk var2 = this.chunks[var1];
            if (var2 != null) {
               var2.Reset();
            }
         }

         Arrays.fill(this.chunks, (Object)null);
      }
   }

   private static final class Chunk {
      final ArrayList<GlobalObject> objects = new ArrayList();

      private Chunk() {
      }

      void Reset() {
         this.objects.clear();
      }
   }
}
