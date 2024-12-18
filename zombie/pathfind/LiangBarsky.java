package zombie.pathfind;

public final class LiangBarsky {
   private final double[] p = new double[4];
   private final double[] q = new double[4];

   public LiangBarsky() {
   }

   public boolean lineRectIntersect(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      return this.lineRectIntersect(var1, var2, var3, var4, var5, var6, var7, var8, (double[])null);
   }

   public boolean lineRectIntersect(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, double[] var9) {
      this.p[0] = (double)(-var3);
      this.p[1] = (double)var3;
      this.p[2] = (double)(-var4);
      this.p[3] = (double)var4;
      this.q[0] = (double)(var1 - var5);
      this.q[1] = (double)(var7 - var1);
      this.q[2] = (double)(var2 - var6);
      this.q[3] = (double)(var8 - var2);
      double var10 = 0.0;
      double var12 = 1.0;

      for(int var14 = 0; var14 < 4; ++var14) {
         if (this.p[var14] == 0.0) {
            if (this.q[var14] < 0.0) {
               return false;
            }
         } else {
            double var15 = this.q[var14] / this.p[var14];
            if (this.p[var14] < 0.0 && var10 < var15) {
               var10 = var15;
            } else if (this.p[var14] > 0.0 && var12 > var15) {
               var12 = var15;
            }
         }
      }

      if (var10 >= var12) {
         return false;
      } else {
         if (var9 != null) {
            var9[0] = var10;
            var9[1] = var12;
         }

         return true;
      }
   }
}
