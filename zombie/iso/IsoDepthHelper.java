package zombie.iso;

import zombie.core.math.PZMath;
import zombie.iso.fboRenderChunk.FBORenderLevels;

public class IsoDepthHelper {
   private static final ThreadLocal<Results> results = ThreadLocal.withInitial(Results::new);
   public static float ChunkDepth = 0.023093667F;
   public static float SquareDepth;
   public static float LevelDepth;
   public static final int ChunkWidthOfDepthBuffer = 20;

   public IsoDepthHelper() {
   }

   public static Results getChunkDepthData(int var0, int var1, int var2, int var3, int var4) {
      Results var5 = (Results)results.get();
      int var6 = var0 + 10;
      int var7 = var1 + 10;
      int var8 = var0 - 10;
      int var9 = var1 - 10;
      int var10 = Math.abs(var8 - var6);
      int var11 = Math.abs(var9 - var7);
      int var12 = var2 - var8;
      int var13 = var3 - var9;
      int var14 = var2 - 1 - var8;
      int var15 = var3 - 1 - var9;
      var12 = var10 - var12;
      var13 = var11 - var13;
      var14 = var10 - var14;
      var15 = var11 - var15;
      var12 *= 8;
      var13 *= 8;
      var14 *= 8;
      var15 *= 8;
      var10 *= 8;
      var11 *= 8;
      float var16 = (float)(var10 + var11);
      var5.sizeX = var10;
      var5.sizeY = var11;
      var5.indexX2 = var14;
      var5.indexY2 = var15;
      var5.indexX = var12;
      var5.indexY = var13;
      var5.maxDepth = var16;
      var5.depthStart = (float)(var12 + var13) / var16;
      var5.depthEnd = (float)(var14 + var15) / var16;
      byte var17 = 8;
      var5.depthStart = (float)(var12 + var13) / (float)var17 / 40.0F;
      var5.depthStart *= ChunkDepth * 20.0F;
      var5.depthStart -= (float)FBORenderLevels.calculateMinLevel(PZMath.fastfloor((float)var4)) * LevelDepth;
      return var5;
   }

   public static Results getDepthSize() {
      Results var0 = (Results)results.get();
      int var1 = 10;
      int var2 = 10;
      int var3 = -10;
      int var4 = -10;
      var1 *= 8;
      var2 *= 8;
      var3 *= 8;
      var4 *= 8;
      int var5 = Math.abs(var3 - var1);
      int var6 = Math.abs(var4 - var2);
      var0.sizeX = var5;
      var0.sizeY = var6;
      return var0;
   }

   public static float calculateDepth(float var0, float var1, float var2) {
      float var3 = (7.0F - var2) * 2.5F;
      float var4 = PZMath.coordmodulof(var0, 8);
      float var5 = PZMath.coordmodulof(var1, 8);
      var4 = 8.0F - var4;
      var5 = 8.0F - var5;
      int var6 = 0;
      int var7 = 0;
      int var8 = -20;
      int var9 = -20;
      var6 *= 8;
      var7 *= 8;
      var8 *= 8;
      var9 *= 8;
      int var10 = Math.abs(var8 - var6);
      int var11 = Math.abs(var9 - var7);
      float var12 = (float)(var10 + var11);
      float var13 = (var4 + var5 + var3) / var12;
      int var14 = FBORenderLevels.calculateMinLevel(PZMath.fastfloor(var2));
      float var15 = (2.0F - (var2 - (float)var14)) * LevelDepth;
      byte var16 = 8;
      return (var4 + var5) / (float)(var16 + var16) * ChunkDepth + var15;
   }

   public static Results getSquareDepthData(int var0, int var1, float var2, float var3, float var4) {
      var0 = PZMath.fastfloor((float)var0 / 8.0F);
      var1 = PZMath.fastfloor((float)var1 / 8.0F);
      Results var5 = getChunkDepthData(var0, var1, PZMath.fastfloor(var2 / 8.0F), PZMath.fastfloor(var3 / 8.0F), PZMath.fastfloor(var4));
      var5.depthStart += calculateDepth(var2, var3, var4);
      return var5;
   }

   static {
      SquareDepth = ChunkDepth / 8.0F;
      LevelDepth = SquareDepth;
   }

   public static class Results {
      public int indexX;
      public int indexY;
      public int indexX2;
      public int indexY2;
      public int sizeX;
      public int sizeY;
      public float depthStart;
      public float depthEnd;
      public float maxDepth;

      public Results() {
      }
   }
}
