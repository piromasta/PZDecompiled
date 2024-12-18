package zombie.iso;

public final class BuildingID {
   public BuildingID() {
   }

   public static long makeID(int var0, int var1, int var2) {
      int var3 = var0 | var1 << 16;
      return (long)var3 << 32 | (long)var2;
   }

   public static int getCellX(long var0) {
      int var2 = (int)(var0 >> 32);
      return var2 & '\uffff';
   }

   public static int getCellY(long var0) {
      int var2 = (int)(var0 >> 32);
      return var2 >> 16 & '\uffff';
   }

   public static int getIndex(long var0) {
      return (int)(var0 & 4294967295L);
   }

   public static boolean isSameCell(long var0, int var2, int var3) {
      return getCellX(var0) == var2 && getCellY(var0) == var3;
   }
}
