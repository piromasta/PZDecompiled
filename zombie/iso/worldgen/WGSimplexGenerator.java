package zombie.iso.worldgen;

import java.util.Random;
import zombie.iso.weather.SimplexNoise;

public class WGSimplexGenerator {
   public static int N_NOISES = 6;
   private final WGSimplex[] noises;
   private final WGSimplex selector;

   public WGSimplexGenerator(long var1) {
      this.noises = new WGSimplex[N_NOISES];
      Random var3 = new Random(var1 + 100L);
      this.noises[0] = new WGSimplex(var3.nextDouble(), var3.nextDouble(), var3.nextDouble(), 128.0, 0.0);
      var3 = new Random(var1 + 200L);
      this.noises[1] = new WGSimplex(var3.nextDouble(), var3.nextDouble(), var3.nextDouble(), 128.0, 0.0);
      var3 = new Random(var1 + 300L);
      this.noises[2] = new WGSimplex(var3.nextDouble(), var3.nextDouble(), var3.nextDouble(), 128.0, 0.0);
      var3 = new Random(var1 + 400L);
      this.noises[3] = new WGSimplex(var3.nextDouble(), var3.nextDouble(), var3.nextDouble(), 128.0, 0.0);
      var3 = new Random(var1 + 500L);
      this.noises[4] = new WGSimplex(var3.nextDouble(), var3.nextDouble(), var3.nextDouble(), 128.0, 0.0);
      var3 = new Random(var1 + 600L);
      this.noises[5] = new WGSimplex(var3.nextDouble(), var3.nextDouble(), var3.nextDouble(), 128.0, 0.0);
      var3 = new Random(var1 + 700L);
      this.selector = new WGSimplex(var3.nextDouble(), var3.nextDouble(), var3.nextDouble(), 16.0, 0.0);
   }

   public double[] noise(double var1, double var3) {
      double[] var5 = new double[N_NOISES];

      for(int var6 = 0; var6 < this.noises.length; ++var6) {
         var5[var6] = this.noises[var6].noise(var1, var3);
      }

      return var5;
   }

   public double selector(double var1, double var3) {
      return (this.selector.noise(var1, var3) + 1.0) / 2.0;
   }

   private static class WGSimplex {
      private final double offsetX;
      private final double offsetY;
      private final double depth;
      private final double scale;
      private final double offsetNoise;

      public WGSimplex(double var1, double var3, double var5, double var7, double var9) {
         this.offsetX = var1;
         this.offsetY = var3;
         this.depth = var5;
         this.scale = var7;
         this.offsetNoise = var9;
      }

      public double noise(double var1, double var3) {
         return SimplexNoise.noise((var1 + this.offsetX) / this.scale, (var3 + this.offsetY) / this.scale, this.depth);
      }
   }
}
