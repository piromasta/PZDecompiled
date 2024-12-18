package zombie.core;

public class ColorMixer {
   public ColorMixer() {
   }

   public static Color LerpLCH(Color var0, Color var1, float var2, Color var3) {
      if ((var0.r != var0.g || var0.r != var0.b) && (var1.r != var1.g || var1.r != var1.b)) {
         float[] var4 = new float[3];
         float[] var5 = new float[3];
         float[] var6 = new float[3];
         float[] var7 = new float[3];
         float[] var8 = new float[3];
         float[] var9 = new float[3];
         ColorToXYZ(var0, var4);
         XYZToLab(var4, var5);
         LabToLCH(var5, var6);
         ColorToXYZ(var1, var7);
         XYZToLab(var7, var8);
         LabToLCH(var8, var9);
         float[] var10 = new float[3];
         float[] var11 = new float[3];
         float[] var12 = new float[3];
         if (var9[2] > var6[2]) {
            if (var9[2] - var6[2] > var6[2] + 360.0F - var9[2]) {
               var6[2] += 360.0F;
            }
         } else if (var6[2] - var9[2] > var9[2] + 360.0F - var6[2]) {
            var9[2] += 360.0F;
         }

         var10[0] = var9[0] * var2 + var6[0] * (1.0F - var2);
         var10[1] = var9[1] * var2 + var6[1] * (1.0F - var2);
         var10[2] = var9[2] * var2 + var6[2] * (1.0F - var2);
         if (var10[2] > 360.0F) {
            var10[2] -= 360.0F;
         }

         LCHToLab(var10, var11);
         LabToXYZ(var11, var12);
         return XYZToRGB(var12[0], var12[1], var12[2], var3);
      } else {
         var0.interp(var1, var2, var3);
         return var3;
      }
   }

   public static void ColorToXYZ(Color var0, float[] var1) {
      float[] var2 = new float[3];
      float[] var3 = new float[3];
      var2[0] = var0.getRedFloat();
      var2[1] = var0.getGreenFloat();
      var2[2] = var0.getBlueFloat();
      if (var2[0] > 0.04045F) {
         var2[0] = (float)Math.pow(((double)var2[0] + 0.055) / 1.055, 2.4);
      } else {
         var2[0] /= 12.92F;
      }

      if (var2[1] > 0.04045F) {
         var2[1] = (float)Math.pow(((double)var2[1] + 0.055) / 1.055, 2.4);
      } else {
         var2[1] /= 12.92F;
      }

      if (var2[2] > 0.04045F) {
         var2[2] = (float)Math.pow(((double)var2[2] + 0.055) / 1.055, 2.4);
      } else {
         var2[2] /= 12.92F;
      }

      var2[0] *= 100.0F;
      var2[1] *= 100.0F;
      var2[2] *= 100.0F;
      var3[0] = var2[0] * 0.412453F + var2[1] * 0.35758F + var2[2] * 0.180423F;
      var3[1] = var2[0] * 0.212671F + var2[1] * 0.71516F + var2[2] * 0.072169F;
      var3[2] = var2[0] * 0.019334F + var2[1] * 0.119193F + var2[2] * 0.950227F;
      var1[0] = var3[0];
      var1[1] = var3[1];
      var1[2] = var3[2];
   }

   private static void XYZToLab(float[] var0, float[] var1) {
      var0[0] /= 95.047F;
      var0[1] /= 100.0F;
      var0[2] /= 108.883F;
      if (var0[0] > 0.008856F) {
         var0[0] = (float)Math.pow((double)var0[0], 0.3333333333333333);
      } else {
         var0[0] = var0[0] * 7.787F + 0.13793103F;
      }

      if (var0[1] > 0.008856F) {
         var0[1] = (float)Math.pow((double)var0[1], 0.3333333333333333);
      } else {
         var0[1] = var0[1] * 7.787F + 0.13793103F;
      }

      if (var0[2] > 0.008856F) {
         var0[2] = (float)Math.pow((double)var0[2], 0.3333333333333333);
      } else {
         var0[2] = var0[2] * 7.787F + 0.13793103F;
      }

      var1[0] = 116.0F * var0[1] - 16.0F;
      var1[1] = 500.0F * (var0[0] - var0[1]);
      var1[2] = 200.0F * (var0[1] - var0[2]);
   }

   private static void LabToLCH(float[] var0, float[] var1) {
      var1[0] = var0[0];
      var1[1] = (float)Math.sqrt((double)(var0[1] * var0[1] + var0[2] * var0[2]));
      var1[2] = (float)Math.atan2((double)var0[2], (double)var0[1]);
      var1[2] = (float)((double)var1[2] * 57.29577951308232);
      if (var1[2] < 0.0F) {
         var1[2] += 360.0F;
      }

   }

   private static void LCHToLab(float[] var0, float[] var1) {
      var1[0] = var0[0];
      var1[1] = var0[1] * (float)Math.cos((double)var0[2] * 0.017453292519943295);
      var1[2] = var0[1] * (float)Math.sin((double)var0[2] * 0.017453292519943295);
   }

   private static void LabToXYZ(float[] var0, float[] var1) {
      float[] var2 = new float[]{0.0F, (var0[0] + 16.0F) / 116.0F, 0.0F};
      var2[0] = var0[1] / 500.0F + var2[1];
      var2[2] = var2[1] - var0[2] / 200.0F;

      for(int var3 = 0; var3 < 3; ++var3) {
         float var4 = var2[var3] * var2[var3] * var2[var3];
         float var5 = 0.20689656F;
         if (var2[var3] > var5) {
            var2[var3] = var4;
         } else {
            var2[var3] = 0.12841856F * (var2[var3] - 0.13793103F);
         }
      }

      var1[0] = var2[0] * 95.047F;
      var1[1] = var2[1] * 100.0F;
      var1[2] = var2[2] * 108.883F;
   }

   public static Color XYZToRGB(float var0, float var1, float var2, Color var3) {
      float[] var4 = new float[]{var0, var1, var2};
      float[] var5 = new float[3];

      int var6;
      for(var6 = 0; var6 < 3; ++var6) {
         var4[var6] /= 100.0F;
      }

      var5[0] = var4[0] * 3.240479F + var4[1] * -1.53715F + var4[2] * -0.498535F;
      var5[1] = var4[0] * -0.969256F + var4[1] * 1.875992F + var4[2] * 0.041556F;
      var5[2] = var4[0] * 0.055648F + var4[1] * -0.204043F + var4[2] * 1.057311F;

      for(var6 = 0; var6 < 3; ++var6) {
         if (var5[var6] > 0.0031308F) {
            var5[var6] = 1.055F * (float)Math.pow((double)var5[var6], 0.4166666567325592) - 0.055F;
         } else {
            var5[var6] *= 12.92F;
         }
      }

      var5[0] = Math.min(Math.max(var5[0] * 255.0F, 0.0F), 255.0F);
      var5[1] = Math.min(Math.max(var5[1] * 255.0F, 0.0F), 255.0F);
      var5[2] = Math.min(Math.max(var5[2] * 255.0F, 0.0F), 255.0F);
      var3.set(var5[0] / 255.0F, var5[1] / 255.0F, var5[2] / 255.0F);
      return var3;
   }
}
