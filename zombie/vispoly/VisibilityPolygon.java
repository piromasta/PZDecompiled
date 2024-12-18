package zombie.vispoly;

import zombie.debug.DebugLog;

public final class VisibilityPolygon {
   private static VisibilityPolygon instance = null;

   public VisibilityPolygon() {
   }

   public static VisibilityPolygon getInstance() {
      if (instance == null) {
         instance = new VisibilityPolygon();
      }

      return instance;
   }

   public static void init() {
      String var0 = "";
      if ("1".equals(System.getProperty("zomboid.debuglibs.vispoly"))) {
         DebugLog.log("***** Loading debug version of PZVisPoly");
         var0 = "d";
      }

      if (System.getProperty("os.name").contains("OS X")) {
         System.loadLibrary("PZVisPoly");
      } else if (System.getProperty("sun.arch.data.model").equals("64")) {
         System.loadLibrary("PZVisPoly64" + var0);
      } else {
         System.loadLibrary("PZVisPoly32" + var0);
      }

      getInstance().test();
   }

   public void test() {
      float[] var1 = new float[]{-250.0F, -250.0F, -250.0F, 250.0F, -250.0F, 250.0F, 250.0F, 250.0F, 250.0F, 250.0F, 250.0F, -250.0F, 250.0F, -250.0F, -250.0F, -250.0F, -50.0F, 50.0F, 50.0F, 50.0F, 50.0F, 50.0F, 50.0F, 100.0F, 50.0F, 100.0F, -50.0F, 100.0F, -50.0F, 100.0F, -50.0F, 50.0F};
      int var2 = calculatePolygon(0.0F, 0.0F, var1.length / 2, var1);
      float[] var3 = new float[var2 * 2];
      getPolygon(var3);
   }

   public static native int calculatePolygon(float var0, float var1, int var2, float[] var3);

   public static native int getPolygon(float[] var0);
}
