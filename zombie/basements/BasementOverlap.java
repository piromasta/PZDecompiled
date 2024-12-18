package zombie.basements;

import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;

public final class BasementOverlap {
   static final int SQUARES_PER_CHUNK = 20;
   static final int CHUNKS_PER_CELL = 10;
   static final int SQUARES_PER_CELL = 200;
   final TLongObjectHashMap<Cell> m_cellLookup = new TLongObjectHashMap();
   final ArrayList<Basement> m_basements = new ArrayList();

   public BasementOverlap() {
   }

   long getCellKey(int var1, int var2) {
      return (long)var2 << 32 | (long)var1;
   }

   Cell getCell(int var1, int var2) {
      long var3 = this.getCellKey(var1, var2);
      return (Cell)this.m_cellLookup.get(var3);
   }

   Cell createCell(int var1, int var2) {
      Cell var3 = new Cell(var1, var2);
      long var4 = this.getCellKey(var1, var2);
      this.m_cellLookup.put(var4, var3);
      return var3;
   }

   Cell getOrCreateCell(int var1, int var2) {
      Cell var3 = this.getCell(var1, var2);
      if (var3 == null) {
         var3 = this.createCell(var1, var2);
      }

      return var3;
   }

   void addBasement(BuildingDef var1, int var2, int var3, int var4) {
      Basement var5 = new Basement();
      var5.m_buildingDef = var1;
      var5.m_placeX = var2;
      var5.m_placeY = var3;
      var5.m_placeZ = var4;
      this.m_basements.add(var5);
      int var6 = var2 / 200;
      int var7 = var3 / 200;
      int var8 = (var2 + var1.getW() - 1) / 200;
      int var9 = (var3 + var1.getH() - 1) / 200;

      for(int var10 = var7; var10 <= var9; ++var10) {
         for(int var11 = var6; var11 <= var8; ++var11) {
            Cell var12 = this.getOrCreateCell(var11, var10);
            var12.addBasement(var5);
         }
      }

   }

   boolean checkOverlap(BuildingDef var1, int var2, int var3, int var4) {
      int var5 = var2 / 200;
      int var6 = var3 / 200;
      int var7 = (var2 + var1.getW() - 1) / 200;
      int var8 = (var3 + var1.getH() - 1) / 200;

      for(int var9 = var6; var9 <= var8; ++var9) {
         for(int var10 = var5; var10 <= var7; ++var10) {
            Cell var11 = this.getCell(var10, var9);
            if (var11 != null && var11.checkOverlap(var1, var2, var3, var4)) {
               return true;
            }
         }
      }

      return false;
   }

   public void Dispose() {
      this.m_cellLookup.forEachValue((var0) -> {
         var0.Dispose();
         return true;
      });
      this.m_cellLookup.clear();
      this.m_basements.forEach((var0) -> {
         var0.Dispose();
      });
      this.m_basements.clear();
   }

   static final class Cell {
      final int m_cellX;
      final int m_cellY;
      final Chunk[] m_chunks = new Chunk[100];

      Cell(int var1, int var2) {
         this.m_cellX = var1;
         this.m_cellY = var2;
      }

      Chunk getChunk(int var1, int var2) {
         return this.m_chunks[var1 + var2 * 10];
      }

      Chunk createChunk(int var1, int var2) {
         Chunk var3 = new Chunk();
         this.m_chunks[var1 + var2 * 10] = var3;
         return var3;
      }

      Chunk getOrCreateChunk(int var1, int var2) {
         Chunk var3 = this.getChunk(var1, var2);
         if (var3 == null) {
            var3 = this.createChunk(var1, var2);
         }

         return var3;
      }

      void addBasement(Basement var1) {
         int var2 = var1.m_placeX - this.m_cellX * 200;
         int var3 = var1.m_placeY - this.m_cellY * 200;
         int var4 = Math.max(var2 / 20, 0);
         int var5 = Math.max(var3 / 20, 0);
         int var6 = Math.min((var2 + var1.m_buildingDef.getW() - 1) / 20, 9);
         int var7 = Math.min((var3 + var1.m_buildingDef.getH() - 1) / 20, 9);

         for(int var8 = var5; var8 <= var7; ++var8) {
            for(int var9 = var4; var9 <= var6; ++var9) {
               Chunk var10 = this.getOrCreateChunk(var9, var8);
               var10.addBasement(var1);
            }
         }

      }

