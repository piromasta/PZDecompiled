package zombie.pathfind.highLevel;

import java.nio.ByteBuffer;
import zombie.core.utils.BooleanGrid;
import zombie.debug.LineDrawer;
import zombie.pathfind.Chunk;
import zombie.pathfind.PolygonalMap2;
import zombie.vehicles.Clipper;

public final class HLChunkRegion {
   static final int CPW = 8;
   HLChunkLevel m_levelData;
   final BooleanGrid m_squaresMask = new BooleanGrid(8, 8);
   int minX;
   int minY;
   int maxX;
   int maxY;
   final int[] edgeN = new int[8];
   final int[] edgeS = new int[8];
   final int[] edgeW = new int[8];
   final int[] edgeE = new int[8];

   public HLChunkRegion() {
   }

   Chunk getChunk() {
      return this.m_levelData.getChunk();
   }

   int getLevel() {
      return this.m_levelData.getLevel();
   }

   boolean containsSquare(int var1, int var2) {
      return this.m_squaresMask.getValue(var1 - this.getChunk().getMinX(), var2 - this.getChunk().getMinY());
   }

   boolean containsSquareLocal(int var1, int var2) {
      return this.m_squaresMask.getValue(var1, var2);
   }

   void initEdges() {
      int[] var10000;
      int var1;
      int var2;
      for(var1 = 0; var1 < 8; ++var1) {
         this.edgeN[var1] = 0;
         this.edgeS[var1] = 0;

         for(var2 = 0; var2 < 8; ++var2) {
            if (this.containsSquareLocal(var2, var1)) {
               if (!this.containsSquareLocal(var2, var1 - 1)) {
                  var10000 = this.edgeN;
                  var10000[var1] |= 1 << var2;
               }

               if (!this.containsSquareLocal(var2, var1 + 1)) {
                  var10000 = this.edgeS;
                  var10000[var1] |= 1 << var2;
               }
            }
         }
      }

      for(var1 = 0; var1 < 8; ++var1) {
         this.edgeW[var1] = 0;
         this.edgeE[var1] = 0;

         for(var2 = 0; var2 < 8; ++var2) {
            if (this.containsSquareLocal(var1, var2)) {
               if (!this.containsSquareLocal(var1 - 1, var2)) {
                  var10000 = this.edgeW;
                  var10000[var1] |= 1 << var2;
               }

               if (!this.containsSquareLocal(var1 + 1, var2)) {
                  var10000 = this.edgeE;
                  var10000[var1] |= 1 << var2;
               }
            }
         }
      }

   }

   boolean isOnEdgeOfLoadedArea() {
      boolean var1 = false;
      if (this.edgeN[0] != 0) {
         var1 |= PolygonalMap2.instance.getChunkFromChunkPos(this.getChunk().wx, this.getChunk().wy - 1) == null;
      }

      if (this.edgeS[7] != 0) {
         var1 |= PolygonalMap2.instance.getChunkFromChunkPos(this.getChunk().wx, this.getChunk().wy + 1) == null;
      }

      if (this.edgeW[0] != 0) {
         var1 |= PolygonalMap2.instance.getChunkFromChunkPos(this.getChunk().wx - 1, this.getChunk().wy) == null;
      }

      if (this.edgeE[7] != 0) {
         var1 |= PolygonalMap2.instance.getChunkFromChunkPos(this.getChunk().wx + 1, this.getChunk().wy) == null;
      }

      return var1;
   }

   void renderDebug() {
      Clipper var1 = HLGlobals.clipper;
      var1.clear();

      int var3;
      for(int var2 = 0; var2 < 8; ++var2) {
         for(var3 = 0; var3 < 8; ++var3) {
            if (this.m_squaresMask.getValue(var3, var2)) {
               var1.addAABB((float)var3, (float)var2, (float)(var3 + 1), (float)(var2 + 1));
            }
         }
      }

      ByteBuffer var7 = HLGlobals.clipperBuffer;
      var3 = var1.generatePolygons(-0.10000000149011612, 2);

      for(int var4 = 0; var4 < var3; ++var4) {
         var7.clear();
         var1.getPolygon(var4, var7);
         this.renderPolygon(var7, true);
         short var5 = var7.getShort();

         for(int var6 = 0; var6 < var5; ++var6) {
            this.renderPolygon(var7, false);
         }
      }

   }

   private void renderPolygon(ByteBuffer var1, boolean var2) {
      short var3 = var1.getShort();
      if (var3 < 3) {
         var1.position(var1.position() + var3 * 4 * 2);
      } else {
         int var4 = this.getChunk().wx * 8;
         int var5 = this.getChunk().wy * 8;
         float var6 = 0.0F;
         float var7 = 0.0F;
         float var8 = 0.0F;
         float var9 = 0.0F;
         float var10 = 1.0F;
         float var11 = 1.0F;
         float var12 = 1.0F;
         float var13 = 1.0F;
         if (!var2) {
            var10 *= 0.5F;
            var11 *= 0.5F;
            var12 *= 0.5F;
         }

         int var14 = this.getLevel() - 32;

         for(int var15 = 0; var15 < var3; ++var15) {
            float var16 = var1.getFloat();
            float var17 = var1.getFloat();
            if (var15 == 0) {
               var6 = var16;
               var7 = var17;
            } else {
               LineDrawer.addLine((float)var4 + var8, (float)var5 + var9, (float)var14, (float)var4 + var16, (float)var5 + var17, (float)var14, var10, var11, var12, var13);
               if (var15 == var3 - 1) {
                  LineDrawer.addLine((float)var4 + var16, (float)var5 + var17, (float)var14, (float)var4 + var6, (float)var5 + var7, (float)var14, var10, var11, var12, var13);
               }
            }

            var8 = var16;
            var9 = var17;
         }

      }
   }
}
