package zombie.debug;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class StackTraceContainer {
   private final int m_depthStart;
   private final int m_depthCount;
   private final StackTraceElement[] m_stackTraceElements;
   private final String m_indent;

   public StackTraceContainer(StackTraceElement[] var1, String var2, int var3, int var4) {
      this.m_depthStart = Math.max(var3, 0);
      this.m_depthCount = var4;
      this.m_stackTraceElements = var1;
      this.m_indent = var2;
   }

   public String toString() {
      return getStackTraceString(this.m_stackTraceElements, this.m_indent, this.m_depthStart, this.m_depthCount);
   }

   public static String getStackTraceString(StackTraceElement[] var0, String var1, int var2, int var3) {
      StringBuilder var4 = new StringBuilder();
      int var5 = var3 <= 0 ? var0.length : var3;
      int var6 = var2;

      for(int var7 = 0; var7 < var5 && var6 < var0.length; ++var6) {
         StackTraceElement var8 = var0[var6];
         String var9 = var8.toString();
         if (var7 <= 0 || !var9.startsWith("zombie.core.profiling.PerformanceProbes$")) {
            if (var7 > 0) {
               var4.append("\r\n");
            }

            var4.append(var1).append(var9);
            ++var7;
         }
      }

      return var4.toString();
   }

   public static StringBuilder getStackTraceString(StringBuilder var0, Throwable var1, String var2, int var3, int var4) {
      StackTraceElement[] var5 = var1.getStackTrace();
      int var6 = var4 <= 0 ? var5.length : var4;
      int var7 = var3;

      for(int var8 = 0; var8 < var6 && var7 < var5.length; ++var7) {
         StackTraceElement var9 = var5[var7];
         String var10 = var9.toString();
         if (var8 <= 0 || !var10.startsWith("zombie.core.profiling.PerformanceProbes$")) {
            var0.append(var2).append(var10).append(System.lineSeparator());
            ++var8;
         }
      }

      return var0;
   }

   public static StringBuilder getStackTraceString(StringBuilder var0, Throwable var1, String var2, String var3, int var4, int var5) {
      Set var6 = Collections.newSetFromMap(new IdentityHashMap());
      var6.add(var1);
      if (var2 != null) {
         var0.append(var3).append(var2).append(System.lineSeparator());
      }

      getStackTraceString(var0, var1, var3 + "\t", var4, var5);
      StackTraceElement[] var7 = var1.getStackTrace();
      Throwable[] var8 = var1.getSuppressed();
      int var9 = var8.length;

      for(int var10 = 0; var10 < var9; ++var10) {
         Throwable var11 = var8[var10];
         getEnclosedStackTraceString(var0, var7, "Suppressed: ", var3, var11, 0, -1, var6);
      }

      Throwable var12 = var1.getCause();
      if (var12 != null) {
         getEnclosedStackTraceString(var0, var7, "Caused by: ", var3, var12, 0, -1, var6);
      }

      return var0;
   }

   public static StringBuilder getEnclosedStackTraceString(StringBuilder var0, StackTraceElement[] var1, String var2, String var3, Throwable var4, int var5, int var6, Set<Throwable> var7) {
      if (var7.contains(var4)) {
         var0.append(var3).append(var2).append("[CIRCULAR REFERENCE: ").append(var4).append("]");
         return var0;
      } else {
         var7.add(var4);
         StackTraceElement[] var8 = var4.getStackTrace();
         int var9 = var8.length - 1;

         for(int var10 = var1.length - 1; var9 >= 0 && var10 >= 0 && var8[var9].equals(var1[var10]); --var10) {
            --var9;
         }

         int var11 = var8.length - 1 - var9;
         var0.append(var3).append(var2).append(var4).append(System.lineSeparator());
         getStackTraceString(var0, var4, var3 + "\t", 0, var9 + 1);
         if (var11 != 0) {
            var0.append(var3).append("\t").append("... ").append(var11).append(" more").append(System.lineSeparator());
         }

         Throwable[] var12 = var4.getSuppressed();
         int var13 = var12.length;

         for(int var14 = 0; var14 < var13; ++var14) {
            Throwable var15 = var12[var14];
            getEnclosedStackTraceString(var0, var8, "Suppressed: ", var3, var15, 0, -1, var7);
         }

         Throwable var16 = var4.getCause();
         if (var16 != null) {
            getEnclosedStackTraceString(var0, var8, "Caused by: ", var3, var16, 0, -1, var7);
         }

         return var0;
      }
   }
}