      boolean checkOverlap(BuildingDef var1, int var2, int var3, int var4) {
         int var5 = var2 - this.m_cellX * 200;
         int var6 = var3 - this.m_cellY * 200;
         int var7 = Math.max(var5 / 20, 0);
         int var8 = Math.max(var6 / 20, 0);
         int var9 = Math.min((var5 + var1.getW() - 1) / 20, 9);
         int var10 = Math.min((var6 + var1.getH() - 1) / 20, 9);

         for(int var11 = var8; var11 <= var10; ++var11) {
            for(int var12 = var7; var12 <= var9; ++var12) {
               Chunk var13 = this.getChunk(var12, var11);
               if (var13 != null && var13.checkOverlap(var1, var2, var3, var4)) {
                  return true;
               }
            }
         }

         return false;
      }

      void Dispose() {
         Chunk[] var1 = this.m_chunks;
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            Chunk var4 = var1[var3];
            if (var4 != null) {
               var4.Dispose();
            }
         }

         Arrays.fill(this.m_chunks, (Object)null);
      }
   }

   static final class Basement {
      BuildingDef m_buildingDef;
      int m_placeX;
      int m_placeY;
      int m_placeZ;

      Basement() {
      }

      boolean checkOverlap(BuildingDef var1, int var2, int var3, int var4) {
         int var5 = this.m_placeZ;
         int var6 = this.m_placeZ + this.m_buildingDef.getMaxLevel();
         int var7 = var4;
         int var8 = var4 + var1.getMaxLevel();
         if (var4 <= var6 && var8 >= var5) {
            if (var2 < this.m_placeX + this.m_buildingDef.getW() && var2 + var1.getW() > this.m_placeX) {
               if (var3 < this.m_placeY + this.m_buildingDef.getH() && var3 + var1.getH() > this.m_placeY) {
                  int var9 = Math.max(var5, var4);
                  int var10 = Math.min(var6, var8);

                  for(int var11 = var9; var11 <= var10; ++var11) {
                     int var12 = var11 - var5;
                     int var13 = var11 - var7;
                     if (this.checkOverlap(this.m_buildingDef, this.m_placeX, this.m_placeY, var12, var1, var2, var3, var13)) {
                        return true;
                     }
                  }

                  return true;
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      boolean checkOverlap(BuildingDef var1, int var2, int var3, int var4, BuildingDef var5, int var6, int var7, int var8) {
         for(int var9 = 0; var9 < var1.rooms.size(); ++var9) {
            RoomDef var10 = (RoomDef)var1.rooms.get(var9);
            if (var10.level == var4) {
               for(int var11 = 0; var11 < var5.rooms.size(); ++var11) {
                  RoomDef var12 = (RoomDef)var5.rooms.get(var11);
                  if (var12.level == var8 && this.checkOverlap(var10, var2, var3, var12, var6, var7)) {
                     return true;
                  }
               }
            }
         }

         return false;
      }

      boolean checkOverlap(RoomDef var1, int var2, int var3, RoomDef var4, int var5, int var6) {
         int var7 = var2 + var1.x;
         int var8 = var3 + var1.y;
         int var9 = var2 + var1.x2;
         int var10 = var3 + var1.y2;
         int var11 = var5 + var4.x;
         int var12 = var6 + var4.y;
         int var13 = var5 + var4.x2;
         int var14 = var6 + var4.y2;
         if (var7 < var13 && var9 > var11) {
            if (var8 < var14 && var10 > var12) {
               for(int var15 = 0; var15 < var4.rects.size(); ++var15) {
                  RoomDef.RoomRect var16 = (RoomDef.RoomRect)var4.rects.get(var15);
                  if (var1.intersects(var5 + var16.x - var2, var6 + var16.y - var3, var16.w, var16.h)) {
                     return true;
                  }
               }

               return false;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      void Dispose() {
         this.m_buildingDef = null;
      }
   }

   static final class Chunk {
      final ArrayList<Basement> m_basements = new ArrayList();

      Chunk() {
      }

      void addBasement(Basement var1) {
         this.m_basements.add(var1);
      }

      boolean checkOverlap(BuildingDef var1, int var2, int var3, int var4) {
         for(int var5 = 0; var5 < this.m_basements.size(); ++var5) {
            Basement var6 = (Basement)this.m_basements.get(var5);
            if (var6.checkOverlap(var1, var2, var3, var4)) {
               return true;
            }
         }

         return false;
      }

      void Dispose() {
         this.m_basements.clear();
      }
   }
}
