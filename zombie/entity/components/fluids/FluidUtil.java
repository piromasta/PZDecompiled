package zombie.entity.components.fluids;

import java.text.DecimalFormat;

public class FluidUtil {
   public static final float UNIT_L = 1.0F;
   public static final float UNIT_dL = 0.1F;
   public static final float UNIT_cL = 0.01F;
   public static final float UNIT_mL = 0.001F;
   public static final float UNIT_dmL = 1.0E-4F;
   public static final float UNIT_cmL = 1.0E-5F;
   public static final float UNIT_uL = 1.0E-6F;
   public static final float MIN_UNIT = 1.0E-4F;
   public static final float MIN_CONTAINER_CAPACITY = 0.05F;
   private static final DecimalFormat df_liter = new DecimalFormat("#.##");
   private static final DecimalFormat df_liter10 = new DecimalFormat("#.#");
   private static final DecimalFormat df_liter1000 = new DecimalFormat("#");
   public static final float TRANSFER_ACTION_TIME_PER_LITER = 40.0F;
   public static final float MIN_TRANSFER_ACTION_TIME = 20.0F;

   public FluidUtil() {
   }

   public static float getUnitLiter() {
      return 1.0F;
   }

   public static float getUnitDeciLiter() {
      return 0.1F;
   }

   public static float getUnitCentiLiter() {
      return 0.01F;
   }

   public static float getUnitMilliLiter() {
      return 0.001F;
   }

   public static float getUnitDeciMilliLiter() {
      return 1.0E-4F;
   }

   public static float getUnitCentiMilliLiter() {
      return 1.0E-5F;
   }

   public static float getUnitMicroLiter() {
      return 1.0E-6F;
   }

   public static float getMinUnit() {
      return 1.0E-4F;
   }

   public static float getMinContainerCapacity() {
      return 0.05F;
   }

   public static String getAmountFormatted(float var0) {
      if (var0 >= 1000.0F) {
         return getAmountLiter1000(var0);
      } else if (var0 >= 10.0F) {
         return getAmountLiter10(var0);
      } else {
         return var0 >= 1.0F ? getAmountLiter(var0) : getAmountMilli(var0);
      }
   }

   public static String getAmountLiter1000(float var0) {
      return df_liter1000.format((double)var0) + " L";
   }

   public static String getAmountLiter10(float var0) {
      return df_liter10.format((double)var0) + " L";
   }

   public static String getAmountLiter(float var0) {
      return df_liter.format((double)var0) + " L";
   }

   public static String getAmountMilli(float var0) {
      int var1 = Math.round(var0 * 1000.0F);
      return "" + var1 + " mL";
   }

   public static float roundTransfer(float var0) {
      float var1 = (float)Math.round(var0 * 100.0F) / 100.0F;
      return var1;
   }

   public static float getTransferActionTimePerLiter() {
      return 40.0F;
   }

   public static float getMinTransferActionTime() {
      return 20.0F;
   }
}
