package zombie.core.random;

public final class Rand {
   public Rand() {
   }

   public static int Next(int var0) {
      return RandStandard.INSTANCE.Next(var0);
   }

   public static long Next(long var0) {
      return RandStandard.INSTANCE.Next(var0);
   }

   public static int Next(int var0, int var1) {
      return RandStandard.INSTANCE.Next(var0, var1);
   }

   public static long Next(long var0, long var2) {
      return RandStandard.INSTANCE.Next(var0, var2);
   }

   public static float Next(float var0, float var1) {
      return RandStandard.INSTANCE.Next(var0, var1);
   }

   public static boolean NextBool(int var0) {
      return RandStandard.INSTANCE.NextBool(var0);
   }

   public static boolean NextBoolFromChance(float var0) {
      return RandStandard.INSTANCE.NextBoolFromChance(var0);
   }

   public static int AdjustForFramerate(int var0) {
      return RandStandard.INSTANCE.AdjustForFramerate(var0);
   }
}
