package zombie.characters.animals.pathfind;

import org.joml.Vector2f;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.IsoUtils;

public final class NestedPath {
   float[] m_points;
   int m_inset;
   float m_minX;
   float m_minY;
   float m_maxX;
   float m_maxY;
   float m_length;

   public NestedPath() {
   }

   public int getNumPoints() {
      return this.m_points.length / 2;
   }

   public float getX(int var1) {
      return this.m_points[var1 * 2];
   }

   public float getY(int var1) {
      return this.m_points[var1 * 2 + 1];
   }

   public float getLength() {
      return this.m_length;
   }

   public boolean getPointOn(float var1, Vector2f var2) {
      var1 = PZMath.clampFloat(var1, 0.0F, 1.0F);
      var2.set(0.0F);
      float var3 = this.getLength();
      if (var3 <= 0.0F) {
         return false;
      } else {
         float var4 = var3 * var1;
         float var5 = 0.0F;

         for(int var6 = 0; var6 < this.getNumPoints(); ++var6) {
            float var7 = this.getX(var6);
            float var8 = this.getY(var6);
            float var9 = this.getX((var6 + 1) % this.getNumPoints());
            float var10 = this.getY((var6 + 1) % this.getNumPoints());
            float var11 = Vector2f.length(var9 - var7, var10 - var8);
            if (var5 + var11 >= var4) {
               float var12 = (var4 - var5) / var11;
               var2.set(var7 + (var9 - var7) * var12, var8 + (var10 - var8) * var12);
               return true;
            }

            var5 += var11;
         }

         return false;
      }
   }

   boolean pickRandomPointOn(Vector2f var1) {
      return this.getPointOn(Rand.Next(0.0F, 1.0F), var1);
   }

   public float getClosestPointOn(float var1, float var2, Vector2f var3) {
      float var4 = 3.4028235E38F;
      float var5 = 0.0F;
      float var6 = 0.0F;

      for(int var7 = 0; var7 < this.getNumPoints(); ++var7) {
         float var8 = this.getX(var7);
         float var9 = this.getY(var7);
         float var10 = this.getX((var7 + 1) % this.getNumPoints());
         float var11 = this.getY((var7 + 1) % this.getNumPoints());
         float var12 = Vector2f.distance(var8, var9, var10, var11);
         double var13 = (double)((var1 - var8) * (var10 - var8) + (var2 - var9) * (var11 - var9)) / (Math.pow((double)(var10 - var8), 2.0) + Math.pow((double)(var11 - var9), 2.0));
         double var15 = (double)var8 + var13 * (double)(var10 - var8);
         double var17 = (double)var9 + var13 * (double)(var11 - var9);
         if (var13 <= 0.0) {
            var15 = (double)var8;
            var17 = (double)var9;
            var13 = 0.0;
         } else if (var13 >= 1.0) {
            var15 = (double)var10;
            var17 = (double)var11;
            var13 = 1.0;
         }

         float var19 = IsoUtils.DistanceToSquared(var1, var2, (float)var15, (float)var17);
         if (var19 < var4) {
            var4 = var19;
            var3.set(var15, var17);
            var5 = var6 + (float)(var13 * (double)var12);
         }

         var6 += var12;
      }

      return var5 / var6;
   }

   public float getDistanceOfPointFromStart(int var1) {
      float var2 = 0.0F;

      for(int var3 = 0; var3 < var1; ++var3) {
         float var4 = this.getX(var3);
         float var5 = this.getY(var3);
         float var6 = this.getX((var3 + 1) % this.getNumPoints());
         float var7 = this.getY((var3 + 1) % this.getNumPoints());
         var2 += Vector2f.length(var6 - var4, var7 - var5);
      }

      return var2;
   }
}
